package iRpc.dataBridge;

import io.netty.channel.Channel;

/**
 * @Description
 * @Author yangcheng
 * @Date 2021/3/6
 */
public class RecieveData<T> {
    private int msgType;//消息类型
    private T data;//

    public RecieveData(int msgType, T data) {
        this.msgType = msgType;
        this.data = data;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
