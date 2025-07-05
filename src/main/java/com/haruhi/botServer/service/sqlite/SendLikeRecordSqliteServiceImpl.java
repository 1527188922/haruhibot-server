package com.haruhi.botServer.service.sqlite;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.sqlite.SendLikeRecordSqlite;
import com.haruhi.botServer.mapper.sqlite.SendLikeRecordSqliteMapper;
import org.springframework.stereotype.Service;

@Service
public class SendLikeRecordSqliteServiceImpl extends ServiceImpl<SendLikeRecordSqliteMapper, SendLikeRecordSqlite> implements SendLikeRecordSqliteService {
}
