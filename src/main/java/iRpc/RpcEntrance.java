package iRpc;

import com.github.benmanes.caffeine.cache.Cache;
import iRpc.cache.CommonLocalCache;
import iRpc.proxy.RPCRequestProxy;
import iRpc.rpcService.RPCExportService;

/**
 * Description:入口
 * 　 * @author hejuanjuan
 * 　 * @date 2021/1/31
 * 　 * @version 1.0
 */
public class RpcEntrance {
//    Cache<String, RPCRequestProxy> caffeineCache= new CommonLocalCache().caffeineCache();
//    public String ip="127.0.0.1";
//    RPCExportService rpcExportService = caffeineCache.getIfPresent(ip)== null ? null : caffeineCache.getIfPresent(ip).isBroadcast(false).create(RPCExportService.class);
    RPCExportService rpcExportService=new RPCRequestProxy().create(RPCExportService.class);

}