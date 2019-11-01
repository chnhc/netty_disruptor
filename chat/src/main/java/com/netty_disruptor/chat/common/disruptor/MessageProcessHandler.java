package com.netty_disruptor.chat.common.disruptor;

import com.lmax.disruptor.WorkHandler;
import com.netty_disruptor.chat.common.data.MessageContent;
import com.netty_disruptor.chat.server.itf.ManageChat;
import com.netty_disruptor.chat.server.itf.ProcessData;

/**
 * 消费者需要 继承
 */
public abstract class MessageProcessHandler implements WorkHandler<MessageContent> {

    // 消费者 ID
    protected String consumerId;

    // 处理数据
    protected ProcessData process;

    // 处理聊天数据
    protected ManageChat manageChat;

    public MessageProcessHandler(String consumerId,ProcessData process, ManageChat manageChat) {
        this.consumerId = consumerId;
        this.process = process;
        this.manageChat = manageChat;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

}
