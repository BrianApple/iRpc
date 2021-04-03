package iRpc.dataBridge.property;
/**
 * 
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotcp.com</p>
 * @author hejuanjuan
 * @date 2021年4月3日
 * @version 1.0
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRpcClientProperty implements PropertyForMap {
	private int retryTimes;
	private boolean isCluster;
	private List<NodeInfo> serverNode;
	
	public int getRetryTimes() {
		return retryTimes;
	}

	public void setRetryTimes(int retryTimes) {
		this.retryTimes = retryTimes;
	}

	public boolean isCluster() {
		return isCluster;
	}

	public void setCluster(boolean isCluster) {
		this.isCluster = isCluster;
	}

	public List<NodeInfo> getServerNode() {
		return serverNode;
	}

	public void setServerNode(List<NodeInfo> serverNode) {
		this.serverNode = serverNode;
	}

	@Override
	public Map<String, Object> fillPropertyByMap() {
		Map<String,Object> map = new HashMap<String, Object>();
		
		
		Map<String,Object> iRpcClientMap = new HashMap<String, Object>();
		iRpcClientMap.put("serverModCluster", this.isCluster);
		iRpcClientMap.put("retryTimes", this.retryTimes);
		List<Map<String,Object>> serverNodes = new ArrayList<Map<String,Object>>();
		Map<String,Object> nodeInfoMap = null;
		for (NodeInfo nodeInfo : this.serverNode) {
			nodeInfoMap = new HashMap<String, Object>();
			nodeInfoMap.put("ip", nodeInfo.getIp());
			nodeInfoMap.put("port", nodeInfo.getPort());
			serverNodes.add(nodeInfoMap);
		}
		iRpcClientMap.put("serverNode", serverNodes);
		return iRpcClientMap;
	}

	
}
