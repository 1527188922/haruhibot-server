package com.haruhi.botServer.service.customReply;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.CustomReply;
import com.haruhi.botServer.mapper.CustomReplyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomReplyServiceImpl extends ServiceImpl<CustomReplyMapper, CustomReply> implements CustomReplyService {

}
