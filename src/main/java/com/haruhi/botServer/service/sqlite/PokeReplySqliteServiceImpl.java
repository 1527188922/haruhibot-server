package com.haruhi.botServer.service.sqlite;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.entity.sqlite.PokeReplySqlite;
import com.haruhi.botServer.handlers.notice.PokeMeHandler;
import com.haruhi.botServer.mapper.sqlite.PokeReplySqliteMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
public class PokeReplySqliteServiceImpl extends ServiceImpl<PokeReplySqliteMapper, PokeReplySqlite> implements PokeReplySqliteService {



    @Override
    public void loadPokeReply() {
        List<PokeReplySqlite> list = this.list(null);
        if(!CollectionUtils.isEmpty(list)){
            int pokeNum = 4;
            int logPokeNum = 0;
            for (PokeReplySqlite pokeReply : list) {
                PokeMeHandler.cache.add(pokeReply.getReply());
            }
            if(list.size() > pokeNum){
                // 如果为空字符串 则发送戳一戳
                for (int i = 0; i < pokeNum; i++) {
                    PokeMeHandler.cache.add("");
                }
                logPokeNum = pokeNum;
            }
            log.info("加载戳一戳回复到内存成功！数量：{}", PokeMeHandler.cache.size() - logPokeNum);
        }else{
            log.warn("表`{}`中数据为空，不能进行戳一戳回复；可对该表添加数据，自定义戳一戳回复内容", DataBaseConfig.T_POKE_REPLY);
        }
    }

    @Override
    public void clearCache() {
        PokeMeHandler.cache.clear();
    }
}
