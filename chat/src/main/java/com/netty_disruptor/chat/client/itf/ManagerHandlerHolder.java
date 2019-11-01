package com.netty_disruptor.chat.client.itf;

import io.netty.channel.ChannelHandlerContext;

public interface ManagerHandlerHolder {

    void activeHandlerHolder(ChannelHandlerContext ctx);

    void removeHandlerHolder();

    void getData(Object msg);

}
