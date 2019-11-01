package com.netty_disruptor.chat.server.itf;

import com.netty_disruptor.chat.common.proto.Request;
import com.netty_disruptor.chat.common.proto.User;

public interface ManageChat {

    void joinChatRoom(Request.SendChatData sendChatData);

    void outChatRoom(Request.SendChatData sendChatData);

    void sendChatRoom(Request.SendChatData sendChatData);
}
