package com.netty_disruptor.chat.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * description:
 * author: ckk
 * create: 2019-05-23 22:16
 */

public class DBService {
    private static Logger logger = LoggerFactory.getLogger(DBService.class);

   // private static final String DB_CONFIG_FILE = "/db.properties";

    // 数据库连接数
    private short db_max_conn = 0;

    // 数据库服务器addr
    private String db_url = null;

    // 数据库连接端口
    private short db_port = 0;

    // 数据库名称
    private String db_name = null;

    // 数据库登录用户名
    private String db_username = null;

    // 数据库登录密码
    private String db_password = null;

    // 数据库连接
    private Connection connection;

    HikariDataSource dataSource;

    private static DBService dBService;

    public static DBService getInstance() {
        if (dBService == null) {
            dBService = new DBService();
        }
        return dBService;
    }

    public void start() {
     /*   Properties properties = new Properties();
        InputStream in = DBService.class.getClass().getResourceAsStream(DB_CONFIG_FILE);
        properties.load(in);*/


     /*
     *
     *  db_url = 192.168.199.132
        db_port = 3306
        db_name = mind
        db_max_conn = 100
        db_username = root
        db_password = root
     * */
        db_max_conn = Short.valueOf(String.valueOf(20));
        db_url = String.valueOf("localhost");
        db_port = Short.valueOf("3306");
        db_name = String.valueOf("chat");
        db_username = String.valueOf("root");
        db_password = String.valueOf("root");

        if (db_url == null || db_url.length() == 0) {
            logger.error("配置的数据库ip地址错误!");
            System.exit(0);
        }

        HikariConfig config = new HikariConfig();
        /*config.setMaximumPoolSize(db_max_conn);
        config.setDataSourceClassName(" com.mysql.cj.jdbc.MysqlDataSource");
        config.addDataSourceProperty("serverName", db_url);
        config.addDataSourceProperty("port", db_port);
        config.addDataSourceProperty("databaseName", db_name);
        config.addDataSourceProperty("user", db_username);
        config.addDataSourceProperty("password", db_password);
         dataSource = new HikariDataSource(config);
*/
        // 也可以这样写
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/chat?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&serverTimezone=GMT%2B8");
        config.setUsername(db_username);
        config.setPassword(db_password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        // 设置连接超时为8小时
        config.setConnectionTimeout(8 * 60 * 60);
        this.dataSource = new HikariDataSource(config);
    }


    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            dataSource.resumePool();
            return null;
        }
    }

    public boolean stop() {
        dataSource.close();
        return true;
    }
}