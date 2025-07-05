package com.haruhi.botServer.service.wordStrip;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.WordStrip;
import com.haruhi.botServer.mapper.WordStripMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class WordStripServiceImpl extends ServiceImpl<WordStripMapper, WordStrip> implements WordStripService{

}
