package iRpc.base.starter;

import iRpc.base.IRpcContext;
import iRpc.socketAware.RemoteClient;
import iRpc.util.YamlUtil;

import java.util.List;
import java.util.Map;

/**
 * @Description 客户端
 * @Author yangcheng
 * @Date 2021/3/6
 */
public class ClientStarter implements Istarter{
    private String pathName;
    public ClientStarter() {
        pathName = "application.yml";
        start();
    }
    public ClientStarter(String pathName) {
        this.pathName = pathName;
        start();
    }

    /**
     * 启动客户端
     * @return
     */
    @Override
    public boolean start() {
        //获取配置信息
        Map<String,Object> map =  YamlUtil.getTypePropertieMap(pathName);
        Map<String,Object> clientMap = (Map<String, Object>) map.get("client");
        if(clientMap != null ){
            List<Map<String,Object>> lists = (List<Map<String, Object>>) clientMap.get("serverNode");
            Map<String,Object> m = lists.get(0);
            String ip = (String) m.get("ip");
            String port = String.valueOf( m.get("port"));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new RemoteClient().start(ip,Integer.parseInt(port), IRpcContext.DEFUAL_CHANNEL);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            },String.format("Client4%s:%s",ip,port)).start();
            return true;
        }
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }
}
