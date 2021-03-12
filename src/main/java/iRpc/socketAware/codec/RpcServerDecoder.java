package iRpc.socketAware.codec;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import iRpc.base.SerializationUtil;
import iRpc.base.messageDeal.MessageType;
import iRpc.dataBridge.RecieveData;
import iRpc.dataBridge.RequestData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.internal.RecyclableArrayList;

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
public class RpcServerDecoder extends ByteToMessageDecoder {

	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		RecyclableArrayList arrayList = RecyclableArrayList.newInstance();
		for (;;){
			if (in.readableBytes() > 4){
				int pos = in.readerIndex();
				int len = in.readShort();
				if(len <= in.readableBytes()  ){

					int dataLen = len - 1;
					int msgType = in.readByte();//消息类型

					switch (MessageType.getMessageType(msgType)){
						case BASE_MSG:
							byte[] contentData = new byte[dataLen];//消息体
							in.readBytes(contentData);//报头数据
							RequestData requestData = SerializationUtil.deserialize(contentData, RequestData.class);
							RecieveData recieveData =  new RecieveData<RequestData>(msgType,requestData);
							arrayList.add(recieveData);
							break;
						case HEART_MSG:
							break;
						case VOTE_MMSG:
							break;
					}
				}else{
					in.readerIndex(pos);
					break;
				}
			}
			break;
		}
		if (arrayList.size() > 0){
			List<Object> list = new ArrayList<>(arrayList.size());
			list.addAll(arrayList);
			arrayList.recycle();
			out.add(list);
		}

	}

}
