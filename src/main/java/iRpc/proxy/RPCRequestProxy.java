package iRpc.proxy;

import iRpc.dataBridge.RequestData;
import iRpc.dataBridge.ResponseData;
import iRpc.socketAware.RemoteClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Description:
 * 　 * Copyright: Copyright (c) 2019
 * 　 * Company: www.uiotcp.com
 * 　 * @author hejuanjuan
 * 　 * @date 2021/2/7
 * 　 * @version 1.0
 */
public class RPCRequestProxy {
    //TODO 缓存所有rpc服务节点信息
    private String rpcServerIP="127.0.0.1";
    private boolean isBroadcast = false;//勿用volatile
    public RPCRequestProxy(String rpcServerIP) {
        super();
        this.rpcServerIP = rpcServerIP;
    }
    public RPCRequestProxy(){
        this.rpcServerIP = rpcServerIP;
    }
    public RPCRequestProxy isBroadcast(boolean isBroadcast){
        this.isBroadcast = isBroadcast;
        return this;
    }
    @SuppressWarnings("unchecked")
    public <T> T create(Class<?> clazz){
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),new Class<?>[]{clazz}, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // TODO Auto-generated method stub
                RequestData requestData = new RequestData();
                requestData.setBroadcast(isBroadcast);
                requestData.setRequestNum(UUID.randomUUID().toString());
                requestData.setClassName(method.getDeclaringClass().getSimpleName());//获取方法所在类名称
                requestData.setMethodName(method.getName());
                requestData.setParamTyps(method.getParameterTypes());
                requestData.setArgs(args);

                RemoteClient remoteClient = new RemoteClient();
                //TODO  改为从zookeeper获取rpc数据
                System.out.println("开始执行："+method.getName()+"方法");
                long startTome = System.currentTimeMillis();
                ResponseData responseData = null;
                try {
                    remoteClient.start(rpcServerIP, 10916);
                    ResponseData send = remoteClient.send(requestData);
                    System.out.println("请求调用返回结果：{}"+ send.getData());
                } catch (Exception e) {
                    e.printStackTrace();
//                    responseData = new ResponseData();
//                    responseData.setResponseNum("500");
                }
                System.out.println("执行："+method.getName()+"方法费时=="+(System.currentTimeMillis()-startTome));
                return responseData;
            }
        });
    }

}
