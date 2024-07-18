package com.haruhi.botServer.service.verbalTricks;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.CustomReply;
import com.haruhi.botServer.handlers.message.CustomReplyHandler;
import com.haruhi.botServer.mapper.CustomReplyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomReplyServiceImpl extends ServiceImpl<CustomReplyMapper, CustomReply> implements CustomReplyService {

    @Autowired
    private CustomReplyMapper customReplyMapper;

    @Override
    public void loadToCache() {
        List<CustomReply> all = customReplyMapper.selectList(new LambdaQueryWrapper<CustomReply>()
                .eq(CustomReply::getDeleted,false));
        if(!CollectionUtils.isEmpty(all)){
            Map<String, List<CustomReply>> groupMap = all.stream().collect(Collectors.groupingBy(CustomReply::getRegex, Collectors.toList()));
            CustomReplyHandler.putAllCache(groupMap);
            log.info("自定义回复数据加载到内存成功，数据总量：{}，分组后的数量：{}",all.size(),groupMap.size());
        }

    }

    @Override
    public void clearCache() {
        CustomReplyHandler.clearCache();
    }
}
