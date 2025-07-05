package com.haruhi.botServer.service.wordStrip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.WordStrip;
import com.haruhi.botServer.handlers.message.wordStrip.WordStripHandler;
import com.haruhi.botServer.mapper.WordStripMapper;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.vo.WordStripQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class WordStripServiceImpl extends ServiceImpl<WordStripMapper, WordStrip> implements WordStripService{

    @Autowired
    private WordStripMapper wordStripMapper;

    /**
     * 将数据库词条加载到缓存
     */
    @Override
    public void loadWordStrip(){
        List<WordStrip> wordStrips = wordStripMapper.selectList(null);
        if (!CollectionUtils.isEmpty(wordStrips)){
            for (WordStrip element : wordStrips) {
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
    public IPage<WordStrip> search(WordStripQueryReq request, boolean isPage) {

        LambdaQueryWrapper<WordStrip> queryWrapper = new LambdaQueryWrapper<WordStrip>()
                .eq(Objects.nonNull(request.getGroupId()),WordStrip::getGroupId,request.getGroupId())
                .eq(Objects.nonNull(request.getUserId()),WordStrip::getUserId,request.getUserId())
                .eq(Objects.nonNull(request.getSelfId()),WordStrip::getSelfId,request.getSelfId())
                .like(StringUtils.isNotBlank(request.getKeyWord()),WordStrip::getKeyWord,request.getKeyWord())
                .like(StringUtils.isNotBlank(request.getAnswer()),WordStrip::getAnswer,request.getAnswer())
                .orderByDesc(WordStrip::getId);

        IPage<WordStrip> pageInfo = null;
        if (isPage) {
            pageInfo = this.page(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);
        }else{
            pageInfo = new Page<>(request.getCurrentPage(), request.getPageSize());
            List<WordStrip> list = this.list(queryWrapper);
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
