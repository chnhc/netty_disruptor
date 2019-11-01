package com.netty_disruptor.chat.server;

import com.google.protobuf.InvalidProtocolBufferException;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import com.netty_disruptor.chat.common.data.MessageContent;
import com.netty_disruptor.chat.common.data.ProtocolId;
import com.netty_disruptor.chat.common.disruptor.DisruptorFactory;
import com.netty_disruptor.chat.common.disruptor.MessageProcessHandler;
import com.netty_disruptor.chat.common.exception.ParamException;
import com.netty_disruptor.chat.common.proto.Request;
import com.netty_disruptor.chat.common.proto.Response;
import com.netty_disruptor.chat.common.proto.User;
import com.netty_disruptor.chat.common.utils.Decoder;
import com.netty_disruptor.chat.common.utils.Encoder;
import com.netty_disruptor.chat.server.InHandler.ChannelStateTrigger;
import com.netty_disruptor.chat.server.InHandler.RegistryChannelHandler;
import com.netty_disruptor.chat.server.InHandler.ServerHandler;
import com.netty_disruptor.chat.server.cache.SimpleCache;
import com.netty_disruptor.chat.server.itf.ManageChannelHandlerContext;
import com.netty_disruptor.chat.server.itf.ManageChat;
import com.netty_disruptor.chat.server.itf.ProcessData;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ChatServer implements ManageChannelHandlerContext, ProcessData, ManageChat {

    // 1.1、用于接受网络请求的线程组.
    // 注意：如果是一个端口，则实际只需要一个线程
    private static final NioEventLoopGroup bossGroup = new NioEventLoopGroup();

    // 1.2、用于实际处理业务的线程组
    // 注意：工作线程池，连接的通道 通过轮询算法 注册到工作组中
    private static final NioEventLoopGroup workGroup = new NioEventLoopGroup();

    // 1.3、用于处理通道不活跃时的操作
    private final ChannelStateTrigger channelStateTrigger = new ChannelStateTrigger();

    // 1.4、 创建辅助类
    private ServerBootstrap serverBootstrap;

    // 1.5、 存储异步通道， 可关闭对应port
    private Map<Integer, ChannelFuture> channelFutureMap = new ConcurrentHashMap<Integer, ChannelFuture>();

    // 1.x、保存 通道 ChannelHandlerContext
    // Map<Integer( 端口 ), Map<String( 用户ID ), ChannelHandlerContext( 通道 )>>
    private Map<Integer, Map<String, ChannelHandlerContext>> chcs;


    public ChatServer(int... ports) {
        // 初始化通道集合
        if (chcs == null) {
            chcs = new ConcurrentHashMap<Integer, Map<String, ChannelHandlerContext>>();
        }
        for (int port : ports) {
            chcs.put(port, new ConcurrentHashMap<String, ChannelHandlerContext>());
        }


    }

    // 初始化Disruptor
    public void initDisruptor() {
        // disruptor 初始化
        MessageProcessHandler[] processes = new MessageProcessHandler[4];
        for (int i = 0; i < processes.length; i++) {
            MessageProcessHandler messageProcess = new ServerProcessHandler("code:serverId:" + i, this, this);
            processes[i] = messageProcess;
        }
        DisruptorFactory.getInstance().initAndStart(ProducerType.MULTI,
                1024 * 1024,
                //new YieldingWaitStrategy(),
                new BlockingWaitStrategy(),
                processes);

    }

    // 启动netty
    public void initNettyAndStart(int... ports) throws Exception {
        // 端口判断
        if (ports == null || ports.length == 0) {
            throw new ParamException("posts 必须设置一个或者多个值");
        }


        // 2.1、 创建辅助类
        serverBootstrap = new ServerBootstrap();

        try {
            // 2.2、 配置辅助类
            serverBootstrap.group(bossGroup, workGroup);
            // 设置通道类
            serverBootstrap.channel(NioServerSocketChannel.class);
            // 服务端接受连接的队列长度，如果队列已满，客户端连接将被拒绝。默认值，Windows为200，其他为128。
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            //表示缓存区动态调配（自适应）
            serverBootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
            //缓存区 池化操作
            serverBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            // 日志
            serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));
            // 设置自定义拦截 注册成功
            //serverBootstrap.handler(new RegistryChannelHandler(channelFutureMap));
            // 该值设置Nagle算法的启用，改算法将小的碎片数据连接成更大的报文来最小化所发送的报文的数量，如果需要发送一些较小的报文，则需要禁用该算法。
            serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
            // TCP会主动探测空闲连接的有效性。可以将此功能视为TCP的心跳机制，需要注意的是：默认的心跳间隔是7200s即2小时
            //serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            // 地址复用
            serverBootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
            // 处理数据，为channel设置通道
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    // 获取通道
                    ChannelPipeline pipeline = ch.pipeline();
                    // 设置IdleStateHandler
                    // @param readerIdleTimeSeconds  多长时间 没有读事件，将触发事件， state = IdleState.READER_IDLE
                    // @param readerIdleTimeSeconds  多长时间 没有写事件，将触发事件， state = IdleState.WRITER_IDLE
                    // @param readerIdleTimeSeconds  多长时间 没有读、写事件，将触发事件， state = IdleState.ALL_IDLE
                    // @param TimeUnit  触发上述事件的 时间单位
                    pipeline.addLast(new IdleStateHandler(20, 0, 0, TimeUnit.SECONDS));
                    // 设置处理状态的方法
                    pipeline.addLast(channelStateTrigger);
                    // 设置编码器
                    pipeline.addLast("Encoder", new Encoder());
                    // 设置解码器
                    pipeline.addLast("Decoder", new Decoder(1024 * 1024 * 2, true));
                    // 设置处理数据Handler
                    pipeline.addLast(new ServerHandler(ChatServer.this, ChatServer.this));
                }
            });


            //多个端口绑定
            for (int port : ports) {
                ChannelFuture channelFuture = serverBootstrap.bind(port);
                channelFutureMap.put(port, channelFuture);
                channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        if (future.isSuccess()) {
                            System.out.println("Started success,port:" + port);
                        } else {
                            System.out.println("Started Failed,port:" + port);
                        }
                    }
                });
                final Channel channel = channelFuture.channel();
                channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) {
                        System.out.println("channel close !");
                        channel.close();
                        channelFutureMap.remove(port);
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 关闭所有连接
    public void stopAll() {
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
        System.out.println("stoped");
    }

    /**
     * 关闭单个端口的NioServerSocketChannel
     * 关闭 port 对外的接收
     * 已经连接的客户端仍可发送数据
     */
    public void stopServerChannel(int port) {

        if (channelFutureMap.containsKey(port)) {
            channelFutureMap.get(port).channel().close();
            System.out.println("stoped " + port);
        }

    }

    // 发送数据
    @Override
    public void sendData() {
        //System.out.println("sendData");
    }

    // 加入聊天室
    @Override
    public void joinChatRoom(Request.SendChatData sendChatData) {
        System.out.println("加入聊天室 ： " + sendChatData.getUser().getUserID());
        // 判断是否存在有该聊天室
        if (SimpleCache.getKey(sendChatData.getRoomId()) == null || SimpleCache.getKey(sendChatData.getRoomId()).length == 1) {
            // 不存在 则创建第一个
            User.ManyUser manyUser = User.ManyUser
                    .newBuilder()
                    .addUsers(sendChatData.getUser())
                    .build();
            SimpleCache.setKey(sendChatData.getRoomId(), manyUser.toByteArray());
        } else {
            // 存在 ，则重新设置 聊天室的人数
            try {
                User.ManyUser manyUser = User.ManyUser.parseFrom(SimpleCache.getKey(sendChatData.getRoomId()));
                User.ManyUser newUsers = User.ManyUser
                        .newBuilder()
                        .addUsers(sendChatData.getUser())
                        .addAllUsers(manyUser.getUsersList())
                        .build();

                SimpleCache.setKey(sendChatData.getRoomId(), newUsers.toByteArray());
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    // 退出聊天室
    @Override
    public void outChatRoom(Request.SendChatData sendChatData) {
        System.out.println("退出聊天室 ： " + sendChatData.getUser().getUserID());
        if (SimpleCache.getKey(sendChatData.getRoomId()) == null || SimpleCache.getKey(sendChatData.getRoomId()).length == 1) {
            return;
        } else {
            try {
                // 发送者
                User.OneUser sender = sendChatData.getUser();
                // 对应房间的人群
                User.ManyUser manyUser = User.ManyUser.parseFrom(SimpleCache.getKey(sendChatData.getRoomId()));
                List<User.OneUser>oneUsers = manyUser.getUsersList() ;
                User.ManyUser.Builder newUsersB = User.ManyUser.newBuilder();

                // 循环发送
                for (User.OneUser oneUser : oneUsers) {
                    // 如果等于自己则不发送
                    if (!oneUser.getUserID().equals(sender.getUserID())) {
                        // 重构
                        newUsersB.addUsers(oneUser);
                        // 退出通知
                        Response.ResponseRoom responseRoom = Response.ResponseRoom
                                .newBuilder()
                                .setType(2)
                                .setRoomId(sendChatData.getRoomId())
                                .setMessage(sender.getUserID()+": 退出聊天室")
                                .setUser(sender)
                                .build();
                        // 一个一个发送数据
                        chcs.get(oneUser.getPort()).get(oneUser.getUserID())
                                .writeAndFlush(new MessageContent(ProtocolId.CHAT, responseRoom.toByteArray()));
                    }
                }

                SimpleCache.setKey(sendChatData.getRoomId(), newUsersB.build().toByteArray());


            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    // 发送数据到对应聊天室
    @Override
    public void sendChatRoom(Request.SendChatData sendChatData) {
        System.out.println("发送数据 ");
        if (SimpleCache.getKey(sendChatData.getRoomId()) == null || SimpleCache.getKey(sendChatData.getRoomId()).length == 1) {
            return;
        } else {
            try {
                // 发送者
                User.OneUser sender = sendChatData.getUser();
                // 对应房间的人群
                User.ManyUser manyUser = User.ManyUser.parseFrom(SimpleCache.getKey(sendChatData.getRoomId()));
                // 判断用户是否存在
                boolean isExist = false;
                for (User.OneUser oneUser : manyUser.getUsersList()) {
                    if(oneUser.getUserID().equals(sender.getUserID())){
                        isExist = true;
                    }
                }
                // 聊天室不存在，该用户，销毁这条消息，并通知用户，需要加入
                if(!isExist){
                    // 通知发送者 发送成功
                    Response.ResponseRoom success = Response.ResponseRoom
                            .newBuilder()
                            .setType(4)
                            .setRoomId(sendChatData.getRoomId())
                            .setMessage("消息发送失败！请加入房间："+sendChatData.getRoomId())
                            .build();
                    chcs.get(sender.getPort()).get(sender.getUserID())
                            .writeAndFlush(new MessageContent(ProtocolId.CHAT, success.toByteArray()));

                }else {
                    // 循环发送
                    for (User.OneUser oneUser : manyUser.getUsersList()) {
                        // 如果等于自己则不发送
                        if (!oneUser.getUserID().equals(sender.getUserID())) {
                            Response.ResponseRoom responseRoom = Response.ResponseRoom
                                    .newBuilder()
                                    .setType(2)
                                    .setRoomId(sendChatData.getRoomId())
                                    .setMessage(sendChatData.getMessage())
                                    .setUser(sender)
                                    .build();
                            // 一个一个发送数据
                            chcs.get(oneUser.getPort()).get(oneUser.getUserID())
                                    .writeAndFlush(new MessageContent(ProtocolId.CHAT, responseRoom.toByteArray()));
                        }
                    }
                    // 通知发送者 发送成功
                    Response.ResponseRoom success = Response.ResponseRoom
                            .newBuilder()
                            .setType(2)
                            .setRoomId(sendChatData.getRoomId())
                            .setMessage("房间发送成功")
                            .build();
                    chcs.get(sender.getPort()).get(sender.getUserID())
                            .writeAndFlush(new MessageContent(ProtocolId.CHAT, success.toByteArray()));
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
        ReferenceCountUtil.release(sendChatData);
    }


    // 存储 cxt
    @Override
    public void putHandlerContext(Integer port, String userId, ChannelHandlerContext channelHandlerContext) {
        // 是否存在端口
        if (chcs.containsKey(port)) {
            //  用户 是否已经注册
            Map<String, ChannelHandlerContext> map = chcs.get(port);
            if (!map.containsKey(userId)) {
                // 没有注册，注册下
                map.put(userId, channelHandlerContext);
            }
        }
    }

    // 移除 cxt
    @Override
    public void removeHandlerContext(Integer port, String userId) {

    }




    /*
    Channel配置参数

    (1).通用参数
        CONNECT_TIMEOUT_MILLIS :
          Netty参数，连接超时毫秒数，默认值30000毫秒即30秒。

        MAX_MESSAGES_PER_READ
          Netty参数，一次Loop读取的最大消息数，对于ServerChannel或者NioByteChannel，
          默认值为16，其他Channel默认值为1。默认值这样设置，是因为：ServerChannel需要接受足够多的连接，
          保证大吞吐量，NioByteChannel可以减少不必要的系统调用select。

        WRITE_SPIN_COUNT
          Netty参数，一个Loop写操作执行的最大次数，默认值为16。
          也就是说，对于大数据量的写操作至多进行16次，如果16次仍没有全部写完数据，
          此时会提交一个新的写任务给EventLoop，任务将在下次调度继续执行。
          这样，其他的写请求才能被响应不会因为单个大数据量写请求而耽误。

        ALLOCATOR
          Netty参数，ByteBuf的分配器，默认值为ByteBufAllocator.DEFAULT，
          4.0版本为UnpooledByteBufAllocator，4.1版本为PooledByteBufAllocator。
          该值也可以使用系统参数io.netty.allocator.type配置，使用字符串值："unpooled"，"pooled"。

        RCVBUF_ALLOCATOR
          Netty参数，用于Channel分配接受Buffer的分配器，
          默认值为AdaptiveRecvByteBufAllocator.DEFAULT，
          是一个自适应的接受缓冲区分配器，能根据接受到的数据自动调节大小。
          可选值为FixedRecvByteBufAllocator，固定大小的接受缓冲区分配器。

        AUTO_READ
          Netty参数，自动读取，默认值为True。Netty只在必要的时候才设置关心相应的I/O事件。
          对于读操作，需要调用channel.read()设置关心的I/O事件为OP_READ，这样若有数据到达才能读取以供用户处理。
          该值为True时，每次读操作完毕后会自动调用channel.read()，从而有数据到达便能读取；
          否则，需要用户手动调用channel.read()。
          需要注意的是：当调用config.setAutoRead(boolean)方法时，
          如果状态由false变为true，将会调用channel.read()方法读取数据；
          由true变为false，将调用config.autoReadCleared()方法终止数据读取。

        WRITE_BUFFER_HIGH_WATER_MARK
          Netty参数，写高水位标记，默认值64KB。如果Netty的写缓冲区中的字节超过该值，Channel的isWritable()返回False。

        WRITE_BUFFER_LOW_WATER_MARK
          Netty参数，写低水位标记，默认值32KB。当Netty的写缓冲区中的字节超过高水位之后若下降到低水位，
          则Channel的isWritable()返回True。写高低水位标记使用户可以控制写入数据速度，从而实现流量控制。
          推荐做法是：每次调用channl.write(msg)方法首先调用channel.isWritable()判断是否可写。

        MESSAGE_SIZE_ESTIMATOR
          Netty参数，消息大小估算器，默认为DefaultMessageSizeEstimator.DEFAULT。
          估算ByteBuf、ByteBufHolder和FileRegion的大小，其中ByteBuf和ByteBufHolder为实际大小，
          FileRegion估算值为0。该值估算的字节数在计算水位时使用，FileRegion为0可知FileRegion不影响高低水位。

        SINGLE_EVENTEXECUTOR_PER_GROUP
          Netty参数，单线程执行ChannelPipeline中的事件，默认值为True。
        该值控制执行ChannelPipeline中执行ChannelHandler的线程。如果为Trye，
        整个pipeline由一个线程执行，这样不需要进行线程切换以及线程同步，
        是Netty4的推荐做法；如果为False，ChannelHandler中的处理过程会由Group中的不同线程执行。



    (2).SocketChannel参数

        SO_RCVBUF
          Socket参数，TCP数据接收缓冲区大小。该缓冲区即TCP接收滑动窗口，
          linux操作系统可使用命令：cat /proc/sys/net/ipv4/tcp_rmem查询其大小。
          一般情况下，该值可由用户在任意时刻设置，但当设置值超过64KB时，需要在连接到远端之前设置。

        SO_SNDBUF
          Socket参数，TCP数据发送缓冲区大小。该缓冲区即TCP发送滑动窗口，
          linux操作系统可使用命令：cat /proc/sys/net/ipv4/tcp_smem查询其大小。

        TCP_NODELAY
          TCP参数，立即发送数据，默认值为Ture（Netty默认为True而操作系统默认为False）。
          该值设置Nagle算法的启用，改算法将小的碎片数据连接成更大的报文来最小化所发送的报文的数量，
          如果需要发送一些较小的报文，则需要禁用该算法。Netty默认禁用该算法，从而最小化报文传输延时。

        SO_KEEPALIVE
          Socket参数，连接保活，默认值为False。启用该功能时，TCP会主动探测空闲连接的有效性。
          可以将此功能视为TCP的心跳机制，需要注意的是：默认的心跳间隔是7200s即2小时。Netty默认关闭该功能。

        SO_REUSEADDR
          Socket参数，地址复用，默认值False。有四种情况可以使用：
          (1).当有一个有相同本地地址和端口的socket1处于TIME_WAIT状态时，
              而你希望启动的程序的socket2要占用该地址和端口，比如重启服务且保持先前端口。
          (2).有多块网卡或用IP Alias技术的机器在同一端口启动多个进程，但每个进程绑定的本地IP地址不能相同。
          (3).单个进程绑定相同的端口到多个socket上，但每个socket绑定的ip地址不同。
          (4).完全相同的地址和端口的重复绑定。但这只用于UDP的多播，不用于TCP。

        SO_LINGER
          Socket参数，关闭Socket的延迟时间，默认值为-1，表示禁用该功能。
          -1表示socket.close()方法立即返回，但OS底层会将发送缓冲区全部发送到对端。
          0表示socket.close()方法立即返回，OS放弃发送缓冲区的数据直接向对端发送RST包，对端收到复位错误。
          非0整数值表示调用socket.close()方法的线程被阻塞直到延迟时间到或发送缓冲区中的数据发送完毕，若超时，则对端会收到复位错误。

        IP_TOS
          IP参数，设置IP头部的Type-of-Service字段，用于描述IP包的优先级和QoS选项。

        ALLOW_HALF_CLOSURE
          Netty参数，一个连接的远端关闭时本地端是否关闭，默认值为False。
          值为False时，连接自动关闭；
          为True时，触发ChannelInboundHandler的userEventTriggered()方法，事件为ChannelInputShutdownEvent。



    (3).ServerSocketChannel参数

        SO_RCVBUF
          已说明，需要注意的是：当设置值超过64KB时，需要在绑定到本地端口前设置。
          该值设置的是由ServerSocketChannel使用accept接受的SocketChannel的接收缓冲区。

        SO_REUSEADDR
          已说明

        SO_BACKLOG
          Socket参数，服务端接受连接的队列长度，如果队列已满，客户端连接将被拒绝。默认值，Windows为200，其他为128。


    (4).DatagramChannel参数
    SO_BROADCAST: Socket参数，设置广播模式。

        SO_RCVBUF: 已说明

        SO_SNDBUF:已说明

        SO_REUSEADDR:已说明

        IP_MULTICAST_LOOP_DISABLED:
          对应IP参数IP_MULTICAST_LOOP，设置本地回环接口的多播功能。由于IP_MULTICAST_LOOP返回True表示关闭，所以Netty加上后缀_DISABLED防止歧义。

        IP_MULTICAST_ADDR:
          对应IP参数IP_MULTICAST_IF，设置对应地址的网卡为多播模式。

        IP_MULTICAST_IF:
          对应IP参数IP_MULTICAST_IF2，同上但支持IPV6。

        IP_MULTICAST_TTL:
          IP参数，多播数据报的time-to-live即存活跳数。

        IP_TOS:
          已说明

        DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION:
          Netty参数，DatagramChannel注册的EventLoop即表示已激活。





     */


}
