package com.haruhi.botServer.handlers.message.system;

import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.cache.CacheMap;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.DownloadFileResp;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.event.message.IPrivateMessageEvent;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.concurrent.TimeUnit;


/**
 * 该功能注意点:
 * 在dev环境下 该功能异常(gocq会下载文件失败)
 * 在pord坏境下且主机非公网上 enable-internet-host要设置为1
 */
@Slf4j
@Component
@DependsOn("botConfig")
public class SendLogFileHandler implements IPrivateMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_350.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_350.getName();
    }
    private static CacheMap<String, File[]> cacheMap = null;
    private static int expireTime = 20;
    private static File logDir;


    @Autowired
    private AbstractWebResourceConfig pathConfig;

    @PostConstruct
    private void initCache(){
        cacheMap = new CacheMap<>(expireTime, TimeUnit.SECONDS, BotConfig.SUPERUSERS.size() == 0 ? 1 : BotConfig.SUPERUSERS.size());
        logDir = new File(FileUtil.getLogsDir());
    }



    @SuperuserAuthentication
    @Override
    public boolean onPrivate(final Bot bot, final Message message) {
        File[] files = cacheMap.get(key(message));
        if (files != null) {
            try {
                int index = Integer.parseInt(message.getRawMessage()) - 1;

                ThreadPoolUtil.getHandleCommandPool().execute(new UploadLogFileTask(files,index,bot,message));
                return true;
            }catch (NumberFormatException e){
                return false;
            }
        }else if(message.getRawMessage().matches(RegexEnum.SEND_LOG.getValue())){

            ThreadPoolUtil.getHandleCommandPool().execute(()->{

                File[] files1 = logDir.listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".log"));
                if(files1 != null && files1.length > 0){
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < files1.length; i++) {
                        builder.append((i + 1) + "：").append(files1[i].getName())
                                .append("\n" + String.format("%.2f",(double)files1[i].length() / 1024 / 1024) + "MB").append("\n");
                    }
                    cacheMap.put(key(message),files1);
                    bot.sendPrivateMessage(message.getUserId(),builder.toString(),true);
                }else {
                    bot.sendPrivateMessage(message.getUserId(),"没有日志文件",true);
                }
            });

            return true;
        }
        return false;
    }

    private String key(final Message message){
        return String.valueOf(message.getSelfId()) + message.getUserId();
    }

    private class UploadLogFileTask implements Runnable{

        private final File[] files;
        private final Bot bot;
        private final Message message;
        private final int index;
        UploadLogFileTask(File[] files,int index,Bot bot,Message message){
            this.bot = bot;
            this.index = index;
            this.message = message;
            this.files = files;
        }

        @Override
        public void run() {
            try {
                if(index < 0 || index >= files.length){
                    bot.sendPrivateMessage(message.getUserId(),"请输入范围内序号",true);
                    return;
                }
                File file = files[index];
                if(file == null || !file.exists() || file.isDirectory()){
                    bot.sendPrivateMessage(message.getUserId(),"文件不存在\n"+file,true);
                    return;
                }
                // 注意该路径可能不存在与bot程序所在的主机上
                SyncResponse<DownloadFileResp> syncResponse = bot.downloadFile(
                        pathConfig.webLogsPath() + "/" + file.getName() + "?t=" + System.currentTimeMillis(),
                        1, null, 30 * 1000);

                if(syncResponse.getData() == null || Strings.isBlank(syncResponse.getData().getFile())){
                    bot.sendPrivateMessage(message.getUserId(),"上传日志失败，napcat下载文件失败",true);
                    return;
                }
                SyncResponse<String> response = bot.uploadPrivateFile(message.getUserId(), syncResponse.getData().getFile(), file.getName(), 60 * 1000);
                if (!response.isSuccess()) {
                    bot.sendPrivateMessage(message.getUserId(),"上传日志失败\n"+response.getMessage(),true);
                }
            }catch (Exception e){
                log.error("上传日志文件异常",e);
            }
        }
    }

}
