package com.haruhi.botServer.service.sqlite;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.sqlite.CustomReplySqlite;
import com.haruhi.botServer.handlers.message.CustomReplyHandler;
import com.haruhi.botServer.mapper.sqlite.CustomReplySqliteMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomReplySqliteServiceImpl extends ServiceImpl<CustomReplySqliteMapper, CustomReplySqlite>
        implements CustomReplySqliteService {

    @Override
    public void loadToCache() {
        List<CustomReplySqlite> all = this.list(new LambdaQueryWrapper<CustomReplySqlite>()
                .eq(CustomReplySqlite::getDeleted,0));
        if(!CollectionUtils.isEmpty(all)){
            Map<String, List<CustomReplySqlite>> groupMap = all.stream().collect(Collectors.groupingBy(CustomReplySqlite::getRegex, Collectors.toList()));
            CustomReplyHandler.putAllCache(groupMap);
            log.info("自定义回复数据加载到内存成功，数据总量：{}，分组后的数量：{}",all.size(),groupMap.size());
        }

    }

    @Override
    public void clearCache() {
        CustomReplyHandler.clearCache();
    }
}
