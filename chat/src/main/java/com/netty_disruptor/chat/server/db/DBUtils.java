package com.netty_disruptor.chat.server.db;

/**
 * description:
 * author: ckk
 * create: 2019-06-04 12:04
 */
public class DBUtils {

    public static boolean isContainArray(String[] arr,String containValue){
        //判断是否为空
        if (arr==null||arr.length==0 || containValue == null){
            return false;
        }
        for (int i = 0, l =arr.length;  i < l; i++) {
            //all null
            if (containValue.equals(arr[i])){
                return true;
            }else if (arr[i]==null){
                return true;
            }
        }
        return false;
    }
}
