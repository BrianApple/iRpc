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

import java.util.List;

public class HeartBeatRequest extends RequestOrResponse {
    private String peers;
    private List<String> reviveNode;
    /**
     * 新扩充的nodes信息
     */
    protected List<String> newNodePeers;
    public String getPeers() {
        return peers;
    }

    public void setPeers(String peers) {
        this.peers = peers;
    }

	public List<String> getReviveNode() {
		return reviveNode;
	}

	public HeartBeatRequest setReviveNode(List<String> reviveNode) {
		this.reviveNode = reviveNode;
		return this;
	}

	public List<String> getNewNodePeers() {
		return newNodePeers;
	}

	public HeartBeatRequest setNewNodePeers(List<String> newNodePeers) {
		this.newNodePeers = newNodePeers;
		return this;
	}
    
}
