package iRpc.socketAware.handler;


import java.nio.ByteBuffer;

import iRpc.base.SerializationUtil;
import iRpc.dataBridge.RequestData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
/**
 * 
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月18日
 */
public class RpcDecoder extends LengthFieldBasedFrameDecoder{

	
	
	public RpcDecoder() {
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
		int lenArea = byteBuffer.getShort();
		int dataLen = dataAllLen - 2;
		byte[] contentData = new byte[dataLen];
        byteBuffer.get(contentData);//报头数据
        RequestData requestData = null;
        try {
        	requestData = SerializationUtil.deserialize(contentData, RequestData.class);
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			return requestData;
		}
       
        
	}

	
}
