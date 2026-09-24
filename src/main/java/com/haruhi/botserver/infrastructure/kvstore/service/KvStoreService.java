package com.haruhi.botserver.infrastructure.kvstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botserver.infrastructure.kvstore.model.KvQuery;
import com.haruhi.botserver.infrastructure.kvstore.persistence.entity.KvEntry;
import com.haruhi.botserver.infrastructure.kvstore.persistence.mapper.KvEntryMapper;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;

import java.util.*;

import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KvStoreService {

    private final KvEntryMapper kvEntryMapper;

    private volatile Map<String, List<String>> cache = Collections.emptyMap();

    /** Replace the complete cache snapshot; readers never observe a partially refreshed map. */
    public synchronized void refreshCache() {
        List<KvEntry> entries = kvEntryMapper.selectList(null);
        cache = CollectionUtils.isEmpty(entries) ? Collections.emptyMap()
                : Collections.unmodifiableMap(entries.stream().collect(Collectors.groupingBy(
                        KvEntry::getKey, Collectors.mapping(KvEntry::getContent, Collectors.toList()))));
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
        List<String> values = cache.get(key);
        if(CollectionUtils.isNotEmpty(values)){
            return values.getFirst();
        }
        return defaultValue;
    }


    public KvEntry getOne(String key){
        LambdaQueryWrapper<KvEntry> queryWrapper = new LambdaQueryWrapper<KvEntry>()
                .eq(KvEntry::getKey, key)
                .orderByDesc(KvEntry::getModifyTime)
                .last("LIMIT 1");
        return kvEntryMapper.selectOne(queryWrapper);
    }

    public String get(String key){
        KvEntry one = getOne(key);
        return one != null ? one.getContent() : null;
    }

    public List<KvEntry> getList(String key){
        LambdaQueryWrapper<KvEntry> queryWrapper = new LambdaQueryWrapper<KvEntry>()
                .eq(StringUtils.isNotBlank(key), KvEntry::getKey, key);
        return kvEntryMapper.selectList(queryWrapper);
    }

    public List<String> getValues(String key){
        List<KvEntry> list = getList(key);
        if(CollectionUtils.isEmpty(list)){
            return null;
        }
        return list.stream().map(KvEntry::getContent).collect(Collectors.toList());
    }

    /**
     * 存在则更新 不存在则新增
     * @param key
     * @param content
     */
    public void put(String key, String content) {
        put(key, content, null);
    }

    /** The remark is applied only when inserting a new key. Existing metadata is preserved. */
    public void put(String key, String content, String remark) {
        LambdaQueryWrapper<KvEntry> queryWrapper = new LambdaQueryWrapper<KvEntry>()
                .eq(KvEntry::getKey, key);
        Long count = kvEntryMapper.selectCount(queryWrapper);

        KvEntry dictionary = new KvEntry();
        if(count > 0){
            dictionary.setContent(content);
            dictionary.setModifyTime(DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
            kvEntryMapper.update(dictionary,queryWrapper);
        }else{
            dictionary.setKey(key);
            dictionary.setRemark(remark);
            dictionary.setContent(content);
            String date = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
            dictionary.setCreateTime(date);
            dictionary.setModifyTime(date);
            kvEntryMapper.insert(dictionary);
        }
    }

    /**
     * 新增一条数据
     * @param key
     * @param content
     */
    public void add(String key,String content){
        KvEntry dictionary = new KvEntry();
        dictionary.setKey(key);
        dictionary.setContent(content);
        String date = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        dictionary.setCreateTime(date);
        dictionary.setModifyTime(date);
        kvEntryMapper.insert(dictionary);
    }

    public int add(KvEntry request){
        request.setId(null);
        request.setContent(request.getContent() != null ? request.getContent() : "");
        String date = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        request.setCreateTime(date);
        request.setModifyTime(date);
        return kvEntryMapper.insert(request);
    }

    public int update(KvEntry request) {
        request.setContent(request.getContent() != null ? request.getContent() : "");
        request.setModifyTime(DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));

        LambdaUpdateWrapper<KvEntry> updateWrapper = new LambdaUpdateWrapper<KvEntry>()
                .eq(KvEntry::getId, request.getId())
                .set(KvEntry::getKey, request.getKey())
                .set(KvEntry::getContent, request.getContent())
                .set(KvEntry::getRemark, request.getRemark())
                .set(KvEntry::getModifyTime, request.getModifyTime());
        return kvEntryMapper.update(updateWrapper);
    }

    public int deleteBatch(List<KvEntry> request) {
        if (CollectionUtils.isEmpty(request)) {
            return 0;
        }
        return kvEntryMapper.deleteByIds(request.stream().map(KvEntry::getId).collect(Collectors.toList()));
    }

    public int remove(String key){
        return kvEntryMapper.delete(new LambdaQueryWrapper<KvEntry>()
                .eq(KvEntry::getKey, key));
    }

    public int removeByValue(String content){
        return kvEntryMapper.delete(new LambdaQueryWrapper<KvEntry>()
                .eq(KvEntry::getContent, content));
    }

    public boolean containsKey(String key){
        LambdaQueryWrapper<KvEntry> queryWrapper = new LambdaQueryWrapper<KvEntry>()
                .eq(KvEntry::getKey, key);
        return kvEntryMapper.selectCount(queryWrapper) > 0;
    }

    public boolean containsValue(String content){
        LambdaQueryWrapper<KvEntry> queryWrapper = new LambdaQueryWrapper<KvEntry>()
                .eq(KvEntry::getContent, content);
        return kvEntryMapper.selectCount(queryWrapper) > 0;
    }


    public IPage<KvEntry> search(KvQuery request, boolean isPage) {
        LambdaQueryWrapper<KvEntry> queryWrapper = new LambdaQueryWrapper<KvEntry>()
                .like(StringUtils.isNotBlank(request.getKey()),KvEntry::getKey,request.getKey())
                .like(StringUtils.isNotBlank(request.getContent()),KvEntry::getContent,request.getContent())
                .like(StringUtils.isNotBlank(request.getRemark()),KvEntry::getRemark,request.getRemark())
                .orderByAsc(KvEntry::getKey)
                .orderByDesc(KvEntry::getModifyTime);

        IPage<KvEntry> pageInfo = null;
        if (isPage) {
            pageInfo = kvEntryMapper.selectPage(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);
        }else{
            pageInfo = new Page<>(request.getCurrentPage(), request.getPageSize());
            List<KvEntry> list = kvEntryMapper.selectList(queryWrapper);
            pageInfo.setRecords(list);
            pageInfo.setTotal(list.size());
        }
        return pageInfo;
    }
}
