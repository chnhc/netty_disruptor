package com.netty_disruptor.chat.server.cache;

import com.google.protobuf.InvalidProtocolBufferException;
import com.netty_disruptor.chat.common.proto.User;

public class TestCache {

    public static void main(String[] args) throws InvalidProtocolBufferException {
        System.out.println( SimpleCache.getKey("111").length);

        User.ManyUser.Builder builder = User.ManyUser.newBuilder();

        User.OneUser oneUser = User.OneUser
                .newBuilder()
                .setPort(123)
                .setUserID("userId:12345")
                .build();

        builder.addUsers(oneUser);


        SimpleCache.setKey("111",builder.build().toByteArray());

        User.ManyUser manyUser = User.ManyUser.parseFrom(SimpleCache.getKey("111"));

        User.ManyUser.Builder builder1 = manyUser.newBuilderForType();

        User.OneUser oneUser1 = User.OneUser
                .newBuilder()
                .setPort(123)
                .setUserID("userId:123451111")
                .build();

        builder1.addAllUsers(manyUser.getUsersList());
        builder1.addUsers(oneUser1);

        SimpleCache.setKey("111",builder1.build().toByteArray());

        SimpleCache.getKey("111");
    }
}
