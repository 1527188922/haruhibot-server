package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.entity.Dictionary;
import com.haruhi.botServer.mapper.DictionaryMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DictionaryService {

    @Autowired
    private DictionaryMapper dictionaryMapper;

    public static final Map<String,List<String>> CACHE = new HashMap<>();

    public void refreshCache(){
        synchronized (DictionaryService.class){
            List<Dictionary> list = getList(null);
            CACHE.clear();
            if(CollectionUtils.isEmpty(list)){
                return;
            }
            Map<String, List<String>> collect = list.stream().collect(
                    Collectors.groupingBy(Dictionary::getKey,Collectors.mapping(
                            Dictionary::getContent,
                            Collectors.toList()
                    )));
            CACHE.putAll(collect);
        }
    }


    public Dictionary getOne(String key){
        LambdaQueryWrapper<Dictionary> queryWrapper = new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getKey, key)
                .orderByDesc(Dictionary::getModifyTime)
                .last("LIMIT 1");
        return dictionaryMapper.selectOne(queryWrapper);
    }

    public String get(String key){
        Dictionary one = getOne(key);
        return one != null ? one.getContent() : null;
    }

    public List<Dictionary> getList(String key){
        LambdaQueryWrapper<Dictionary> queryWrapper = new LambdaQueryWrapper<Dictionary>()
                .eq(StringUtils.isNotBlank(key), Dictionary::getKey, key);
        return dictionaryMapper.selectList(queryWrapper);
    }

    public List<String> getValues(String key){
        List<Dictionary> list = getList(key);
        if(CollectionUtils.isEmpty(list)){
            return null;
        }
        return list.stream().map(Dictionary::getContent).collect(Collectors.toList());
    }


    public void put(String key,String content){
        LambdaQueryWrapper<Dictionary> queryWrapper = new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getKey, key);
        Integer count = dictionaryMapper.selectCount(queryWrapper);

        Dictionary dictionary = new Dictionary();
        if(count > 0){
            dictionary.setContent(content);
            dictionary.setModifyTime(new Date());
            dictionaryMapper.update(dictionary,queryWrapper);
        }else{
            dictionary.setKey(key);
            dictionary.setContent(content);
            Date date = new Date();
            dictionary.setCreateTime(date);
            dictionary.setModifyTime(date);
            dictionaryMapper.insert(dictionary);
        }
    }


    public void add(String key,String content){
        Dictionary dictionary = new Dictionary();
        dictionary.setKey(key);
        dictionary.setContent(content);
        Date date = new Date();
        dictionary.setCreateTime(date);
        dictionary.setModifyTime(date);
        dictionaryMapper.insert(dictionary);
    }

    public int remove(String key){
        return dictionaryMapper.delete(new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getKey, key));
    }

    public int removeByValue(String content){
        return dictionaryMapper.delete(new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getContent, content));
    }

    public boolean containsKey(String key){
        LambdaQueryWrapper<Dictionary> queryWrapper = new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getKey, key);
        return dictionaryMapper.selectCount(queryWrapper) > 0;
    }

    public boolean containsValue(String content){
        LambdaQueryWrapper<Dictionary> queryWrapper = new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getContent, content);
        return dictionaryMapper.selectCount(queryWrapper) > 0;
    }
}
