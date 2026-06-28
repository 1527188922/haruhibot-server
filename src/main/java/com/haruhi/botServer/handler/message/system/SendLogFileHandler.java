//package com.haruhi.botServer.handlers.message.system;
//
//import com.haruhi.botServer.annotation.SuperuserAuthentication;
//import com.haruhi.botServer.cache.CacheMap;
//import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
//import com.haruhi.botServer.constant.HandlerWeightEnum;
//import com.haruhi.botServer.constant.RegexEnum;
//import com.haruhi.botServer.dto.qqclient.DownloadFileResp;
//import com.haruhi.botServer.dto.qqclient.Message;
//import com.haruhi.botServer.dto.qqclient.MessageHolder;
//import com.haruhi.botServer.dto.qqclient.SyncResponse;
//import com.haruhi.botServer.event.message.IPrivateMessageEvent;
//import com.haruhi.botServer.service.DictionarySqliteService;
//import com.haruhi.botServer.utils.FileUtil;
//import com.haruhi.botServer.utils.ThreadPoolUtil;
//import com.haruhi.botServer.ws.Bot;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.logging.log4j.util.Strings;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.io.File;
//import java.util.concurrent.TimeUnit;
//
//
///**
// * 该功能注意点:
// * 在dev环境下 该功能异常(gocq会下载文件失败)
// * 在pord坏境下且主机非公网上 enable-internet-host要设置为1
// */
//@Slf4j
//@Component
////@DependsOn("botConfig")
//public class SendLogFileHandler implements IPrivateMessageEvent {
//    @Override
//    public int weight() {
//        return HandlerWeightEnum.W_350.getWeight();
//    }
//
//    @Override
//    public String funName() {
//        return HandlerWeightEnum.W_350.getName();
//    }
//    private static CacheMap<String, File[]> cacheMap = null;
//    private static final int EXPIRE_TIME = 20;
//
//    @Autowired
//    private AbstractWebResourceConfig pathConfig;
//    @Autowired
//    private DictionarySqliteService dictionarySqliteService;
//
//
//    private void initCacheMap() {
//        if (cacheMap != null) {
//          return;
//        }
//        synchronized (SendLogFileHandler.class) {
//            if (cacheMap != null) {
//                return;
//            }
//            cacheMap = new CacheMap<>(EXPIRE_TIME, TimeUnit.SECONDS, dictionarySqliteService.getBotSuperUsers().isEmpty() ? 1 : dictionarySqliteService.getBotSuperUsers().size());
//        }
//    }
//
//    @SuperuserAuthentication
//    @Override
//    public boolean onPrivate(final Bot bot, final Message message) {
//        initCacheMap();
//        File[] files = cacheMap.get(key(message));
//        if (files != null) {
//            try {
//                int index = Integer.parseInt(message.getRawMessage()) - 1;
//
//                ThreadPoolUtil.getHandleCommandPool().execute(new UploadLogFileTask(files,index,bot,message));
//                return true;
//            }catch (NumberFormatException e){
//                return false;
//            }
//        }else if(message.getRawMessage().matches(RegexEnum.SEND_LOG.getValue())){
//
//            ThreadPoolUtil.getHandleCommandPool().execute(()->{
//
//                File[] files1 = new File(FileUtil.getLogsDir()).listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".log"));
//                if(files1 != null && files1.length > 0){
//                    StringBuilder builder = new StringBuilder();
//                    for (int i = 0; i < files1.length; i++) {
//                        builder.append((i + 1) + "：").append(files1[i].getName())
//                                .append("\n" + String.format("%.2f",(double)files1[i].length() / 1024 / 1024) + "MB").append("\n");
//                    }
//                    cacheMap.put(key(message),files1);
//                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText(builder.toString()));
//                }else {
//                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("没有日志文件"));
//                }
//            });
//
//            return true;
//        }
//        return false;
//    }
//
//    private String key(final Message message){
//        return String.valueOf(message.getSelfId()) + message.getUserId();
//    }
//
//    private class UploadLogFileTask implements Runnable{
//
//        private final File[] files;
//        private final Bot bot;
//        private final Message message;
//        private final int index;
//        UploadLogFileTask(File[] files,int index,Bot bot,Message message){
//            this.bot = bot;
//            this.index = index;
//            this.message = message;
//            this.files = files;
//        }
//
//        @Override
//        public void run() {
//            try {
//                if(index < 0 || index >= files.length){
//                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageHolder.instanceText("请输入范围内序号"));
//
//                    return;
//                }
//                File file = files[index];
//                if(file == null || !file.exists() || file.isDirectory()){
//                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageHolder.instanceText("文件不存在\n"+file));
//                    return;
//                }
//                // 注意该路径可能不存在与bot程序所在的主机上
//                SyncResponse<DownloadFileResp> syncResponse = bot.downloadFile(
//                        pathConfig.webLogsPath() + "/" + file.getName() + "?t=" + System.currentTimeMillis(),
//                        1, null, 30 * 1000);
//
//                if(syncResponse.getData() == null || Strings.isBlank(syncResponse.getData().getFile())){
//                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageHolder.instanceText("上传日志失败，napcat下载文件失败"));
//                    return;
//                }
//                SyncResponse<String> response = bot.uploadPrivateFile(message.getUserId(), syncResponse.getData().getFile(), file.getName(), 60 * 1000);
//                if (!response.isSuccess()) {
//                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageHolder.instanceText("上传日志失败\n"+response.getMessage()));
//                }
//            }catch (Exception e){
//                log.error("上传日志文件异常",e);
//            }
//        }
//    }
//
//}
