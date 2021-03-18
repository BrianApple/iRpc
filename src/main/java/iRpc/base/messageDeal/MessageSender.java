package iRpc.base.messageDeal;

import iRpc.base.IRpcContext;
import iRpc.base.concurrent.ThreadFactoryImpl;
import iRpc.base.processor.IProcessor;
import iRpc.cache.CommonLocalCache;
import iRpc.dataBridge.IDataSend;
import iRpc.dataBridge.RequestData;
import iRpc.dataBridge.ResponseData;
import iRpc.dataBridge.SendData;
import iRpc.dataBridge.vote.HeartBeatResponse;
import iRpc.dataBridge.vote.VoteResponse;
import iRpc.util.CommonUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消息发送
 */
public class MessageSender implements IMessageSender {
    private static ExecutorService executorService = null;
    static{
        executorService = Executors.newFixedThreadPool(10,new ThreadFactoryImpl("messageSendSyn_",false));
    }
    /**
     * 同步发送数据
     * @param msg
     * @param <R>
     * @param <T>
     * @return
     */
    private static <R,T> R  synMessageSend(T msg,int timeout){
        if(msg instanceof  SendData){
            SendData<IDataSend>  sendData= (SendData<IDataSend>) msg;
            Channel  channel= ((SendData<IDataSend>) msg).getChannel();
            channel.writeAndFlush(msg).addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(!future.isSuccess()){
                        //not success 自己报文结果
                        CommonLocalCache.RetCache.putRet(sendData.getData().getRequestNum(),
                                new ResponseData(sendData.getData().getRequestNum(), 500));
                    }else{
                        //发送成功---巧用异步task,将返回值写入RET结果缓存
                        CommonLocalCache.AsynTaskCache.putAsynTask(sendData.getData().getRequestNum(), new IProcessor() {
                            @Override
                            public void run(ResponseData ret) {
                                CommonLocalCache.RetCache.putRet(ret.getResponseNum(),ret);
                            }
                        });
                    }
                }
            });
            int index = timeout / 1000;
            while(CommonLocalCache.RetCache.getRet(sendData.getData().getRequestNum()) == null){
                if (index-- <= 0  ){
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return (R) CommonLocalCache.RetCache.getRet(sendData.getData().getRequestNum());
        }
        return null;
    }

    /**
     * 异步发送数据
     * @param msg
     * @return
     */
    private static  boolean asynMessageSend(SendData<IDataSend> msg, IProcessor task) {
        Channel  channel= ((SendData<IDataSend>) msg).getChannel();
        channel.writeAndFlush(msg).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    CommonLocalCache.AsynTaskCache.putAsynTask(msg.getData().getRequestNum(),task);
                }
            }
        });
        return true;
    }

    /**
     * 同步消息发送，超时时间为毫秒
     * @return
     */
    private  static ResponseData synMessageSend2Server(int msgType, IDataSend data, int timeout){
        Channel channel = CommonLocalCache.ChannelCache.getChannel(IRpcContext.DEFUAL_CHANNEL);
        ResponseData ret = synMessageSend(new SendData<IDataSend>(msgType,channel,data),timeout);
        return ret == null ? new ResponseData(data.getRequestNum(),500) : ret;
    }

    /**
     * 异步发送消息，
     * @param data 发送出去的方法
     * @param task 回调方法
     * @return
     */
    private static boolean asynMessaSend2Server(int msgType,IDataSend data,IProcessor task){
        Channel channel = CommonLocalCache.ChannelCache.getChannel(IRpcContext.DEFUAL_CHANNEL);
        boolean suc = asynMessageSend(new SendData<IDataSend>(msgType,channel,data),task);
        return suc;
    }

    /**
     * 同步发送消息，消息类型为1
     * @param isBroadcast
     * @param className
     * @param methodName
     * @param argsType
     * @param args
     * @param timeout
     * @return
     */
    public static ResponseData synBaseMsgSend(boolean isBroadcast,String className,String methodName,Class<?>[] argsType,Object[] args, int timeout){
        RequestData requestData = new RequestData();
        requestData.setBroadcast(isBroadcast);
        requestData.setRequestNum(String.valueOf(CommonUtil.getSeq()));
        requestData.setClassName(className);//获取方法所在类名称
        requestData.setMethodName(methodName);
        requestData.setParamTyps(argsType);
        requestData.setArgs(args);
        return synMessageSend2Server( 1,  requestData,  timeout);
    }

    /**
     * 同步发送消息，消息类型为1
     * @param isBroadcast
     * @param className
     * @param methodName
     * @param args
     * @param timeout
     * @return
     */
    public static ResponseData synBaseMsgSend(boolean isBroadcast,String className,String methodName,Object[] args, int timeout){
        RequestData requestData = new RequestData();
        requestData.setBroadcast(isBroadcast);
        requestData.setRequestNum(String.valueOf(CommonUtil.getSeq()));
        requestData.setClassName(className);//获取方法所在类名称
        requestData.setMethodName(methodName);
        requestData.setArgs(args);
        return synMessageSend2Server( 1,  requestData,  timeout);
    }

    /**
     * 异步发送消息，消息类型为1
     * @param isBroadcast
     * @param className
     * @param methodName
     * @param argsType
     * @param args
     * @param task
     * @return
     */
    public static boolean asynBaseMsgSend(boolean isBroadcast,String className,String methodName,Class<?>[] argsType,Object[] args,IProcessor task){
        RequestData requestData = new RequestData();
        requestData.setBroadcast(isBroadcast);
        requestData.setRequestNum(String.valueOf(CommonUtil.getSeq()));
        requestData.setClassName(className);//获取方法所在类名称
        requestData.setMethodName(methodName);
        requestData.setParamTyps(argsType);
        requestData.setArgs(args);
        return asynMessaSend2Server(1, requestData, task);
    }
    /**
     * 异步发送消息(type=1)
     * @param isBroadcast
     * @param className
     * @param methodName
     * @param args
     * @param task
     * @return
     */
    public static boolean asynBaseMsgSend(boolean isBroadcast,String className,String methodName,Object[] args,IProcessor task){
        RequestData requestData = new RequestData();
        requestData.setBroadcast(isBroadcast);
        requestData.setRequestNum(String.valueOf(CommonUtil.getSeq()));
        requestData.setClassName(className);//获取方法所在类名称
        requestData.setMethodName(methodName);
        requestData.setArgs(args);
        return asynMessaSend2Server(1, requestData, task);
    }

    /**
     * 选举消息(type=2)
     * @param sendData
     * @return
     */
    public static CompletableFuture<VoteResponse> vote(IDataSend sendData){
        CompletableFuture<VoteResponse> future = new CompletableFuture<>();
        asynMessaSend2Server(2, sendData, new IProcessor() {
            @Override
            public void run(ResponseData ret) {
                VoteResponse voteResponse = (VoteResponse)ret.getData();
                if(ret.getReturnCode() == 200 && ret.getData() != null){
                    future.complete(voteResponse);
                }else{
                    future.completeExceptionally(ret.getErroInfo());
                }
            }
        });
        return future;
    }

    /**
     * 心跳消息（type=0)
     * @param sendData
     * @return
     */
    public static CompletableFuture<HeartBeatResponse>  heartBeat(IDataSend sendData){
        CompletableFuture<HeartBeatResponse> future = new CompletableFuture<>();
        asynMessaSend2Server(0, sendData, new IProcessor() {
            @Override
            public void run(ResponseData ret) {
                HeartBeatResponse heartBeatResponse = (HeartBeatResponse)ret.getData();
                if(ret.getReturnCode() == 200 && ret.getData() != null){
                    future.complete(heartBeatResponse);
                }else{
                    future.completeExceptionally(ret.getErroInfo());
                }
            }
        });
        return future;
    }

}
