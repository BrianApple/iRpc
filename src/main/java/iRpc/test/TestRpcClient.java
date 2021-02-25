package iRpc.test;

import iRpc.socketAware.RemoteClient;


/**
 * RPC测试入口
 * <p>Description: </p>
 * @author yangcheng
 * @date 2021年1月23日
 */
public class TestRpcClient {
	public static void main(String[] args) {
		ClientRpc();
	}
	/**
	 * rpc服务端
	 */
	public static void ClientRpc(){
		RemoteClient client = new RemoteClient();
		new Thread(()->{
			try {
				client.start("127.0.0.1", 10916);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
		
	}
}
