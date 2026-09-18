package com.haruhi.botServer.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.config.util.PropertiesFileUtil;
import com.haruhi.botServer.entity.DictionarySqlite;
import com.haruhi.botServer.mapper.DictionarySqliteMapper;
import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 老版本配置迁移
 * <p>
 * 旧版本把配置存在数据库 t_dictionary 表里，本类在启动时把它搬到 ./config/*.properties。
 * <ul>
 *     <li>支持"旧key -&gt; 新key"的映射（{@link ConfigKey#ofLegacy}），key改名不会丢配置</li>
 *     <li>只处理"配置文件里还没有"的key，不会覆盖用户已经改好的配置，可重复执行</li>
 *     <li>表不存在（全新部署）时直接跳过</li>
 *     <li><b>不属于配置的key不会被搬走，也不会被删除</b>（如 {@code db.sql_cache} 这类操作数据）</li>
 * </ul>
 * 迁移成功的配置会从字典表中删除（配置的唯一真源是文件），其它数据原样保留。
 */
@Slf4j
@Service
public class ConfigMigrator {

    @Autowired
    private DictionarySqliteMapper dictionarySqliteMapper;

    /**
     * @return 迁移的配置项数量
     */
    public int migrate() {
        List<DictionarySqlite> list;
        try {
            if (!tableExists()) {
                return 0;
            }
            list = dictionarySqliteMapper.selectList(null);
        } catch (Exception e) {
            log.warn("读取旧配置表失败，跳过迁移：{}", e.getMessage());
            return 0;
        }
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int count = 0;
        List<Long> migratedIds = new ArrayList<>();
        for (DictionarySqlite dictionary : list) {
            String oldKey = dictionary.getKey();
            if (oldKey == null) {
                continue;
            }
            // 新key优先，其次按老key映射（改名过的配置项）
            ConfigKey key = ConfigKey.of(oldKey);
            if (key == null) {
                key = ConfigKey.ofLegacy(oldKey);
            }
            if (key == null) {
                // 不是配置项（如 db.sql_cache），保留在数据库里，不迁移也不删除
                continue;
            }
            // 配置文件里已有该key则不覆盖
            if (Configs.isConfigured(key)) {
                continue;
            }
            String value = dictionary.getContent() == null ? "" : dictionary.getContent();
            try {
                PropertiesFileUtil.save(key.getFile().getFileName(), key.getKey(), value);
                count++;
                if (dictionary.getId() != null) {
                    migratedIds.add(dictionary.getId());
                }
            } catch (Exception e) {
                log.error("迁移配置失败 key:{} -> {}", oldKey, key.getKey(), e);
            }
        }

        if (count > 0) {
            Configs.reloadAll();
            removeMigrated(migratedIds);
            log.info("已从数据库迁移{}项配置到 {}", count, FileUtil.getConfigDir());
        }
        return count;
    }

    /**
     * 迁移完成的配置从字典表移除（失败的保留，下次启动可重试）
     */
    private void removeMigrated(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        try {
            dictionarySqliteMapper.deleteByIds(ids);
        } catch (Exception e) {
            log.warn("清理已迁移的旧配置记录失败：{}", e.getMessage());
        }
    }

    private boolean tableExists() {
        try {
            dictionarySqliteMapper.selectCount(new LambdaQueryWrapper<DictionarySqlite>()
                    .eq(DictionarySqlite::getKey, SqlCacheStore.KEY_SQL_CACHE));
            return true;
        } catch (Exception e) {
            // 表不存在时sqlite会抛异常
            return false;
        }
    }
}
