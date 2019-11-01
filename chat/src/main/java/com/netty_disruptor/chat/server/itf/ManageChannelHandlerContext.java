package com.netty_disruptor.chat.server.itf;

import io.netty.channel.ChannelHandlerContext;

public interface ManageChannelHandlerContext {

    /**
     * 存储 ChannelHandlerContext
     * @param port
     * @param userId
     * @param channelHandlerContext
     */
    void putHandlerContext(Integer port, String userId, ChannelHandlerContext channelHandlerContext);


    /**
     * 移除 ChannelHandlerContext
     * @param port
     * @param userId
     */
    void removeHandlerContext(Integer port, String userId);



}
