package iRpc.serverTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import iRpc.base.starter.ServerStarter;
import iRpc.dataBridge.property.IRpcServerProperty;
import iRpc.dataBridge.property.NodeInfo;
/**
 * 集群扩展测试 node1 和 node2为集群iRpcGroup，startNode3为iRpcGroup中动态新增节点
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotp.com</p>
 * @author yangcheng hejuanjuan
 * @date 2021年4月3日
 * @version 1.0
 */
public class ClusterNodeExtend {
	/**
	 * AB
	 */
	@Test
	public void startNode1(){
		IRpcServerProperty serverProperty = new IRpcServerProperty();
		serverProperty.setServerPort("10916");
		serverProperty.setHeartbeat("60");
		serverProperty.setNodeName("n0");
		serverProperty.setGroupName("iRpcGroup");
		List<NodeInfo> lits = new ArrayList<NodeInfo>();
		NodeInfo node1 = new NodeInfo();
		node1.setNode("n0");
		node1.setIp("127.0.0.1");
		node1.setPort("10916");
		lits.add(node1);
		NodeInfo node2 = new NodeInfo();
		node2.setNode("n1");
		node2.setIp("127.0.0.1");
		node2.setPort("10917");
		lits.add(node2);
		serverProperty.setClusterNode(lits);;
		
		new ServerStarter(serverProperty);
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * AB
	 */
	@Test
	public void startNode2(){
		IRpcServerProperty serverProperty = new IRpcServerProperty();
		serverProperty.setServerPort("10917");
		serverProperty.setHeartbeat("60");
		serverProperty.setNodeName("n1");
		serverProperty.setGroupName("iRpcGroup");
		List<NodeInfo> lits = new ArrayList<NodeInfo>();
		NodeInfo node1 = new NodeInfo();
		node1.setNode("n0");
		node1.setIp("127.0.0.1");
		node1.setPort("10916");
		lits.add(node1);
		NodeInfo node2 = new NodeInfo();
		node2.setNode("n1");
		node2.setIp("127.0.0.1");
		node2.setPort("10917");
		lits.add(node2);
		serverProperty.setClusterNode(lits);;
		
		new ServerStarter(serverProperty);
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * ABC
	 */
	@Test
	public void startNode3(){
		IRpcServerProperty serverProperty = new IRpcServerProperty();
		serverProperty.setServerPort("10918");
		serverProperty.setHeartbeat("60");
		serverProperty.setNodeName("n2");
		serverProperty.setGroupName("iRpcGroup");
		List<NodeInfo> lits = new ArrayList<NodeInfo>();
		NodeInfo node1 = new NodeInfo();
		node1.setNode("n0");
		node1.setIp("127.0.0.1");
		node1.setPort("10916");
		lits.add(node1);
		NodeInfo node2 = new NodeInfo();
		node2.setNode("n1");
		node2.setIp("127.0.0.1");
		node2.setPort("10917");
		lits.add(node2);
		NodeInfo node3 = new NodeInfo();
		node3.setNode("n2");
		node3.setIp("127.0.0.1");
		node3.setPort("10918");
		lits.add(node3);
		serverProperty.setClusterNode(lits);;
		
		new ServerStarter(serverProperty);
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
