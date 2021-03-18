package iRpc.base.starter;

import iRpc.cache.CommonLocalCache;
import iRpc.socketAware.RemoteClient;
import iRpc.socketAware.RemoteServer;
import iRpc.util.YamlUtil;
import iRpc.vote.DLedgerConfig;
import iRpc.vote.DLedgerLeaderElector;
import iRpc.vote.MemberState;

import java.util.List;
import java.util.Map;

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
            String port  = String.valueOf( serverMap.get("serverPort"));
            String heartbeat= String.valueOf(serverMap.get("heartbeat"));

            /**
             * 集群相关节点--涉及节点选举等操作
             */
            if(serverMap.containsKey("ClusterNode")){
                //初始化选举器
                initLeaderElector();
                List<Map<String,Object>> lists = (List<Map<String, Object>>)serverMap.get("ClusterNode");
                Map<String,Object> m = lists.get(0);

            }

//            String nodeIp = (String) m.get("ip");
//            String nodePort = (String) m.get("port");


            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //启动服务端
                        new RemoteServer().start(Integer.parseInt(port),Integer.parseInt(heartbeat));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            },String.format("server:%s",port)).start();
        }
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }
}
