package com.netty_disruptor.chat.common.data;


import com.netty_disruptor.chat.common.utils.CrcUtil;
import io.netty.buffer.ByteBuf;

/**
 *  上下行消息的封装类.
 *  netty 只跟byte数组打交道.
 *  其它自行解析
 */
public class MessageContent {
	protected byte [] bytes;
	protected int protocolId;


	public MessageContent() {
	}

	public MessageContent(ProtocolId protocolId, byte [] bytes) {
		this.bytes = bytes;
		this.protocolId = protocolId.getId();
	}

	public MessageContent(int protocolId, byte [] bytes) {
		this.bytes = bytes;
		this.protocolId = protocolId;
	}

	public int getProtocolId() {
		return protocolId;
	}

	public byte [] bytes() {
		return bytes;
	}

	/***
	 * 把header信息也encode 进去. 返回bytebuf
	 *
	 * 业务不要调用这个方法.
	 *
	 * @return
	 */
	public ByteBuf encodeToByteBuf(){
		// 获取 缓存区 大小 = bytes.length + ProtocolHeader.REQUEST_HEADER_LENGTH
		ByteBuf byteBuf = PooledBytebufFactory.getInstance().alloc(bytes.length + ProtocolHeader.REQUEST_HEADER_LENGTH);
		//
		ProtocolHeader header = new ProtocolHeader(bytes.length, protocolId, (int) CrcUtil.getCrc32Value(bytes));
		header.writeToByteBuf(byteBuf);
		byteBuf.writeBytes(bytes);
		return byteBuf;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public void setProtocolId(int protocolId) {
		this.protocolId = protocolId;
	}
}
