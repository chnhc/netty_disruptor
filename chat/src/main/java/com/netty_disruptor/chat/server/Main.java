package com.netty_disruptor.chat.server;

public class Main {


    public static void main(String[] args) throws Exception {

        int[] ports = new int[]{30010, 30011, 30012};

        // netty 启动
        ChatServer server = new ChatServer(ports);

        server.initDisruptor();
        server.initNettyAndStart(ports);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }

        //server.stopServerChannel(30010);

    }



}
