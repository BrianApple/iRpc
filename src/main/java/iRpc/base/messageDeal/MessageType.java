package iRpc.base.messageDeal;

/**
 * iRpc消息类型
 */
public enum MessageType {
    BASE_MSG(1),//rpc基本执行消息
    VOTE_MMSG(2),//节点选举消息
    HEART_MSG(0);//客户端与服务端心跳消息

    private int val;

    MessageType(int val) {
        this.val = val;
    }

    public int getVal(){
        return this.val;
    }

    public static MessageType getMessageType(int val ){
        for (MessageType type:MessageType.values()) {
            if (type.getVal() == val){
                return type;
            }
        }
        return null;
    }
}
