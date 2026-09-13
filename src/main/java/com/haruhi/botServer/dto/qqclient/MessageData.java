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
        private String name;

        // json
        private String data;//json格式字符串


        private String type;//音乐平台： qq 163 kugou migu kuwo custom
        private String audio;
        private String title;
        private String image;
        private String content;
}