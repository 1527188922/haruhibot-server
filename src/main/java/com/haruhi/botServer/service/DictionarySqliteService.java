package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.entity.DictionarySqlite;
import com.haruhi.botServer.mapper.DictionarySqliteMapper;
import com.haruhi.botServer.utils.DateTimeUtil;
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
