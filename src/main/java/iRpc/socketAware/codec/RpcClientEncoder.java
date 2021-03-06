package iRpc.socketAware.codec;

import iRpc.base.SerializationUtil;
import iRpc.base.messageDeal.MessageType;
import iRpc.dataBridge.IDataSend;
import iRpc.dataBridge.RequestData;
import iRpc.dataBridge.SendData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 *  * 协议类型：
 *  *  len   type     data
 *  * 2byte  1byte  长度值减1
 * <p>Description: </p>
 * @author yangcheng
 * @date 2019年3月18日
 * @version 1.0
 */
public class RpcClientEncoder extends MessageToByteEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		SendData<IDataSend> sendData = (SendData<IDataSend>)msg;


		switch (MessageType.getMessageType(sendData.getMsgType())){
			case BASE_MSG:
				//基本消息
				byte[] data = SerializationUtil.serialize((RequestData)sendData.getData());
				out.writeShort(data.length+1);
				out.writeByte(sendData.getMsgType());
				out.writeBytes(data);
			case HEART_MSG:
				//心跳消息
				break;
			case VOTE_MMSG:
				//选举消息
				break;
		}

	}

}
