package com.netty_disruptor.chat.common.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.netty_disruptor.chat.common.data.MessageContent;

/**
 * 不同服务存在不同生产者ID
 */
public class MessageProducer {

    // 特定生产者ID
    private String producerID;

    // 获取ringBuffer
    private RingBuffer<MessageContent> ringBuffer;

    public MessageProducer(String producerID, RingBuffer<MessageContent> ringBuffer) {
        this.producerID = producerID;
        this.ringBuffer = ringBuffer;
    }

    public void onSendData(MessageContent data){
        // 1、 在生产者发送消息的时候， 首先 需要从 RingBuffer 里面获取一个可用的序号
        long sequence = ringBuffer.next();
        try{
            // 2、 获取这个序号，获取到 RingBuffer 的空对象，直接赋值即可
            MessageContent dataEvent = ringBuffer.get(sequence);

            // 3、 进行实际的赋值处理
            dataEvent.setBytes(data.bytes());
            dataEvent.setProtocolId(data.getProtocolId());

        }finally {
            // 保证最后一定发布
            ringBuffer.publish(sequence);
        }
    }

}
