package com.netty_disruptor.chat.common.utils;


import com.netty_disruptor.chat.common.data.MessageContent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


/**
 * 编码
 */
public class Encoder extends MessageToByteEncoder<MessageContent> {
	//private QLogger logger = LoggerManager.getLogger(LoggerType.FLASH_HANDLER);
	@Override
	protected void encode(ChannelHandlerContext ctx, MessageContent msg, ByteBuf out) throws Exception {
		ByteBuf srcMsg = msg.encodeToByteBuf();
		try {
			out.writeBytes(srcMsg);
		}finally {
			srcMsg.release();
		}
	}
}
