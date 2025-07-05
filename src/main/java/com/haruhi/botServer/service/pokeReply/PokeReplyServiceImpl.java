package com.haruhi.botServer.service.pokeReply;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.PokeReply;
import com.haruhi.botServer.mapper.PokeReplyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class PokeReplyServiceImpl extends ServiceImpl<PokeReplyMapper, PokeReply> implements PokeReplyService{

}
