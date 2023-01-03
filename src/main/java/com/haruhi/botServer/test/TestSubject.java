package com.haruhi.botServer.test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class TestSubject {

    private static Set<ISpringTester> testers = new HashSet<>();

    @Autowired
    public void setTesters(Map<String, ISpringTester> testerMap){
        testers.addAll(testerMap.values());
    }

    public static void startTest(String... args){
        for (ISpringTester tester : testers) {
            if (tester.enable()) {
                tester.test(args);
            }
        }
    }

}
