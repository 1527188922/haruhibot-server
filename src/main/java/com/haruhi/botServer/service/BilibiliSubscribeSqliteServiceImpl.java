package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.BilibiliSubscribeSqlite;
import com.haruhi.botServer.mapper.BilibiliSubscribeSqliteMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class BilibiliSubscribeSqliteServiceImpl extends ServiceImpl<BilibiliSubscribeSqliteMapper, BilibiliSubscribeSqlite>
        implements BilibiliSubscribeSqliteService {

    @Override
    public List<BilibiliSubscribeSqlite> listEnabled(String subType) {
        if (StringUtils.isBlank(subType)) {
            return Collections.emptyList();
        }
        return this.list(new LambdaQueryWrapper<BilibiliSubscribeSqlite>()
                .eq(BilibiliSubscribeSqlite::getSubType, subType)
                .eq(BilibiliSubscribeSqlite::getEnableStatus, BilibiliSubscribeSqlite.ENABLE_STATUS_ENABLE));
    }
}
