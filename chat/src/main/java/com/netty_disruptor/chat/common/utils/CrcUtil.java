package com.netty_disruptor.chat.common.utils;

import java.util.zip.CRC32;

/**
 * CRC校验实用程序库 在数据存储和数据通讯领域，为了保证数据的正确，
 * 就不得不采用检错的手段。在诸多检错手段中，CRC是最著名的一种。
 * CRC的全称是循环冗余校验
 */
public class CrcUtil {
	/**
	 * 得到crc32计算的crc值
	 * @param bytes
	 * @return
	 */
	public static long getCrc32Value(byte [] bytes) {
		CRC32 crc32 = new CRC32();
		crc32.update(bytes);
		return crc32.getValue();
	}
}
