package iRpc.base.starter;

import iRpc.base.IRpcContext;
import iRpc.base.messageDeal.MessageSender;
import iRpc.cache.CommonLocalCache;
import iRpc.dataBridge.ResponseData;
import iRpc.dataBridge.property.IRpcClientProperty;
import iRpc.dataBridge.vote.ClusterInfo;
import iRpc.socketAware.RemoteClient;
import iRpc.util.CommonUtil;
import iRpc.util.YamlUtil;
import io.netty.channel.Channel;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @Description 客户端
 * @Author yangcheng
 * @Date 2021/3/6
 */
public class ClientStarter implements Istarter{
    private String pathName;
    public ClientStarter() {
        pathName = IRpcContext.PropertyName;
        IRpcContext.yumInfo.put("iRpcClient", YamlUtil.getTypePropertieMap(pathName).get("iRpcClient"));
        start();
    }
    public ClientStarter(String pathName) {
        this.pathName = pathName;
        IRpcContext.yumInfo.put("iRpcClient", YamlUtil.getTypePropertieMap(pathName).get("iRpcClient"));
        start();
    }
    public ClientStarter(IRpcClientProperty property) {
    	if(IRpcContext.yumInfo == null ){
    		IRpcContext.yumInfo = new HashMap<String, Object>();
    	}
        IRpcContext.yumInfo.put("iRpcClient", property.fillPropertyByMap());
        start();
    }

    /**
     * 启动客户端
     * @return
     */
    @Override
    public boolean start() {
        if(!IRpcContext.clientStarted.compareAndSet(false,true)){
            throw new RuntimeException("ClientStarter is started once");
        }
        //获取配置信息
        @SuppressWarnings("unchecked")
		Map<String,Object> clientMap = (Map<String, Object>) IRpcContext.yumInfo.get("iRpcClient");
        if(clientMap != null ){
            boolean isCluster = clientMap.get("serverModCluster") == null ?
                                        false :  Boolean.valueOf(String.valueOf(clientMap.get("serverModCluster"))) ;
            @SuppressWarnings("unchecked")
			List<Map<String,Object>> lists = (List<Map<String, Object>>) clientMap.get("serverNode");
            //启动时，配置文件0节点为服务端节点
            IRpcContext.LeaderNode = IRpcContext.DEFUAL_CLIENT_CHANNEL_PREFIX+lists.get(0).get("ip")+":"+lists.get(0).get("port");

            int retryTimes = clientMap.get("retryTimes") == null ?
                                3 : Integer.parseInt(String.valueOf(clientMap.get("retryTimes")));
                    try {
                        for (Map<String,Object> node : lists) {

                            String ip = (String) node.get("ip");
                            String port = String.valueOf( node.get("port"));
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = 0 ; i < retryTimes ; i ++){
                                        RemoteClient remoteClient = new RemoteClient();
                                        boolean isSuc = remoteClient.start(ip,Integer.parseInt(port),
                                                IRpcContext.DEFUAL_CLIENT_CHANNEL_PREFIX+String.format("%s:%s",ip,port));
                                        if (!isSuc){
                                            try {
                                                Thread.sleep(2000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            continue;
                                        }
                                        return;
                                    }
                                }
                            },String.format("Client4%s:%s",ip,port)).start();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            if (isCluster){
                CommonUtil.timer.newTimeout(new ClusterInfoInterval(), 2, TimeUnit.SECONDS);
            }
            return true;

        }
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }


}
class ClusterInfoInterval implements  TimerTask{
    private static Logger logger = LoggerFactory.getLogger(ClusterInfoInterval.class);
    private List<String> clientChannelKeys;

    @Override
    public void run(Timeout timeout) throws Exception {
        CommonUtil.timer.newTimeout(this,2,TimeUnit.SECONDS);
        getClusterInfo();

    }

    /**
     * 获取集群信息
     */
    public void getClusterInfo(){
        @SuppressWarnings("unchecked")
		Map<String,Object> clientMap = (Map<String, Object>) IRpcContext.yumInfo.get("iRpcClient");
        int retryTimes = clientMap.get("retryTimes") == null ?
                3 : Integer.parseInt(String.valueOf(clientMap.get("retryTimes")));
        List<String> clientChannelKeys = new ArrayList<>();
        if (IRpcContext.ClusterPeersInfo == null || IRpcContext.ClusterPeersInfo.isEmpty()){
            clientChannelKeys.add(IRpcContext.LeaderNode);//首次登录默认使用
        }else{
            //成功获取过集群节点信息
            Set<String> keys=  IRpcContext.ClusterPeersInfo.keySet();
            for (String key: keys
                 ) {
                clientChannelKeys.add(IRpcContext.DEFUAL_CLIENT_CHANNEL_PREFIX+IRpcContext.ClusterPeersInfo.get(key));
            }
        }
        ResponseData responseData = MessageSender.synBaseMsgSend(clientChannelKeys,
                false,
                "iRpc.vote.service.ClusterInfoService",
                "getClusterInfo",
                1800);
        ClusterInfo clusterInfo = (ClusterInfo) responseData.getData();
        
        if(clusterInfo == null 
        		|| clusterInfo.getPeers().get(clusterInfo.getLeaderId()) == null
        			|| clusterInfo.getPeers() == null
        				|| clusterInfo.getPeers().size() == 0 ){
        	//leader为空/集群节点为空
            return;
        }
        logger.debug("fetch leaderNode：{}",IRpcContext.DEFUAL_CLIENT_CHANNEL_PREFIX+clusterInfo.getPeers().get(clusterInfo.getLeaderId()));
        IRpcContext.LeaderNode = IRpcContext.DEFUAL_CLIENT_CHANNEL_PREFIX+clusterInfo.getPeers().get(clusterInfo.getLeaderId());
        
        IRpcContext.ClusterPeersInfo = clusterInfo.getPeers();

        Set<Map.Entry<String, String>> set = clusterInfo.getPeers().entrySet();
        for (Map.Entry<String, String> entry:set
             ) {
            String addr = entry.getValue();
            Channel channel = CommonLocalCache.ClientChannelCache.getChannel(IRpcContext.DEFUAL_CLIENT_CHANNEL_PREFIX+addr);
            if (channel == null){
                RemoteClient remoteClient = new RemoteClient();
                String ip = addr.split(":")[0];
                String port = addr.split(":")[1];
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0 ; i < retryTimes ; i ++){

                            boolean isSuc = remoteClient.start(ip,Integer.parseInt(port),
                                    IRpcContext.DEFUAL_CLIENT_CHANNEL_PREFIX+String.format("%s:%s",ip,port));
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (!isSuc){

                                continue;
                            }
                            return;
                        }
                    }
                },String.format("Client4%s:%s",ip,port)).start();
            }
        }

    }

    public List<String> getClientChannelKeys() {
        return clientChannelKeys;
    }

    public void setClientChannelKeys(List<String> clientChannelKeys) {
        this.clientChannelKeys = clientChannelKeys;
    }
}
