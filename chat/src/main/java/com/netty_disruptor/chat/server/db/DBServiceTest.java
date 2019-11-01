package com.netty_disruptor.chat.server.db;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * description:
 * author: ckk
 * create: 2019-05-23 22:23
 */

public class DBServiceTest {
    public static void main(String[] args) throws IOException, SQLException {
        DBService.getInstance().start();

        // statement用来执行SQL语句
        Statement statement = DBService.getInstance().getConnection().createStatement();

        // 要执行的SQL语句id和content是表review中的项。
        String sql = "select * from name";

        // 得到结果
        ResultSet rs = statement.executeQuery(sql);



        while(rs.next()){
            int id = rs.getInt("id");
            String name = rs.getString("name");
            System.out.println("id:"+id+" 姓名："+name);
        }

        rs.close();
        DBService.getInstance().stop();
    }

    /*
       博客地址 ： https://blog.csdn.net/rushkid02/article/details/7481818
       -- executeQuery
            用于产生单个结果集的语句，例如 SELECT 语句。
            被使用最多的执行 SQL 语句的方法是 executeQuery。
            这个方法被用来执行 SELECT 语句，它几乎是使用最多的 SQL 语句
       -- executeUpdate
            用于执行 INSERT、UPDATE 或 DELETE 语句以及 SQL DDL（数据定义语言）语句，例如 CREATE TABLE 和 DROP TABLE。
            INSERT、UPDATE 或 DELETE 语句的效果是修改表中零行或多行中的一列或多列。
            executeUpdate 的返回值是一个整数，指示受影响的行数（即更新计数）。
            对于 CREATE TABLE 或 DROP TABLE 等不操作行的语句，executeUpdate 的返回值总为零。

            使用executeUpdate方法是因为在 createTableCoffees 中的 SQL 语句是 DDL （数据定义语言）语句。
            创建表，改变表，删除表都是 DDL 语句的例子，要用 executeUpdate 方法来执行。
            你也可以从它的名字里看出，方法 executeUpdate 也被用于执行更新表 SQL 语句。
            实际上，相对于创建表来说，executeUpdate 用于更新表的时间更多，因为表只需要创建一次，但经常被更新。

        -- execute
            用于执行返回多个结果集、多个更新计数或二者组合的语句。因为多数程序员不会需要该高级功能
    * */
}
