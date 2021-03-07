package iRpc.rpcService;

import iRpc.dataBridge.ResponseData;
import iRpc.service.IRPCService;

/**
 * Description:
 * 　* @author hejuanjuan
 * 　* @date 2021/2/25
 */
@IRPCService
public class RPCExportServiceImpl{
    public String test(String str) {

        return "hello "+str;
    }
}
