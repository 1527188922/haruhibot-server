package com.haruhi.botserver.dictionary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botserver.dictionary.model.DictQueryReq;
import com.haruhi.botserver.dictionary.persistence.entity.DictionarySqlite;
import com.haruhi.botserver.dictionary.persistence.mapper.DictionarySqliteMapper;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DictionaryService {

    @Autowired
    private DictionarySqliteMapper dictionarySqliteMapper;

    public static final Map<String, List<String>> CACHE = new ConcurrentHashMap<>();

    public void refreshCache(){
        synchronized (DictionaryService.class){
            List<DictionarySqlite> list = dictionarySqliteMapper.selectList(null);
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

    public <T> List<T> getList(String key, String regex, Class<T> tClass, List<T> defaultList){
        String inCache = this.getInCache(key, null);
        if (StringUtils.isBlank(inCache)) {
            return defaultList;
        }
        String[] split = inCache.split(regex);

        return Arrays.stream(split).map(e -> {
            if (StringUtils.isEmpty(e)) {
                return null;
            }
            try {
                return convertToType(e, tClass);
            } catch (Exception ex) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }


    private static <T> T convertToType(String value, Class<T> tClass) throws Exception {
        if (tClass == String.class) {
            return tClass.cast(value);
        }
        if (tClass == Integer.class || tClass == int.class) {
            return tClass.cast(Integer.parseInt(value));
        }
        if (tClass == Long.class || tClass == long.class) {
            return tClass.cast(Long.parseLong(value));
        }
        if (tClass == Double.class || tClass == double.class) {
            return tClass.cast(Double.parseDouble(value));
        }
        if (tClass == Float.class || tClass == float.class) {
            return tClass.cast(Float.parseFloat(value));
        }
        if (tClass == Boolean.class || tClass == boolean.class) {
            return tClass.cast(Boolean.parseBoolean(value));
        }
        if (tClass == Short.class || tClass == short.class) {
            return tClass.cast(Short.parseShort(value));
        }
        if (tClass == Byte.class || tClass == byte.class) {
            return tClass.cast(Byte.parseByte(value));
        } else {
            try {
                return (T) tClass.getMethod("valueOf", String.class).invoke(null, value);
            } catch (NoSuchMethodException e) {
                return tClass.getConstructor(String.class).newInstance(value);
            }
        }
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

    public String getInCache(String key, String defaultValue){
        List<String> values = CACHE.get(key);
        if(CollectionUtils.isNotEmpty(values)){
            return values.getFirst();
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
        return dictionarySqliteMapper.deleteByIds(request.stream().map(DictionarySqlite::getId).collect(Collectors.toList()));
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
