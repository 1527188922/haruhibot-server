package com.haruhi.botServer.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpResp<T> {

    public static int SUCCESS = 200;
    public static int BUSI_ERROR = 500;//通用业务异常
    public static int SERVER_ERROR = 501;// 服务异常

    private int code;
    private String message;
    private T data;

    public static <T> HttpResp<T> fail(){
        return new HttpResp<>(BUSI_ERROR,null,null);
    }
    public static <T> HttpResp<T> fail(String msg,T data){
        return new HttpResp<>(BUSI_ERROR,msg,data);
    }
    public static <T> HttpResp<T> fail(T data){
        return new HttpResp<>(BUSI_ERROR,null,data);
    }
    public static <T> HttpResp<T> fail(int code,String msg,T data){
        return new HttpResp<>(code,msg,data);
    }

    public static <T> HttpResp<T> success(){
        return new HttpResp<>(SUCCESS,null,null);
    }
    public static <T> HttpResp<T> success(String msg,T data){
        return new HttpResp<>(SUCCESS,msg,data);
    }
    public static <T> HttpResp<T> success(T data){
        return new HttpResp<>(SUCCESS, null, data);
    }
}
