package iRpc.test;

import iRpc.base.IRpcContext;
import iRpc.base.messageDeal.MessageSender;
import iRpc.base.processor.IProcessor;
import iRpc.base.starter.ClientStarter;
import iRpc.base.starter.ServerStarter;
import iRpc.dataBridge.RequestData;
import iRpc.dataBridge.ResponseData;
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
		ServerStarter serverStarter = new ServerStarter();
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ClientStarter clientStarter = new ClientStarter();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

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




	}
}
