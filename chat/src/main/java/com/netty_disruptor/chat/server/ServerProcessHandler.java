package com.netty_disruptor.chat.server;

import com.netty_disruptor.chat.common.data.MessageContent;
import com.netty_disruptor.chat.common.data.ProtocolId;
import com.netty_disruptor.chat.common.disruptor.MessageProcessHandler;
import com.netty_disruptor.chat.common.proto.Request;
import com.netty_disruptor.chat.server.itf.ManageChat;
import com.netty_disruptor.chat.server.itf.ProcessData;

public class ServerProcessHandler extends MessageProcessHandler {

    public ServerProcessHandler(String consumerId, ProcessData process, ManageChat manageChat) {
        super(consumerId,process, manageChat);
    }

    @Override
    public void onEvent(MessageContent requestData) throws Exception {

        if (requestData.getProtocolId() == ProtocolId.CHAT.getId()) {
            Request.SendChatData sendChatData = Request.SendChatData.parseFrom(requestData.bytes());
          // 聊天信息
            if(sendChatData.getType() == 2){
                // 群发消息
                manageChat.sendChatRoom(sendChatData);
            }
        }

    }

}
