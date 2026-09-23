package com.haruhi.botserver.features.anime.client.model.agefans;

import lombok.Data;

@Data
public class NewAnimationTodayResp {
    private String id;
    private Boolean isnew;
    private Integer wd;
    private String name;
    private String mtime;
    private String namefornew;

}
