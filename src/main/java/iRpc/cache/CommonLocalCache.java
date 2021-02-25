package iRpc.cache;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import iRpc.proxy.RPCRequestProxy;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

	public Cache caffeineCache() {
		return Caffeine.newBuilder()
				// 设置最后一次写入或访问后经过固定时间过期
				.expireAfterWrite(600, TimeUnit.SECONDS)
				// 初始的缓存空间大小
				.initialCapacity(100)
				// 缓存的最大条数
				.maximumSize(1000)
				.build();
	}
}
