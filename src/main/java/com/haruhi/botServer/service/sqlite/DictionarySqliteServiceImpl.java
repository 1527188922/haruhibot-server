package com.haruhi.botServer.service.sqlite;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.sqlite.DictionarySqlite;
import com.haruhi.botServer.mapper.sqlite.DictionarySqliteMapper;
import org.springframework.stereotype.Service;

@Service
public class DictionarySqliteServiceImpl extends ServiceImpl<DictionarySqliteMapper, DictionarySqlite>
        implements DictionarySqliteService {
}
