package iRpc.socketAware;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iRpc.base.concurrent.ThreadFactoryImpl;
import iRpc.dataBridge.RequestData;
import iRpc.dataBridge.ResponseData;
import iRpc.socketAware.codec.RpcServerDecoder;
import iRpc.socketAware.codec.RpcServerEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
/**
 * 通用通讯服务端
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月20日
 */
public class RemoteServer {
	protected static Logger logger = LoggerFactory.getLogger(RemoteServer.class);
	private final ServerBootstrap bootstrap;
	private final EventLoopGroup eventLoopGroupWorker;
    private final EventLoopGroup eventLoopGroupBoss;
	public RemoteServer() {
		bootstrap = new ServerBootstrap();
		eventLoopGroupBoss = new NioEventLoopGroup(1);
		eventLoopGroupWorker = new NioEventLoopGroup(2,new ThreadFactoryImpl("netty_RPC_selecter_", false));
	}
	
	public void start(){
		bootstrap.group(eventLoopGroupBoss, eventLoopGroupWorker)
		.channel(NioServerSocketChannel.class)
		//
        .option(ChannelOption.SO_BACKLOG, 1024)
        //
        .option(ChannelOption.SO_REUSEADDR, true)
        //
        .option(ChannelOption.SO_KEEPALIVE, false)
        //
        .childOption(ChannelOption.TCP_NODELAY, true)
        //
        .option(ChannelOption.SO_SNDBUF, 65535)
        //
        .option(ChannelOption.SO_RCVBUF, 65535)
        //
        .localAddress(new InetSocketAddress(10916))//默认端口10916
        .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                    new RpcServerEncoder(), //
                    new RpcServerDecoder(), //
                    new IdleStateHandler(0, 0, 120),//
                    new NettyConnetManageHandler(), //
                    new NettyServerHandler());
            }
        });
		logger.info("server started at port: 10916");
		try {
			this.bootstrap.bind().sync();
		}
		catch (InterruptedException e1) {
			throw new RuntimeException("this.bootstrap.bind().sync() InterruptedException", e1);
		}
	}
	
	
	class NettyConnetManageHandler extends ChannelDuplexHandler {

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
	
    class NettyServerHandler extends SimpleChannelInboundHandler<RequestData> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RequestData msg) throws Exception {

            ResponseData rpcResponse = new ResponseData(msg.getRequestNum(),200);
            try {
                Object handler = handler(msg);
                rpcResponse.setData(handler);
            } catch (Throwable throwable) {
            	rpcResponse.setReturnCode(500);
                rpcResponse.setErroInfo(throwable);
                throwable.printStackTrace();
            }
            ctx.writeAndFlush(rpcResponse);
        }
        /**
         * 服务端使用代理处理请求
         *
         * @param request
         * @return
         */
        private Object handler(RequestData request) throws Exception {
           /* //使用Class.forName进行加载Class文件
            Class<?> clazz = Class.forName(request.getClassName());
            Object serviceBean = applicationContext.getBean(clazz);
            Class<?> serviceClass = serviceBean.getClass();
            String methodName = request.getMethodName();

            Class<?>[] parameterTypes = request.getParamTyps();
            Object[] parameters = request.getArgs();

            //使用CGLIB Reflect
            FastClass fastClass = FastClass.create(serviceClass);
            FastMethod fastMethod = fastClass.getMethod(methodName, parameterTypes);
            System.out.println("开始调用CGLIB动态代理执行服务端方法...");
            return fastMethod.invoke(serviceBean, parameters);*/

//            Class<?> clazz = RPCCache.getClass(request.getClassName()+"Impl");
            Class<?> clazz = Class.forName(request.getClassName());
            ResponseData responseData = null;
            Method method = clazz.getMethod(request.getMethodName(), request.getParamTyps());
            responseData = (ResponseData) method.invoke(clazz.newInstance(), request.getArgs());
            //请求响应代码一一对应
//            responseData.setResponseNum(request.getRequestNum());
            return responseData.getData();
        }
    }
	
}
