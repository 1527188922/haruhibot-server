package com.haruhi.botServer.service.pixiv;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.Pixiv;
import com.haruhi.botServer.mapper.PixivMapper;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.ws.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


@Service
public class PixivServiceImpl extends ServiceImpl<PixivMapper, Pixiv> implements PixivService{


}

