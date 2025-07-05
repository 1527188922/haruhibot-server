package com.haruhi.botServer.service.sqlite;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.sqlite.ChatRecordSqlite;
import com.haruhi.botServer.mapper.sqlite.ChatRecordSqliteMapper;
import org.springframework.stereotype.Service;

@Service
public class ChatRecordSqliteServiceImpl extends ServiceImpl<ChatRecordSqliteMapper, ChatRecordSqlite>
        implements ChatRecordSqliteService {
}
