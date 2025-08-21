package com.haruhi.botServer.service.music;


import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.dto.music163.Song;
import com.haruhi.botServer.factory.MusicServiceFactory;
import com.simplerobot.modules.utils.KQCodeUtils;

import java.util.List;

public abstract class AbstractMusicService {

    public abstract List<Song> searchMusic(Object param);

    public abstract MusicServiceFactory.MusicType type();

    // 如果以后做了其他平台音乐的搜索（比如虾米和qq音乐），需要在子类实现该抽象方法
    // 不同的平台搜歌 匹配命令不同
    // public abstract Map<String,Object> matches(String command);

    /**
     * 每个子类共同的方法
     * @param songs 歌曲搜索结果
     * @param index 下标 表示从songs中取出哪一条
     * @param checked 调用者是否以及对参数做了校验 true:是
     * @return
     */
   public String createMusicCq(List<Song> songs, int index,boolean checked){
        if(!checked && (index < 0 || index >= songs.size())){
            return null;
        }
        Song song = songs.get(index);
        String s = KQCodeUtils.getInstance().toCq(CqCodeTypeEnum.music.getType(), "type=" + type().getType(), "id=" + song.getId());
        return s;
    }
}
