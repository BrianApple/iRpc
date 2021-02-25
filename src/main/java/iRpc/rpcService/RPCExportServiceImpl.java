package iRpc.rpcService;

import iRpc.dataBridge.ResponseData;

/**
 * Description:
 * 　* @author hejuanjuan
 * 　* @date 2021/2/25
 */
public class RPCExportServiceImpl implements RPCExportService{
    @Override
    public String test(String str) {

        return "hello "+str;
    }
}
