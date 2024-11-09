package com.haruhi.botServer.service.sendLikeRecord;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.SendLikeRecord;
import com.haruhi.botServer.mapper.SendLikeRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class SendLikeRecordServiceImpl extends ServiceImpl<SendLikeRecordMapper, SendLikeRecord> implements SendLikeRecordService{
}
