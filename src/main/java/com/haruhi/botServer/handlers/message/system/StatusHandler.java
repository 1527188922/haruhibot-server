package com.haruhi.botServer.handlers.message.system;

import cn.hutool.core.util.RuntimeUtil;
import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.utils.system.SystemUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StatusHandler implements IAllMessageEvent {


    @Override
    public int weight() {
        return HandlerWeightEnum.W_300.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_300.getName();
    }

    @Autowired
    private DictionarySqliteService dictionaryService;

    @SuperuserAuthentication
    @Override
    public boolean onMessage(final Bot bot, final Message message) {

        if (message.getRawMessage().matches(RegexEnum.STATUS.getValue())) {
            ThreadPoolUtil.getHandleCommandPool().execute(()->{
                String properties = getProperties(message.getMessageType());
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),properties,true);

            });

            return true;
        }
        return false;
    }

    @Autowired
    private AbstractWebResourceConfig pathConfig;
    @Autowired
    private DictionarySqliteService dictionarySqliteService;

    private String getProperties(String messageType){
        StringBuilder sysPropStr = new StringBuilder("------系统信息------\n");
        sysPropStr.append(BotConfig.NAME).append("\n");
        sysPropStr.append("CPU负载：" + (Double.parseDouble(String.format("%.2f",SystemUtil.getOperatingSystemMXBeanJson().getDoubleValue(SystemUtil.OSXMB_KEY_SYSTEM_LOAD))) * 100d) + "%").append("\n");
        sysPropStr.append("可用内存："+ String.format("%.2f",SystemUtil.getFreePhysicalMemorySize() / 1024 / 1024 / 1024)).append("GB").append("\n");
        sysPropStr.append("总内存："+ String.format("%.2f",SystemUtil.getTotalPhysicalMemorySize() / 1024 / 1024 / 1024)).append("GB").append("\n");
        sysPropStr.append("可用存储："+ String.format("%.2f",SystemUtil.getFreeSpace() / 1024 / 1024 / 1024)).append("GB").append("\n");
        sysPropStr.append("总存储："+ String.format("%.2f",SystemUtil.getTotalSpace() / 1024 / 1024 / 1024)).append("GB").append("\n");
        sysPropStr.append("系统："+ SystemInfo.OS_NAME).append("\t" + SystemInfo.OS_VERSION).append("\n");
        sysPropStr.append("CPU核心数："+ SystemInfo.AVAILABLE_PROCESSORS);

        if(MessageTypeEnum.privat.getType().equals(messageType)){
            sysPropStr.append("\nPID："+ RuntimeUtil.getPid()).append("\n");
            sysPropStr.append("profile："+ SystemInfo.PROFILE).append("\n");
            sysPropStr.append("WEB Path："+ pathConfig.webHomePath()).append("\n");
            sysPropStr.append("ContextPath：" + BotConfig.CONTEXT_PATH).append("\n");
            sysPropStr.append("AccessToken：" + dictionaryService.getBotAccessToken()).append("\n");
            sysPropStr.append("超级用户：" + dictionarySqliteService.getSuperUsers()).append("\n");
            sysPropStr.append("程序路径："+ FileUtil.getAppDir());
        }

        return sysPropStr.toString();
    }


}
