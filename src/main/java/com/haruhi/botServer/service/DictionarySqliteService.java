package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.entity.DictionarySqlite;
import com.haruhi.botServer.mapper.DictionarySqliteMapper;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.vo.DictQueryReq;
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



    @Autowired
    private DictionarySqliteMapper dictionarySqliteMapper;

    public static final Map<String, List<String>> CACHE = new HashMap<>();

    public void refreshCache(){
        synchronized (DictionarySqliteService.class){
            List<String> keys = Arrays.stream(DictionaryEnum.values())
                    .filter(DictionaryEnum::isNeedInit)
                    .map(DictionaryEnum::getKey)
                    .collect(Collectors.toList());
            List<DictionarySqlite> list = dictionarySqliteMapper.selectList(new LambdaQueryWrapper<DictionarySqlite>()
                    .in(DictionarySqlite::getKey, keys));
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
        String superusers = this.getInCache(DictionaryEnum.BOT_SUPERUSERS.getKey(),null);
        if (StringUtils.isBlank(superusers)) {
            return Collections.emptyList();
        }
        return Arrays.stream(superusers.split("[,，]")).filter(StringUtils::isNotBlank).distinct().map(Long::valueOf).collect(Collectors.toList());
    }

    public String getBotAccessToken(){
        return this.getInCache(DictionaryEnum.BOT_ACCESS_TOKEN.getKey(),null);
    }
    public int getBotMaxConnections(){
        return getInt(DictionaryEnum.BOT_MAX_CONNECTIONS.getKey(),0);
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
        List<DictionaryEnum> collect = Arrays.stream(DictionaryEnum.values())
                .filter(DictionaryEnum::isNeedInit)
                .collect(Collectors.toList());
        for (DictionaryEnum value : collect) {
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
        Long count = dictionarySqliteMapper.selectCount(queryWrapper);

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

        LambdaUpdateWrapper<DictionarySqlite> updateWrapper = new LambdaUpdateWrapper<DictionarySqlite>()
                .eq(DictionarySqlite::getId, request.getId())
                .set(DictionarySqlite::getKey, request.getKey())
                .set(DictionarySqlite::getContent, request.getContent())
                .set(DictionarySqlite::getRemark, request.getRemark())
                .set(DictionarySqlite::getModifyTime, request.getModifyTime());
        return dictionarySqliteMapper.update(updateWrapper);
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
