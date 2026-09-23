//package com.haruhi.botserver.administration.handler;
//
//import cn.hutool.core.io.LineHandler;
//import cn.hutool.core.io.file.Tailer;
//import com.haruhi.botserver.shared.annotation.SuperuserAuthentication;
//import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
//import com.haruhi.botserver.shared.constant.RegexEnum;
//import com.haruhi.botserver.integration.onebot.model.MessageTypeEnum;
//import com.haruhi.botserver.integration.onebot.model.Message;
//import com.haruhi.botserver.integration.onebot.model.MessageHolder;
//import com.haruhi.botserver.event.message.IPrivateMessageEvent;
//import com.haruhi.botserver.service.DictionarySqliteService;
//import com.haruhi.botserver.shared.util.FileUtil;
//import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
//import com.haruhi.botserver.bot.session.Bot;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.io.File;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//
//
//@Slf4j
//@Component
//public class LogMonitorHandler implements IPrivateMessageEvent {
//
//	private final String filename = "haruhibot.log";
//
//	private volatile static Tailer tailer;
//
//	@Override
//	public int weight() {
//		return HandlerWeightEnum.W_330.getWeight();
//	}
//
//	@Override
//	public String funName() {
//		return HandlerWeightEnum.W_330.getName();
//	}
//	@Autowired
//	private DictionarySqliteService dictionarySqliteService;
//
//
//	@SuperuserAuthentication
//	@Override
//	public boolean onPrivate(Bot bot, final Message message) {
//
//		if (message.getRawMessage().matches(RegexEnum.START_MONITOR_LOG.getValue())) {
//
//			ThreadPoolUtil.getHandleCommandPool().execute(()->{
//				if(tailer == null){
//					startTailer(true, StandardCharsets.UTF_8, s -> {
//                        // 这里不用抓异常 如果发发生异常就让这个线程中断
//                        bot.sendMessage(dictionarySqliteService.getBotSuperUsers().get(0),null, MessageTypeEnum.privat.getType(),MessageHolder.instanceText(s));
//                    });
//					bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("已开启\n日志将实时发送给第一个超级用户"));
//
//				}else {
//					bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("已处于开启状态"));
//				}
//
//			});
//
//			return true;
//		}else if(message.getRawMessage().matches(RegexEnum.STOP_MONITOR_LOG.getValue())){
//			ThreadPoolUtil.getHandleCommandPool().execute(()->{
//				if(tailer != null){
//					stopTailer();
//					bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("已关闭"));
//				}else {
//					bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("已处于关闭状态"));
//				}
//			});
//			return true;
//		}
//
//		return false;
//	}
//
//
//	private void startTailer(boolean async, Charset charset,LineHandler handler){
//		synchronized (LogMonitorHandler.class){
//			if(tailer == null && !dictionarySqliteService.getBotSuperUsers().isEmpty()){
//				tailer = new Tailer(new File(FileUtil.getLogsDir() + File.separator + filename), charset, handler);
//				tailer.start(async);
//			}
//		}
//	}
//
//	private void stopTailer(){
//		synchronized (LogMonitorHandler.class){
//			if(tailer != null){
//				tailer.stop();
//				tailer = null;
//			}
//		}
//	}
//}
//
