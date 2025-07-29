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
