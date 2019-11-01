package com.netty_disruptor.chat.server.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

public class SimpleCache {

    //guava缓存
    //声明静态的内存块
    //initialCapacity为缓存的初始化容量
    // maximumSize缓存的最大容量，当超过这个容量，guava的cache就会使用LRU算法
    //expireAfterAccess缓存的有效期为12个小时
    private static LoadingCache<String,byte[]> localCache= CacheBuilder.newBuilder()
            .initialCapacity(1000)
            .maximumSize(10000)
            .expireAfterAccess(12, TimeUnit.HOURS).build(new CacheLoader<String, byte[]>() {
        @Override
        //默认的数据加载实现,当调用get取值的时候，如果key没有对应的值，就调用这个方法进行加载
        public byte[] load(String s) throws Exception {
            return new byte[1];
        }
    });
    //把数据存入缓存
    public static void setKey(String key,byte[] value){
        localCache.put(key, value);
    }
    //通过key取出缓存的value值
    public static byte[] getKey(String key){
        byte[] value=null;
        try{
            value=localCache.get(key);
            if ("null".equals(value)){
                return null;
            }
            return value;
        }catch (Exception e){
            e.printStackTrace();
            //logger.error("localCache get error",e);
        }
        return null;

    }
}
