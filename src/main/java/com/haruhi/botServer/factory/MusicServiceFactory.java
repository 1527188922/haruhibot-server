package com.haruhi.botServer.factory;

import com.haruhi.botServer.service.music.AbstractMusicService;
import org.apache.ibatis.javassist.tools.rmi.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class MusicServiceFactory {

    @Autowired
    private Map<String, AbstractMusicService> musicServiceBeans;

    private static Map<MusicType, AbstractMusicService> musicServiceContainer = new HashMap<>(3);


    @PostConstruct
    private void init(){
        for (AbstractMusicService value : musicServiceBeans.values()) {
            MusicServiceFactory.musicServiceContainer.put(value.type(),value);
        }
    }

    public static AbstractMusicService getMusicService(MusicType MusicType) throws ObjectNotFoundException {
        AbstractMusicService musicService = MusicServiceFactory.musicServiceContainer.get(MusicType);
        if (musicService == null) {
            throw new ObjectNotFoundException("没有type为:" + MusicType.getType() + "的bean！");
        }
        return musicService;
    }


    public enum MusicType{
        cloudMusic("163"),
        xm("xm"),
        tencent("qq");

        private String type;
        MusicType(String type){
            this.type = type;
        }
        public String getType(){
            return type;
        }

    }

}
