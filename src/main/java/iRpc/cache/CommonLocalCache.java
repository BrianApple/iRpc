package iRpc.cache;


import iRpc.proxy.RPCRequestProxy;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月20日
 */
public class CommonLocalCache {
	private CommonLocalCache(){
		throw new AssertionError();
	}
	/**
	 * 缓存所有rpcNode IP
	 */
	public static List<String> rpcServerCache = new CopyOnWriteArrayList<>();
	/**
	 * 缓存rpc代理---保持最新
	 */
	public static ConcurrentHashMap<String , RPCRequestProxy> rpcProxys = new ConcurrentHashMap<>();
}
