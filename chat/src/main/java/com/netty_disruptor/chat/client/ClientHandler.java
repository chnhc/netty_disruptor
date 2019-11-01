package com.netty_disruptor.chat.client;

import com.netty_disruptor.chat.client.itf.ManagerHandlerHolder;
import com.netty_disruptor.chat.common.data.MessageContent;
import com.netty_disruptor.chat.common.data.ProtocolId;
import com.netty_disruptor.chat.common.proto.Request;
import com.netty_disruptor.chat.common.proto.Response;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.util.Date;

@Sharable
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private ManagerHandlerHolder manager;

    private String userId;

    public ClientHandler(ManagerHandlerHolder manager, String userId) {
        this.manager = manager;
        this.userId = userId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 通道连接成功，通知服务器，注册成功
        this.manager.activeHandlerHolder(ctx);

        System.out.println("HeartBeatClientHandler channelActive 激活时间是："+new Date());

        InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        System.out.println("注册通道 ，端口port : "+ inetSocketAddress.getPort());

        Request.RegistryChannel registryChannel = Request.RegistryChannel
                .newBuilder()
                .setUserID(userId)
                .setPort(inetSocketAddress.getPort())
                .build();

        MessageContent content = new MessageContent(ProtocolId.REGISTRY_CHANNEL, registryChannel.toByteArray());

        // write heartbeat to server
        ctx.writeAndFlush(content);


    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        manager.removeHandlerHolder();
        System.out.println("停止时间是："+new Date());
        System.out.println("HeartBeatClientHandler channelInactive");
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MessageContent responseData = (MessageContent) msg;


        if (responseData.getProtocolId() == ProtocolId.CHAT.getId()) {
            Response.ResponseRoom responseRoom =Response.ResponseRoom.parseFrom(responseData.bytes());
            if(responseRoom.getType() == 0){
                // 是否加入成功
                System.out.println("加入房间失败，请重试!" );

            }else if(responseRoom.getType() == 1){
                // 是否加入成功
                System.out.println("成功加入房间 : " + responseRoom.getRoomId() );

            }else if(responseRoom.getType() == 2){
                // 接收到房间消息
                System.out.println("接收到来自房间 : " + responseRoom.getRoomId() + " , 发送者" + responseRoom.getUser().getUserID() +" , 信息为 : "+
                        responseRoom.getMessage());

            }else if(responseRoom.getType() == 3){
                // 消息发送成功
                System.out.println("消息发送成功");

            }else if(responseRoom.getType() == 4){
                // 失败提醒
                System.out.println(responseRoom.getMessage());
            }

        } else if (responseData.getProtocolId() == ProtocolId.CHAT_OUT.getId()) {
            Response.ResponseRoom responseRoom =Response.ResponseRoom.parseFrom(responseData.bytes());
            if(responseRoom.getType() == 2){
                System.out.println("退出房间 : "+responseRoom.getRoomId());
            }
        }

        ReferenceCountUtil.release(msg);
        // 回写数据  ctx.writeAndFlush(msg);

    }
}
