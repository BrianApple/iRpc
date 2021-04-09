package iRpc.serverTest;

import iRpc.base.starter.ServerStarter;
import iRpc.dataBridge.property.IRpcServerProperty;
import iRpc.dataBridge.property.NodeInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description 网络分裂测试--模拟ABC + AD -> ABCD iRpc支持
 * @Author yangcheng
 * @Date 2021/4/6
 */
public class ClusterMuiltNodeExtendDivision {
    /**
     * ABC
     */
    @Test
    public void startCluster1_Node1(){
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

    /**
     * ABC
     */
    @Test
    public void startCluster1_Node2(){
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
    /**
     * ABC
     */
    @Test
    public void startCluster1_Node3(){
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
        serverProperty.setClusterNode(lits);

        new ServerStarter(serverProperty);
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void startCluster1_Node4(){
        IRpcServerProperty serverProperty = new IRpcServerProperty();
        serverProperty.setServerPort("10919");
        serverProperty.setHeartbeat("60");
        serverProperty.setNodeName("n3");
        serverProperty.setGroupName("iRpcGroup");
        List<NodeInfo> lits = new ArrayList<NodeInfo>();
        NodeInfo node1 = new NodeInfo();
        node1.setNode("n0");
        node1.setIp("127.0.0.1");
        node1.setPort("10916");
        lits.add(node1);
        NodeInfo node2 = new NodeInfo();
        node2.setNode("n3");
        node2.setIp("127.0.0.1");
        node2.setPort("10919");
        lits.add(node2);
        serverProperty.setClusterNode(lits);

        new ServerStarter(serverProperty);
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
