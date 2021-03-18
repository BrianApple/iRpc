package iRpc.base.starter;

import iRpc.base.concurrent.ThreadFactoryImpl;
import iRpc.cache.CommonLocalCache;
import iRpc.socketAware.RemoteClient;
import iRpc.socketAware.RemoteServer;
import iRpc.util.YamlUtil;
import iRpc.vote.DLedgerConfig;
import iRpc.vote.DLedgerLeaderElector;
import iRpc.vote.MemberState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description
 * @Author yangcheng
 * @Date 2021/3/6
 */
public class ServerStarter implements Istarter{
    private String pathName;
    public ServerStarter() {
        pathName = "application.yml";
        start();
    }
    public ServerStarter(String pathName) {
        this.pathName = pathName;
        start();

    }

    /**
     * 初始化选举器，只有集群条件下才会选举
     * @return
     */
    public boolean initLeaderElector(){
        if(CommonLocalCache.BasicInfoCache.getProperty("elector") == null){
            DLedgerConfig dLedgerConfig = new DLedgerConfig();
            DLedgerLeaderElector elector = new DLedgerLeaderElector(dLedgerConfig,new MemberState(dLedgerConfig));
            CommonLocalCache.BasicInfoCache.putProperty("elector",elector);
            elector.startup();
        }
        return true;
    }

    @Override
    public boolean start() {
        //获取配置信息
        Map<String,Object> map =  YamlUtil.getTypePropertieMap(pathName);
        Map<String,Object> serverMap = (Map<String, Object>) map.get("server");
        if(serverMap != null ){
            String serverPort  = String.valueOf( serverMap.get("serverPort"));
            String heartbeat= String.valueOf(serverMap.get("heartbeat"));

            /**
             * 集群相关节点--涉及节点选举等操作
             */
            if(serverMap.containsKey("ClusterNode")){
                DLedgerConfig config = new DLedgerConfig();
                //初始化选举器
                List<Map<String,Object>> lists = (List<Map<String, Object>>)serverMap.get("ClusterNode");
                StringBuffer sb = new StringBuffer("n0-localhost:20911;");
                for (Map<String,Object> nodeInfo
                     : lists) {

                    ClusterExecutors.executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            String nodeName = (String) nodeInfo.get("node");
                            String ip = (String) nodeInfo.get("ip");
                            String port = String.valueOf(nodeInfo.get("port"));
                            try {
                                new RemoteClient().start(ip,Integer.parseInt(port),String.format("%s:$s",ip,port));
                            } catch (InterruptedException e) {
                            }
                            sb.append(String.format("%s-%s:%s;",nodeName,ip,port));
                        }
                    });

                }
                config.setPeers(sb.substring(0,sb.length()-1));
                initLeaderElector();

            }

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
            },String.format("server:%s",serverPort)).start();
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