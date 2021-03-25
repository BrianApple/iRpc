package iRpc.socketAware;

import iRpc.base.IRpcContext;
import iRpc.base.messageDeal.MessageReciever;
import iRpc.base.messageDeal.MessageType;
import iRpc.cache.CommonLocalCache;
import iRpc.dataBridge.RecieveData;
import iRpc.dataBridge.vote.HeartBeatResponse;
import iRpc.dataBridge.vote.VoteResponse;
import iRpc.util.CommonUtil;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iRpc.base.concurrent.ThreadFactoryImpl;
import iRpc.dataBridge.ResponseData;
import iRpc.socketAware.codec.RpcClientDecoder;
import iRpc.socketAware.codec.RpcClientEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 通用通讯客户端
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月20日
 */
public class RemoteClient {
	protected static Logger logger = LoggerFactory.getLogger(RemoteClient.class);
	private final Bootstrap bootstrap = new Bootstrap();
	private final EventLoopGroup eventLoopGroupWorker;
	private Channel channel;

	public RemoteClient() {
		eventLoopGroupWorker = new NioEventLoopGroup(1, new ThreadFactoryImpl("netty_rpc_client_", false));
	}

	public void start(String ip, int port, String channelName) {
		ClientHandler clientHandler = new ClientHandler();
		Bootstrap handler = this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)//
				//
				.option(ChannelOption.TCP_NODELAY, true)
				//
				.option(ChannelOption.SO_KEEPALIVE, false)
				//
				.option(ChannelOption.SO_SNDBUF, 65535)
				//
				.option(ChannelOption.SO_RCVBUF, 65535)
				//
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(
							new IOTGateWacthDog(bootstrap, ip, port, CommonUtil.timer, true) {
								@Override
								protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
									ctx.fireChannelRead(msg);
								}

								@Override
								public ChannelHandler[] getChannelHandlers() {
									return new ChannelHandler[]{
											new RpcClientEncoder(), //
											new RpcClientDecoder(),
											clientHandler
									};
								}
							}.getChannelHandlers());
					}
				});

		/**
		 *
		 */
		logger.info("rpc client is connect to server {}:{}", ip, port);
		if(IRpcContext.DEFUAL_CHANNEL.equals(channelName )){
			//irpc客户端连接
			 try {
				handler.connect(ip, port).sync().addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture channelFuture) throws Exception {
						channel = channelFuture.channel();
						CommonLocalCache.ChannelCache.putRet(channelName, channel);
					}
				});
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
				while(true){
					ChannelFuture channelFuture = handler.connect(ip, port).awaitUninterruptibly();
					if(channelFuture.isSuccess()){
						logger.info("cluster node {} connected success",String.format("%s:%s",ip,port));
						channel = channelFuture.channel();
						CommonLocalCache.ChannelCache.putRet(channelName, channel);
						break;
					}else {
						logger.info("cluster node {} connected failed ,try again later.....",String.format("%s:%s",ip,port));
					}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
		}
		
		

	}

	@ChannelHandler.Sharable
	class ClientHandler extends ChannelDuplexHandler {
		/**
		 * 处理rpc服务端响应消息
		 *
		 * @param ctx
		 * @param msg
		 * @throws Exception
		 */
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof List) {
				List<RecieveData> listData = (List) msg;
				int size = listData.size();
				for (int i = 0; i < size; i++) {
					RecieveData recieveData = listData.get(i);
					switch (MessageType.getMessageType(recieveData.getMsgType())) {
						case BASE_MSG:
						case HEART_MSG:
						case VOTE_MMSG:
							/**
							 * 不同消息类型，responseData.getData()返回值不同
							 */
							ResponseData responseData = (ResponseData) recieveData.getData();
							MessageReciever.reciveMsg(new Runnable() {
								@Override
								public void run() {
									String responseNum = responseData.getResponseNum();
									//执行回调
									CommonLocalCache.AsynTaskCache.getAsynTask(responseNum).run(responseData);
								}
							});
							break;

//							HeartBeatResponse heartBeatResponse = (HeartBeatResponse) recieveData.getData();
//							MessageReciever.reciveMsg(new Runnable() {
//								@Override
//								public void run() {
//									String responseNum = heartBeatResponse.getRequestNum();
//									//执行回调
//									CommonLocalCache.AsynTaskCache.getAsynTask(responseNum).run(heartBeatResponse);
//								}
//							});
//
//							VoteResponse voteResponse = (VoteResponse) recieveData.getData();
//							MessageReciever.reciveMsg(new Runnable() {
//								@Override
//								public void run() {
//									String responseNum = voteResponse.getRequestNum();
//									//执行回调
//									CommonLocalCache.AsynTaskCache.getAsynTask(responseNum).run(voteResponse);
//								}
//							});
//							break;
					}
				}
			}
		}

		/**
		 * 无论是
		 * @param ctx
		 * @throws Exception
		 */
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			super.channelActive(ctx);
			channel = ctx.channel();
			InetSocketAddress inetSocketAddress = (InetSocketAddress)channel.remoteAddress();
			CommonLocalCache.ChannelCache.putRet(String.format("%s:%s",
					inetSocketAddress.getHostName(), 
					String.valueOf(inetSocketAddress.getPort())), channel);
		}
		
		
	}
}
