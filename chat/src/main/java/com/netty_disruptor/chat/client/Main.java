package com.netty_disruptor.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws Exception {

        Random r = new Random();

        int[] post = new int[]{30010, 30011, 30012};

        ChatClient chatClient =  new ChatClient(
                "userId:"+UUID.randomUUID().toString().replaceAll("-", ""),
                post[r.nextInt(3)],
                "127.0.0.1");
        Thread.sleep(5000);

        todo(chatClient);
        // --joinRoom 9000

        //  --chat 9000 消息

        // --outRoom 9000

        // --10chat 9000 消息
}


    private static void todo(ChatClient chatClient) throws IOException {

        boolean isActive = true;
        // 构建键盘输入流
        InputStream in = System.in;
        // 用于接收键盘的输入
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 读取键盘一行的输入
            String str = input.readLine();

            if("bye".equalsIgnoreCase(str)){
                isActive = false;
            }else{
                chatClient.sendData(str);
            }


        }while (isActive);

        input.close();
    }
}
