package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.entity.DictionarySqlite;
import com.haruhi.botServer.mapper.DictionarySqliteMapper;
import com.haruhi.botServer.utils.DateTimeUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
public class DictionarySqliteService {

    @AllArgsConstructor
    @Getter
    public enum DictionaryEnum{

        BILIBILI_COOKIES_SESSDATA("bilibili.cookies.sessdata",null,"b站cookie中获取，用于解析b站视频等需要调用b站api的功能"),
        BILIBILI_COOKIES_BILI_JCT("bilibili.cookies.bili_jct",null,"b站cookie中获取，用于解析b站视频等需要调用b站api的功能"),
        BILIBILI_UPLOAD_VIDEO_DURATION_LIMIT("bilibili.upload_video.duration_limit","10","上传b站视频时长限制，单位分钟"),
        BILIBILI_DOWNLOAD_VIDEO_DURATION_LIMIT("bilibili.download_video.duration_limit","10","下载b站视频时长限制，单位分钟"),
        SAUCENAO_SEARCH_IMAGE__KEY("saucenao.search_image_key",null,"用于请求识图接口认证,从https://saucenao.com获取"),
        QIANWEN_API_KEY("qianwen.api_key",null,"请求阿里巴巴千问模型认证"),

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
}
