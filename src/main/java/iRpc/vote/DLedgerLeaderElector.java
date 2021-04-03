/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package iRpc.vote;

import com.alibaba.fastjson.JSON;
import iRpc.base.concurrent.ThreadFactoryImpl;
import iRpc.base.messageDeal.MessageSender;
import iRpc.dataBridge.vote.*;
import iRpc.socketAware.RemoteClient;
import iRpc.util.CommonUtil;
import iRpc.util.DLedgerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static iRpc.dataBridge.vote.VoteResponse.ParseResult.WAIT_TO_VOTE_NEXT;

/**
 * @Description:    描述
 * @author: https://github.com/openmessaging/openmessaging-storage-dledger
 * @date:   2020年6月28日  
 * @version V1.0
 */
public class DLedgerLeaderElector {

    private static Logger logger = LoggerFactory.getLogger(DLedgerLeaderElector.class);
    private Random random = new Random();
    private DLedgerConfig dLedgerConfig;//配置参数
    private final MemberState memberState;//节点状态机
    //as a server handler
    //record the last leader state
    private long lastLeaderHeartBeatTime = -1;//上次收到心跳包的时间戳
    /** 心跳发送计时器 */
    private long lastSendHeartBeatTime = -1;//上次发送心跳包的时间戳
    /** 主节点上次心跳检测成功时间 */
    private long lastSuccHeartBeatTime = -1;//上次成功收到心跳包的时间戳
    /** 心跳发送间隔 */
    private int heartBeatTimeIntervalMs = 2000;//一个心跳包的周期，默认为2s
    /**
     * 允许最大的N个心跳周期内未收到心跳包
     */
    private int maxHeartBeatLeak = 3;

    //as a client
    /** 下一次发发起的投票的时间 */
    private long nextTimeToRequestVote = -1;
    /** 是否应该立即发起投票。如果为true，则忽略计时器，该值默认为false */
    private boolean needIncreaseTermImmediately = false;

    /** 最小投票间隔 */
    private int minVoteIntervalMs = 300;
    /** 最大投票间隔 */
    private int maxVoteIntervalMs = 1000;

    /**注册的节点状态变更促发器，通过 addRoleChangeHandler 方法添加**/
    private List<RoleChangeHandler> roleChangeHandlers = new ArrayList<>();

    /** 上一轮投票结果 */
    private VoteResponse.ParseResult lastParseResult = VoteResponse.ParseResult.WAIT_TO_REVOTE;
    
    /**上一次投票的开销**/
    private long lastVoteCost = 0L;

    /**状态机管理器**/
    private StateMaintainer stateMaintainer = new StateMaintainer();

    private ExecutorService executorService = Executors.newFixedThreadPool(4,new ThreadFactoryImpl("dledgerLeaderElector_",false));
    public DLedgerLeaderElector(DLedgerConfig dLedgerConfig, MemberState memberState ) {
        this.dLedgerConfig = dLedgerConfig;
        this.memberState = memberState;
        refreshIntervals(dLedgerConfig);
    }

