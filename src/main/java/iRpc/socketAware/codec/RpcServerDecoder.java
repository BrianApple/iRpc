package iRpc.socketAware.codec;


import java.nio.ByteBuffer;

import iRpc.base.SerializationUtil;
import iRpc.base.messageDeal.MessageType;
import iRpc.dataBridge.RecieveData;
import iRpc.dataBridge.RequestData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
/**
 * 协议类型：
 *  len   type     data
 * 2byte  1byte  长度值减1
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotcp.com</p>
 * @author yangcheng
 * @date 2019年3月8日
 * @version 1.0
 */
public class RpcServerDecoder extends LengthFieldBasedFrameDecoder{

	
	
	public RpcServerDecoder() {
		super(1024*4, 0, 2, 0, 0);
	}

	@SuppressWarnings({ "finally", "unused" })
	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		ByteBuf buff =  (ByteBuf) super.decode(ctx, in);
		if(buff == null){
			return null;
		}
		ByteBuffer byteBuffer = buff.nioBuffer();
		int dataAllLen = byteBuffer.limit();
		// TODO 后续增加报文校验机制
		int lenArea = byteBuffer.getShort();
		int dataLen = dataAllLen - 3;
		int msgType = byteBuffer.get();//消息类型

		switch (MessageType.getMessageType(msgType)){
			case BASE_MSG:
				byte[] contentData = new byte[dataLen];//消息体
				byteBuffer.get(contentData);//报头数据
				try {
					RequestData requestData = SerializationUtil.deserialize(contentData, RequestData.class);
					return new RecieveData<RequestData>(msgType,requestData);
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			case HEART_MSG:
				break;
			case VOTE_MMSG:
				break;
		}
		return null;
	}
}
