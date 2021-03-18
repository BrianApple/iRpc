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
import iRpc.base.messageDeal.MessageSender;
import iRpc.dataBridge.vote.*;
import iRpc.util.DLedgerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Description:    描述
 * @author: https://github.com/openmessaging/openmessaging-storage-dledger
 * @date:   2020年6月28日  
 * @version V1.0
 */
public class DLedgerLeaderElector {

    private static Logger logger = LoggerFactory.getLogger(DLedgerLeaderElector.class);
    /**
     * 随机数生成器，对应raft协议中选举超时时间是一随机数
     */
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

    public DLedgerLeaderElector(DLedgerConfig dLedgerConfig, MemberState memberState ) {
        this.dLedgerConfig = dLedgerConfig;
        this.memberState = memberState;
        refreshIntervals(dLedgerConfig);
    }

    /**
     * 启动状态管理机
     */
    public void startup() {
        stateMaintainer.start();
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

    /**
     * The core method of maintainer.
     * 根据当前角色运行指定的逻辑:
     *  candidate => propose a vote.
     *  leader => send heartbeats to followers, and step down to candidate when quorum followers do not respond.
     *  follower => accept heartbeats, and change to candidate when no heartbeat from leader.
     *
     *  该方法会循环执行
     */
    private void maintainState() throws Exception {
        if (memberState.isLeader()) {
            maintainAsLeader();
        } else if (memberState.isFollower()) {
            maintainAsFollower();
        } else {
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
            // master节点向集群内的所有节点发送心跳包
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
        // Leader节点当前的投票轮次
        long ledgerEndTerm;
        // 当前日志的最大序列，即下一条日志的开始index
        long ledgerEndIndex;
        synchronized (memberState) {
            if (!memberState.isCandidate()) {
                return;
            }
            if (lastParseResult == VoteResponse.ParseResult.WAIT_TO_VOTE_NEXT || needIncreaseTermImmediately) {
                // 如果上一次的投票结果为“等待下一次投票”或应该“立即开启投票”，则根据当前节点的状态机获取下一轮的投票轮次
                long prevTerm = memberState.currTerm();
                term = memberState.nextTerm();
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
        // 已知的最大投票轮次
        final AtomicLong knownMaxTermInGroup = new AtomicLong(-1);
        // 所有投票票数
        final AtomicInteger allNum = new AtomicInteger(0);
        // 有效投票数
        final AtomicInteger validNum = new AtomicInteger(0);
        // 获得的投票数
        final AtomicInteger acceptedNum = new AtomicInteger(0);
        // 未准备投票的节点数量，如果对端节点的投票轮次小于发起投票的轮次，则认为对端未准备好，对端节点使用本次的轮次进入Candidate状态
        final AtomicInteger notReadyTermNum = new AtomicInteger(0);
        // 发起投票的节点的ledgerEndTerm小于对端节点的个数
        final AtomicInteger biggerLedgerNum = new AtomicInteger(0);
        // 是否已经存在Leader
        final AtomicBoolean alreadyHasLeader = new AtomicBoolean(false);

        CountDownLatch voteLatch = new CountDownLatch(1);
        // 遍历投票结果，收集投票结果
        for (CompletableFuture<VoteResponse> future : quorumVoteResponses) {
            future.whenComplete((VoteResponse x, Throwable ex) -> {
                try {
                    if (ex != null) {
                        throw ex;
                    }
                    logger.info("[{}][GetVoteResponse] {}", memberState.getSelfId(), JSON.toJSONString(x));
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
                                //拒绝票，如果自己维护的term小于远端维护的ledgerEndTerm，则返回该结果，如果对端的team大于自己的team，需要记录对端最大的投票轮次，以便更新自己的投票轮次。
                            case REJECT_EXPIRED_VOTE_TERM:
                                //拒绝票，如果自己维护的term小于远端维护的term，更新自己维护的投票轮次
                                if (x.getTerm() > knownMaxTermInGroup.get()) {
                                    knownMaxTermInGroup.set(x.getTerm());
                                }
                                break;
                            case REJECT_EXPIRED_LEDGER_TERM:
                                //拒绝票，如果自己维护的 ledgerTerm小于对端维护的ledgerTerm，则返回该结果。如果是此种情况，增加计数器- biggerLedgerNum的值。
                            case REJECT_SMALL_LEDGER_END_INDEX:
                                //拒绝票，如果对端的ledgerTeam与自己维护的ledgerTeam相等，但是自己维护的dedgerEndIndex小于对端维护的值，返回该值，增加biggerLedgerNum计数器的值。
                                biggerLedgerNum.incrementAndGet();
                                break;
                            case REJECT_TERM_NOT_READY:
                                //拒绝票，对端的投票轮次小于自己的team，则认为对端还未准备好投票，对端使用自己的投票轮次，使自己进入到Candidate状态。
                                notReadyTermNum.incrementAndGet();
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

        // 根据收集的投票结果判断是否能成为Leader
        lastVoteCost = DLedgerUtils.elapsed(startVoteTimeMs);
        VoteResponse.ParseResult parseResult;
        if (knownMaxTermInGroup.get() > term) {
            // 如果对端的投票轮次大于发起投票的节点，则该节点使用对端的轮次，重新进入到Candidate状态，并且重置投票计时器，其值为"1个常规计时器"
            parseResult = VoteResponse.ParseResult.WAIT_TO_VOTE_NEXT;
            nextTimeToRequestVote = getNextTimeToRequestVote();
            changeRoleToCandidate(knownMaxTermInGroup.get());
        } else if (alreadyHasLeader.get()) {
            // 如果已经存在Leader，该节点重新进入到Candidate，并重置定时器
            parseResult = VoteResponse.ParseResult.WAIT_TO_VOTE_NEXT;
            nextTimeToRequestVote = getNextTimeToRequestVote() + heartBeatTimeIntervalMs * maxHeartBeatLeak;
        } else if (!memberState.isQuorum(validNum.get())) {
            // 如果收到的有效票数未超过半数，则重置计时器为"1个常规计时器"，然后等待重新投票
            // 注意状态为WAIT_TO_REVOTE，该状态下的特征是下次投票时不增加投票轮次
            parseResult = VoteResponse.ParseResult.WAIT_TO_REVOTE;
            nextTimeToRequestVote = getNextTimeToRequestVote();
        } else if (memberState.isQuorum(acceptedNum.get())) {
            // 如果得到的赞同票超过半数，则成为Leader
            parseResult = VoteResponse.ParseResult.PASSED;
        } else if (memberState.isQuorum(acceptedNum.get() + notReadyTermNum.get())) {
            // 如果得到的赞成票加上未准备投票的节点数超过半数，则应该立即发起投票，故其结果为REVOTE_IMMEDIATELY
            parseResult = VoteResponse.ParseResult.REVOTE_IMMEDIATELY;
        } else if (memberState.isQuorum(acceptedNum.get() + biggerLedgerNum.get())) {
            // 如果得到的赞成票加上对端维护的ledgerEndIndex超过半数，则重置计时器，继续本轮次的选举
            parseResult = VoteResponse.ParseResult.WAIT_TO_REVOTE;
            nextTimeToRequestVote = getNextTimeToRequestVote();
        } else {
            // 其他情况，开启下一轮投票
            parseResult = VoteResponse.ParseResult.WAIT_TO_VOTE_NEXT;
            nextTimeToRequestVote = getNextTimeToRequestVote();
        }
        lastParseResult = parseResult;
        logger.info("[{}] [PARSE_VOTE_RESULT] cost={} term={} memberNum={} allNum={} acceptedNum={} notReadyTermNum={} biggerLedgerNum={} alreadyHasLeader={} maxTerm={} result={}",
                memberState.getSelfId(), lastVoteCost, term, memberState.peerSize(), allNum, acceptedNum, notReadyTermNum, biggerLedgerNum, alreadyHasLeader, knownMaxTermInGroup.get(), parseResult);

        if (parseResult == VoteResponse.ParseResult.PASSED) {
            // 如果投票成功，则状态机状态设置为Leader
            logger.info("[{}] [VOTE_RESULT] has been elected to be the leader in term {}", memberState.getSelfId(), term);
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
        // 遍历集群中的节点，异步发送心跳包
        for (String id : memberState.getPeerMap().keySet()) {
            if (memberState.getSelfId().equals(id)) {
                continue;
            }
            HeartBeatRequest heartBeatRequest = new HeartBeatRequest();
            heartBeatRequest.setGroup(memberState.getGroup());
            heartBeatRequest.setLocalId(memberState.getSelfId());
            heartBeatRequest.setRemoteId(id);
            heartBeatRequest.setLeaderId(leaderId);
            heartBeatRequest.setTerm(term);
            CompletableFuture<HeartBeatResponse> future = MessageSender.heartBeat(heartBeatRequest);
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
            logger.info("[{}] Parse heartbeat responses in cost={} term={} allNum={} succNum={} notReadyNum={} inconsistLeader={} maxTerm={} peerSize={} lastSuccHeartBeatTime={}",
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

        /**
         * 首先判断当前节点是否在集群节点集合之中，
         */
        if (!memberState.isPeerMember(request.getLeaderId())) {
            logger.warn("[BUG] [HandleHeartBeat] remoteId={} is an unknown member", request.getLeaderId());
            return CompletableFuture.completedFuture(new HeartBeatResponse().term(memberState.currTerm()).code(DLedgerResponseCode.UNKNOWN_MEMBER.getCode()));
        }

        /**
         * 如果当前集群中的leader的id等于了接收到当前心跳信息的节点id时（leader节点接收到了从节点发送的心跳信息）
         */
        if (memberState.getSelfId().equals(request.getLeaderId())) {
            logger.warn("[BUG] [HandleHeartBeat] selfId={} but remoteId={}", memberState.getSelfId(), request.getLeaderId());
            return CompletableFuture.completedFuture(new HeartBeatResponse().term(memberState.currTerm()).code(DLedgerResponseCode.UNEXPECTED_MEMBER.getCode()));
        }
        /**
         *
         */
        if (request.getTerm() < memberState.currTerm()) {
            // 如果主节点的Term小于从节点的Term，发送反馈给主节点，告知主节点的Term已过时
            return CompletableFuture.completedFuture(new HeartBeatResponse().term(memberState.currTerm()).code(DLedgerResponseCode.EXPIRED_TERM.getCode()));
        } else if (request.getTerm() == memberState.currTerm()) {
            if (request.getLeaderId().equals(memberState.getLeaderId())) {
                // ****如果投票轮次相同，并且发送心跳包的节点是该节点的主节点，则返回成功****
                lastLeaderHeartBeatTime = System.currentTimeMillis();
                return CompletableFuture.completedFuture(new HeartBeatResponse());
            }
        }

        //异常情况
        //hold the lock to get the latest term and leaderId
        // 加锁来处理（这里更多的是从节点第一次收到主节点的心跳包）
        synchronized (memberState) {
            if (request.getTerm() < memberState.currTerm()) {
                // 如果主节的投票轮次小于当前投票轮次，则返回主节点投票轮次过期
                return CompletableFuture.completedFuture(new HeartBeatResponse().term(memberState.currTerm()).code(DLedgerResponseCode.EXPIRED_TERM.getCode()));
            } else if (request.getTerm() == memberState.currTerm()) {
                /**
                 * 当接收到的leader心跳数据中选举轮次与当前节点轮次值一致时
                 */
                if (memberState.getLeaderId() == null) {
                    // 如果当前节点的主节点字段为空，则使用主节点的ID，并返回成功
                    changeRoleToFollower(request.getTerm(), request.getLeaderId());
                    return CompletableFuture.completedFuture(new HeartBeatResponse());
                } else if (request.getLeaderId().equals(memberState.getLeaderId())) {
                    // 如果当前节点的主节点就是发送心跳包的节点，则更新上一次收到心跳包的时间戳，并返回成功
                    lastLeaderHeartBeatTime = System.currentTimeMillis();
                    return CompletableFuture.completedFuture(new HeartBeatResponse());
                } else {
                    //this should not happen, but if happened
                    // 如果从节点的主节点与发送心跳包的节点ID不同，说明有另外一个Leader，按道理来说是不会发生的
                    // 如果发生，则返回已存在-主节点，标记该心跳包处理结束
                    logger.error("[{}][BUG] currTerm {} has leader {}, but received leader {}", memberState.getSelfId(), memberState.currTerm(), memberState.getLeaderId(), request.getLeaderId());
                    return CompletableFuture.completedFuture(new HeartBeatResponse().code(DLedgerResponseCode.INCONSISTENT_LEADER.getCode()));
                }
            } else {
                //To make it simple, for larger term, do not change to follower immediately
                //first change to candidate, and notify the state-maintainer thread
                // 如果主节点的投票轮次大于从节点的投票轮次，则认为从节点没有准备好，则从节点进入Candidate状态，并立即发起一次投票。
                changeRoleToCandidate(request.getTerm());
                needIncreaseTermImmediately = true;
                //TOOD notify
                return CompletableFuture.completedFuture(new HeartBeatResponse().code(DLedgerResponseCode.TERM_NOT_READY.getCode()));
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
            voteRequest.setGroup(memberState.getGroup());
            voteRequest.setLedgerEndIndex(ledgerEndIndex);//发起投票节点维护的已知的最大日志条目索引。
            voteRequest.setLedgerEndTerm(ledgerEndTerm);//发起投票节点维护的已知的最大投票轮次。
            voteRequest.setLeaderId(memberState.getSelfId());
            voteRequest.setTerm(term);//发起投票的节点当前的投票轮次
            voteRequest.setRemoteId(id);
            CompletableFuture<VoteResponse> voteResponse;
            if (memberState.getSelfId().equals(id)) {
                /**
                 * 自己投自己一票
                 */
                voteResponse = handleVote(voteRequest, true);
            } else {
                //async
                voteResponse = MessageSender.vote(voteRequest);
            }
            responses.add(voteResponse);

        }
        return responses;
    }
    /**
     * 处理接受到的投票请求
     * @param request
     * @param self
     * @return
     */
    public CompletableFuture<VoteResponse> handleVote(VoteRequest request, boolean self) {
        //hold the lock to get the latest term, leaderId, ledgerEndIndex
        synchronized (memberState) {
            // 为了逻辑的完整性对其请求进行检验，除非有BUG存在，否则是不会出现
            if (!memberState.isPeerMember(request.getLeaderId())) {
                logger.warn("[BUG] [HandleVote] remoteId={} is an unknown member", request.getLeaderId());
                return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.REJECT_UNKNOWN_LEADER));
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

            // 判断请求节点的ledgerEndTerm与当前节点的ledgerEndTerm，这里主要是为了判断日志的复制进度
            // assert acceptedTerm is true
            if (request.getLedgerEndTerm() < memberState.getLedgerEndTerm()) {
                // 如果请求节点的ledgerEndTerm小于当前节点的ledgerEndTerm则拒绝，其原因是请求节点的日志复制进度比当前节点低，这种情况是不能成为主节点的
                return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.REJECT_EXPIRED_LEDGER_TERM));
            } else if (request.getLedgerEndTerm() == memberState.getLedgerEndTerm() && request.getLedgerEndIndex() < memberState.getLedgerEndIndex()) {
                // 如果ledgerEndTerm相等，但是ledgerEndIndex比当前节点小，则拒绝，原因同上
                return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.REJECT_SMALL_LEDGER_END_INDEX));
            }

            if (request.getTerm() < memberState.getLedgerEndTerm()) {
                // 如果请求的Term小于ledgerEndTerm，则拒绝
                return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.getLedgerEndTerm()).voteResult(VoteResponse.RESULT.REJECT_TERM_SMALL_THAN_LEDGER));
            }

            memberState.setCurrVoteFor(request.getLeaderId());
            return CompletableFuture.completedFuture(new VoteResponse(request).term(memberState.currTerm()).voteResult(VoteResponse.RESULT.ACCEPT));
        }
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
