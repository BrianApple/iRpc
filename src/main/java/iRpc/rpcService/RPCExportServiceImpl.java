package iRpc.rpcService;

import iRpc.dataBridge.ResponseData;
import iRpc.service.IRPCService;

import java.util.ArrayList;
import java.util.List;

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

    public int test(ArrayList<String> args){
        return args.size();
    }
}
