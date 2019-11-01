package com.netty_disruptor.chat.common.data;

public enum ProtocolId {

    // 注册通道
    REGISTRY_CHANNEL(100000),

    // 聊天室
    CHAT(600000),
    CHAT_OUT(600004),
    CHAT_JOIN(600001),

    // 心跳检测
    HEART_BEAT(700001);


    private int id ;


    ProtocolId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


}