    /**
     * 启动状态管理机
     */
    public void startup() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                stateMaintainer.start();
            }
        });
        for (RoleChangeHandler roleChangeHandler : roleChangeHandlers) {
            roleChangeHandler.startup();
        }
    }

    public void shutdown() {
        stateMaintainer.shutDown();
        for (RoleChangeHandler roleChangeHandler : roleChangeHandlers) {
            roleChangeHandler.shutdown();
        }
    }

    private void refreshIntervals(DLedgerConfig dLedgerConfig) {
        this.heartBeatTimeIntervalMs = dLedgerConfig.getHeartBeatTimeIntervalMs();
        this.maxHeartBeatLeak = dLedgerConfig.getMaxHeartBeatLeak();
        this.minVoteIntervalMs = dLedgerConfig.getMinVoteIntervalMs();
        this.maxVoteIntervalMs = dLedgerConfig.getMaxVoteIntervalMs();
    }

    private void maintainState() throws Exception {
        if (memberState.isLeader()) {
            maintainAsLeader();
        } else if (memberState.isFollower()) {
            maintainAsFollower();
        } else {
            //默认状态--candidate和follower才能当作voter
            maintainAsCandidate();
        }
    }

    private void maintainAsLeader() throws Exception {
        // 判断上一次发送心跳的时间与当前时间的差值是否大于心跳包发送间隔，如果超过，则说明需要发送心跳包
        if (DLedgerUtils.elapsed(lastSendHeartBeatTime) > heartBeatTimeIntervalMs) {
            long term;
            String leaderId;
            synchronized (memberState) {
                // 如果当前不是leader节点，则直接返回，主要是为了二次判断
                if (!memberState.isLeader()) {
                    //stop sending
                    return;
                }
                term = memberState.currTerm();
                leaderId = memberState.getLeaderId();
                // 重置心跳包发送计时器
                lastSendHeartBeatTime = System.currentTimeMillis();
            }
            sendHeartbeats(term, leaderId);
        }
    }

    private void maintainAsFollower() {
        if (DLedgerUtils.elapsed(lastLeaderHeartBeatTime) > 2 * heartBeatTimeIntervalMs) {
            synchronized (memberState) {
                // maxHeartBeatLeak默认为3，即3个心跳包周期内未收到心跳，则将状态变更为Candidate
                if (memberState.isFollower() && (DLedgerUtils.elapsed(lastLeaderHeartBeatTime) > maxHeartBeatLeak * heartBeatTimeIntervalMs)) {
                    logger.info("[{}][HeartBeatTimeOut] lastLeaderHeartBeatTime: {} heartBeatTimeIntervalMs: {} lastLeader={}", memberState.getSelfId(), new Timestamp(lastLeaderHeartBeatTime), heartBeatTimeIntervalMs, memberState.getLeaderId());
                    changeRoleToCandidate(memberState.currTerm());
                }
            }
        }
    }

    private void maintainAsCandidate() throws Exception {
        //for candidate
        if (System.currentTimeMillis() < nextTimeToRequestVote && !needIncreaseTermImmediately) {
            return;
        }
        // 投票轮次
        long term;
        // 未使用
        long ledgerEndTerm;
        // 未使用
        long ledgerEndIndex;
        synchronized (memberState) {
            if (!memberState.isCandidate()) {
                return;
            }
            if (lastParseResult == WAIT_TO_VOTE_NEXT || needIncreaseTermImmediately) {
                // 如果上一次的投票结果为“等待下一次投票”或应该“立即开启投票”，则根据当前节点的状态机获取下一轮的投票轮次
                long prevTerm = memberState.currTerm();

                term = memberState.nextTerm();//清空已投过的票
                logger.info("{}_[INCREASE_TERM] from {} to {}", memberState.getSelfId(), prevTerm, term);
                lastParseResult = VoteResponse.ParseResult.WAIT_TO_REVOTE;
            } else {
                // 如果上一次的投票结果不是WAIT_TO_VOTE_NEXT(等待下一轮投票)，则投票轮次依然为状态机内部维护的轮次
                term = memberState.currTerm();
            }
            ledgerEndIndex = memberState.getLedgerEndIndex();
            ledgerEndTerm = memberState.getLedgerEndTerm();
        }
        if (needIncreaseTermImmediately) {
            // 重置该标记位为false，并重新设置下一次投票超时时间
            nextTimeToRequestVote = getNextTimeToRequestVote();
            needIncreaseTermImmediately = false;
            return;
        }

        long startVoteTimeMs = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        /** 向集群内的其他节点发起投票请求，并返回投票结果列表**/
        final List<CompletableFuture<VoteResponse>> quorumVoteResponses = voteForQuorumResponses(term, ledgerEndTerm, ledgerEndIndex);
        final AtomicLong knownMaxTermInGroup = new AtomicLong(-1);
        final AtomicInteger allNum = new AtomicInteger(0);
        final AtomicInteger validNum = new AtomicInteger(0);
        final AtomicInteger acceptedNum = new AtomicInteger(0);
        final AtomicInteger notReadyTermNum = new AtomicInteger(0);
        final AtomicInteger biggerLedgerNum = new AtomicInteger(0);
        final AtomicBoolean alreadyHasLeader = new AtomicBoolean(false);

        CountDownLatch voteLatch = new CountDownLatch(1);
        // 遍历投票结果，收集投票结果
        for (CompletableFuture<VoteResponse> future : quorumVoteResponses) {
            future.whenComplete((VoteResponse x, Throwable ex) -> {
                try {
                    if (ex != null) {
                        logger.error("vote failed ",ex);
                        return;
                    }
                    logger.debug("[{}][投票回调执行] {}", memberState.getSelfId(), JSON.toJSONString(x));
                    if (x.getVoteResult() != VoteResponse.RESULT.UNKNOWN) {
                        /**如果投票结果不是UNKNOW，则有效投票数量增1**/
                        validNum.incrementAndGet();
                    }
                    synchronized (knownMaxTermInGroup) {
                        switch (x.getVoteResult()) {
                            case ACCEPT:
                                //赞成票，acceptedNum加一，只有得到的赞成票超过集群节点数量的一半才能成为Leader
                                acceptedNum.incrementAndGet();
                                break;
                            case REJECT_ALREADY_VOTED:
                                break;
                            case REJECT_ALREADY__HAS_LEADER:
                                alreadyHasLeader.compareAndSet(false, true);
                                break;
                            case REJECT_TERM_SMALL_THAN_LEDGER:
                            case REJECT_EXPIRED_VOTE_TERM:
                                //拒绝票，如果自己维护的term小于远端维护的term，更新自己维护的投票轮次
                                if (x.getTerm() > knownMaxTermInGroup.get()) {
                                    knownMaxTermInGroup.set(x.getTerm());
                                }
                                break;
                            case REJECT_EXPIRED_LEDGER_TERM:
                            case REJECT_SMALL_LEDGER_END_INDEX:
                                biggerLedgerNum.incrementAndGet();
                                break;
                            case REJECT_TERM_NOT_READY:
                                //拒绝票，对端的投票轮次小于自己的team
                                notReadyTermNum.incrementAndGet();
                                break;
                            case MEMBER_ADDED_VOTE_NEXT:
                            	//拒绝票，当前节点为新扩集群节点
                            	break;
                            default:
                                break;
                        }
                    }
                    if (alreadyHasLeader.get()
                            || memberState.isQuorum(acceptedNum.get())
                            || memberState.isQuorum(acceptedNum.get() + notReadyTermNum.get())) {
                        /**
                         * 当以及存在leader 或者半数节点已投票，则可以结束投票统计
                         */
                        voteLatch.countDown();
                    }
                } catch (Throwable t) {
                    logger.error("Get error when parsing vote response ", t);
                } finally {
                    allNum.incrementAndGet();
                    if (allNum.get() == memberState.peerSize()) {
                        voteLatch.countDown();
                    }
                }
            });

        }
        try {
            // 等待收集投票结果，并设置超时时间
            voteLatch.await(3000 + random.nextInt(maxVoteIntervalMs), TimeUnit.MILLISECONDS);
        } catch (Throwable ignore) {
            // ignore
        }

        lastVoteCost = DLedgerUtils.elapsed(startVoteTimeMs);
        VoteResponse.ParseResult parseResult;
        if (knownMaxTermInGroup.get() > term) {
            // 如果对端的投票轮次大于发起投票的节点
            parseResult = WAIT_TO_VOTE_NEXT;
            nextTimeToRequestVote = getNextTimeToRequestVote();
            changeRoleToCandidate(knownMaxTermInGroup.get());
        } else if (alreadyHasLeader.get()) {
            // 如果已经存在Leader，该节点重新进入到Candidate，并重置定时器
            parseResult = WAIT_TO_VOTE_NEXT;
            nextTimeToRequestVote = getNextTimeToRequestVote() + heartBeatTimeIntervalMs * maxHeartBeatLeak;
        } else if (!memberState.isQuorum(validNum.get())) {
            // 如果收到的有效票数未超过半数
            // WAIT_TO_REVOTE，下次投票时不增加投票轮次
            parseResult = VoteResponse.ParseResult.WAIT_TO_REVOTE;
            nextTimeToRequestVote = getNextTimeToRequestVote();
        } else if (memberState.isQuorum(acceptedNum.get())) {
            // 如果得到的赞同票超过半数，则成为Leader
            parseResult = VoteResponse.ParseResult.PASSED;
        } else if (memberState.isQuorum(acceptedNum.get() + notReadyTermNum.get())) {
            // 如果得到的赞成票加上未准备投票的节点数超过半数
            parseResult = VoteResponse.ParseResult.REVOTE_IMMEDIATELY;
        } else if (memberState.isQuorum(acceptedNum.get() + biggerLedgerNum.get())) {
            // 如果得到的赞成票加上对端维护的ledgerEndIndex超过半数
            parseResult = VoteResponse.ParseResult.WAIT_TO_REVOTE;
            nextTimeToRequestVote = getNextTimeToRequestVote();
        } else {
            // 其他情况，开启下一轮投票
            parseResult = WAIT_TO_VOTE_NEXT;
            nextTimeToRequestVote = getNextTimeToRequestVote();
        }
        lastParseResult = parseResult;
        logger.debug("[{}] [投票综合结果] cost={} term={} memberNum={} allNum={} acceptedNum={} notReadyTermNum={} biggerLedgerNum={} alreadyHasLeader={} maxTerm={} result={}",
                memberState.getSelfId(), lastVoteCost, term, memberState.peerSize(), allNum, acceptedNum, notReadyTermNum, biggerLedgerNum, alreadyHasLeader, knownMaxTermInGroup.get(), parseResult);

        if (parseResult == VoteResponse.ParseResult.PASSED) {
            // 如果投票成功，则状态机状态设置为Leader
            logger.debug("[{}] [投票最终结果] has been elected to be the leader in term {}", memberState.getSelfId(), term);
            changeRoleToLeader(term);
        }

    }

    /**
     * Candidate节点如果得到的赞同票超过半数，则成为Leader
     */
    public void changeRoleToLeader(long term) {
        synchronized (memberState) {
            if (memberState.currTerm() == term) {
                memberState.changeToLeader(term);
                lastSendHeartBeatTime = -1;
                handleRoleChange(term, MemberState.Role.LEADER);
                logger.info("[{}] [ChangeRoleToLeader] from term: {} and currTerm: {}", memberState.getSelfId(), term, memberState.currTerm());
            } else {
                logger.warn("[{}] skip to be the leader in term: {}, but currTerm is: {}", memberState.getSelfId(), term, memberState.currTerm());
            }
        }
    }

    /**
     * 每个Follower节点会有一个超时时间(计时器)，其时间设置为150ms~300ms之间的随机值。当计时器到期后，节点状态从Follower变成Candidate
     */
    public void changeRoleToCandidate(long term) {
        synchronized (memberState) {
            if (term >= memberState.currTerm()) {
                memberState.changeToCandidate(term);
                handleRoleChange(term, MemberState.Role.CANDIDATE);
                logger.info("[{}] [ChangeRoleToCandidate] from term: {} and currTerm: {}", memberState.getSelfId(), term, memberState.currTerm());
            } else {
                logger.info("[{}] skip to be candidate in term: {}, but currTerm: {}", memberState.getSelfId(), term, memberState.currTerm());
            }
        }
    }

    public void changeRoleToFollower(long term, String leaderId) {
        logger.info("[{}][ChangeRoleToFollower] from term: {} leaderId: {} and currTerm: {}", memberState.getSelfId(), term, leaderId, memberState.currTerm());
        memberState.changeToFollower(term, leaderId);
        lastLeaderHeartBeatTime = System.currentTimeMillis();
        handleRoleChange(term, MemberState.Role.FOLLOWER);
    }

    /**
     * 获取下一次投票时间
     */
    private long getNextTimeToRequestVote() {
        // 当前时间戳 + 上次投票的开销 + 最小投票间隔(300ms) + （1000 - 300）之间的随机值
        return System.currentTimeMillis() + lastVoteCost + minVoteIntervalMs + random.nextInt(maxVoteIntervalMs - minVoteIntervalMs);
    }

    private void sendHeartbeats(long term, String leaderId) throws Exception {
        final AtomicInteger allNum = new AtomicInteger(1);
        final AtomicInteger succNum = new AtomicInteger(1);
        final AtomicInteger notReadyNum = new AtomicInteger(0);
        final AtomicLong maxTerm = new AtomicLong(-1);
        final AtomicBoolean inconsistLeader = new AtomicBoolean(false);//标识从节点已经有了其他
        final CountDownLatch beatLatch = new CountDownLatch(1);
        long startHeartbeatTimeMs = System.currentTimeMillis();
        for (String id : memberState.getPeerMap().keySet()) {
            if (memberState.getSelfId().equals(id)) {
                continue;
            }
            HeartBeatRequest heartBeatRequest = new HeartBeatRequest();
            heartBeatRequest.setRequestNum(String.valueOf(CommonUtil.getSeq()));
            heartBeatRequest.setGroup(memberState.getGroup());
            heartBeatRequest.setLocalId(memberState.getSelfId());
            heartBeatRequest.setRemoteId(id);
            heartBeatRequest.setLeaderId(leaderId);
            heartBeatRequest.setTerm(term);
            CompletableFuture<HeartBeatResponse> future = MessageSender.heartBeat(heartBeatRequest, memberState.getPeerMap().get(id));
            future.whenComplete((HeartBeatResponse x, Throwable ex) -> {
                // 统计心跳包发送响应结果
                try {
                    if (ex != null) {
                        throw ex;
                    }
                    switch (DLedgerResponseCode.valueOf(x.getCode())) {
                        case SUCCESS:
                            succNum.incrementAndGet();
                            break;
                        case EXPIRED_TERM:
                            // 主节点的投票Term小于从节点的投票轮次
                            maxTerm.set(x.getTerm());
                            break;
                        case INCONSISTENT_LEADER:
                            // 从节点已经有了新的主节点
                            inconsistLeader.compareAndSet(false, true);
                            break;
                        case TERM_NOT_READY:
                            // 从节点未准备好
                            notReadyNum.incrementAndGet();
                            break;
                        default:
                            break;
                    }
                    if (memberState.isQuorum(succNum.get())
                            || memberState.isQuorum(succNum.get() + notReadyNum.get())) {
                        beatLatch.countDown();
                    }
                } catch (Throwable t) {
                    logger.error("Parse heartbeat response failed", t);
                } finally {
                    allNum.incrementAndGet();
                    if (allNum.get() == memberState.peerSize()) {
                        beatLatch.countDown();
                    }
                }
            });
        }
        beatLatch.await(heartBeatTimeIntervalMs, TimeUnit.MILLISECONDS);
        if (memberState.isQuorum(succNum.get())) {
            // 如果成功的票数大于进群内的半数，则表示集群状态正常，正常按照心跳包间隔发送心跳包
            lastSuccHeartBeatTime = System.currentTimeMillis();
        } else {
            logger.debug("[{}] Parse heartbeat responses in cost={} term={} allNum={} succNum={} notReadyNum={} inconsistLeader={} maxTerm={} peerSize={} lastSuccHeartBeatTime={}",
                    memberState.getSelfId(), DLedgerUtils.elapsed(startHeartbeatTimeMs), term, allNum.get(), succNum.get(), notReadyNum.get(), inconsistLeader.get(), maxTerm.get(), memberState.peerSize(), new Timestamp(lastSuccHeartBeatTime));
            if (memberState.isQuorum(succNum.get() + notReadyNum.get())) {
                // 如果成功的票数加上未准备的投票的节点数量超过集群内的半数，则立即发送心跳包
                lastSendHeartBeatTime = -1;
            } else if (maxTerm.get() > term) {
                // 如果从节点的投票轮次比主节点的大，则使用从节点的投票轮次，节点状态从Leader转换为Candidate
                changeRoleToCandidate(maxTerm.get());
            } else if (inconsistLeader.get()) {
                // 如果从节点已经有了另外的主节点，节点状态从Leader转换为Candidate
                changeRoleToCandidate(term);
            } else if (DLedgerUtils.elapsed(lastSuccHeartBeatTime) > maxHeartBeatLeak * heartBeatTimeIntervalMs) {
                // 最后心跳成功时间大于允许的投票失败时间，节点状态从Leader转换为Candidate
                changeRoleToCandidate(term);
            }
        }
    }

    /**
     * 处理当前节点接收到的心跳信息
     * @param request
     * @return
     * @throws Exception
     */
    public CompletableFuture<HeartBeatResponse> handleHeartBeat(HeartBeatRequest request) throws Exception {

        if (!memberState.isPeerMember(request.getLeaderId())) {
    		logger.warn("[BUG] [HandleHeartBeat] remoteId={} is an unknown member", request.getLeaderId());
    		return CompletableFuture.completedFuture(new HeartBeatResponse(request).term(memberState.currTerm()).code(DLedgerResponseCode.UNKNOWN_MEMBER.getCode()));
        }

        /**
         * 如果当前集群中的leader的id等于了接收到当前心跳信息的节点id时（leader节点接收到了从节点发送的心跳信息）
         */
        if (memberState.getSelfId().equals(request.getLeaderId())) {
            logger.warn("[BUG] [HandleHeartBeat] selfId={} but remoteId={}", memberState.getSelfId(), request.getLeaderId());
            return CompletableFuture.completedFuture(new HeartBeatResponse(request).term(memberState.currTerm()).code(DLedgerResponseCode.UNEXPECTED_MEMBER.getCode()));
        }
        /**
         *
         */
        if (request.getTerm() < memberState.currTerm()) {
            // 如果主节点的Term小于从节点的Term，发送反馈给主节点，告知主节点的Term已过时
            return CompletableFuture.completedFuture(new HeartBeatResponse(request).term(memberState.currTerm()).code(DLedgerResponseCode.EXPIRED_TERM.getCode()));
        } else if (request.getTerm() == memberState.currTerm()) {
            if (request.getLeaderId().equals(memberState.getLeaderId())) {
                // ****如果投票轮次相同，并且发送心跳包的节点是该节点的主节点，则返回成功****
                lastLeaderHeartBeatTime = System.currentTimeMillis();
                return CompletableFuture.completedFuture(new HeartBeatResponse(request));
            }
        }

        //异常情况
        //hold the lock to get the latest term and leaderId
        synchronized (memberState) {
            if (request.getTerm() < memberState.currTerm()) {
                // 如果主节的投票轮次小于当前投票轮次，则返回主节点投票轮次过期
                return CompletableFuture.completedFuture(new HeartBeatResponse(request).term(memberState.currTerm()).code(DLedgerResponseCode.EXPIRED_TERM.getCode()));
            } else if (request.getTerm() == memberState.currTerm()) {
                /**
                 * 当接收到的leader心跳数据中选举轮次与当前节点轮次值一致时
                 */
                if (memberState.getLeaderId() == null) {
                    // 如果当前节点的主节点字段为空，则使用主节点的ID，并返回成功
                    changeRoleToFollower(request.getTerm(), request.getLeaderId());
                    return CompletableFuture.completedFuture(new HeartBeatResponse(request));
                } else if (request.getLeaderId().equals(memberState.getLeaderId())) {
                    // 如果当前节点的主节点就是发送心跳包的节点，则更新上一次收到心跳包的时间戳，并返回成功
                    lastLeaderHeartBeatTime = System.currentTimeMillis();
                    return CompletableFuture.completedFuture(new HeartBeatResponse(request));
                } else {
                    // 如果发生，则返回已存在-主节点，标记该心跳包处理结束
                    logger.error("[{}][BUG] currTerm {} has leader {}, but received leader {}", memberState.getSelfId(), memberState.currTerm(), memberState.getLeaderId(), request.getLeaderId());
                    return CompletableFuture.completedFuture(new HeartBeatResponse(request).code(DLedgerResponseCode.INCONSISTENT_LEADER.getCode()));
                }
            } else {
                // 如果主节点的投票轮次大于从节点的投票轮次，则认为从节点没有准备好，则从节点进入Candidate状态，并立即发起一次投票。
                changeRoleToCandidate(request.getTerm());
                needIncreaseTermImmediately = true;
                //TOOD notify
                return CompletableFuture.completedFuture(new HeartBeatResponse(request).code(DLedgerResponseCode.TERM_NOT_READY.getCode()));
            }
        }
    }

    /**
     * 向其他节点发起投票
     * @param term
     * @param ledgerEndTerm
     * @param ledgerEndIndex
     * @return
     * @throws Exception
     */
    private List<CompletableFuture<VoteResponse>> voteForQuorumResponses(long term, long ledgerEndTerm,
        long ledgerEndIndex) throws Exception {
        List<CompletableFuture<VoteResponse>> responses = new ArrayList<>();
        for (String id : memberState.getPeerMap().keySet()) {
            VoteRequest voteRequest = new VoteRequest();
            voteRequest.setRequestNum(String.valueOf(CommonUtil.getSeq()));//发送序号
            voteRequest.setGroup(memberState.getGroup());
            voteRequest.setLedgerEndIndex(ledgerEndIndex);//未使用，默认值
            voteRequest.setLedgerEndTerm(ledgerEndTerm);//未使用，默认值
            voteRequest.setLeaderId(memberState.getSelfId());
            voteRequest.setTerm(term);//发起投票的节点当前的投票轮次
            voteRequest.setRemoteId(id);
            voteRequest.setLocalIP(memberState.getSelfAddr().split("\\:")[0]);
            voteRequest.setLocalPort(memberState.getSelfAddr().split("\\:")[1]);
            CompletableFuture<VoteResponse> voteResponse = null;
            if (memberState.getSelfId().equals(id)) {
                voteResponse = handleVote(voteRequest, true);
            } else {
                voteResponse = MessageSender.vote(voteRequest, memberState.getPeerMap().get(id));
            }
            if (voteResponse != null){
                responses.add(voteResponse);
            }

        }
        return responses;
    }
    /**
     * 处理接收到的投票请求
     * @param request
     * @param self
     * @return
     */
    public CompletableFuture<VoteResponse> handleVote(VoteRequest request, boolean self) {
        //hold the lock to get the latest term, leaderId, ledgerEndIndex
        synchronized (memberState) {
            if (!memberState.isPeerMember(request.getLeaderId())) {
            	if(!memberState.isSameGroup(request.getGroup()) 
            			|| memberState.getPeerMap().containsKey(request.getLeaderId())
            					|| request.getLocalIP() == null
            						|| request.getLocalPort() == null/*nodeID is exist in peers*/){
            		logger.warn("[BUG] [HandleVote] remoteId={} is an unknown member", request.getLeaderId());
                    return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.REJECT_UNKNOWN_LEADER));
            	}else{
            		//n0-localhost:10916
            		memberState.getPeerMap().put(request.getLeaderId(), String.format("%s:%s",request.getLocalIP(),request.getLocalPort()));
            		String newPeers = this.dLedgerConfig.getPeers()+
            				String.format(";%s-%s:%s", request.getLeaderId(),request.getLocalIP(),request.getLocalPort());
            		this.dLedgerConfig.setPeers(newPeers);
            		startClusterInnerClient(request.getLocalIP(),request.getLocalPort());
            		if (memberState.getLeaderId() != null) {
            			//use the old leader 
                        return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.REJECT_ALREADY__HAS_LEADER));
                    }
            		// vote new leader
            		return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.MEMBER_ADDED_VOTE_NEXT));
            	}
                
            }
            if (!self && memberState.getSelfId().equals(request.getLeaderId())) {
                logger.warn("[BUG] [HandleVote] selfId={} but remoteId={}", memberState.getSelfId(), request.getLeaderId());
                return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.REJECT_UNEXPECTED_LEADER));
            }

            if (request.getTerm() < memberState.currTerm()) {
                return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.REJECT_EXPIRED_VOTE_TERM));
            } else if (request.getTerm() == memberState.currTerm()) {
                // 如果两者的Term相等，说明两者都处在同一个投票轮次中，地位平等
                if (memberState.currVoteFor() == null) {
                    //let it go
                } else if (memberState.currVoteFor().equals(request.getLeaderId())) {
                    // 已经投票给当前节点
                } else {
                    if (memberState.getLeaderId() != null) {
                        // 如果该节点已存在的Leader节点，则拒绝并告知已存在Leader节点
                        return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.REJECT_ALREADY__HAS_LEADER));
                    } else {
                        // 如果该节点还未有Leader节点，但已经投了其他节点的票，则拒绝请求节点，并告知已投票
                        return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.REJECT_ALREADY_VOTED));
                    }
                }
            } else {
                // 如果发起投票节点的Term大于当前节点的Term。拒绝请求节点的投票请求，并告知自身还未准备投票，自身会使用请求节点的投票轮次立即进入到Candidate状态
                //stepped down by larger term
                changeRoleToCandidate(request.getTerm());
                needIncreaseTermImmediately = true;
                //only can handleVote when the term is consistent
                return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.REJECT_TERM_NOT_READY));
            }

            memberState.setCurrVoteFor(request.getLeaderId());
            return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.ACCEPT));
        }
    }
    /**
     * connect to the cluster node
     * @param ip
     * @param port
     */
    public void startClusterInnerClient(String ip , String port){
    	iRpc.base.concurrent.ClusterExecutors.executorService.execute(new Runnable() {
            @Override
            public void run() {
                new RemoteClient().start(ip,Integer.parseInt(port),String.format("%s:%s",ip,port));

            }
        });
    }

    /**
     * 节点状态机类 
     * All rights Reserved, Designed By www.uiotp.com
     * @Description:    描述   
     * @author: yangcheng     
     * @date:   2020年6月28日  
     * @version V1.0
     */
    public class StateMaintainer {

        private volatile boolean canbeRun = true;

        public boolean start(){
            canbeRun = true;
            doWork();
            return true;
        }
        public boolean shutDown(){
            canbeRun = false;
            return true;
        }
        /**
         * 会一直循环执行
         */
        public void doWork() {
            for (;canbeRun;){
                try {
                    //如果该节点参与Leader选举
                    if (DLedgerLeaderElector.this.dLedgerConfig.isEnableLeaderElector()) {
                        DLedgerLeaderElector.this.refreshIntervals(dLedgerConfig);//重置定时器
                        DLedgerLeaderElector.this.maintainState();//驱动状态机
                    }
                    Thread.sleep(10);//执行一次选主，休息10ms
                } catch (Throwable t) {
                    DLedgerLeaderElector.logger.error("Error in heartbeat", t);
                }
            }
        }
    }

    /** 无用代码 */
    private void handleRoleChange(long term, MemberState.Role role) {
        for (RoleChangeHandler roleChangeHandler : roleChangeHandlers) {
            try {
                roleChangeHandler.handle(term, role);
            } catch (Throwable t) {
                logger.warn("Handle role change failed term={} role={} handler={}", term, role, roleChangeHandler.getClass(), t);
            }
        }
    }

    /**
     * 增加状态变化监听器
     * @param roleChangeHandler
     */
    public void addRoleChangeHandler(RoleChangeHandler roleChangeHandler) {
        if (!roleChangeHandlers.contains(roleChangeHandler)) {
            roleChangeHandlers.add(roleChangeHandler);
        }
    }

    public interface RoleChangeHandler {
        void handle(long term, MemberState.Role role);

        void startup();

        void shutdown();
    }

}
