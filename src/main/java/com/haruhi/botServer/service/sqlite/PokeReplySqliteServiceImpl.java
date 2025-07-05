package com.haruhi.botServer.service.sqlite;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.sqlite.PokeReplySqlite;
import com.haruhi.botServer.mapper.sqlite.PokeReplySqliteMapper;
import org.springframework.stereotype.Service;

@Service
public class PokeReplySqliteServiceImpl extends ServiceImpl<PokeReplySqliteMapper, PokeReplySqlite> implements PokeReplySqliteService {
}
