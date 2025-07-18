package com.haruhi.botServer.dto.qqclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageData{
        // image
        private String file;// video
        private String url;
        private String fileSize;

        // reply
        private String id;

        // text
        private String text;
        // at
        private String qq;

        // json
        private String data;//json格式字符串
}