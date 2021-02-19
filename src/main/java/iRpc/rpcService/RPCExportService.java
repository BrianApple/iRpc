package iRpc.rpcService;


import iRpc.dataBridge.ResponseData;

import java.util.List;


/**
 * rpc服务接口
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月19日
 */
public interface RPCExportService {
	
	
	/**
	 * 测试用rpc
	 * @param str
	 * @return
	 */
	ResponseData test(String str);
	

}

