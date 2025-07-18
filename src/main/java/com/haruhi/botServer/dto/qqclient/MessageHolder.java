package com.haruhi.botServer.dto.qqclient;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageHolder{
    private String type;
    private MessageData data;
}