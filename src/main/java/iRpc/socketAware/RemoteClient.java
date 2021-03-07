package iRpc.socketAware;

import iRpc.base.IRpcContext;
import iRpc.base.messageDeal.MessageReciever;
import iRpc.base.messageDeal.MessageType;
import iRpc.cache.CommonLocalCache;
import iRpc.dataBridge.RecieveData;
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
import io.netty.handler.timeout.IdleStateHandler;

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
	public RemoteClient(){
		eventLoopGroupWorker = new NioEventLoopGroup(1, new ThreadFactoryImpl("netty_rpc_client_", false));
	}

	public void start(String ip ,int port) throws InterruptedException{
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
								 new RpcClientEncoder(), //
								 new RpcClientDecoder(),
								clientHandler);//获取数据
					}
				});
		/**
		*
		*/
		logger.info("rpc client is connected to server {}:{}",ip, port);
		ChannelFuture future = handler.connect(ip, port).sync().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture channelFuture) throws Exception {
				channel=channelFuture.channel();
				CommonLocalCache.ChannelCache.putRet(IRpcContext.DEFUAL_CHANNEL,channel);
			}
		});
	}
}

@ChannelHandler.Sharable
class ClientHandler extends ChannelDuplexHandler {
	/**
	 * 处理rpc服务端响应消息
	 * @param ctx
	 * @param msg
	 * @throws Exception
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof RecieveData) {
			RecieveData recieveData = (RecieveData)msg ;
			switch (MessageType.getMessageType(recieveData.getMsgType())){
				case BASE_MSG:
					ResponseData responseData = (ResponseData) recieveData.getData();
					MessageReciever.reciveMsg(new Runnable() {
						@Override
						public void run() {
							String responseNum =responseData.getResponseNum();
							//执行回调
							CommonLocalCache.AsynTaskCache.getAsynTask(responseNum).run(responseData);
						}
					});
				case HEART_MSG:
					break;
				case VOTE_MMSG:
					break;
			}
		}
	}
}
