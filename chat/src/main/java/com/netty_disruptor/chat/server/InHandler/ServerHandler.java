package com.netty_disruptor.chat.server.InHandler;

import com.netty_disruptor.chat.common.data.MessageContent;
import com.netty_disruptor.chat.common.data.ProtocolId;
import com.netty_disruptor.chat.common.disruptor.DisruptorFactory;
import com.netty_disruptor.chat.common.proto.Request;
import com.netty_disruptor.chat.common.proto.Response;
import com.netty_disruptor.chat.server.itf.ManageChannelHandlerContext;
import com.netty_disruptor.chat.server.itf.ManageChat;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    // 管理 ChannelHandlerContext manager
    private ManageChannelHandlerContext manager;

    // 管理聊天
    private ManageChat manageChat;

    public ServerHandler(ManageChannelHandlerContext manager ,  ManageChat manageChat) {
        this.manager = manager;
        this.manageChat = manageChat;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("ChannelHandlerContext 启动 --- ");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // System.out.println("server channelRead..");
        MessageContent requestData = (MessageContent) msg;

        if (requestData.getProtocolId() == ProtocolId.HEART_BEAT.getId()) {
            // 心跳检测
            //System.out.println(" ---- heart beat ----");
            ReferenceCountUtil.release(msg);
        } else if (requestData.getProtocolId() == ProtocolId.REGISTRY_CHANNEL.getId()) {
            // 注册通道
            userRegisterChannel(Request.RegistryChannel.parseFrom(requestData.bytes()), ctx);
            System.out.println(ctx.channel().remoteAddress() + " 进行通道注册 ");
            ReferenceCountUtil.release(msg);
        } else if(requestData.getProtocolId() == ProtocolId.CHAT_JOIN.getId()){
            Request.SendChatData sendChatData =Request.SendChatData.parseFrom(requestData.bytes());
            // 加入聊天室
            manageChat.joinChatRoom(sendChatData);
            // 通知客户端加入成功
            Response.ResponseRoom responseRoom = Response.ResponseRoom
                    .newBuilder()
                    .setType(1)
                    .setRoomId(sendChatData.getRoomId())
                    .build();
            System.out.println(ctx.channel().remoteAddress() + " 加入房间： "+ sendChatData.getRoomId());
            ctx.writeAndFlush(new MessageContent(ProtocolId.CHAT, responseRoom.toByteArray()));
        }else if(requestData.getProtocolId() == ProtocolId.CHAT_OUT.getId()){
            Request.SendChatData sendChatData =Request.SendChatData.parseFrom(requestData.bytes());
            // 退出聊天室
            manageChat.outChatRoom(sendChatData);
            // 通知客户端退出成功
            Response.ResponseRoom responseRoom = Response.ResponseRoom
                    .newBuilder()
                    .setType(2)
                    .setRoomId(sendChatData.getRoomId())
                    .build();
            System.out.println(ctx.channel().remoteAddress() + " 退出房间： "+ sendChatData.getRoomId());
            ctx.writeAndFlush(new MessageContent(ProtocolId.CHAT_OUT, responseRoom.toByteArray()));

        }else {
            // 提交到 disruptor 处理数据
            //自已的应用服务应该有一个ID生成规则
            String producerId = "code:sessionId:001";
            DisruptorFactory.getInstance()
                    .getMessageProducer(producerId)
                    .onSendData(requestData);

        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    // 为用户注册通道
    private void userRegisterChannel(Request.RegistryChannel registryChannel, ChannelHandlerContext ctx) {
        manager.putHandlerContext(registryChannel.getPort(), registryChannel.getUserID(), ctx);
    }

/*
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("server channelRead..");



        // 提交到 disruptor 处理数据
        //自已的应用服务应该有一个ID生成规则
        String producerId = "code:sessionId:001";
        DisruptorFactory.getInstance()
                .getMessageProducer(producerId)
                .onSendData((MessageContent) msg);


       *//* MessageContent m = (MessageContent) msg;
        Request.RequestData requestData = Request.RequestData.parseFrom(m.bytes());


        if(requestData.getTypeValue() == Request.RequestTypes.RQ_DEFAULT_VALUE){
            System.out.println(ctx.channel().remoteAddress() + "request Message : " + requestData.getMessage() );
            // 释放资源
            ReferenceCountUtil.release(msg);
        }else{

            // 回写数据
            Response.ResponseData responseData = Response.ResponseData
                    .newBuilder()
                    .setType(Response.ResponseTypes.RP_ONE)
                    .setMessage("回调数据")
                    .build();
            // 或者 返回数据
            ctx.writeAndFlush(new MessageContent(1000, responseData.toByteArray()));
            // 释放资源
            ReferenceCountUtil.release(msg);
        }*//*



    }*/

}