package iRpc.clientTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.alibaba.fastjson.JSON;

import iRpc.base.messageDeal.MessageSender;
import iRpc.base.starter.ClientStarter;
import iRpc.dataBridge.ResponseData;
import iRpc.dataBridge.property.IRpcClientProperty;
import iRpc.dataBridge.property.NodeInfo;

public class ClientNodePointProperty {
	@Test
	public void clientStartedByYML(){
        @SuppressWarnings("unused")
		ClientStarter clientStarter = new ClientStarter();
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /**
         * 同步消息发送
         */
        Class<? >[] classType = new Class[]{String.class};
        Object[] argsData = new Object[]{"world"};
        ResponseData ret = MessageSender.synBaseMsgSend(false,
                "iRpc.rpcService.RPCExportServiceImpl",
                "test",
                classType,
                argsData,
                5000);

        System.out.println("client recived message by syn："+ret.getData());
        while(true) {
            getClusterInfo();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
	
	@Test
	public void clientStartedByBean(){
		IRpcClientProperty clientProperty = new IRpcClientProperty();
		clientProperty.setCluster(true);
		clientProperty.setRetryTimes(3);//default 3 times
		
		List<NodeInfo> lits = new ArrayList<NodeInfo>();
		NodeInfo node1 = new NodeInfo();
		node1.setIp("127.0.0.1");
		node1.setPort("10916");
		lits.add(node1);
		NodeInfo node2 = new NodeInfo();
		node2.setIp("127.0.0.1");
		node2.setPort("10917");
		lits.add(node2);
		NodeInfo node3 = new NodeInfo();
		node3.setIp("127.0.0.1");
		node3.setPort("10918");
		lits.add(node3);
		clientProperty.setServerNode(lits);
		@SuppressWarnings("unused")
		ClientStarter clientStarter = new ClientStarter(clientProperty);
		while(true) {
            getClusterInfo();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
	}
	
	
    public void getClusterInfo(){
        ResponseData ret = MessageSender.synBaseMsgSend(false,
                "iRpc.vote.service.ClusterInfoService",
                "getClusterInfo",
                5000);

        System.out.println("客户端同步获取集群信息："+ JSON.toJSONString(ret)+" 执行时间："+ (System.currentTimeMillis()- Long.parseLong(ret.getResponseNum())));
    }
}
