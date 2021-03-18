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

package iRpc.dataBridge.vote;

public class VoteResponse extends RequestOrResponse {

    public RESULT voteResult = RESULT.UNKNOWN;

    public VoteResponse() {

    }

    public VoteResponse(VoteRequest request) {
        copyBaseInfo(request);
    }

    public RESULT getVoteResult() {
        return voteResult;
    }

    public void setVoteResult(RESULT voteResult) {
        this.voteResult = voteResult;
    }

    public VoteResponse voteResult(RESULT voteResult) {
        this.voteResult = voteResult;
        return this;
    }

    public VoteResponse term(long term) {
        this.term = term;
        return this;
    }

    public enum RESULT {
        /** 不是UNKNOWN，就是有效的投票 */
        UNKNOWN,
        /** 赞成票，得到的赞成票超过集群节点数量的一半才能成为Leader */
        ACCEPT,
        /**  */
        REJECT_UNKNOWN_LEADER,
        /**  */
        REJECT_UNEXPECTED_LEADER,
        /** 拒绝票，如果自己维护的term小于远端维护的term，更新自己维护的投票轮次 */
        REJECT_EXPIRED_VOTE_TERM,
        /** 拒绝票，原因是已经投了其他节点的票 */
        REJECT_ALREADY_VOTED,
        /** 拒绝票，原因是因为集群中已经存在Leader了。alreadyHasLeader设置为true，无需在判断其他投票结果了，结束本轮投票 */
        REJECT_ALREADY__HAS_LEADER,
        /** 拒绝票，对端的投票轮次小于自己的team，则认为对端还未准备好投票，对端使用自己的投票轮次，是自己进入到Candidate状态 */
        REJECT_TERM_NOT_READY,
        /** 拒绝票，如果自己维护的term小于远端维护的ledgerEndTerm，则返回该结果，如果对端的team大于自己的team，
         * 需要记录对端最大的投票轮次，以便更新自己的投票轮次 */
        REJECT_TERM_SMALL_THAN_LEDGER,
        /** 拒绝票，如果自己维护的ledgerTerm小于对端维护的ledgerTerm，则返回该结果。如果是此种情况，增加计数器-biggerLedgerNum的值 */
        REJECT_EXPIRED_LEDGER_TERM,
        /** 拒绝票，如果对端的ledgerTeam与自己维护的ledgerTeam相等，但是自己维护的ledgerEndIndex小于对端维护的值，返回该值，
         * 增加biggerLedgerNum计数器的值 */
        REJECT_SMALL_LEDGER_END_INDEX;
    }

    public enum ParseResult {
        WAIT_TO_REVOTE,
        REVOTE_IMMEDIATELY,
        PASSED,
        WAIT_TO_VOTE_NEXT;
    }
}
