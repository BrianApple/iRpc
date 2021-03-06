package iRpc.dataBridge;

import io.netty.channel.Channel;

/**
 * @Description
 * @Author yangcheng
 * @Date 2021/3/5
 */
public class SendData<T> {
    private int msgType;//消息类型
    private Channel channel ;
    private T data;//请求或者相应data对象

    public SendData(int msgType, T data) {
        this.msgType = msgType;
        this.data = data;
    }

    public SendData(int msgType,Channel channel, T data) {
        this.msgType = msgType;
        this.channel = channel;
        this.data = data;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }
}
