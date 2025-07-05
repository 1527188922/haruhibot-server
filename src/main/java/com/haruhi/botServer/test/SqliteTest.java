package com.haruhi.botServer.test;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botServer.entity.*;
import com.haruhi.botServer.entity.sqlite.*;
import com.haruhi.botServer.service.chatRecord.ChatRecordService;
import com.haruhi.botServer.service.customReply.CustomReplyService;
import com.haruhi.botServer.service.pixiv.PixivService;
import com.haruhi.botServer.service.pokeReply.PokeReplyService;
import com.haruhi.botServer.service.sendLikeRecord.SendLikeRecordService;
import com.haruhi.botServer.service.sqlite.*;
import com.haruhi.botServer.service.wordStrip.WordStripService;
import com.haruhi.botServer.utils.DateTimeUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Component
public class SqliteTest implements ISpringTester{

    @Override
    public boolean enable() {
        return false;
    }


    @Autowired
    private CustomReplyService customReplyService;
    @Autowired
    private CustomReplySqliteService customReplySqliteService;

    @Autowired
    private PixivService pixivService;
    @Autowired
    private PixivSqliteService pixivSqliteService;

    @Autowired
    private PokeReplyService pokeReplyService;
    @Autowired
    private PokeReplySqliteService pokeReplySqliteService;

    @Autowired
    private SendLikeRecordService sendLikeRecordService;
    @Autowired
    private SendLikeRecordSqliteService sendLikeRecordSqliteService;

    @Autowired
    private WordStripService wordStripService;
    @Autowired
    private WordStripSqliteService wordStripSqliteService;


    @Autowired
    private ChatRecordService chatRecordService;
    @Autowired
    private ChatRecordSqliteService chatRecordSqliteService;


    @Override
    public void test(String... args) {
//        try {
//            List<CustomReply> list = customReplyService.list(null);
//            List<CustomReplySqlite> collect = list.stream().map(e -> {
//                CustomReplySqlite customReplySqlite = new CustomReplySqlite();
//                BeanUtils.copyProperties(e, customReplySqlite);
//                customReplySqlite.setDeleted(e.getDeleted() ? 1 : 0);
//                customReplySqlite.setIsText(e.getIsText() ? 1 : 0);
//                customReplySqlite.setId(null);
//                return customReplySqlite;
//            }).collect(Collectors.toList());
//            customReplySqliteService.saveBatch(collect);
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        try {
//            List<Pixiv> list1 = pixivService.list(null);
//            List<PixivSqlite> collect1 = list1.stream().map(e -> {
//                PixivSqlite pixivSqlite = new PixivSqlite();
//                BeanUtils.copyProperties(e, pixivSqlite);
//                pixivSqlite.setId(null);
//                pixivSqlite.setIsR18(e.getIsR18() ? 1 : 0);
//                return pixivSqlite;
//            }).collect(Collectors.toList());
//            pixivSqliteService.saveBatch(collect1);
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        try {
//            List<PokeReply> list1 = pokeReplyService.list(null);
//            List<PokeReplySqlite> collect1 = list1.stream().map(e -> {
//                PokeReplySqlite pokeReplySqlite = new PokeReplySqlite();
//                BeanUtils.copyProperties(e, pokeReplySqlite);
//                pokeReplySqlite.setId(null);
//                return pokeReplySqlite;
//            }).collect(Collectors.toList());
//            pokeReplySqliteService.saveBatch(collect1);
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//        try {
//            List<SendLikeRecord> list1 = sendLikeRecordService.list(null);
//            List<SendLikeRecordSqlite> collect1 = list1.stream().map(e -> {
//                SendLikeRecordSqlite sendLikeRecordSqlite = new SendLikeRecordSqlite();
//                BeanUtils.copyProperties(e, sendLikeRecordSqlite);
//                sendLikeRecordSqlite.setId(null);
//                sendLikeRecordSqlite.setSendTime(DateTimeUtil.dateTimeFormat(e.getSendTime(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
//                return sendLikeRecordSqlite;
//            }).collect(Collectors.toList());
//            sendLikeRecordSqliteService.saveBatch(collect1);
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        try {
//            List<WordStrip> list1 = wordStripService.list(null);
//            List<WordStripSqlite> collect1 = list1.stream().map(e -> {
//                WordStripSqlite wordStripSqlite = new WordStripSqlite();
//                BeanUtils.copyProperties(e, wordStripSqlite);
//                wordStripSqlite.setId(null);
//                return wordStripSqlite;
//            }).collect(Collectors.toList());
//            wordStripSqliteService.saveBatch(collect1);
//        }catch (Exception e) {
//            e.printStackTrace();
//        }

        try {
            int pageSize = 5000;
            int currentPage = 0;
            while (true){
                currentPage++;
                System.out.println("currentPage:" + currentPage);
                IPage<ChatRecord> page = chatRecordService.page(new Page<>(currentPage, pageSize), null);
                List<ChatRecord> records = page.getRecords();
                if (CollectionUtils.isEmpty(records)) {
                    break;
                }

                List<ChatRecordSqlite> collect = records.stream().map(e -> {
                    ChatRecordSqlite chatRecordSqlite = new ChatRecordSqlite();
                    BeanUtils.copyProperties(e, chatRecordSqlite);
                    chatRecordSqlite.setId(null);
                    chatRecordSqlite.setDeleted(e.getDeleted() ? 1 : 0);
                    chatRecordSqlite.setTime(DateTimeUtil.dateTimeFormat(e.getTime(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
                    return chatRecordSqlite;
                }).collect(Collectors.toList());

                chatRecordSqliteService.saveBatch(collect);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println("完成111111111111111111111");
    }
}
