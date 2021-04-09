package iRpc.cache;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import iRpc.base.processor.IProcessor;
import iRpc.dataBridge.ResponseData;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

	public static <T> Cache<String,T>  newCaffeineCache(int timeout) {
		return Caffeine.newBuilder()
				// 设置最后一次写入或访问后经过固定时间过期
				.expireAfterWrite(timeout, TimeUnit.SECONDS)
				// 初始的缓存空间大小
				.initialCapacity(1000)
				// 缓存的最大条数
				.maximumSize(10000)
				.build();
	}
	public static <T> Cache<String,T>  newCaffeineCacheNoExpireTime() {
		return Caffeine.newBuilder()
				// 初始的缓存空间大小
				.initialCapacity(1000)
				// 缓存的最大条数
				.maximumSize(10000)
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
			retCache = CommonLocalCache.newCaffeineCache(60);
		}
		
		public static void putRet(String key,Object value){
			retCache.put(key, value);
		}
		public static Object getRet(String key){
			return retCache.getIfPresent(key);
		}
		
		
	}
	/**
	 * channel cache
	 * <p>Description: </p>
	 * <p>Copyright: Copyright (c) 2019</p>
	 * <p>Company: www.uiotp.com</p>
	 * @author yangcheng
	 * @date 2021年2月27日
	 * @version 1.0
	 */
	public static class ChannelCache {
		private static Cache<String, Channel> retCache;
		static{
			retCache = CommonLocalCache.newCaffeineCacheNoExpireTime();
		}

		public static void putRet(String key,Channel value){
			retCache.put(key, value);
		}
		public static Channel getChannel(String key){
			return retCache.getIfPresent(key);
		}


	}

	/**
	 * client channel cache
	 * <p>Description: </p>
	 * <p>Copyright: Copyright (c) 2019</p>
	 * <p>Company: www.uiotp.com</p>
	 * @author yangcheng
	 * @date 2021年2月27日
	 * @version 1.0
	 */
	public static class ClientChannelCache {
		private static Cache<String, Channel> retCache;
		static{
			retCache = CommonLocalCache.newCaffeineCacheNoExpireTime();
		}

		public static void putClientChannel(String key,Channel value){
			retCache.put(key, value);
		}
		public static Channel getChannel(String key){
			return retCache.getIfPresent(key);
		}
		public static void removeChannel(String key){
			retCache.invalidate(key);
		}

	}

	/**
	 * 
	 * <p>Description: </p>
	 * <p>Copyright: Copyright (c) 2021</p>
	 * <p>Company: www.uiotp.com</p>
	 * @author yangcheng
	 * @date 2021年2月27日
	 * @version 1.0
	 */
	public static class AsynTaskCache{
		private static Cache<String,IProcessor> callTaskCache;
		static{
			callTaskCache = CommonLocalCache.newCaffeineCache(60);
		}

		public static void putAsynTask(String key,IProcessor task){
			callTaskCache.put(key , task);
		}
		public static IProcessor getAsynTask(String key){
			return callTaskCache.getIfPresent(key);
		}
	}

	/**
	 * property cache
	 * <p>Description: 基础配置信息 </p>
	 * <p>Copyright: Copyright (c) 2019</p>
	 * <p>Company: www.uiotp.com</p>
	 * @author hejuanjuan
	 * @date 2021年2月27日
	 * @version 1.0
	 */
	public static class BasicInfoCache {
		private static Cache<String, Object> propertyCache;
		static{
			propertyCache = CommonLocalCache.newCaffeineCacheNoExpireTime();
		}

		public static void putProperty(String key,Object value){
			propertyCache.put(key, value);
		}
		public static Object getProperty(String key){
			return propertyCache.getIfPresent(key);
		}


	}

	/**
	 * 用于缓存选举过程中，server node节点的网络重连接线程
	 */
	public static class Client2ServerThreadCache{
		private static Map<String,Thread> cache = new ConcurrentHashMap<>();

		/**
		 *
		 * @param key  ip:port
		 * @param thread
		 */
		public static void putThread(String key ,Thread thread){
			cache.put(key,thread);
		}
		public static boolean isExist(String key){
			return cache.containsKey(key);
		}
		public static void remove(String key){
			cache.remove(key);
		}
	}
}
