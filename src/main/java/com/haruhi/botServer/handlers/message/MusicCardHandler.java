package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.cache.CacheMap;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.music.response.Song;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.service.music.AbstractMusicService;
import com.haruhi.botServer.factory.MusicServiceFactory;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.tools.rmi.ObjectNotFoundException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MusicCardHandler implements IAllMessageEvent {

    private static int expireTime = 60;
    private static CacheMap<String, List<Song>> cache = new CacheMap<>(expireTime, TimeUnit.SECONDS,800);

    @Override
    public int weight() {
        return HandlerWeightEnum.W_380.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_380.getName();
    }

    private String getKey(final Message message){
        return getKey(message.getSelfId(),message.getUserId(),message.getGroupId());
    }

    private String getKey(Long selfId,Long userId, Long groupId){
        String gid = groupId == null ? String.valueOf(groupId) : "";
        return String.valueOf(selfId) + userId + "-" + gid;
    }

    @Override
    public boolean onMessage(final Bot bot, final Message message) {
        Integer index = null;
        try {
            index = Integer.valueOf(message.getRawMessage().replace(" ",""));
        }catch (Exception e){}

        String key = getKey(message);
        List<Song> songs = cache.get(key);
        if(songs != null && index == null){
            String songName = getSongName(message.getRawMessage());
            if(Strings.isNotBlank(songName)){
                // 表示再次点歌
                search(bot,message,songName);
                return true;
            }
            // 存在缓存 但是输入的不是纯数字并且不是再次点歌 返回false，继续往下匹配
            return false;
        }else if(songs != null){
            // 存在缓存 输入了纯数字
            if(index <= 0 || index > songs.size()){
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"不存在序号" + index + "的歌曲",true);
                return true;
            }
            // 若这里删除缓存，那么一次搜索只能点一次歌
            // 若不删除，那么在缓存 存在期间，都可以通过输入纯数字持续点歌
            // cache.remove(key);
            ThreadPoolUtil.getHandleCommandPool().execute(new SendMusicCardTask(bot,songs,index,message));
            return true;
        }else{
            // 不存在缓存
            String songName = getSongName(message.getRawMessage());
            if(Strings.isNotBlank(songName)){
                // 匹配上命令 开始搜索歌曲
                search(bot,message,songName);
                return true;
            }
        }
        return false;
    }
    private void search(Bot bot,Message message,String songName){
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"开始搜索歌曲：" + songName,true);
        ThreadPoolUtil.getHandleCommandPool().execute(new SearchMusicTask(bot,message,songName));
    }

    private String getSongName(final String command){
        return CommonUtil.commandReplaceFirst(command, RegexEnum.MUSIC_CARD);
    }

    private class SearchMusicTask implements Runnable{

        private Bot bot;
        private Message message;
        private String musicName;

        SearchMusicTask(Bot bot, Message message, String musicName){
            this.bot = bot;
            this.message = message;
            this.musicName = musicName;
        }

        @Override
        public void run() {
            try {
                List<Song> res = MusicServiceFactory.getMusicService(MusicServiceFactory.MusicType.cloudMusic).searchMusic(musicName);
                if(CollectionUtils.isEmpty(res)){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"未找到歌曲：" + musicName,true);
                    return;
                }
                if(res.size() == 1){
                    // 搜索结果只有一条，直接发送这个音乐卡片
                    sendMusicCard(bot,message,res,0,true);
                    return;
                }
                // 将搜索结果保存到缓存
                cache.put(getKey(message),res);
                if(message.isGroupMsg()){
                    sendGroup(bot,message,res,musicName);
                }else if(message.isPrivateMsg()){
                    sendPrivate(bot,message,res,musicName);
                }
            } catch (Exception e) {
                log.error("搜索歌曲发生异常",e);
            }
        }
    }

    private void sendGroup(Bot bot,Message message,List<Song> songs,String songName){
        int size = songs.size();
        List<String> forwardMsgs = new ArrayList<>(size + 1);
        forwardMsgs.add(MessageFormat.format("搜索【{0}】成功！接下来请在{1}秒内发送纯数字序号选择歌曲",songName,expireTime));
        for (int i = 0; i < size; i++) {
            Song e = songs.get(i);
            forwardMsgs.add(MessageFormat.format("{0}：{1}\n歌手：{2}\n专辑：{3}",(i + 1),e.getName(),e.getArtists(),e.getAlbumName()));
        }
        bot.sendGroupMessage(message.getGroupId(),message.getSelfId(),BotConfig.NAME,forwardMsgs);
    }
    private void sendPrivate(Bot bot,Message message,List<Song> songs,String songName){
        StringBuilder stringBuilder = new StringBuilder(MessageFormat.format("搜索【{0}】成功！接下来请在{1}秒内发送纯数字序号选择歌曲\n\n",songName,expireTime));
        int size = songs.size();
        for (int i = 0; i < size; i++) {
            Song e = songs.get(i);
            stringBuilder.append(MessageFormat.format("{0}：{1}\n歌手：{2}\n专辑：{3}\n\n",(i + 1),e.getName(),e.getArtists(),e.getAlbumName()));
        }
        bot.sendPrivateMessage(message.getUserId(),stringBuilder.toString(),true);
    }

    private class SendMusicCardTask implements Runnable{

        private Bot bot;
        private List<Song> songs;
        private Integer index;
        private Message message;

        SendMusicCardTask(Bot bot,List<Song> songs,Integer index,Message message){
            this.bot = bot;
            this.songs = songs;
            this.index = index;
            this.message = message;
        }

        @Override
        public void run() {
            try {
                sendMusicCard(bot,message,songs,index - 1,true);
            } catch (ObjectNotFoundException e) {
                log.error("发送音乐卡片异常",e);
            }
        }
    }

    private void sendMusicCard(Bot bot, Message message, List<Song> songs, Integer index, boolean checked) throws ObjectNotFoundException {
        AbstractMusicService musicService = MusicServiceFactory.getMusicService(MusicServiceFactory.MusicType.cloudMusic);
        String musicCq = musicService.createMusicCq(songs, index,checked);
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),musicCq,false);
    }


}
