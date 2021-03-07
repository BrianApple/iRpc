package iRpc.test;

import iRpc.base.messageDeal.MessageSender;
import iRpc.base.processor.IProcessor;
import iRpc.base.starter.ClientStarter;
import iRpc.base.starter.ServerStarter;
import iRpc.dataBridge.RequestData;
import iRpc.dataBridge.ResponseData;
import iRpc.socketAware.RemoteServer;
import org.checkerframework.checker.units.qual.C;

import java.util.UUID;

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
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ClientStarter clientStarter = new ClientStarter();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		RequestData requestData = new RequestData();
		requestData.setBroadcast(true);
		requestData.setRequestNum(UUID.randomUUID().toString());
		requestData.setClassName("iRpc.rpcService.RPCExportServiceImpl");//获取方法所在类名称
		requestData.setMethodName("test");
		Class<? >[] classes = new Class[]{String.class};
		requestData.setParamTyps(classes);
		Object[] args = new Object[]{"world"};
		requestData.setArgs(args);
		/**
		 * 异步消息发送
		 */
//		MessageSender.asynBaseMsgSend( requestData, new IProcessor() {
//			@Override
//			public void run(ResponseData ret) {
//				System.out.println("客户端收到数据："+ret.getData());
//			}
//		});
		/**
		 * 同步消息发送
		 */
		ResponseData ret = MessageSender.synBaseMsgSend(requestData,10000);
		System.out.println("客户端收到数据："+ret.getData());
		System.out.println("main方法执行结束");
	}
}
