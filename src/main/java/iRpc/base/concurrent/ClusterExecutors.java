package iRpc.base.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * 
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotcp.com</p>
 * @author hejuanjuan
 * @date 2021年4月3日
 * @version 1.0
 */
public class ClusterExecutors {
    public static ExecutorService executorService = null;
    static{
        executorService = Executors.newFixedThreadPool(108,new ThreadFactoryImpl("messageSendSyn_",false));
    }
}