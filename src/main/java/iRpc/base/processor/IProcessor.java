package iRpc.base.processor;

import java.util.HashMap;
import java.util.Map;

import iRpc.dataBridge.ResponseData;

/**
 * 执行器
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotp.com</p>
 * @author yangcheng
 * @date 2021年2月27日
 * @version 1.0
 */
public interface IProcessor<T> {
	public void run(ResponseData<T> ret);
	
	
	
	public static void main(String[] args) {
		Map<String,IProcessor<Object>> retCache = new HashMap<>();
		retCache.put("1", new IProcessor<Object>() {

			@Override
			public void run(ResponseData<Object> ret) {
				
				System.out.println(ret.getReturnCode());
			}
		});
		
		//-------------------------------------------------------------
		ResponseData<Object> ret2= new ResponseData<>("1", 200);
		retCache.get(ret2.getResponseNum()).run(ret2);
		
	}

}
