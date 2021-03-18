package iRpc.socketAware;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iRpc.base.SerializationUtil;
import iRpc.base.exception.IRPCServerNotFound;
import iRpc.base.messageDeal.MessageType;
import iRpc.cache.CommonLocalCache;
import iRpc.dataBridge.RecieveData;
import iRpc.dataBridge.SendData;
import iRpc.dataBridge.vote.HeartBeatRequest;
import iRpc.dataBridge.vote.VoteRequest;
import iRpc.vote.DLedgerLeaderElector;
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
	
	public void start(int port , int heartbeat){
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
        .localAddress(new InetSocketAddress(port))//默认端口10916
        .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                    new RpcServerEncoder(), //
                    new RpcServerDecoder(), //
                    new IdleStateHandler(0, 0, heartbeat),//
                    new NettyConnetManageHandler(), //
                    new NettyServerHandler());
            }
        });
		logger.info("server started at port: "+ port);
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

    /**
     * 服务端handler
     */
    class NettyServerHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(msg instanceof List){
                List<RecieveData> listData = (List)msg;
                int size = listData.size();
                for (int i = 0 ; i < size ; i++){
                    RecieveData recieveData = listData.get(i);
                    switch (MessageType.getMessageType(recieveData.getMsgType())){
                        case BASE_MSG:
                            RequestData requestData = (RequestData) recieveData.getData();
                            ResponseData rpcResponse = new ResponseData(requestData.getRequestNum(),200);
                            try {
                                Object data = handleRpcRquest(requestData);
                                rpcResponse.setData(data);
                            } catch (Throwable throwable) {
                                rpcResponse.setReturnCode(500);
                                rpcResponse.setErroInfo(throwable);
                                logger.error("服务执行异常",throwable);
                            }
                            SendData<ResponseData> sendData = new SendData<ResponseData>(recieveData.getMsgType(), rpcResponse);
                            ctx.writeAndFlush(sendData);
                            break;
                        case HEART_MSG:
                            DLedgerLeaderElector elector4h = (DLedgerLeaderElector) CommonLocalCache.BasicInfoCache.getProperty("elector");
                            HeartBeatRequest heartBeatRequest4h = (HeartBeatRequest) recieveData.getData();
                            elector4h.handleHeartBeat(heartBeatRequest4h);
                            break;
                        case VOTE_MMSG:
                            DLedgerLeaderElector elector4v = (DLedgerLeaderElector) CommonLocalCache.BasicInfoCache.getProperty("elector");
                            VoteRequest voteRequest = (VoteRequest) recieveData.getData();
                            elector4v.handleVote(voteRequest,false);
                            break;
                    }
                }
            }

        }
        /**
         * 服务端使用代理处理请求
         *
         * @param request
         * @return null执行失败
         */
        private Object handleRpcRquest(RequestData request) {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(request.getClassName());
            } catch (ClassNotFoundException e) {
                throw  new IRPCServerNotFound("server not found（ClassNotFoundException）!");
            }
            Object data = null;
            Object[] args = request.getArgs();
            int argsLen = args.length;
            Class<?>[] clazzs = new Class[argsLen];
            if(request.getArgs()!= null && request.getArgs().length > 0 &&  ( request.getParamTyps() == null || request.getParamTyps().length < request.getArgs().length)){
                for (int i = 0 ; i < argsLen; i ++ ) {
                    clazzs[i] = args[i].getClass();
                }
            }else{
                clazzs = request.getParamTyps();
            }
            Method method = null;
            try {
                method = clazz.getMethod(request.getMethodName(), clazzs);
            } catch (NoSuchMethodException e) {
                throw  new IRPCServerNotFound("server not found（NoSuchMethodException）!");
            }
            if(clazz.isAnnotationPresent(iRpc.service.IRPCService.class) || method.isAnnotationPresent(iRpc.service.IRPCService.class) ){
                try {
                    data = method.invoke(clazz.newInstance(), request.getArgs());
                } catch (Exception e) {
                    throw  new IRPCServerNotFound("server not found!");
                }
            }else{
                throw  new IRPCServerNotFound("server not found!");
            }
            //请求响应代码一一对应
//            responseData.setResponseNum(request.getRequestNum());
            return data;
        }
    }
	
}
