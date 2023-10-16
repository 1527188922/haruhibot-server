package com.haruhi.botServer.handlers.message.system;

import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.Tailer;
import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.path.AbstractPathConfig;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IPrivateMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


@Slf4j
@Component
public class LogMonitorHandler implements IPrivateMessageEvent {

	@Value("${log.path}")
	private String logPath;
	@Value("${log.prefix}")
	private String logPrefix;
	@Value("${log.keep}")
	private String logKeep;
	@Value("${log.separator}")
	private String logSeparator;
	@Value("${log.suffix}")
	private String logSuffix;

	@Autowired
	private AbstractPathConfig pathConfig;

	private static File LOG_FILE;

	private volatile static Tailer tailer;

	
	@PostConstruct
	public void initFile() {
		String logFileName = logPrefix + logKeep + logSeparator + logSuffix;
		String filePath = pathConfig.applicationHomePath() + File.separator + logPath;
		LOG_FILE = new File(filePath + File.separator + logFileName);

	}

	@Override
	public int weight() {
		return 62;
	}

	@Override
	public String funName() {
		return "实时日志";
	}


	@SuperuserAuthentication
	@Override
	public boolean onPrivate(final WebSocketSession session,final Message message,final String command) {

		if (command.matches(RegexEnum.START_MONITOR_LOG.getValue())) {

			ThreadPoolUtil.getHandleCommandPool().execute(()->{
				if(tailer == null){
					startTailer(true,StandardCharsets.UTF_8, new LogFileLineHandler(session));
					Server.sendPrivateMessage(session,message.getUserId(),"已开启\n日志将实时发送给第一个超级用户",true);
				}else {
					Server.sendPrivateMessage(session,message.getUserId(),"已处于开启状态",true);
				}

			});

			return true;
		}else if(command.matches(RegexEnum.STOP_MONITOR_LOG.getValue())){
			ThreadPoolUtil.getHandleCommandPool().execute(()->{
				if(tailer != null){
					stopTailer();
					Server.sendPrivateMessage(session,message.getUserId(),"已关闭",true);
				}else {
					Server.sendPrivateMessage(session,message.getUserId(),"已处于关闭状态",true);
				}
			});
			return true;
		}

		return false;
	}


	private void startTailer(boolean async, Charset charset,LogFileLineHandler handler){
		synchronized (LogMonitorHandler.class){
			if(tailer == null && BotConfig.SUPERUSERS.size() > 0){
				tailer = new Tailer(LOG_FILE, charset, handler);
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

		private final WebSocketSession session;


		LogFileLineHandler(final WebSocketSession session){
			this.session = session;
		}

		@Override
		public void handle(String s) {
			// 这里不用抓异常 如果发发生异常就让这个线程中断
			Server.sendPrivateMessage(session, BotConfig.SUPERUSERS.get(0),s,true);
		}
	}
}

