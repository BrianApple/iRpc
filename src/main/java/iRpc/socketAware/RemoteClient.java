package iRpc.socketAware;

import com.github.benmanes.caffeine.cache.Cache;
import iRpc.cache.CommonLocalCache;
import iRpc.dataBridge.RequestData;
import iRpc.future.DefaultFuture;
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
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.ConcurrentHashMap;

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
	Cache<String, DefaultFuture> caffeineCache= new CommonLocalCache().caffeineCache();
	public RemoteClient(){
		eventLoopGroupWorker = new NioEventLoopGroup(1, new ThreadFactoryImpl("netty_rpc_client_", false));
	}

	public void start(String ip ,int port) throws InterruptedException{
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
		                        new IdleStateHandler(0, 0, 120),//
		                        new ClientHandler());//获取数据
		                }
		            });
		 /**
		  * 
		  */
		 logger.info("rpc client is connected to server {}:{}",ip, port);
		ChannelFuture future = handler.connect(ip, port).sync();
		channel=future.channel();
	}
	/**
	 * 发送消息
	 *
	 * @param request
	 * @return
	 */
	public ResponseData send(final RequestData request) {
		try {
			channel.writeAndFlush(request).await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new ClientHandler().getRpcResponse(request.getRequestNum());
	}



	class ClientHandler extends ChannelDuplexHandler {

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			if (msg instanceof RequestData) {
				RequestData request = (RequestData) msg;
				//发送请求对象之前，先把请求ID保存下来，并构建一个与响应Future的映射关系
				caffeineCache.put(request.getRequestNum(), new DefaultFuture());
			}
			super.write(ctx, msg, promise);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof ResponseData) {
				//获取响应对象
				ResponseData response = (ResponseData) msg;
				DefaultFuture defaultFuture = caffeineCache.getIfPresent(response.getResponseNum());
				defaultFuture.setResponse(response);
			}
			super.channelRead(ctx,msg);
		}
		/**
		 * 获取响应结果
		 *
		 * @param requestId
		 * @return
		 */
		public ResponseData getRpcResponse(String requestId) {
			try {
				DefaultFuture future = caffeineCache.getIfPresent(requestId);
				return future.getRpcResponse(10);
			} finally {
				//获取成功以后，从map中移除
				caffeineCache.asMap().remove(requestId);
			}
		}
	}
}
