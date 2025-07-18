package com.haruhi.botServer.dto.qqclient;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncResponse<T> {

    public static final String STATUS_OK = "ok";
    public static final String STATUS_FAILED = "failed";

    private Integer retcode;
    private String status;
    private String echo;
    private String wording;
    private String message;
    private T data;

    private JSONObject raw;
    
    public boolean isSuccess(){
        return retcode != null && retcode == 0 && STATUS_OK.equals(status);
    }
    
    public static SyncResponse failed(){
        SyncResponse syncResponse = new SyncResponse(500, STATUS_FAILED, null, "无响应", "无响应", null, null);
        syncResponse.setRaw(JSONObject.parseObject(JSONObject.toJSONString(syncResponse)));
        return syncResponse;
    }
}
