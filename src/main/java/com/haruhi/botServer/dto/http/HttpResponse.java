package com.haruhi.botServer.dto.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HttpResponse<T> implements Serializable {
    private transient static final int CODE_SUCCESS = 0;
    private transient static final int CODE_FAIL = -1;
    private transient static final int CODE_ERROR = -2;
    private transient static final String MESSAGE_SUCCESS = "success";
    private transient static final String MESSAGE_ERROR = "fail";
    private int code;
    private String message;
    private T data;


    public static <T> HttpResponse<T> success(){
        return new HttpResponse<T>(CODE_SUCCESS,MESSAGE_SUCCESS,null);
    }
    public static <T> HttpResponse<T> success(T data){
        return new HttpResponse<T>(CODE_SUCCESS,MESSAGE_SUCCESS,data);
    }
    public static <T> HttpResponse<T> success(String message, T data){
        return new HttpResponse<T>(CODE_SUCCESS,message,data);
    }

    public static <T> HttpResponse<T> fail(){
        return new HttpResponse<T>(CODE_FAIL,MESSAGE_ERROR,null);
    }
    public static <T> HttpResponse<T> fail(T data){
        return new HttpResponse<T>(CODE_FAIL,MESSAGE_ERROR,data);
    }
    public static <T> HttpResponse<T> fail(String message, T data){
        return new HttpResponse<T>(CODE_FAIL,message,data);
    }

}
