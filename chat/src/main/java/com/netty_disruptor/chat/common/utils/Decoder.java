package com.netty_disruptor.chat.common.utils;

import com.netty_disruptor.chat.common.data.MessageContent;
import com.netty_disruptor.chat.common.data.ProtocolHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 解码
 */
public class Decoder extends ByteToMessageDecoder {
	//private QLogger logger = LoggerManager.getLogger(LoggerType.FLASH_HANDLER);
	private int maxReceivedLength;
	private boolean crc;
	public Decoder(int maxReceivedLength, boolean needCrc) {
		this.crc = needCrc;
		this.maxReceivedLength = maxReceivedLength;
	}
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (! in.isReadable(ProtocolHeader.REQUEST_HEADER_LENGTH)) return;
		in.markReaderIndex();

		ProtocolHeader header = new ProtocolHeader(in);
		// 判断头包，数据是否是我们的
		if (! header.isMagicValid()) {
			//Log.e("aaa==","Invalid message, magic is error! "+ Arrays.toString(header.getMagic()));
			ctx.channel().close();
			return;
		}
		// 判断头包，数据大小 < maxReceivedLength
		if (header.getLength() < 0 || header.getLength() > maxReceivedLength) {
			//Log.e("aaa==","Invalid message, length is error! length is : "+ header.getLength());
			ctx.channel().close();
			return;
		}
		// 判断 缓冲区 数据是否可读
		if (! in.isReadable(header.getLength())) {
			in.resetReaderIndex();
			return;
		}

		byte [] bytes = new byte[header.getLength()];
		// 从缓存中读取 header.getLength() 长度的数据
		in.readBytes(bytes);
		// crc是否有效
		if (crc && ! header.crcIsValid(CrcUtil.getCrc32Value(bytes))) {
			//Log.e("aaa==","Invalid message crc! server is : "+ CrcUtil.getCrc32Value(bytes) +" client is "+header.getCrc());
			ctx.channel().close();
			return;
		}
		// 客户端可用数据
		MessageContent context = new MessageContent(header.getProtocolId(), bytes);
		out.add(context);
	}
}
