# netty_disruptor
连接-netty + 处理-disruptor

# chat
### 聊天实现
* 1、[client Mian()](https://github.com/chnhc/netty_disruptor/blob/master/chat/src/main/java/com/netty_disruptor/chat/client/Main.java)  
  执行（前后无空格）：  
    --joinRoom 9000 （加入房间）  
    --chat 9000 消息 （发送消息）  
    --outRoom 9000  （退出房间）  
    --10chat 9000 消息  （连续发10条消息）  
 
* 2、 [Server Mian()](https://github.com/chnhc/netty_disruptor/blob/master/chat/src/main/java/com/netty_disruptor/chat/server/Main.java)    
  执行流程：  
    初始化disruptor ：server.initDisruptor();  
    启动netty多端口 ：server.initNettyAndStart(ports);  
    
* 3、[disruptor简易封装](https://github.com/chnhc/netty_disruptor/blob/master/chat/src/main/java/com/netty_disruptor/chat/common/disruptor/DisruptorFactory.java)    
  
 
# protoTest
* 网络序列化定义
    1、[请求](https://github.com/chnhc/netty_disruptor/blob/master/protoTest/src/main/proto/Request.proto)  
    2、[响应](https://github.com/chnhc/netty_disruptor/blob/master/protoTest/src/main/proto/Response.proto)  
    3、[用户](https://github.com/chnhc/netty_disruptor/blob/master/protoTest/src/main/proto/User.proto)  
