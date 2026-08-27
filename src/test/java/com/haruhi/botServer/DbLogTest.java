package com.haruhi.botServer;


import com.haruhi.botServer.constant.BusinessModuleEnum;
import com.haruhi.botServer.utils.DbLog;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ActiveProfiles("dev")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = HaruhiBotServer.class)
public class DbLogTest {


    @Test
    public void test() {

        t1();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String t1(){
        new Thread(()->{

            t2();


            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        return "";
    }

    private void t2(){
        try {
            int i = 10 / 0;
        }catch (Exception e) {
            DbLog.error(BusinessModuleEnum.SYSTEM, "异常测试:{}",e.getMessage(),e);
            DbLog.debug(BusinessModuleEnum.SYSTEM, "异常测试:{}",e.getMessage(),e);
            DbLog.info(BusinessModuleEnum.SYSTEM, "异常测试:{}",e.getMessage(),e);
        }
    }

}
