package iRpc.socketAware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iRpc.base.concurrent.ThreadFactoryImpl;
import iRpc.dataBridge.ResponseData;
import iRpc.socketAware.codec.RpcClientDecoder;
import iRpc.socketAware.codec.RpcClientEncoder;
import iRpc.socketAware.codec.RpcServerDecoder;
import iRpc.socketAware.codec.RpcServerEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
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
		                        new NettyConnetManageHandler(), //
		                        new NettyClientHandler());//获取数据
		                }
		            });
		 /**
		  * 
		  */
		 logger.info("rpc client is connected to server {}:{}",ip, port);
		 handler.connect(ip, port).sync();
	}
	
	
	class NettyConnetManageHandler extends ChannelDuplexHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            super.channelRegistered(ctx);
        }


        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            super.channelUnregistered(ctx);
        }


        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);

        }


        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        }


        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent evnet = (IdleStateEvent) evt;
                if (evnet.state().equals(IdleState.ALL_IDLE)) {
                    ctx.channel().close();
                }
            }

            ctx.fireUserEventTriggered(evt);
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.channel().close();
        }
    }
	
	class NettyClientHandler extends SimpleChannelInboundHandler<ResponseData> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ResponseData msg) throws Exception {
        	/**
        	 * TODO 调用的处理响应数据的方法
        	 */
//            processMessageReceived(ctx, msg);

        }
    }
}
