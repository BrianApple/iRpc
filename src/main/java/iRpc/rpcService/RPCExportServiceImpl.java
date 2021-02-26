package iRpc.rpcService;

import iRpc.dataBridge.ResponseData;
import iRpc.service.RPCService;
import io.protostuff.Service;

/**
 * Description:
 * 　* @author hejuanjuan
 * 　* @date 2021/2/25
 */
@RPCService
public class RPCExportServiceImpl implements RPCExportService{
    @Override
    public String test(String str) {

        return "hello "+str;
    }
}
