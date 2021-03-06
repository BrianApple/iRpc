package iRpc.socketAware;


import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import iRpc.cache.CommonLocalCache;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
/**
 * 
 * @Description: 
 * @author  yangcheng
 * @date:   2019年9月15日
 */
public abstract class IOTGateWacthDog extends SimpleChannelInboundHandler<Object> implements IHolderHanders , TimerTask{
	
	private Bootstrap bootstrap;
	private String ip;
	private int port;
	private HashedWheelTimer timer; //定时任务执行者
	private boolean  flag; //是否执行重试机制
	private String  channelName; //是否执行重试机制
	/**
	 * IOTGateWacthDog
	 * @param bootstrap 引导
	 * @param ip master IP
	 * @param port master PORT
	 * @param timer 定时器
	 * @param flag 是否执行重试机制
	 * @param channelName channel缓存的名称
	 */
	public IOTGateWacthDog(Bootstrap bootstrap, String ip, int port,HashedWheelTimer timer,boolean flag,String channelName) {
		super();
		this.bootstrap = bootstrap;
		this.ip = ip;
		this.port = port;
		this.timer = timer;
		this.flag = flag;
		this.channelName = channelName;
	}


//	public IOTGateWacthDog() {
//		// TODO Auto-generated constructor stub
//	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelActive();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if(flag){
			timer.newTimeout(this, 800, TimeUnit.MILLISECONDS);
		}else{
			//客户端不会自动重连
			CommonLocalCache.Client2ServerThreadCache.remove(channelName);//删除channel绑定本地线程标识
			CommonLocalCache.ClientChannelCache.removeChannel(channelName);
		}
		ctx.fireChannelInactive();
	}

	public void run(Timeout timeout) throws Exception {
			final ChannelFuture channelFuture=bootstrap.connect(ip, port);
			
			channelFuture.addListener(new ChannelFutureListener() {
				
				public void operationComplete(ChannelFuture future) throws Exception {
					boolean isSuc = future.isSuccess();
					if(isSuc){
						future.channel().pipeline().fireChannelActive();
						CommonLocalCache.ChannelCache.putRet(channelName,future.channel());
					}else{
						future.channel().pipeline().fireChannelInactive();
					}
				}
			});
	}

}
