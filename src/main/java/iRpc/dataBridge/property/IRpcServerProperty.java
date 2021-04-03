package iRpc.dataBridge.property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotcp.com</p>
 * @author hejuanjuan
 * @date 2021年4月3日
 * @version 1.0
 */
public class IRpcServerProperty implements PropertyForMap {
	private String serverPort;
	private String heartbeat;
	private List<NodeInfo> clusterNode;
	public String getServerPort() {
		return serverPort;
	}
	public void setServerPort(String serverPort) {
		this.serverPort = serverPort;
	}
	public String getHeartbeat() {
		return heartbeat;
	}
	public void setHeartbeat(String heartbeat) {
		this.heartbeat = heartbeat;
	}
	public List<NodeInfo> getClusterNode() {
		return clusterNode;
	}
	public void setClusterNode(List<NodeInfo> clusterNode) {
		this.clusterNode = clusterNode;
	}
	@Override
	public Map<String, Object> fillPropertyByMap() {
		
		Map<String,Object> iRpcClientMap = new HashMap<String, Object>();
		iRpcClientMap.put("serverPort", this.serverPort);
		iRpcClientMap.put("heartbeat", this.heartbeat);
		List<Map<String,Object>> serverNodes = new ArrayList<Map<String,Object>>();
		Map<String,Object> nodeInfoMap = null;
		for (NodeInfo nodeInfo : this.clusterNode) {
			nodeInfoMap = new HashMap<String, Object>();
			nodeInfoMap.put("node", nodeInfo.getNode());
			nodeInfoMap.put("ip", nodeInfo.getIp());
			nodeInfoMap.put("port", nodeInfo.getPort());
			serverNodes.add(nodeInfoMap);
		}
		iRpcClientMap.put("ClusterNode", serverNodes);
		return iRpcClientMap;
	}
}
