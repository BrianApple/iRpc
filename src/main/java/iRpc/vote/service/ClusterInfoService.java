package iRpc.vote.service;

import iRpc.cache.CommonLocalCache;
import iRpc.dataBridge.vote.ClusterInfo;
import iRpc.service.IRPCService;
import iRpc.vote.MemberState;

/**
 * @Description
 * @Author yangcheng
 * @Date 2021/3/26
 */
@IRPCService
public class ClusterInfoService {

    /**
     * 获取集群信息
     * @return
     */
    public ClusterInfo getClusterInfo(){
        ClusterInfo clusterInfo = new ClusterInfo();
        MemberState elector = (MemberState) CommonLocalCache.BasicInfoCache.getProperty("memberState");
        if(elector == null){
            clusterInfo.setCluster(false);
        }else{
            clusterInfo.setCluster(true);
            clusterInfo.setPeers(elector.getPeerMap());
            clusterInfo.setLeaderId(elector.getLeaderId());
        }
        return clusterInfo;
    }
}
