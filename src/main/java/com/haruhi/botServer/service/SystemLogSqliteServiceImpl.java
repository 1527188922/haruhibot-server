package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.entity.SystemLogSqlite;
import com.haruhi.botServer.mapper.SystemLogSqliteMapper;
import com.haruhi.botServer.vo.SystemLogQueryReq;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemLogSqliteServiceImpl extends ServiceImpl<SystemLogSqliteMapper, SystemLogSqlite>
        implements SystemLogSqliteService {

    @Override
    public IPage<SystemLogSqlite> search(SystemLogQueryReq request) {
        LambdaQueryWrapper<SystemLogSqlite> queryWrapper = new LambdaQueryWrapper<SystemLogSqlite>()
                .eq(StringUtils.isNotBlank(request.getTraceId()), SystemLogSqlite::getTraceId, request.getTraceId())
                .eq(StringUtils.isNotBlank(request.getLevel()), SystemLogSqlite::getLevel, request.getLevel())
                .like(StringUtils.isNotBlank(request.getBusinessModule()), SystemLogSqlite::getBusinessModule, request.getBusinessModule())
                .like(StringUtils.isNotBlank(request.getLoggerName()), SystemLogSqlite::getLoggerName, request.getLoggerName())
                .like(StringUtils.isNotBlank(request.getClassName()), SystemLogSqlite::getClassName, request.getClassName())
                .like(StringUtils.isNotBlank(request.getMethodName()), SystemLogSqlite::getMethodName, request.getMethodName())
                .like(StringUtils.isNotBlank(request.getThreadName()), SystemLogSqlite::getThreadName, request.getThreadName())
                .like(StringUtils.isNotBlank(request.getMessage()), SystemLogSqlite::getMessage, request.getMessage())
                .like(StringUtils.isNotBlank(request.getThrowable()), SystemLogSqlite::getThrowable, request.getThrowable())
                .eq(StringUtils.isNotBlank(request.getRequestMethod()), SystemLogSqlite::getRequestMethod, request.getRequestMethod())
                .like(StringUtils.isNotBlank(request.getRequestUri()), SystemLogSqlite::getRequestUri, request.getRequestUri())
                .like(StringUtils.isNotBlank(request.getClientIp()), SystemLogSqlite::getClientIp, request.getClientIp())
                .like(StringUtils.isNotBlank(request.getUserName()), SystemLogSqlite::getUserName, request.getUserName())
                .like(StringUtils.isNotBlank(request.getHandlerClass()), SystemLogSqlite::getHandlerClass, request.getHandlerClass())
                .like(StringUtils.isNotBlank(request.getHandlerMethod()), SystemLogSqlite::getHandlerMethod, request.getHandlerMethod());

        applyTimeRange(queryWrapper, request.getDatetimerange());
        queryWrapper.orderByDesc(SystemLogSqlite::getCreateTime).orderByDesc(SystemLogSqlite::getId);

        return this.page(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);
    }

    private void applyTimeRange(LambdaQueryWrapper<SystemLogSqlite> queryWrapper, List<String> datetimerange) {
        if (CollectionUtils.isEmpty(datetimerange)) {
            return;
        }
        String startTime = normalizeStartTime(datetimerange.get(0));
        if (StringUtils.isNotBlank(startTime)) {
            queryWrapper.ge(SystemLogSqlite::getCreateTime, startTime);
        }
        if (datetimerange.size() < 2) {
            return;
        }
        String endTime = normalizeEndTime(datetimerange.get(1));
        if (StringUtils.isNotBlank(endTime)) {
            queryWrapper.le(SystemLogSqlite::getCreateTime, endTime);
        }
    }

    private String normalizeStartTime(String time) {
        if (StringUtils.isBlank(time) || time.contains(".")) {
            return time;
        }
        return time + ".000";
    }

    private String normalizeEndTime(String time) {
        if (StringUtils.isBlank(time) || time.contains(".")) {
            return time;
        }
        return time + ".999";
    }
}
