package iRpc.socketAware.codec;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import iRpc.base.SerializationUtil;
import iRpc.base.messageDeal.MessageReciever;
import iRpc.base.messageDeal.MessageType;
import iRpc.base.processor.IProcessor;
import iRpc.cache.CommonLocalCache;
import iRpc.dataBridge.RecieveData;
import iRpc.dataBridge.RequestData;
import iRpc.dataBridge.ResponseData;
import iRpc.dataBridge.vote.HeartBeatResponse;
import iRpc.dataBridge.vote.VoteResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.internal.RecyclableArrayList;

/**
 * 
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月18日
 */
public class RpcClientDecoder extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		RecyclableArrayList  list = RecyclableArrayList.newInstance();

		for(; ; ){
			if (in.readableBytes() > 4){
				int pos = in.readerIndex();
				int len = in.readShort();//长度
				if(len <= in.readableBytes()){

					int dataLen = len - 1;//数据区
					int msgType = in.readByte();
					switch (MessageType.getMessageType(msgType)){
						case BASE_MSG:
							//基本消息类型的数据响应
							byte[] contentData = new byte[dataLen];
							in.readBytes(contentData);//报头数据
							ResponseData responseData = SerializationUtil.deserialize(contentData, ResponseData.class);
							RecieveData<ResponseData> recieveData= new RecieveData<ResponseData>(msgType,responseData);
							list.add(recieveData);
							break;
						case HEART_MSG:
							//基本消息类型的数据响应
							byte[] heartData = new byte[dataLen];
							in.readBytes(heartData);//报头数据
							HeartBeatResponse heartBeatResponse = SerializationUtil.deserialize(heartData, HeartBeatResponse.class);
							RecieveData<HeartBeatResponse> recieveHeartData= new RecieveData<HeartBeatResponse>(msgType,heartBeatResponse);
							list.add(recieveHeartData);

							break;
						case VOTE_MMSG:
							//基本消息类型的数据响应
							byte[] voteData = new byte[dataLen];
							in.readBytes(voteData);//报头数据
							VoteResponse voteResponse = SerializationUtil.deserialize(voteData, VoteResponse.class);
							RecieveData<VoteResponse> recieveVoteData= new RecieveData<VoteResponse>(msgType,voteResponse);
							list.add(recieveVoteData);
							break;
					}
				}else{
					in.readerIndex(pos);
					break;
				}
			}
			break;

		}
		if( list.size() > 0 ){
			List data = new ArrayList<>(list.size());
			data.addAll(list);
			list.recycle();
			out.add(data);
		}

	}
}
