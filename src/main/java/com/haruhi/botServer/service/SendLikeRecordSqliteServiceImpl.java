package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.SendLikeRecordSqlite;
import com.haruhi.botServer.mapper.SendLikeRecordSqliteMapper;
import org.springframework.stereotype.Service;

@Service
public class SendLikeRecordSqliteServiceImpl extends ServiceImpl<SendLikeRecordSqliteMapper, SendLikeRecordSqlite> implements SendLikeRecordSqliteService {
}
