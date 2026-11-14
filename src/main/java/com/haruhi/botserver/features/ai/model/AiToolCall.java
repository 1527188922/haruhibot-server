package com.haruhi.botserver.features.ai.model;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 模型请求的一次工具调用。
 */
@Data
public class AiToolCall {

    private String name;
    private JSONObject args;
}
