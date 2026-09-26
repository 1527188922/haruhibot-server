package com.haruhi.botserver.features.jmcomic.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务面板的JSON字段名是前后端约定，这里把它固定下来，避免字段改名后前端静默失效
 */
class JmTaskSnapshotJsonTest {

    @Test
    void snapshotJsonKeepsFrontendContract() throws Exception {
        JmTaskInfo running = new JmTaskInfo();
        running.setTaskId("task-1");
        running.setAid("452699");
        running.setAlbumName("本子名");
        running.setCoverUrl("https://example.com/cover.jpg");
        running.setAction(JmTaskAction.DOWNLOAD.getCode());
        running.setActionName(JmTaskAction.DOWNLOAD.getActionName());
        running.setStatus(JmTaskStatusEnum.RUNNING.name());
        running.setStatusName(JmTaskStatusEnum.RUNNING.getStatusName());
        running.setCancellable(false);
        running.setEnqueueTime(1L);
        running.setStartTime(2L);
        running.setStage("下载漫画图片");
        running.setChapterIndex(2);
        running.setChapterTotal(5);
        running.setChapterTitle("第2话");
        running.setImageTotal(60);
        running.setImageDownloaded(45);

        JmTaskSnapshot snapshot = new JmTaskSnapshot();
        snapshot.setParallel(false);
        snapshot.setRunningList(List.of(running));
        snapshot.setQueuedList(List.of());
        snapshot.setFinishedList(List.of());
        snapshot.setPollIntervalMillis(2000L);
        JmTaskSnapshot.Counters counters = new JmTaskSnapshot.Counters();
        counters.setRunning(1);
        snapshot.setCounters(counters);

        String json = new ObjectMapper().writeValueAsString(snapshot);

        for (String field : List.of("\"parallel\"", "\"runningList\"", "\"queuedList\"", "\"finishedList\"",
                "\"counters\"", "\"pollIntervalMillis\"", "\"taskId\"", "\"aid\"", "\"albumName\"", "\"coverUrl\"",
                "\"action\"", "\"actionName\"", "\"status\"", "\"statusName\"", "\"cancellable\"", "\"queuePosition\"",
                "\"enqueueTime\"", "\"startTime\"", "\"endTime\"", "\"waitMillis\"", "\"costMillis\"", "\"message\"",
                "\"stage\"", "\"chapterIndex\"", "\"chapterTotal\"", "\"chapterTitle\"", "\"imageTotal\"",
                "\"imageDownloaded\"", "\"running\"", "\"queued\"", "\"success\"", "\"fail\"", "\"cancelled\"")) {
            assertTrue(json.contains(field), "任务快照JSON缺少字段:" + field);
        }
    }
}
