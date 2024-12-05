package com.haruhi.botServer.dto.news.response;

import com.haruhi.botServer.utils.DateTimeUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 网易新闻响应体
 */
@Data
public class NewsBy163Resp implements Serializable {

    private String sourceId;

    private String template;

    private Integer	riskLevel;

    private Integer upTimes;

    private Date lmodify;

    public void setLmodify(String lmodify) {
        this.lmodify = DateTimeUtil.parseDate(lmodify,"yyyy-MM-dd HH','mm','ss");
    }

    private String source;

    private String postid;

    private String title;

    private String mtime;

    private Integer	hasImg;

    private String topicid;

    private String topic_background;

    private String digest;

    private String boardid;

    private String alias;

    private Integer	hasAD;

    private String imgsrc;

    private String ptime;

    private String daynum;

    private String extraShowFields;

    private Integer	hasHead;

    private Integer	order;

    private Integer	votecount;

    private String hasCover;

    private String docid;

    private String tname;

    private String url_3w;

    private Integer	priority;

    private Integer	downTimes;

    private String url;

    private Integer	quality;

    private Integer	commentStatus;

    private String ename;

    private Integer	replyCount;

    private String ltitle;

    private String hasIcon;

    private String subtitle;

    private String cid;

}
