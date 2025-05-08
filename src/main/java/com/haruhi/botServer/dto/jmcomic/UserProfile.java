package com.haruhi.botServer.dto.jmcomic;

import lombok.Data;

import java.util.List;

@Data
public class UserProfile {
    private String uid;
    private String username;
    private String email;
    private String emailVerified;
    private String photo;
    private String fname;
    private String gender;
    private String message;
    private String coin;
    private Integer albumFavorites;
    private String s;
    private String levelName;
    private Integer level;
    private Integer nextLevelExp;
    private String exp;
    private Double expPercent;
    private List<Object> badges;
    private Integer albumFavoritesMax;
    private Boolean adFree;
    private String adFreeBefore;
    private String charge;
    private String jar;
    private String invitationQrcode;
    private String invitationUrl;
    private String invitedCnt;
    private String jwttoken;

}
