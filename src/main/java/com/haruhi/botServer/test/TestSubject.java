package com.haruhi.botServer.test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class TestSubject implements CommandLineRunner {

    private static Set<ISpringTester> testers = new HashSet<>();

    @Autowired(required = false)
    public void setTesters(Map<String, ISpringTester> testerMap){
        testers.addAll(testerMap.values());
    }

    @Override
    public void run(String... args) throws Exception {
        for (ISpringTester tester : testers) {
            if (tester.enable()) {
                tester.test(args);
            }
        }
    }
}
