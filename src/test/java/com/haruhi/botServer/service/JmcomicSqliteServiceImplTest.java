package com.haruhi.botServer.service;

import com.haruhi.botServer.dto.jmcomic.Chapter;
import com.haruhi.botServer.dto.jmcomic.Series;
import com.haruhi.botServer.entity.JmChapterImageSqlite;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JmcomicSqliteServiceImplTest {

    private final JmcomicSqliteServiceImpl service = new JmcomicSqliteServiceImpl();

    @Test
    void toChapterImageEntitiesSortsImageFilesByNumericFileNameBeforeAssigningImageSort() {
        Chapter chapter = new Chapter();
        chapter.setId(654321L);
        chapter.setImages(Arrays.asList("00010.webp", "00002.webp", "00001.webp"));
        Series series = new Series();
        series.setSort("3");
        series.setTitle("第3话");

        List<JmChapterImageSqlite> entities = service.toChapterImageEntities(123456L, chapter, series);

        assertEquals("00001.webp", entities.get(0).getImageFile());
        assertEquals(1, entities.get(0).getImageSort());
        assertEquals("00002.webp", entities.get(1).getImageFile());
        assertEquals(2, entities.get(1).getImageSort());
        assertEquals("00010.webp", entities.get(2).getImageFile());
        assertEquals(3, entities.get(2).getImageSort());
    }
}
