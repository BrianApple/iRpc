package iRpc.base.starter;

import iRpc.socketAware.RemoteClient;
import iRpc.socketAware.RemoteServer;
import iRpc.util.YamlUtil;

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

    @Override
    public boolean start() {
        //获取配置信息
        Map<String,Object> map =  YamlUtil.getTypePropertieMap(pathName);
        Map<String,Object> serverMap = (Map<String, Object>) map.get("server");
        if(serverMap != null ){
            String port  = (String) serverMap.get("serverPort");
            String heartbeat= (String) serverMap.get("heartbeat");

            /**
             * 集群相关节点--涉及节点选举等操作
             */
            List<Map<String,Object>> lists = (List<Map<String, Object>>) serverMap.get("ClusterNode");
            Map<String,Object> m = lists.get(0);
            String nodeIp = (String) m.get("ip");
            String nodePort = (String) m.get("port");
            try {
                //启动服务端
                new RemoteServer().start(Integer.parseInt(port),Integer.parseInt(heartbeat));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }
}
