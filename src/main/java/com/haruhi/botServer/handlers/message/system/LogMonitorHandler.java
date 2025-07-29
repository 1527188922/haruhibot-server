package com.haruhi.botServer.handlers.message.system;

import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.Tailer;
import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.event.message.IPrivateMessageEvent;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


@Slf4j
@Component
public class LogMonitorHandler implements IPrivateMessageEvent {

	private final String filename = "haruhibot.log";

	private volatile static Tailer tailer;

	@Override
	public int weight() {
		return HandlerWeightEnum.W_330.getWeight();
	}

	@Override
	public String funName() {
		return HandlerWeightEnum.W_330.getName();
	}

	@Autowired
	private DictionarySqliteService dictionarySqliteService;


	@SuperuserAuthentication
	@Override
	public boolean onPrivate(Bot bot, final Message message) {

		if (message.getRawMessage().matches(RegexEnum.START_MONITOR_LOG.getValue())) {

			ThreadPoolUtil.getHandleCommandPool().execute(()->{
				if(tailer == null){
					startTailer(true,StandardCharsets.UTF_8, s -> {
                        // 这里不用抓异常 如果发发生异常就让这个线程中断
                        bot.sendMessage(dictionarySqliteService.getSuperUsers().get(0),null, MessageTypeEnum.privat.getType(),MessageHolder.instanceText(s));
                    });
					bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("已开启\n日志将实时发送给第一个超级用户"));

				}else {
					bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("已处于开启状态"));
				}

			});

			return true;
		}else if(message.getRawMessage().matches(RegexEnum.STOP_MONITOR_LOG.getValue())){
			ThreadPoolUtil.getHandleCommandPool().execute(()->{
				if(tailer != null){
					stopTailer();
					bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("已关闭"));
				}else {
					bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("已处于关闭状态"));
				}
			});
			return true;
		}

		return false;
	}


	private void startTailer(boolean async, Charset charset,LineHandler handler){
		synchronized (LogMonitorHandler.class){
			if(tailer == null && !dictionarySqliteService.getSuperUsers().isEmpty()){
				tailer = new Tailer(new File(FileUtil.getLogsDir() + File.separator + filename), charset, handler);
				tailer.start(async);
			}
		}
	}

	private void stopTailer(){
		synchronized (LogMonitorHandler.class){
			if(tailer != null){
				tailer.stop();
				tailer = null;
			}
		}
	}
}

