package com.netty_disruptor.chat.client;

import com.netty_disruptor.chat.common.data.MessageContent;
import com.netty_disruptor.chat.common.data.ProtocolId;
import com.netty_disruptor.chat.common.proto.Request;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

@Sharable
public class ConnectorIdleStateTrigger extends ChannelInboundHandlerAdapter {
    
    private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Heartbeat",
            CharsetUtil.UTF_8));

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {

                Request.RequestData requestData = Request.RequestData
                        .newBuilder()
                        .setMessage("Heartbeat")
                        .setType(Request.RequestTypes.RQ_DEFAULT)
                        .build();

                MessageContent content = new MessageContent(ProtocolId.HEART_BEAT,requestData.toByteArray());

                // write heartbeat to server
                ctx.writeAndFlush(content.encodeToByteBuf());
                //System.out.println("--------Send Heartbeat --------");
            }
        } else {
            // 继续 父类 fireusereventtrigged（）
            super.userEventTriggered(ctx, evt);
        }
    }
}
