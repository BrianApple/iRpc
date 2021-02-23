package iRpc.main;

import iRpc.cache.CommonLocalCache;
import iRpc.rpcService.RPCExportService;

/**
 * Description:
 * 　 * @author hejuanjuan
 * 　 * @date 2021/1/31
 * 　 * @version 1.0
 */
public class RpcClient {
    public String ip="127.0.0.1";
    RPCExportService rpcExportService = CommonLocalCache.rpcProxys.get(ip)== null ? null : CommonLocalCache.rpcProxys.get(ip).isBroadcast(false).create(RPCExportService.class);


}