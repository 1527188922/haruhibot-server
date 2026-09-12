package com.haruhi.botServer.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订阅推送目标(群号/qq号)解析与拼接
 * 数据库中以逗号分割的字符串存储，兼容中文逗号与空白符
 */
@Slf4j
public class PushTargetUtil {

    private PushTargetUtil() {
    }

    /**
     * 解析逗号分割的群号/qq号
     */
    public static List<Long> parseIds(String ids) {
        if (StringUtils.isBlank(ids)) {
            return Collections.emptyList();
        }
        return Arrays.stream(ids.split("[,，\\s]+"))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(id -> {
                    try {
                        return Long.parseLong(id);
                    } catch (NumberFormatException e) {
                        log.error("解析订阅推送目标异常 id:{}", id);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 将群号/qq号拼接为逗号分割的字符串
     */
    public static String joinIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return "";
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
