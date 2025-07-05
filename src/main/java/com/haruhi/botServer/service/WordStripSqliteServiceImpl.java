package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.sqlite.WordStripSqlite;
import com.haruhi.botServer.handlers.message.wordStrip.WordStripHandler;
import com.haruhi.botServer.mapper.WordStripSqliteMapper;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.vo.WordStripQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class WordStripSqliteServiceImpl extends ServiceImpl<WordStripSqliteMapper,WordStripSqlite> implements WordStripSqliteService {

    /**
     * 将数据库词条加载到缓存
     */
    @Override
    public void loadWordStrip(){
        List<WordStripSqlite> wordStrips = this.list(null);
        if (!CollectionUtils.isEmpty(wordStrips)){
            for (WordStripSqlite element : wordStrips) {
                WordStripHandler.putCache(element.getSelfId(),element.getGroupId(),element.getKeyWord(),element.getAnswer());
            }
            log.info("加载词条数据到内存完成，数量：{}",wordStrips.size());
        }
    }

    @Override
    public void clearCache() {
        WordStripHandler.clearCache();
    }

    @Override
    public IPage<WordStripSqlite> search(WordStripQueryReq request, boolean isPage) {

        LambdaQueryWrapper<WordStripSqlite> queryWrapper = new LambdaQueryWrapper<WordStripSqlite>()
                .eq(Objects.nonNull(request.getGroupId()),WordStripSqlite::getGroupId,request.getGroupId())
                .eq(Objects.nonNull(request.getUserId()),WordStripSqlite::getUserId,request.getUserId())
                .eq(Objects.nonNull(request.getSelfId()),WordStripSqlite::getSelfId,request.getSelfId())
                .like(StringUtils.isNotBlank(request.getKeyWord()),WordStripSqlite::getKeyWord,request.getKeyWord())
                .like(StringUtils.isNotBlank(request.getAnswer()),WordStripSqlite::getAnswer,request.getAnswer())
                .orderByDesc(WordStripSqlite::getId);

        IPage<WordStripSqlite> pageInfo = null;
        if (isPage) {
            pageInfo = this.page(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);
        }else{
            pageInfo = new Page<>(request.getCurrentPage(), request.getPageSize());
            List<WordStripSqlite> list = this.list(queryWrapper);
            pageInfo.setRecords(list);
            pageInfo.setTotal(list.size());
        }
        if (pageInfo != null && CollectionUtils.isNotEmpty(pageInfo.getRecords())) {
            pageInfo.getRecords().forEach(e -> {
                e.setUserAvatarUrl(CommonUtil.getAvatarUrl(e.getUserId(),false));
                e.setSelfAvatarUrl(CommonUtil.getAvatarUrl(e.getSelfId(),false));
            });
        }
        return pageInfo;
    }

}
