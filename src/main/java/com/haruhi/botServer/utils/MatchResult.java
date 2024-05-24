package com.haruhi.botServer.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchResult<T> {
    
    private boolean matched;
    private String msg;
    private T data;

    public static <T> MatchResult<T> unmatched(){
        return new MatchResult<>(false,null,null);
    }
    public static <T> MatchResult<T> unmatched(String msg,T data){
        return new MatchResult<>(false,msg,data);
    }
    public static <T> MatchResult<T> unmatched(T data){
        return new MatchResult<>(false,null,data);
    }

    public static <T> MatchResult<T> matched(){
        return new MatchResult<>(true,null,null);
    }
    public static <T> MatchResult<T> matched(String msg,T data){
        return new MatchResult<>(true,msg,data);
    }
    public static <T> MatchResult<T> matched(T data){
        return new MatchResult<>(true, null, data);
    }
    
}
