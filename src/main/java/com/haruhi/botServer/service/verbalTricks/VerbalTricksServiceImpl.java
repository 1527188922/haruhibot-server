package com.haruhi.botServer.service.verbalTricks;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.VerbalTricks;
import com.haruhi.botServer.handlers.message.VerbalTricksHandler;
import com.haruhi.botServer.mapper.VerbalTricksMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VerbalTricksServiceImpl extends ServiceImpl<VerbalTricksMapper, VerbalTricks> implements VerbalTricksService {

    @Autowired
    private VerbalTricksMapper verbalTricksMapper;

    @Override
    public void loadVerbalTricks() {
        List<VerbalTricks> all = verbalTricksMapper.selectList(new LambdaQueryWrapper<VerbalTricks>()
                .ne(VerbalTricks::getRegex,"")
                .ne(VerbalTricks::getAnswer,"")
                .eq(VerbalTricks::getDeleted,false));
        if(!CollectionUtils.isEmpty(all)){
            Map<String, List<VerbalTricks>> groupMap = all.stream().collect(Collectors.groupingBy(VerbalTricks::getRegex, Collectors.toList()));
            VerbalTricksHandler.putAllCache(groupMap);
            log.info("自定义回复数据加载到内存成功，数据总量：{}，分组后的数量：{}",all.size(),groupMap.size());
        }

    }

    @Override
    public void clearCache() {
        VerbalTricksHandler.clearCache();
    }
}
