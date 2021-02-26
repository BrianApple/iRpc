package iRpc;

import iRpc.socketAware.RemoteServer;

/**
 * RPC测试入口
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotcp.com</p>
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
		RemoteServer server = new RemoteServer();
		server.start();
	}
}
