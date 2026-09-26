package com.haruhi.botserver.features.jmcomic.service;

import com.haruhi.botserver.features.jmcomic.model.JmTaskAction;
import com.haruhi.botserver.features.jmcomic.model.JmTaskInfo;
import com.haruhi.botserver.features.jmcomic.model.JmTaskSnapshot;
import com.haruhi.botserver.features.jmcomic.model.JmTaskStatusEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JM任务推送的去重签名测试
 */
class JmTaskPushServiceTest {

    @Test
    void signatureIgnoresTimeFields() {
        JmTaskSnapshot first = snapshot(3, 10, 100L, 200L);
        JmTaskSnapshot later = snapshot(3, 10, 9999L, 8888L);

        assertEquals(JmTaskPushService.signature(first), JmTaskPushService.signature(later),
                "只有等待/耗时变化的快照不应触发推送");
    }

    @Test
    void signatureChangesWithImageProgress() {
        JmTaskSnapshot first = snapshot(3, 10, 100L, 200L);
        JmTaskSnapshot progressed = snapshot(4, 10, 100L, 200L);

        assertNotEquals(JmTaskPushService.signature(first), JmTaskPushService.signature(progressed));
    }

    @Test
    void signatureChangesWithStatusAndCounters() {
        JmTaskSnapshot first = snapshot(3, 10, 0L, 0L);
        JmTaskSnapshot second = snapshot(3, 10, 0L, 0L);
        first.getRunningList().getFirst().setStatus(JmTaskStatusEnum.FAIL.name());
        second.getCounters().setRunning(0);
        second.getCounters().setFail(1);

        assertNotEquals(JmTaskPushService.signature(first), JmTaskPushService.signature(second));
    }

    @Test
    void signatureHandlesNullAndEmpty() {
        assertEquals("", JmTaskPushService.signature(null));
        String empty = JmTaskPushService.signature(new JmTaskSnapshot());
        assertFalse(empty.isEmpty());
        assertTrue(empty.contains("false"), "并行标记应包含在签名中");
    }

    private JmTaskSnapshot snapshot(int downloaded, int total, Long waitMillis, Long costMillis) {
        JmTaskInfo running = new JmTaskInfo();
        running.setTaskId("task-1");
        running.setAid("452699");
        running.setAction(JmTaskAction.DOWNLOAD.getCode());
        running.setActionName(JmTaskAction.DOWNLOAD.getActionName());
        running.setStatus(JmTaskStatusEnum.RUNNING.name());
        running.setStatusName(JmTaskStatusEnum.RUNNING.getStatusName());
        running.setStage("下载漫画图片");
        running.setChapterIndex(1);
        running.setChapterTotal(2);
        running.setImageDownloaded(downloaded);
        running.setImageTotal(total);
        running.setWaitMillis(waitMillis);
        running.setCostMillis(costMillis);

        JmTaskSnapshot snapshot = new JmTaskSnapshot();
        snapshot.setRunningList(List.of(running));
        snapshot.setQueuedList(List.of());
        snapshot.setFinishedList(List.of());
        JmTaskSnapshot.Counters counters = new JmTaskSnapshot.Counters();
        counters.setRunning(1);
        snapshot.setCounters(counters);
        return snapshot;
    }
}
