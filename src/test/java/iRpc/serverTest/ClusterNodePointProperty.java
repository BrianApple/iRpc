package iRpc.serverTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import iRpc.base.starter.ServerStarter;
import iRpc.dataBridge.property.IRpcServerProperty;
import iRpc.dataBridge.property.NodeInfo;
/**
 * 
 * <p>Description: 不同的配置文件方式启动服务端 </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotp.com</p>
 * @author yangcheng
 * @date 2021年4月3日
 * @version 1.0
 */
public class ClusterNodePointProperty {
	@Test
	public void defaultyml(){
		new ServerStarter();
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	@Test
	public void pointyml(){
		new ServerStarter("application2.yml");
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	@Test
	public void noyml(){
		IRpcServerProperty serverProperty = new IRpcServerProperty();
		serverProperty.setServerPort("10916");
		serverProperty.setHeartbeat("60");
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
}
