package com.haruhi.botServer.job.schedule;

import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.config.service.ConfigApplier;
import com.haruhi.botServer.config.service.ConfigChange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定时任务管理
 * <p>
 * 任务的开关与cron表达式都来自 ./config/job.properties，并且是<b>热更新</b>的：
 * <ul>
 *     <li>把 enable 改成 true：立刻注册并启动任务</li>
 *     <li>把 enable 改成 false：立刻取消任务</li>
 *     <li>修改 cron：立刻重新排期，无需重启</li>
 * </ul>
 */
@Slf4j
@Component
public class JobManage implements ApplicationContextAware, CommandLineRunner, ConfigApplier {

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    private ApplicationContext applicationContext;

    /** className -> 实例，启动时装载一次 */
    private final Map<String, AbstractJob> jobs = new LinkedHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public synchronized void startAllJob() {
        jobs.clear();
        jobs.putAll(applicationContext.getBeansOfType(AbstractJob.class));
        if (jobs.isEmpty()) {
            return;
        }
        log.info("开始注册定时任务...");
        int count = 0;
        for (AbstractJob job : jobs.values()) {
            if (register(job)) {
                count++;
            }
        }
        log.info("共注册{}个定时任务", count);
    }

    // ==================================================================
    // 注册/取消
    // ==================================================================

    /**
     * 按配置注册任务，未开启则确保其处于未注册状态
     *
     * @return 是否已注册
     */
    private boolean register(AbstractJob job) {
        ConfigKey enableKey = enableKeyOf(job);
        if (enableKey == null) {
            log.warn("定时任务{}没有对应的配置项，已跳过", job.getClass().getSimpleName());
            return false;
        }
        if (!Configs.getBool(enableKey)) {
            unregister(job);
            return false;
        }
        ConfigKey cronKey = cronKeyOf(job);
        String cron = cronKey == null ? job.cronExpression() : Configs.getStr(cronKey, job.cronExpression());
        if (StringUtils.isBlank(cron)) {
            log.warn("定时任务{}未配置cron表达式，已跳过", job.getClass().getSimpleName());
            return false;
        }

        return doSchedule(job, cron);
    }

    private boolean doSchedule(AbstractJob job, String cron) {
        Class<? extends AbstractJob> jobClass = job.getClass();
        String simpleName = jobClass.getSimpleName();
        String name = simpleName + "_job";
        String triggerName = simpleName + "_trigger";
        String group = simpleName + "_group";
        JobKey jobKey = new JobKey(name, group);
        TriggerKey triggerKey = new TriggerKey(triggerName, group);

        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            if (scheduler.checkExists(triggerKey)) {
                // 已注册：cron没变就不动，变了就重新排期
                Trigger exist = scheduler.getTrigger(triggerKey);
                if (exist instanceof CronTrigger cronTrigger
                        && cron.equals(cronTrigger.getCronExpression())) {
                    return true;
                }
                scheduler.rescheduleJob(triggerKey, buildTrigger(triggerName, group, cron));
                log.info("定时任务 {} cron已更新为 {}", name, cron);
                return true;
            }

            JobDetail detail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
            scheduler.scheduleJob(detail, buildTrigger(triggerName, group, cron));
            log.info("定时任务 {} 启动成功 {}", name, cron);
            return true;
        } catch (SchedulerException e) {
            log.error("定时任务{}启动异常 cron:{}", simpleName, cron, e);
            return false;
        }
    }

    private Trigger buildTrigger(String triggerName, String group, String cron) {
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerName, group)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
    }

    private void unregister(AbstractJob job) {
        Class<? extends AbstractJob> jobClass = job.getClass();
        String simpleName = jobClass.getSimpleName();
        String group = simpleName + "_group";
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            TriggerKey triggerKey = new TriggerKey(simpleName + "_trigger", group);
            if (scheduler.checkExists(triggerKey)) {
                scheduler.unscheduleJob(triggerKey);
                scheduler.deleteJob(new JobKey(simpleName + "_job", group));
                log.info("定时任务 {} 已取消", simpleName);
            }
        } catch (SchedulerException e) {
            log.error("取消定时任务{}异常", simpleName, e);
        }
    }

    // ==================================================================
    // 配置热更新
    // ==================================================================

    @Override
    public Collection<ConfigKey> keys() {
        return ConfigKey.of(com.haruhi.botServer.config.config.ConfigFile.JOB);
    }

    @Override
    public void onConfigChange(ConfigChange change) {
        if (jobs.isEmpty()) {
            jobs.putAll(applicationContext.getBeansOfType(AbstractJob.class));
        }
        for (AbstractJob job : jobs.values()) {
            if (change.key().equals(enableKeyOf(job)) || change.key().equals(cronKeyOf(job))) {
                log.info("定时任务配置变更 {},重新应用 {}", change.keyName(), job.getClass().getSimpleName());
                register(job);
            }
        }
    }

    /**
     * 任务开关配置项。key命名规则：job.{任务名}.enable
     */
    private ConfigKey enableKeyOf(AbstractJob job) {
        return switch (job.getClass().getSimpleName()) {
            case "DownloadPixivJob" -> ConfigKey.JOB_DOWNLOAD_PIXIV_ENABLE;
            case "BilibiliLiveJob" -> ConfigKey.JOB_BILIBILI_LIVE_ENABLE;
            default -> null;
        };
    }

    /**
     * 任务cron配置项。key命名规则：job.{任务名}.cron
     */
    private ConfigKey cronKeyOf(AbstractJob job) {
        return switch (job.getClass().getSimpleName()) {
            case "DownloadPixivJob" -> ConfigKey.JOB_DOWNLOAD_PIXIV_CRON;
            case "BilibiliLiveJob" -> ConfigKey.JOB_BILIBILI_LIVE_CRON;
            default -> null;
        };
    }

    @Override
    public void run(String... args) {
        startAllJob();
    }
}
