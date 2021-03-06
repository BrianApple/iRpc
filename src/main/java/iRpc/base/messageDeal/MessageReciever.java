package iRpc.base.messageDeal;

import iRpc.base.concurrent.ThreadFactoryImpl;
import iRpc.base.processor.IProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description 异步消息接收执行器
 * @Author yangcheng
 * @Date 2021/3/6
 */
public class MessageReciever implements IMessageReciever {
    private static ExecutorService executorService = null;
    static{
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2,new ThreadFactoryImpl("messageRecive_",false));
    }
    public static void reciveMsg(Runnable processor){
        executorService.execute(processor);
    }
}
