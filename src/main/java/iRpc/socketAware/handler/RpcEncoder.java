package iRpc.socketAware.handler;

import iRpc.base.SerializationUtil;
import iRpc.dataBridge.ResponseData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
/**
 * 
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月18日
 */
public class RpcEncoder extends MessageToByteEncoder<ResponseData> {

	@Override
	protected void encode(ChannelHandlerContext ctx, ResponseData msg, ByteBuf out) throws Exception {
		byte[] data;
		try {
			data = SerializationUtil.serialize(msg);
			out.writeShort(data.length);
			out.writeBytes(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
