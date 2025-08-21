package com.haruhi.botServer.dto.music163;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Song implements Serializable {

    // 歌曲id
    private String id;
    // 歌名
    private String name;
    // 歌手
    private String artists;
    // 专辑名称
    private String albumName;
}
