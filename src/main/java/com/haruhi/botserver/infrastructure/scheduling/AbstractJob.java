package com.haruhi.botserver.infrastructure.scheduling;

import org.quartz.Job;

public abstract class AbstractJob implements Job {
    public abstract String cronExpression();
}
