package com.haruhi.botServer.test;

import com.haruhi.botServer.entity.sqlite.SendLikeRecordSqlite;
import com.haruhi.botServer.service.sendLikeRecord.SendLikeRecordService;
import com.haruhi.botServer.service.sqlite.SendLikeRecordSqliteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class SqliteTest implements ISpringTester{

    @Override
    public boolean enable() {
        return true;
    }


    @Autowired
    private SendLikeRecordSqliteService sendLikeRecordSqliteService;
    @Autowired
    private SendLikeRecordService sendLikeRecordService;

    @Override
    public void test(String... args) {
        try {

//            List<SendLikeRecord> list = sendLikeRecordService.list(null);

//            for (SendLikeRecord sendLikeRecord : list) {
//                SendLikeRecordSqlite sendLikeRecordSqlite = new SendLikeRecordSqlite();
//
//                BeanUtils.copyProperties(sendLikeRecord, sendLikeRecordSqlite);
//                sendLikeRecordSqlite.setSendTime(DateTimeUtil.dateTimeFormat(sendLikeRecord.getSendTime(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
//                sendLikeRecordSqliteService.save(sendLikeRecordSqlite);
//            }


            SendLikeRecordSqlite byId = sendLikeRecordSqliteService.getById(381L);


            System.out.println();

        }catch (Exception e) {
            e.printStackTrace();
        }




    }
}
