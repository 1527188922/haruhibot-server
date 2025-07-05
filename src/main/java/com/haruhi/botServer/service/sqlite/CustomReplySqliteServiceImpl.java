package com.haruhi.botServer.service.sqlite;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.sqlite.CustomReplySqlite;
import com.haruhi.botServer.mapper.sqlite.CustomReplySqliteMapper;
import org.springframework.stereotype.Service;

@Service
public class CustomReplySqliteServiceImpl extends ServiceImpl<CustomReplySqliteMapper, CustomReplySqlite>
        implements CustomReplySqliteService {
}
