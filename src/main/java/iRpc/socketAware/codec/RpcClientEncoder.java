package iRpc.socketAware.codec;

import iRpc.base.SerializationUtil;
import iRpc.dataBridge.RequestData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2019</p>
 * <p>Company: www.uiotcp.com</p>
 * @author yangcheng
 * @date 2019年3月18日
 * @version 1.0
 */
public class RpcClientEncoder extends MessageToByteEncoder<RequestData> {

	@Override
	protected void encode(ChannelHandlerContext ctx, RequestData msg, ByteBuf out) throws Exception {
		byte[] data = SerializationUtil.serialize(msg);
		out.writeShort(data.length);
		out.writeBytes(data);
	}

}
