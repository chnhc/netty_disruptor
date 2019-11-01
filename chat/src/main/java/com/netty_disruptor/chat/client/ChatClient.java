package com.netty_disruptor.chat.client;

import com.netty_disruptor.chat.client.itf.ManagerHandlerHolder;
import com.netty_disruptor.chat.common.data.MessageContent;
import com.netty_disruptor.chat.common.data.ProtocolId;
import com.netty_disruptor.chat.common.proto.Request;
import com.netty_disruptor.chat.common.proto.User;
import com.netty_disruptor.chat.common.utils.Decoder;
import com.netty_disruptor.chat.common.utils.Encoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ChatClient implements ManagerHandlerHolder {

    // 1.1 设置工作线程组
    private static final NioEventLoopGroup group = new NioEventLoopGroup();

    // 1.2 初始化定时器
    protected final HashedWheelTimer timer = new HashedWheelTimer();

    // 1.3 初始化心跳检测
    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();

    // 1.4 保存通道
    private ChannelHandlerContext ctx;

    // 保存用户数据
    private User.OneUser user;

    // 用户 id
    private String userId;


    public ChatClient(String userId, int port, String host) throws Exception {

        this.userId = userId;

        // 2.1、创建辅助类
        Bootstrap bootstrap = new Bootstrap();
        // 2.2、配置辅助类
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                //表示缓存区动态调配（自适应）
                .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
                //缓存区 池化操作
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new LoggingHandler(LogLevel.INFO));

        // 3.1 设置重连检测
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(bootstrap, timer, port, host, true) {

            public ChannelHandler[] handlers() {
                return new ChannelHandler[]{
                        this,
                        new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS),
                        idleStateTrigger,
                        // 设置编码器
                        new Encoder(),
                        // 设置解码器
                        new Decoder(1024 * 1024 * 2, true),
                        new ClientHandler(ChatClient.this, userId)
                };
            }
        };

        // 4.1 初始化连接

        ChannelFuture future;
        //进行连接
        try {
            synchronized (bootstrap) {
                bootstrap.handler(new ChannelInitializer<Channel>() {

                    //初始化channel
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = bootstrap.connect(host, port);
            }

            // 以下代码在synchronized同步块外面是安全的
            future.sync();
        } catch (Throwable t) {
            throw new Exception("connects to  fails", t);
        }

    }

    // 客户端发送数据
    public void sendData(String str) {

        if (ctx != null) {
            // 加入房间
            if (str.startsWith("--joinRoom")) {
                // --joinRoom 9000
                Request.SendChatData sendChatData = Request.SendChatData
                        .newBuilder()
                        .setType(1)
                        .setRoomId(str.split(" ")[1])
                        .setUser(user)
                        .build();


                ctx.writeAndFlush(new MessageContent(ProtocolId.CHAT_JOIN, sendChatData.toByteArray()));

            } else if(str.startsWith("--chat")){
                // --chat 9000 消息
                Request.SendChatData sendChatData = Request.SendChatData
                        .newBuilder()
                        .setType(2)
                        .setRoomId(str.split(" ")[1])
                        .setMessage(str.split(" ")[2])
                        .setUser(user)
                        .build();


                ctx.writeAndFlush(new MessageContent(ProtocolId.CHAT, sendChatData.toByteArray()));
            }else if(str.startsWith("--outRoom")){
                // --outRoom 9000
                Request.SendChatData sendChatData = Request.SendChatData
                        .newBuilder()
                        .setType(0)
                        .setRoomId(str.split(" ")[1])
                        .setUser(user)
                        .build();

                ctx.writeAndFlush(new MessageContent(ProtocolId.CHAT_OUT, sendChatData.toByteArray()));
            }else if(str.startsWith("--10chat")){
                // --10chat 9000 消息
                for(int i = 1 ; i <= 10; i++){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Request.SendChatData sendChatData = Request.SendChatData
                            .newBuilder()
                            .setType(2)
                            .setRoomId(str.split(" ")[1])
                            .setMessage(str.split(" ")[2]+i)
                            .setUser(user)
                            .build();


                    ctx.writeAndFlush(new MessageContent(ProtocolId.CHAT, sendChatData.toByteArray()));
                }

            }


        }

    }


    @Override
    public void activeHandlerHolder(ChannelHandlerContext ctx) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        user = User.OneUser
                .newBuilder()
                .setUserID(this.userId)
                .setPort(inetSocketAddress.getPort())
                .build();
        this.ctx = ctx;
    }

    @Override
    public void removeHandlerHolder() {
        this.ctx.close();
        this.user = null;
        this.ctx = null;
    }

    @Override
    public void getData(Object msg) {

    }
}
