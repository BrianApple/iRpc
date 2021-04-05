package iRpc.socketAware;

import com.alibaba.fastjson.JSON;
import iRpc.base.IRpcContext;
import iRpc.base.messageDeal.MessageReciever;
import iRpc.base.messageDeal.MessageType;
import iRpc.base.processor.IProcessor;
import iRpc.cache.CommonLocalCache;
import iRpc.dataBridge.RecieveData;
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

	private Bootstrap singleBootstrap ;
	private Object lock = new Object();
	public RemoteClient() {
		eventLoopGroupWorker = new NioEventLoopGroup(1, new ThreadFactoryImpl("netty_rpc_client_", false));
	}

	public boolean start(String ip, int port, String channelName) {
		ClientHandler clientHandler = new ClientHandler();
		//iRpc客户端不通过看门狗重连
		boolean isTryAgain = channelName.startsWith(IRpcContext.DEFUAL_CLIENT_CHANNEL_PREFIX) ? false :true ;
		if (singleBootstrap == null){
			synchronized (lock){
				if (singleBootstrap == null){
					singleBootstrap = this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)//
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
											new IOTGateWacthDog(bootstrap, ip, port, CommonUtil.timer, isTryAgain,channelName) {
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
				}
			}
		}


		/**
		 *
		 */
		if(channelName.startsWith(IRpcContext.DEFUAL_CLIENT_CHANNEL_PREFIX)){
			//iRpc client 
			ChannelFuture channelFuture = singleBootstrap.connect(ip, port).awaitUninterruptibly();
			if(channelFuture.isSuccess()){
				channel = channelFuture.channel();
				channel.attr(IRpcContext.ATTRIBUTEKEY_IRPC_CLIENT).set("iRpcClient");
				CommonLocalCache.ClientChannelCache.putClientChannel(channelName, channel);
				logger.info("rpc client is connect to server {}:{}", ip, port);
				return true;
			}else{
				logger.error("rpc client is connect to server {}:{} failed ", ip, port);
				return false;
			}
		}else{
			//inner cluster node
			while(true){
				if(CommonLocalCache.ChannelCache.getChannel(channelName)!= null){
            		break;
            	}
				ChannelFuture channelFuture = singleBootstrap.connect(ip, port).awaitUninterruptibly();
				if(channelFuture.isSuccess()){
					logger.info("cluster node {} connected success",String.format("%s:%s",ip,port));
					channel = channelFuture.channel();
					CommonLocalCache.ChannelCache.putRet(channelName, channel);
					break;
				}else {
					logger.error("cluster node {} connected failed ,try again later.....",String.format("%s:%s",ip,port));
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return true;
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
				@SuppressWarnings({ "rawtypes", "unchecked" })
				List<RecieveData> listData = (List) msg;
				int size = listData.size();
				for (int i = 0; i < size; i++) {
					@SuppressWarnings("rawtypes")
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
									IProcessor iProcessor = CommonLocalCache.AsynTaskCache.getAsynTask(responseNum);
									if (iProcessor != null){
										iProcessor.run(responseData);
									}else{
										logger.warn("not found callback method：{}", JSON.toJSONString(responseData));
									}
								}
							});
							break;
					}
				}
			}
		}

		/**
		 * 
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
