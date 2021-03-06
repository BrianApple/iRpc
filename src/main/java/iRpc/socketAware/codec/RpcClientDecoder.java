package iRpc.socketAware.codec;


import java.nio.ByteBuffer;

import iRpc.base.SerializationUtil;
import iRpc.base.messageDeal.MessageReciever;
import iRpc.base.messageDeal.MessageType;
import iRpc.base.processor.IProcessor;
import iRpc.cache.CommonLocalCache;
import iRpc.dataBridge.RecieveData;
import iRpc.dataBridge.RequestData;
import iRpc.dataBridge.ResponseData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
/**
 * 
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月18日
 */
public class RpcClientDecoder extends LengthFieldBasedFrameDecoder{

	
	public RpcClientDecoder() {
		super(10240, 0, 2, 0, 0);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		ByteBuf buff =  (ByteBuf) super.decode(ctx, in);
		if(buff == null){
			return null;
		}
		ByteBuffer byteBuffer = buff.nioBuffer();
		int dataAllLen = byteBuffer.limit();
		int lenArea = byteBuffer.getShort();
		int dataLen = dataAllLen - 3;//数据区
		int msgType = byteBuffer.get();
		switch (MessageType.getMessageType(msgType)){
			case BASE_MSG:
				//基本消息类型的数据响应
				byte[] contentData = new byte[dataLen];
				byteBuffer.get(contentData);//报头数据
				ResponseData responseData = SerializationUtil.deserialize(contentData, ResponseData.class);
				RecieveData<ResponseData> recieveData= new RecieveData<ResponseData>(msgType,responseData);
				return recieveData;
			case HEART_MSG:
				break;
			case VOTE_MMSG:
				break;
		}
		return null;

	}

	
}
