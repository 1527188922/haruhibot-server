package com.haruhi.botServer.service.sqlite;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.sqlite.WordStripSqlite;
import com.haruhi.botServer.mapper.sqlite.WordStripSqliteMapper;
import org.springframework.stereotype.Service;

@Service
public class WordStripSqliteServiceImpl extends ServiceImpl<WordStripSqliteMapper,WordStripSqlite> implements WordStripSqliteService {
}
