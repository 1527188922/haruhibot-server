package com.haruhi.botServer;

import com.haruhi.botServer.thread.FirstTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SystemCommandLineRunner implements CommandLineRunner {

    @Autowired
    private FirstTask firstTask;

    @Override
    public void run(String... args) throws Exception {
        firstTask.execute(firstTask);
    }
}
