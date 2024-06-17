package com.haruhi.botServer.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpResp<T> {

    private int code;
    private String message;
    private T data;

    public static <T> HttpResp<T> fail(){
        return new HttpResp<>(500,null,null);
    }
    public static <T> HttpResp<T> fail(String msg,T data){
        return new HttpResp<>(500,msg,data);
    }
    public static <T> HttpResp<T> fail(T data){
        return new HttpResp<>(500,null,data);
    }

    public static <T> HttpResp<T> success(){
        return new HttpResp<>(200,null,null);
    }
    public static <T> HttpResp<T> success(String msg,T data){
        return new HttpResp<>(200,msg,data);
    }
    public static <T> HttpResp<T> success(T data){
        return new HttpResp<>(200, null, data);
    }
}
