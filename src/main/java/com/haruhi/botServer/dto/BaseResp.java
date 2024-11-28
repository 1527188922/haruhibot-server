package com.haruhi.botServer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResp<T> implements Serializable {

    private static final long serialVersionUID = 3644594880158646600L;
    public static final String SUCCESS_CODE = "000";
    public static final String SUCCESS_MESSAGE = "成功";
    public static final String FAIL_CODE = "100";

    private String code;
    private String msg;
    private T data;

    public static BaseResp fail(String msg){
        return new BaseResp(FAIL_CODE,msg,null);
    }
    public static BaseResp fail(){
        return new BaseResp(FAIL_CODE,null,null);
    }
    public static <T> BaseResp<T> fail(String msg, T data){
        return new BaseResp<T>(FAIL_CODE,msg,data);
    }
    public static <T> BaseResp<T> success(String msg, T data){
        return new BaseResp<T>(SUCCESS_CODE,msg,data);
    }
    public static <T> BaseResp<T> success(T data){
        return new BaseResp<T>(SUCCESS_CODE,null,data);
    }

}
