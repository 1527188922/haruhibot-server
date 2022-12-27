package com.haruhi.botServer;

import com.haruhi.botServer.job.schedule.JobManage;
import com.haruhi.botServer.service.DataBaseService;
import com.haruhi.botServer.test.TestSubject;
import com.haruhi.botServer.thread.FirstTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SystemCommandLineRunner implements CommandLineRunner {

    @Autowired
    private FirstTask firstTask;
    @Autowired
    private DataBaseService dataBaseService;
    @Autowired
    private JobManage jobManage;

    @Override
    public void run(String... args) throws Exception {
        dataBaseService.initDataBase();
        firstTask.execute(firstTask);
        jobManage.startAllJob();
        TestSubject.startTest();
    }
}
