package iRpc.socketAware.codec;

import iRpc.base.SerializationUtil;
import iRpc.base.messageDeal.MessageType;
import iRpc.dataBridge.RecieveData;
import iRpc.dataBridge.ResponseData;
import iRpc.dataBridge.SendData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
/**
 * 
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotp.com</p>
 * @author yangcheng
 * @date 2019年3月18日
 * @version 1.0
 */
public class RpcServerEncoder extends MessageToByteEncoder<SendData> {

	@Override
	protected void encode(ChannelHandlerContext ctx, SendData msg, ByteBuf out) throws Exception {
		switch (MessageType.getMessageType(msg.getMsgType())){
			case BASE_MSG:
				try {
					byte[] data = SerializationUtil.serialize((ResponseData)msg.getData());
					out.writeShort(data.length+1);
					out.writeByte(msg.getMsgType());
					out.writeBytes(data);
				} catch (Exception e) {
					e.printStackTrace();
				}
			case HEART_MSG:
				break;
			case VOTE_MMSG:
				break;
		}


		
	}

}
