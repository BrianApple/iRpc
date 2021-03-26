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

package iRpc.vote.store;


import iRpc.vote.MemberState;

/**
 * dledger两大核心类之一，另一个是DLedgerLeaderElector
 */
public abstract class DLedgerStore {

    public MemberState getMemberState() {
        return null;
    }
    /**向主节点追加日志(数据)。**/
    public abstract DLedgerEntry appendAsLeader(DLedgerEntry entry);
    /**向从节点同步日志**/
    public abstract DLedgerEntry appendAsFollower(DLedgerEntry entry, long leaderTerm, String leaderId);
    /**根据日志下标查找日志 **/
    public abstract DLedgerEntry get(Long index);
    /** 获取已提交的下标**/
    public abstract long getCommittedIndex();
    
    //更新commitedIndex的值，由具体的存储子类实现
    public void updateCommittedIndex(long term, long committedIndex) {

    }
    /**获取 Leader 当前最大的投票轮次 **/
    public abstract long getLedgerEndTerm();

    /**获取 Leader 下一条日志写入的下标（最新日志的下标）**/
    public abstract long getLedgerEndIndex();
    /**获取 Leader 第一条消息的下标**/
    public abstract long getLedgerBeginIndex();

    protected void updateLedgerEndIndexAndTerm() {
        if (getMemberState() != null) {
            getMemberState().updateLedgerIndexAndTerm(getLedgerEndIndex(), getLedgerEndTerm());
        }
    }

    public void flush() {

    }

    public long truncate(DLedgerEntry entry, long leaderTerm, String leaderId) {
        return -1;
    }

    public void startup() {

    }

    public void shutdown() {

    }
}
