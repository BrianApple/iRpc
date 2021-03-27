package iRpc.dataBridge.vote;

import java.util.Map;

/**
 * @Description 集群信息
 * @Author yangcheng
 * @Date 2021/3/26
 */
public class ClusterInfo  {
    private boolean isCluster;
    private String leaderId;
    private Map<String, String> peers;//全量节点信息

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }


    public boolean isCluster() {
        return isCluster;
    }

    public void setCluster(boolean cluster) {
        isCluster = cluster;
    }

    public Map<String, String> getPeers() {
        return peers;
    }

    public void setPeers(Map<String, String> peers) {
        this.peers = peers;
    }
}
