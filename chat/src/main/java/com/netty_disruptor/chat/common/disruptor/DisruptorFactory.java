package com.netty_disruptor.chat.common.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;
import com.netty_disruptor.chat.common.data.MessageContent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * MessageContent 处理工厂 -- 单例
 */
public class DisruptorFactory {

    // 双静态 实现
    private static class Singletion {
        private static DisruptorFactory factory = new DisruptorFactory();
    }
    // 获取单例
    public static DisruptorFactory getInstance() {
        return  Singletion.factory;
    }

    // 空构造
    public DisruptorFactory() {

    }

    // 保存 生产者，通过  producerID -> MessageProducer
    private static Map<String, MessageProducer> producers = new ConcurrentHashMap<String, MessageProducer>();

    // 保存 消费者 队列
    private static Map<String, MessageProcessHandler> processes = new ConcurrentHashMap<String, MessageProcessHandler>();

    private RingBuffer<MessageContent> ringBuffer;

    private SequenceBarrier sequenceBarrier;

    private WorkerPool<MessageContent> workerPool;

    public void initAndStart(ProducerType type, int bufferSize, WaitStrategy waitStrategy, MessageProcessHandler[] MessageProcesses) {
        //1. 构建ringBuffer对象
        this.ringBuffer = RingBuffer.create(type,
                new EventFactory<MessageContent>() {
                    public MessageContent newInstance() {
                        return new MessageContent();
                    }
                },
                bufferSize,
                waitStrategy);
        //2.设置序号栅栏
        this.sequenceBarrier = this.ringBuffer.newBarrier();
        //3.设置工作池
        this.workerPool = new WorkerPool<MessageContent>(this.ringBuffer,
                this.sequenceBarrier,
                new EventExceptionHandler(), MessageProcesses);
        //4 把所构建的消费者置入池中
        for(MessageProcessHandler mc : MessageProcesses){
            this.processes.put(mc.getConsumerId(), mc);
        }
        //5 添加我们的sequences
        this.ringBuffer.addGatingSequences(this.workerPool.getWorkerSequences());
        //6 启动我们的工作池
        this.workerPool.start(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()/2));
    }

    // 通过 id 获取 Producer
    public MessageProducer getMessageProducer(String producerId){
        MessageProducer messageProducer = this.producers.get(producerId);
        if(null == messageProducer) {
            messageProducer = new MessageProducer(producerId, this.ringBuffer);
            this.producers.put(producerId, messageProducer);
        }
        return messageProducer;
    }

    /**
     * 异常静态类
     *
     */
    static class EventExceptionHandler implements ExceptionHandler<MessageContent> {
        public void handleEventException(Throwable ex, long sequence, MessageContent event) {
        }

        public void handleOnStartException(Throwable ex) {
        }

        public void handleOnShutdownException(Throwable ex) {
        }
    }

}
