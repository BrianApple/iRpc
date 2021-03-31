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

import iRpc.dataBridge.vote.DLedgerResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 节点状态机
 * @author https://github.com/openmessaging/openmessaging-storage-dledger
 */
public class MemberState {

    public static final String TERM_PERSIST_FILE = "currterm";
    public static final String TERM_PERSIST_KEY_TERM = "currTerm";
    public static final String TERM_PERSIST_KEY_VOTE_FOR = "voteLeader";

    public static Logger logger = LoggerFactory.getLogger(MemberState.class);

    public final DLedgerConfig dLedgerConfig;
    private final ReentrantLock defaultLock = new ReentrantLock();
    private final String group;
    private final String selfId;
    private final String peers;
    
    /**节点状态默认为CANDIDATE**/
    private Role role = Role.CANDIDATE;//
    private String leaderId;
    /** 当前投票轮次 */
    private long currTerm = -1;
    /** 当前投票轮次中已经投票给某个节点，
     * 为非空时标识本轮投票已经投过
     *
     * nextTerm()方法会清空该值
     */
    private String currVoteFor;
    /** 当前日志的最大序列，即下一条日志的开始Index */
    private long ledgerEndIndex = -1;
    /** 每次写入日志后被赋值为currTerm */
    private long ledgerEndTerm = -1;
    /**当前分组中最大的term值**/
    private long knownMaxTermInGroup = -1;
    /** 所有节点及其地址 eq:key= n0  value = "localhost:10916" */
    private Map<String, String> peerMap = new HashMap<>();

    public MemberState(DLedgerConfig config) {
        this.group = config.getGroup();
        this.selfId = config.getSelfId();
        this.peers = config.getPeers();
        for (String peerInfo : this.peers.split(";")) {
            peerMap.put(peerInfo.split("-")[0], peerInfo.split("-")[1]);
        }
        this.dLedgerConfig = config;
//        loadTerm();
    }

    public long currTerm() {
        return currTerm;
    }

    public String currVoteFor() {
        return currVoteFor;
    }

    public synchronized void setCurrVoteFor(String currVoteFor) {
        this.currVoteFor = currVoteFor;
//        persistTerm();
    }

    public synchronized long nextTerm() {
        PreConditions.check(role == Role.CANDIDATE, DLedgerResponseCode.ILLEGAL_MEMBER_STATE, "%s != %s", role, Role.CANDIDATE);
        if (knownMaxTermInGroup > currTerm) {
            currTerm = knownMaxTermInGroup;
        } else {
            ++currTerm;
        }
        currVoteFor = null;
//        persistTerm();
        return currTerm;
    }

    public synchronized void changeToLeader(long term) {
        PreConditions.check(currTerm == term, DLedgerResponseCode.ILLEGAL_MEMBER_STATE, "%d != %d", currTerm, term);
        this.role = Role.LEADER;
        this.leaderId = selfId;
    }

    public synchronized void changeToFollower(long term, String leaderId) {
        PreConditions.check(currTerm == term, DLedgerResponseCode.ILLEGAL_MEMBER_STATE, "%d != %d", currTerm, term);
        this.role = Role.FOLLOWER;
        this.leaderId = leaderId;
    }

    public synchronized void changeToCandidate(long term) {
        assert term >= currTerm;
        PreConditions.check(term >= currTerm, DLedgerResponseCode.ILLEGAL_MEMBER_STATE, "should %d >= %d", term, currTerm);
        if (term > knownMaxTermInGroup) {
            knownMaxTermInGroup = term;
        }
        //the currTerm should be promoted in handleVote thread
        this.role = Role.CANDIDATE;
        this.leaderId = null;
    }

    //////////////////////////////// get set方法 /////////////////////////////////////

    public String getSelfId() {
        return selfId;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public String getGroup() {
        return group;
    }

    public String getSelfAddr() {
        return peerMap.get(selfId);
    }

    public String getLeaderAddr() {
        return peerMap.get(leaderId);
    }

    public String getPeerAddr(String peerId) {
        return peerMap.get(peerId);
    }

    public boolean isLeader() {
        return role == Role.LEADER;
    }

    public boolean isFollower() {
        return role == Role.FOLLOWER;
    }

    public boolean isCandidate() {
        return role == Role.CANDIDATE;
    }

    /**
     * 判断参数值是否大于节点数的一半值
     * @param num
     * @return
     */
    public boolean isQuorum(int num) {
        return num >= ((peerSize() / 2) + 1);
    }

    public int peerSize() {
        return peerMap.size();
    }

    public boolean isPeerMember(String id) {
        return id != null && peerMap.containsKey(id);
    }

    public Map<String, String> getPeerMap() {
        return peerMap;
    }

    //just for test
    public void setCurrTermForTest(long term) {
        PreConditions.check(term >= currTerm, DLedgerResponseCode.ILLEGAL_MEMBER_STATE);
        this.currTerm = term;
    }

    public Role getRole() {
        return role;
    }

    public ReentrantLock getDefaultLock() {
        return defaultLock;
    }

    public void updateLedgerIndexAndTerm(long index, long term) {
        this.ledgerEndIndex = index;
        this.ledgerEndTerm = term;
    }

    public long getLedgerEndIndex() {
        return ledgerEndIndex;
    }

    public long getLedgerEndTerm() {
        return ledgerEndTerm;
    }
    /**
     * 节点状态：raft只有：leader、follower、candidate
     * All rights Reserved, Designed By www.uiotcp.com
     * @Description:    描述   
     * @author: yangcheng     
     * @date:   2020年6月28日  
     * @version V1.0
     */
    public enum Role {
        UNKNOWN,
        CANDIDATE,
        LEADER,
        FOLLOWER;
    }
}
