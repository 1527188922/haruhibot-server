package com.haruhi.botServer.config;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatgptConfig {

    public static String EMAIL;
    public static String PASSWORD;
    public static String SESSION_TOKEN;


    @Autowired
    public void setEmail(@Value("${chatgpt.email}") String email){
        EMAIL = email;
    }
    @Autowired
    public void setPassword(@Value("${chatgpt.password}") String password){
        PASSWORD = password;
    }
    @Autowired
    public void setSessionToken(@Value("${chatgpt.session-token}") String sessionToken){
        SESSION_TOKEN = sessionToken;
    }

    public static boolean support(){
        if(Strings.isNotBlank(SESSION_TOKEN) || (Strings.isNotBlank(EMAIL) && Strings.isNotBlank(PASSWORD))){
            return true;
        }
        return false;
    }

}
