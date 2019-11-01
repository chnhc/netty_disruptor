package com.netty_disruptor.chat.server.InHandler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

/**
 * 获取到通道第一次注册事件，并保存通道实例
 */
@ChannelHandler.Sharable
public class RegistryChannelHandler  extends ChannelDuplexHandler {



    private Map<Integer, ChannelFuture> channelFutureMap;

    public RegistryChannelHandler(Map<Integer, ChannelFuture> channelFutureMap) {
        this.channelFutureMap = channelFutureMap;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx,msg);
        System.out.println("RegistryChannelHandler : "+ msg);
    }


}
