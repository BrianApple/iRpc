package iRpc.base.starter;

import iRpc.base.IRpcContext;
import iRpc.base.concurrent.ThreadFactoryImpl;
import iRpc.cache.CommonLocalCache;
import iRpc.socketAware.RemoteClient;
import iRpc.socketAware.RemoteServer;
import iRpc.util.YamlUtil;
import iRpc.vote.DLedgerConfig;
import iRpc.vote.DLedgerLeaderElector;
import iRpc.vote.MemberState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description 服务端启动类
 * @Author yangcheng
 * @Date 2021/3/6
 */
public class ServerStarter implements Istarter{
    Logger logger = LoggerFactory.getLogger("serverStarter");

    private String pathName;
    public ServerStarter() {
        pathName = IRpcContext.PropertyName;
        start();
        logger.info("the profile name is {}",pathName);
    }
    public ServerStarter(String pathName) {
        this.pathName = pathName;
        start();
        logger.info("the profile name is {}",pathName);
    }

    /**
     * 初始化选举器，只有集群条件下才会选举
     * @return
     */
    public boolean initLeaderElector(DLedgerConfig dLedgerConfig){
        if(CommonLocalCache.BasicInfoCache.getProperty("elector") == null){
            MemberState memberState = new MemberState(dLedgerConfig);
            DLedgerLeaderElector elector = new DLedgerLeaderElector(dLedgerConfig,memberState);
            CommonLocalCache.BasicInfoCache.putProperty("elector",elector);
            CommonLocalCache.BasicInfoCache.putProperty("memberState",memberState);
            elector.startup();
        }
        return true;
    }

    @Override
    public boolean start() {
        if(!IRpcContext.serverStarted.compareAndSet(false,true)){
                return false;
        }
        //获取配置信息
        Map<String,Object> map =  YamlUtil.getTypePropertieMap(pathName);
        Map<String,Object> serverMap = (Map<String, Object>) map.get("iRpcServer");
        if(serverMap != null ){
            String serverPort  = String.valueOf( serverMap.get("serverPort"));
            String heartbeat= String.valueOf(serverMap.get("heartbeat"));

            //            String nodeIp = (String) m.get("ip");
//            String nodePort = (String) m.get("port");


            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //启动服务端
                        new RemoteServer().start(Integer.parseInt(serverPort),Integer.parseInt(heartbeat));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            },String.format("iRpc_server:%s",serverPort)).start();
            /**
             * 集群相关节点--涉及节点选举等操作
             */
            if(serverMap.containsKey("ClusterNode")){
                DLedgerConfig config = new DLedgerConfig();
                //初始化选举器
                List<Map<String,Object>> lists = (List<Map<String, Object>>)serverMap.get("ClusterNode");
                StringBuffer sb = new StringBuffer();
                for (Map<String,Object> nodeInfo
                     : lists) {
                    String nodeName = (String) nodeInfo.get("node");
                    String ip = (String) nodeInfo.get("ip");
                    String port = String.valueOf(nodeInfo.get("port"));
                    if(serverPort.equals(port)){
                        config.setSelfId(nodeName);
                    }
                    sb.append(String.format("%s-%s:%s;",nodeName,ip,port));
                    ClusterExecutors.executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            new RemoteClient().start(ip,Integer.parseInt(port),String.format("%s:%s",ip,port));

                        }
                    });

                }
                logger.info("cluster peers info {}" , sb.substring(0,sb.length()-1));
                config.setPeers(sb.substring(0,sb.length()-1));
                initLeaderElector(config);

            }


        }
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }
}
class ClusterExecutors {
    public static ExecutorService executorService = null;
    static{
        executorService = Executors.newFixedThreadPool(10,new ThreadFactoryImpl("messageSendSyn_",false));
    }
}