package iRpc.test;

import com.alibaba.fastjson.JSON;
import iRpc.base.IRpcContext;
import iRpc.base.messageDeal.MessageSender;
import iRpc.base.processor.IProcessor;
import iRpc.base.starter.ClientStarter;
import iRpc.base.starter.ServerStarter;
import iRpc.dataBridge.RequestData;
import iRpc.dataBridge.ResponseData;
import iRpc.dataBridge.property.IRpcServerProperty;
import iRpc.dataBridge.property.NodeInfo;
import iRpc.dataBridge.vote.VoteRequest;
import iRpc.dataBridge.vote.VoteResponse;
import iRpc.socketAware.RemoteServer;
import iRpc.util.CommonUtil;
import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * RPC测试入口
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotp.com</p>
 * @author yangcheng
 * @date 2021年1月23日
 * @version 1.0
 */
public class TestRpcServer {
	public static void main(String[] args) {
		ServerRpc();
	}
	/**
	 * rpc服务端
	 */
	public static void ServerRpc(){
		IRpcServerProperty serverProperty = new IRpcServerProperty();
		serverProperty.setServerPort("10918");
		serverProperty.setHeartbeat("60");
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
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		ClientStarter clientStarter = new ClientStarter();
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}

		/**
		 * 同步消息发送
		 */
		Class<? >[] classType = new Class[]{String.class};
		Object[] argsData = new Object[]{"world"};

		/**
		 * 异步消息发送
		 */
//		MessageSender.asynBaseMsgSend( false,
//				"iRpc.rpcService.RPCExportServiceImpl",
//				"test",
//				classType,
//				argsData,
//				new IProcessor() {
//					@Override
//					public void run(ResponseData ret) {
//						System.out.println("客户端异步收到数据："+ret.getData());
//					}
//				});

//		ResponseData ret = MessageSender.synBaseMsgSend(false,
//				"iRpc.rpcService.RPCExportServiceImpl",
//				"test",
//				classType,
//				argsData,
//				10000);
//
//		System.out.println("客户端同步收到数据："+ret.getData()+" 执行时间："+ (System.currentTimeMillis()- Long.parseLong(ret.getResponseNum())));
//		System.out.println("main方法执行结束");


//		VoteRequest voteRequest = new VoteRequest();
//		voteRequest.setRequestNum(String.valueOf(CommonUtil.getSeq()));//发送序号
//		voteRequest.setGroup("testgroup");
//		voteRequest.setLedgerEndIndex(-1);//发起投票节点维护的已知的最大日志条目索引。
//		voteRequest.setLedgerEndTerm(-1);//发起投票节点维护的已知的最大投票轮次。
//		voteRequest.setLeaderId("n0");
//		voteRequest.setTerm(-1);//发起投票的节点当前的投票轮次
//		voteRequest.setRemoteId("n0");
//		CompletableFuture<VoteResponse> voteResponse;
//		voteResponse = MessageSender.vote(voteRequest, IRpcContext.DEFUAL_CHANNEL);

//		for (;;){
//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			getClusterInfo();
//		}


	}


	public static void getClusterInfo(){
		ResponseData ret = MessageSender.synBaseMsgSend(false,
						"iRpc.vote.service.ClusterInfoService",
				"getClusterInfo",
				5000);

		System.out.println("客户端同步获取集群信息："+ JSON.toJSONString(ret.getData())+" 执行时间："+ (System.currentTimeMillis()- Long.parseLong(ret.getResponseNum())));
	}
}
