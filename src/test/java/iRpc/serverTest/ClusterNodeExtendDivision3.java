package iRpc.serverTest;

import iRpc.base.starter.ServerStarter;
import iRpc.dataBridge.property.IRpcServerProperty;
import iRpc.dataBridge.property.NodeInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Description 网络分裂测试--模拟AC + AB + C -> ABC iRpc不支持
 * @Author yangcheng
 * @Date 2021/4/6
 */
public class ClusterNodeExtendDivision3 {
    /**
     * AC
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
        node2.setPort("10918");
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
     * C
     */
    @Test
    public void startCluster1_Node3(){
        IRpcServerProperty serverProperty = new IRpcServerProperty();
        serverProperty.setServerPort("10918");
        serverProperty.setHeartbeat("60");
        serverProperty.setNodeName("n2");
        serverProperty.setGroupName("iRpcGroup");
        List<NodeInfo> lits = new ArrayList<NodeInfo>();
        NodeInfo node2 = new NodeInfo();
        node2.setNode("n2");
        node2.setIp("127.0.0.1");
        node2.setPort("10918");
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


    @Test
    public void test(){
        ConcurrentLinkedQueue<String> reviveNodeInfoQueue = new ConcurrentLinkedQueue<String> ();
        for (int i = 0 ; i < 10 ; i ++){
            reviveNodeInfoQueue.add("data");
        }
        Object[] arra = reviveNodeInfoQueue.toArray();
        System.out.println(arra);
    }
}
