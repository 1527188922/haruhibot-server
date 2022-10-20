package com.haruhi.botServer.dto.searchImage.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class Results implements Serializable {
    public Header header;
    private com.haruhi.botServer.dto.searchImage.response.Data data;
}
