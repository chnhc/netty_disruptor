package com.netty_disruptor.chat.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Test {

    public static void main(String[] args) throws Exception {
        DBService.getInstance().start();

        // 查询
        //check();

        // 插入
        //insert("323", "123".getBytes());

        // 删除
        //remove("323");

        // 更新
        update("323", "5555".getBytes());

        DBService.getInstance().stop();
    }

    private static void update( String roomid , byte[] user) throws SQLException {

        String sql = "Update room_user set userids_bytes = ?  where roomid = ?";
        // statement用来执行SQL语句
        PreparedStatement statement = DBService.getInstance().getConnection().prepareStatement(sql);


        statement.setObject(1, user);
        statement.setObject(2, roomid);

        int i = statement.executeUpdate();
        System.out.println(i);

    }

    private static void remove(String roomid ) throws SQLException {

        String sql = "Delete from room_user where roomid = ?";
        // statement用来执行SQL语句
        PreparedStatement statement = DBService.getInstance().getConnection().prepareStatement(sql);


        statement.setObject(1, roomid);

        int i = statement.executeUpdate();
        System.out.println(i);
    }


    private static void insert( String roomid , byte[] user) throws SQLException {
        String sql = "Insert into room_user (roomid, userids_bytes) values ( ?, ?)";
        // statement用来执行SQL语句
        PreparedStatement statement = DBService.getInstance().getConnection().prepareStatement(sql);


        statement.setObject(1, roomid);
        statement.setObject(2, user);

        int i = statement.executeUpdate();
        System.out.println(i);
    }

    private static void check() throws SQLException {

        // statement用来执行SQL语句
        Statement statement = DBService.getInstance().getConnection().createStatement();

        // 要执行的SQL语句id和content是表review中的项。
        String sql = "select * from room_user";

        // 得到结果
        ResultSet rs = statement.executeQuery(sql);



        while(rs.next()){
            int id = rs.getInt("id");
            String name = rs.getString("roomid");
            System.out.println("id:"+id+" 姓名："+name);
        }

        rs.close();
    }

}
