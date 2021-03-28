package iRpc.test;

import com.alibaba.fastjson.JSON;
import iRpc.base.messageDeal.MessageSender;
import iRpc.base.starter.ClientStarter;
import iRpc.dataBridge.ResponseData;

/**
 * @Description
 * @Author yangcheng
 * @Date 2021/3/28
 */
public class TestRpcClient {

    public static void main(String[] args) {
        ServerRpc();
    }
    /**
     * rpc服务端
     */
    public static void ServerRpc(){
        ClientStarter clientStarter = new ClientStarter();
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        /**
         * 同步消息发送
         */
        Class<? >[] classType = new Class[]{String.class};
        Object[] argsData = new Object[]{"world"};
        ResponseData ret = MessageSender.synBaseMsgSend(false,
                "iRpc.rpcService.RPCExportServiceImpl",
                "test",
                classType,
                argsData,
                5000);

        System.out.println("客户端同步收到数据："+ret.getData());
        while(true) {
            getClusterInfo();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public static void getClusterInfo(){
        ResponseData ret = MessageSender.synBaseMsgSend(false,
                "iRpc.vote.service.ClusterInfoService",
                "getClusterInfo",
                5000);

        System.out.println("客户端同步获取集群信息："+ JSON.toJSONString(ret.getData())+" 执行时间："+ (System.currentTimeMillis()- Long.parseLong(ret.getResponseNum())));
    }

}
