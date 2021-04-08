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

import iRpc.dataBridge.IDataSend;

/**
 * 
 * All rights Reserved, Designed By www.uiotcp.com
 * @Description:    描述 请求或者响应消息体的公共信息   
 * @author: yangcheng     
 * @date:   2020年6月29日  
 * @version V1.0
 */

public class RequestOrResponse implements IDataSend {

    protected String requestNum;
    protected String responseNum;
    /**
     * 该集群所属组名
     */
    protected String group;
    /**
     * 请求目的节点ID
     */
    protected String remoteId;
    
    /**
     * 节点ID
     */
    protected String localId;
    
    /**
     * 请求响应字段，表示返回响应码
     */
    protected int code = DLedgerResponseCode.SUCCESS.getCode();

    /**
     * 集群中的Leader Id
     */
    protected String leaderId = null;
    
    /**
     * 集群当前的选举轮次
     */
    protected long term = -1;


    
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public RequestOrResponse code(int code) {
        this.code = code;
        return this;
    }

    public void setIds(String localId, String remoteId, String leaderId) {
        this.localId = localId;
        this.remoteId = remoteId;
        this.leaderId = leaderId;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public RequestOrResponse copyBaseInfo(RequestOrResponse other) {
        this.group = other.group;
        this.term = other.term;
        this.code = other.code;
        this.localId = other.localId;
        this.remoteId = other.remoteId;
        this.leaderId = other.leaderId;
        this.requestNum = other.requestNum;
        return this;
    }

    public String baseInfo() {
        return String.format("info[group=%s,term=%d,code=%d,local=%s,remote=%s,leader=%s]", group, term, code, localId, remoteId, leaderId);
    }

    @Override
    public String getRequestNum() {
        return requestNum;
    }

    @Override
    public void setRequestNum(String requestNum) {
        this.requestNum = requestNum;
    }
}
