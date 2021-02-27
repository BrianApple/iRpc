package iRpc.cache;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import iRpc.base.processor.IProcessor;
import iRpc.dataBridge.ResponseData;

import java.util.concurrent.TimeUnit;

/**
 * 
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月20日
 */
public class CommonLocalCache {
	public CommonLocalCache(){
		throw new AssertionError();
	}
	/**
	 * 缓存所有rpcNode IP
	 */
//	public static List<String> rpcServerCache = new CopyOnWriteArrayList<>();
	/**
	 * 缓存rpc代理---保持最新
	 */
//	public static ConcurrentHashMap<String , RPCRequestProxy> rpcProxys = new ConcurrentHashMap<>();

	public static <T> Cache<String,T>  newCaffeineCache() {
		return Caffeine.newBuilder()
				// 设置最后一次写入或访问后经过固定时间过期
				.expireAfterWrite(600, TimeUnit.SECONDS)
				// 初始的缓存空间大小
				.initialCapacity(100)
				// 缓存的最大条数
				.maximumSize(1000)
				.build();
	}
	
	/**
	 * return data cache
	 * <p>Description: </p>
	 * <p>Copyright: Copyright (c) 2019</p>
	 * <p>Company: www.uiotp.com</p>
	 * @author yangcheng
	 * @date 2021年2月27日
	 * @version 1.0
	 */
	public static class RetCache {
		private static Cache<String,Object> retCache;
		static{
			retCache = CommonLocalCache.newCaffeineCache();
		}
		
		public static void putRet(String key,Object value){
			retCache.put(key, value);
		}
		public static Object getRet(String key){
			return retCache.getIfPresent(key);
		}
		
		
	}
	/**
	 * 
	 * <p>Description: </p>
	 * <p>Copyright: Copyright (c) 2019</p>
	 * <p>Company: www.uiotcp.com</p>
	 * @author yangcheng
	 * @date 2021年2月27日
	 * @version 1.0
	 */
	public static class AsynchoTask{
		private static Cache<String,IProcessor<Object>> callTaskCache;
		static{
			callTaskCache = CommonLocalCache.newCaffeineCache();
		}
		
		
	}
}
