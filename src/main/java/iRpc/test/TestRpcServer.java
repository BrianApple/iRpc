package iRpc.test;

import iRpc.base.messageDeal.MessageSender;
import iRpc.base.processor.IProcessor;
import iRpc.base.starter.ClientStarter;
import iRpc.base.starter.ServerStarter;
import iRpc.dataBridge.RequestData;
import iRpc.dataBridge.ResponseData;
import iRpc.socketAware.RemoteServer;
import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.List;
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
//						System.out.println("客户端收到数据："+ret.getData());
//					}
//				});

		ResponseData ret = MessageSender.synBaseMsgSend(false,
				"iRpc.rpcService.RPCExportServiceImpl",
				"test",
				classType,
				argsData,
				10000);
		System.out.println("客户端收到数据："+ret.getData());
		System.out.println("main方法执行结束");
	}
}
