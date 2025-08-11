package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botServer.entity.DictionarySqlite;
import com.haruhi.botServer.mapper.DictionarySqliteMapper;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.vo.DictQueryReq;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class DictionarySqliteService {

    @AllArgsConstructor
    @Getter
    public enum DictionaryEnum{

        BILIBILI_COOKIES_SESSDATA("bilibili.cookies.sessdata",null,"b站cookie中获取，用于解析b站视频等需要调用b站api的功能"),
        BILIBILI_COOKIES_BILI_JCT("bilibili.cookies.bili_jct",null,"b站cookie中获取，用于解析b站视频等需要调用b站api的功能"),
        BILIBILI_UPLOAD_VIDEO_DURATION_LIMIT("bilibili.upload_video.duration_limit","600","上传b站视频时长限制，单位秒"),
        BILIBILI_DOWNLOAD_VIDEO_DURATION_LIMIT("bilibili.download_video.duration_limit","600","下载b站视频时长限制，单位秒"),

        SAUCENAO_SEARCH_IMAGE__KEY("saucenao.search_image_key",null,"用于请求识图接口认证,从https://saucenao.com获取"),

        QIANWEN_API_KEY("qianwen.api_key",null,"请求阿里巴巴千问模型认证"),

        JM_PASSWORD_ZIP("jm.password.zip",JmcomicService.JM_DEFAULT_PASSWORD,"jm本子zip包解压密码,注意：修改密码不会改变之前已经存在的zip包密码，重复下载可重新生成使用新密码的zip包"),
        JM_PASSWORD_PDF("jm.password.pdf",JmcomicService.JM_DEFAULT_PASSWORD,"jm本子pdf保护密码,注意：修改密码不会改变之前已经存在的pdf文件密码，重复下载可重新生成使用新密码的pdf文件"),
        JM_ALBUM_NAME_MAX_LENGTH("jm.album_name_max_length","215","jm本子名称最大长度(注意是名称的bytes.length而非名称字符个数！)，由于使用本子名做为下载后的本子文件名，文件名有长度限制，linux系统文件名称最长支持255，注意：这里配置要小于255，预留出文件名称后面拼接的jm号"),

        BOT_ACCESS_TOKEN("bot.access_token",null,"机器人Websocket服务，建立连接握手时认证token，未配置表示无需认证"),
        BOT_SUPERUSERS("bot.superusers","1527188922","机器人超级管理员qq号，多个qq号逗号分割,注意：未配置超级用户则一些超级用户功能不可使用"),
        BOT_MAX_CONNECTIONS("bot.max_connections","5","机器人Websocket服务最大连接数，小于0表示无限制，0表示禁止连接（改成0不会断开已有连接）"),

        SWITCH_DISABLE_GROUP("switch.disable_group","false","是否禁用所有群功能（对有所有群消息不予理睬，但会保留保存聊天记录功能），true:禁用"),
        SWITCH_QINGYUNKE_CHAT("switch.qingyunke_chat","true","是否启用青云可聊天api，当at机器人或私聊机器人任何命令都未触发时，会触发该api，true:启用"),
        SWITCH_SEARCH_IMAGE_ALLOW_GROUP("switch.search_image_allow_group","true","是否允许群聊中使用识图功能，true:允许"),
        SWITCH_SEARCH_BT_ALLOW_GROUP("switch.search_bt_allow_group","true","是否允许群聊中使用bt搜索功能，true:允许"),
        SWITCH_GROUP_INCREASE("switch.group_increase","true","是否开启加群提示，true:开启"),
        SWITCH_GROUP_DECREASE("switch.group_decrease","true","是否开启群成员离群提示，true:开启"),

        URL_CONF_AGEFANS("url_conf.agefans","https://www.agemys.vip","agefans网站地址，用于今日新番功能，末尾不需斜杠，备用：https://www.age.tv，https://www.agemys.net，https://www.agemys.cc"),
        URL_CONF_BT_SEARCH("url_conf.bt_search","http://www.eclzz.bio","磁力搜索网站地址，用于bt搜索功能，末尾不需斜杠，备用：http://www.eclzz.art\n" +
                "http://www.eclzz.win\n" +
                "http://www.eclzz.love\n" +
                "http://www.eclzz.guru\n" +
                "http://www.eclzz.city\n" +
                "http://www.eclzz.me\n" +
                "http://www.eclzz.ink\n" +
                "http://www.eclzz.mobi\n" +
                "http://www.eclzz.wiki\n" +
                "http://www.eclzz.bio\n" +
                "http://www.eclzz.lat"),


        ;
        private final String key;
        private final String defaultValue;
        private final String remark;
    }

    @Autowired
    private DictionarySqliteMapper dictionarySqliteMapper;

    public static final Map<String, List<String>> CACHE = new HashMap<>();

    public void refreshCache(){
        synchronized (DictionarySqliteService.class){
            List<DictionarySqlite> list = getList(null);
            CACHE.clear();
            if(CollectionUtils.isEmpty(list)){
                return;
            }
            Map<String, List<String>> collect = list.stream().collect(
                    Collectors.groupingBy(DictionarySqlite::getKey,Collectors.mapping(
                            DictionarySqlite::getContent,
                            Collectors.toList()
                    )));
            CACHE.putAll(collect);
        }
    }

    public List<Long> getBotSuperUsers(){
        String superusers = this.getInCache(DictionarySqliteService.DictionaryEnum.BOT_SUPERUSERS.getKey(),null);
        if (StringUtils.isBlank(superusers)) {
            return Collections.emptyList();
        }
        return Arrays.stream(superusers.split("[,，]")).filter(StringUtils::isNotBlank).distinct().map(Long::valueOf).collect(Collectors.toList());
    }

    public String getBotAccessToken(){
        return this.getInCache(DictionarySqliteService.DictionaryEnum.BOT_ACCESS_TOKEN.getKey(),null);
    }
    public int getBotMaxConnections(){
        return getInt(DictionarySqliteService.DictionaryEnum.BOT_MAX_CONNECTIONS.getKey(),0);
    }

    public boolean getBoolean(String key, boolean defaultValue){
        return Boolean.parseBoolean(this.getInCache(key, String.valueOf(defaultValue)).trim());
    }

    public int getInt(String key, int defaultValue){
        try {
            return Integer.parseInt(this.getInCache(key, String.valueOf(defaultValue)).trim());
        }catch (NumberFormatException e){
            return defaultValue;
        }
    }

    public void initData(boolean refreshCache){
        boolean changed = false;
        for (DictionaryEnum value : DictionaryEnum.values()) {
            if (!containsKey(value.getKey())) {
                DictionarySqlite dictionary = new DictionarySqlite();
                dictionary.setKey(value.getKey());
                dictionary.setContent(value.getDefaultValue() != null ? value.getDefaultValue() : "");
                dictionary.setRemark(value.getRemark());
                String date = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
                dictionary.setCreateTime(date);
                dictionary.setModifyTime(date);
                dictionarySqliteMapper.insert(dictionary);
                changed = true;
            }
        }
        if (changed && refreshCache) {
            refreshCache();
        }
    }

    public String getInCache(String key, String defaultValue){
        List<String> values = CACHE.get(key);
        if(CollectionUtils.isNotEmpty(values)){
            return values.get(0);
        }
        return defaultValue;
    }


    public DictionarySqlite getOne(String key){
        LambdaQueryWrapper<DictionarySqlite> queryWrapper = new LambdaQueryWrapper<DictionarySqlite>()
                .eq(DictionarySqlite::getKey, key)
                .orderByDesc(DictionarySqlite::getModifyTime)
                .last("LIMIT 1");
        return dictionarySqliteMapper.selectOne(queryWrapper);
    }

    public String get(String key){
        DictionarySqlite one = getOne(key);
        return one != null ? one.getContent() : null;
    }

    public List<DictionarySqlite> getList(String key){
        LambdaQueryWrapper<DictionarySqlite> queryWrapper = new LambdaQueryWrapper<DictionarySqlite>()
                .eq(StringUtils.isNotBlank(key), DictionarySqlite::getKey, key);
        return dictionarySqliteMapper.selectList(queryWrapper);
    }

    public List<String> getValues(String key){
        List<DictionarySqlite> list = getList(key);
        if(CollectionUtils.isEmpty(list)){
            return null;
        }
        return list.stream().map(DictionarySqlite::getContent).collect(Collectors.toList());
    }

    /**
     * 存在则更新 不存在则新增
     * @param key
     * @param content
     */
    public void put(String key,String content){
        LambdaQueryWrapper<DictionarySqlite> queryWrapper = new LambdaQueryWrapper<DictionarySqlite>()
                .eq(DictionarySqlite::getKey, key);
        Integer count = dictionarySqliteMapper.selectCount(queryWrapper);

        DictionarySqlite dictionary = new DictionarySqlite();
        if(count > 0){
            dictionary.setContent(content);
            dictionary.setModifyTime(DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
            dictionarySqliteMapper.update(dictionary,queryWrapper);
        }else{
            dictionary.setKey(key);
            dictionary.setContent(content);
            String date = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
            dictionary.setCreateTime(date);
            dictionary.setModifyTime(date);
            dictionarySqliteMapper.insert(dictionary);
        }
    }

    /**
     * 新增一条数据
     * @param key
     * @param content
     */
    public void add(String key,String content){
        DictionarySqlite dictionary = new DictionarySqlite();
        dictionary.setKey(key);
        dictionary.setContent(content);
        String date = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        dictionary.setCreateTime(date);
        dictionary.setModifyTime(date);
        dictionarySqliteMapper.insert(dictionary);
    }

    public int add(DictionarySqlite request){
        request.setId(null);
        request.setContent(request.getContent() != null ? request.getContent() : "");
        String date = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        request.setCreateTime(date);
        request.setModifyTime(date);
        return dictionarySqliteMapper.insert(request);
    }

    public int update(DictionarySqlite request) {
        request.setContent(request.getContent() != null ? request.getContent() : "");
        request.setModifyTime(DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
        return dictionarySqliteMapper.updateById(request);
    }

    public int deleteBatch(List<DictionarySqlite> request) {
        if (CollectionUtils.isEmpty(request)) {
            return 0;
        }
        return dictionarySqliteMapper.deleteBatchIds(request.stream().map(DictionarySqlite::getId).collect(Collectors.toList()));
    }

    public int remove(String key){
        return dictionarySqliteMapper.delete(new LambdaQueryWrapper<DictionarySqlite>()
                .eq(DictionarySqlite::getKey, key));
    }

    public int removeByValue(String content){
        return dictionarySqliteMapper.delete(new LambdaQueryWrapper<DictionarySqlite>()
                .eq(DictionarySqlite::getContent, content));
    }

    public boolean containsKey(String key){
        LambdaQueryWrapper<DictionarySqlite> queryWrapper = new LambdaQueryWrapper<DictionarySqlite>()
                .eq(DictionarySqlite::getKey, key);
        return dictionarySqliteMapper.selectCount(queryWrapper) > 0;
    }

    public boolean containsValue(String content){
        LambdaQueryWrapper<DictionarySqlite> queryWrapper = new LambdaQueryWrapper<DictionarySqlite>()
                .eq(DictionarySqlite::getContent, content);
        return dictionarySqliteMapper.selectCount(queryWrapper) > 0;
    }


    public IPage<DictionarySqlite> search(DictQueryReq request, boolean isPage) {
        LambdaQueryWrapper<DictionarySqlite> queryWrapper = new LambdaQueryWrapper<DictionarySqlite>()
                .like(StringUtils.isNotBlank(request.getKey()),DictionarySqlite::getKey,request.getKey())
                .like(StringUtils.isNotBlank(request.getContent()),DictionarySqlite::getContent,request.getContent())
                .like(StringUtils.isNotBlank(request.getRemark()),DictionarySqlite::getRemark,request.getRemark())
                .orderByAsc(DictionarySqlite::getKey)
                .orderByDesc(DictionarySqlite::getModifyTime);

        IPage<DictionarySqlite> pageInfo = null;
        if (isPage) {
            pageInfo = dictionarySqliteMapper.selectPage(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);
        }else{
            pageInfo = new Page<>(request.getCurrentPage(), request.getPageSize());
            List<DictionarySqlite> list = dictionarySqliteMapper.selectList(queryWrapper);
            pageInfo.setRecords(list);
            pageInfo.setTotal(list.size());
        }
        return pageInfo;
    }
}
