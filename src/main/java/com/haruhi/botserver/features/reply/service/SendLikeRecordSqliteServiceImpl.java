package com.haruhi.botserver.features.reply.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botserver.features.reply.persistence.entity.SendLikeRecordSqlite;
import com.haruhi.botserver.features.reply.persistence.mapper.SendLikeRecordSqliteMapper;
import org.springframework.stereotype.Service;

@Service
public class SendLikeRecordSqliteServiceImpl extends ServiceImpl<SendLikeRecordSqliteMapper, SendLikeRecordSqlite> implements SendLikeRecordSqliteService {
}
