package com.haruhi.botServer.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.entity.DictionarySqlite;
import com.haruhi.botServer.mapper.DictionarySqliteMapper;
import com.haruhi.botServer.utils.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * WebUI SQL 编辑器的内容缓存
 * <p>
 * 这不是"配置"，而是用户的操作数据（随手写过的SQL），所以它<b>不进 ./config/*.properties</b>，
 * 仍然存放在数据库 t_dictionary 表中，key 为 {@value #KEY_SQL_CACHE}。
 * <p>
 * 说明：配置管理重构前它也是存在这张表里的，这里保持原有行为不变。
 */
@Slf4j
@Service
public class SqlCacheStore {

    /** 存放SQL编辑器内容的key */
    public static final String KEY_SQL_CACHE = "db.sql_cache";

    @Autowired
    private DictionarySqliteMapper dictionarySqliteMapper;

    /**
     * 读取缓存内容，没有记录返回null
     */
    public String get() {
        try {
            DictionarySqlite one = dictionarySqliteMapper.selectOne(new LambdaQueryWrapper<DictionarySqlite>()
                    .eq(DictionarySqlite::getKey, KEY_SQL_CACHE)
                    .orderByDesc(DictionarySqlite::getModifyTime)
                    .last("LIMIT 1"));
            return one == null ? null : one.getContent();
        } catch (Exception e) {
            log.warn("读取SQL编辑器缓存失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 保存缓存内容（存在则更新，不存在则新增）
     */
    public void put(String content) {
        String value = content == null ? "" : content;
        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        try {
            LambdaQueryWrapper<DictionarySqlite> wrapper = new LambdaQueryWrapper<DictionarySqlite>()
                    .eq(DictionarySqlite::getKey, KEY_SQL_CACHE);
            Long count = dictionarySqliteMapper.selectCount(wrapper);
            DictionarySqlite entity = new DictionarySqlite();
            entity.setContent(value);
            entity.setModifyTime(now);
            if (count != null && count > 0) {
                dictionarySqliteMapper.update(entity, wrapper);
            } else {
                entity.setKey(KEY_SQL_CACHE);
                entity.setRemark("WebUI sql编辑器内容缓存");
                entity.setCreateTime(now);
                dictionarySqliteMapper.insert(entity);
            }
        } catch (Exception e) {
            log.error("保存SQL编辑器缓存失败", e);
        }
    }

    /**
     * 判断是否值得写入（内容为空且此前也没有记录时不写）
     */
    public boolean shouldSave(String content) {
        return StringUtils.isNotBlank(content) || StringUtils.isNotBlank(get());
    }
}
