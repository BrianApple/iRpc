package iRpc.base;

import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description iRpc上下文
 * @Author yangcheng
 * @Date 2021/3/6
 */
public class IRpcContext {
    public static boolean isCluster = false;
    public static final String DEFUAL_CHANNEL = "cliChannel_";//iRpc 客户端会话信息
    public static final AttributeKey<String> ATTRIBUTEKEY_IRPC_CLIENT = AttributeKey.valueOf("iRpcClient");
    public static final String PropertyName = "application.yml";

    /**
     * yml配置文件map
     */
    public static Map<String,Object> yumInfo ;

    public static AtomicBoolean serverStarted = new AtomicBoolean(false);
    public static AtomicBoolean clientStarted = new AtomicBoolean(false);

    /**
     * 客户端获取iRpc集群信息
     */
    public static Map<String,String> ClusterPeersInfo;
    public static String LeaderNode;//主节点名称 = cliChannel_ip:port,单机时为服务端地址，集群时为leader节点呢地址
}
