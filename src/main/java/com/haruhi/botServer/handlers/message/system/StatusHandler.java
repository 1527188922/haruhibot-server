package com.haruhi.botServer.handlers.message.system;

import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
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
    private DataBaseConfig dataBaseConfig;

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
            sysPropStr.append("\nPID："+ SystemInfo.PID).append("\n");
            sysPropStr.append("profile："+ SystemInfo.PROFILE).append("\n");
            sysPropStr.append("WEB Path："+ pathConfig.webHomePath()).append("\n");
            sysPropStr.append("ContextPath：" + BotConfig.CONTEXT_PATH).append("\n");
            sysPropStr.append("AccessToken：" + BotConfig.ACCESS_TOKEN).append("\n");
            sysPropStr.append("识图key：" + BotConfig.SEARCH_IMAGE_KEY).append("\n");
            sysPropStr.append("超级用户：" + BotConfig.SUPERUSERS).append("\n");
            sysPropStr.append("程序路径："+ FileUtil.getAppDir()).append("\n");
            sysPropStr.append("------数据库信息------ \n");
            sysPropStr.append("数据库名称：" + dataBaseConfig.getMasterDBName()).append("\n");
            sysPropStr.append("IP:PORT：" + dataBaseConfig.getMasterHost() + ":" + dataBaseConfig.getMasterPort()).append("\n");
            sysPropStr.append("username：" + dataBaseConfig.getMasterUsername()).append("\n");
            sysPropStr.append("password：" + dataBaseConfig.getMasterPassword());
        }

        return sysPropStr.toString();
    }


}
