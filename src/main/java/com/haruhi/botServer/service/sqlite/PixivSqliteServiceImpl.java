package com.haruhi.botServer.service.sqlite;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.sqlite.PixivSqlite;
import com.haruhi.botServer.mapper.sqlite.PixivSqliteMapper;
import org.springframework.stereotype.Service;

@Service
public class PixivSqliteServiceImpl extends ServiceImpl<PixivSqliteMapper, PixivSqlite> implements PixivSqliteService {
}
