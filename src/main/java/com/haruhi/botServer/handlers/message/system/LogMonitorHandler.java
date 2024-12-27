package com.haruhi.botServer.handlers.message.system;

import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.Tailer;
import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IPrivateMessageEvent;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
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


	@SuperuserAuthentication
	@Override
	public boolean onPrivate(Bot bot, final Message message) {

		if (message.getRawMessage().matches(RegexEnum.START_MONITOR_LOG.getValue())) {

			ThreadPoolUtil.getHandleCommandPool().execute(()->{
				if(tailer == null){
					startTailer(true,StandardCharsets.UTF_8, new LogFileLineHandler(bot));
					bot.sendPrivateMessage(message.getUserId(),"已开启\n日志将实时发送给第一个超级用户",true);
				}else {
					bot.sendPrivateMessage(message.getUserId(),"已处于开启状态",true);
				}

			});

			return true;
		}else if(message.getRawMessage().matches(RegexEnum.STOP_MONITOR_LOG.getValue())){
			ThreadPoolUtil.getHandleCommandPool().execute(()->{
				if(tailer != null){
					stopTailer();
					bot.sendPrivateMessage(message.getUserId(),"已关闭",true);
				}else {
					bot.sendPrivateMessage(message.getUserId(),"已处于关闭状态",true);
				}
			});
			return true;
		}

		return false;
	}


	private void startTailer(boolean async, Charset charset,LogFileLineHandler handler){
		synchronized (LogMonitorHandler.class){
			if(tailer == null && BotConfig.SUPERUSERS.size() > 0){
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

	private static class LogFileLineHandler implements LineHandler{

		private final Bot bot;


		LogFileLineHandler(final Bot bot){
			this.bot = bot;
		}

		@Override
		public void handle(String s) {
			// 这里不用抓异常 如果发发生异常就让这个线程中断
			bot.sendPrivateMessage(BotConfig.SUPERUSERS.get(0),s,true);
		}
	}
}

