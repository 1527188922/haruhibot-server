import request from '@/router/axios';
import { baseUrl } from '@/config/env';

/**
 * AI 概览：今日用量、运行状态、余额
 */
export const overview = () => request({
    url: baseUrl + '/ai/overview',
    method: 'post',
    timeout: 30 * 1000
});

/**
 * 会话分页查询
 * @param data {currentPage, pageSize, selfId, scene, targetId, roleName, status, datetimerange}
 */
export const sessionSearch = (data) => request({
    url: baseUrl + '/ai/session/search',
    method: 'post',
    data
});

/**
 * 会话详情统计
 * @param data {id}
 */
export const sessionDetail = (data) => request({
    url: baseUrl + '/ai/session/detail',
    method: 'post',
    data
});

/**
 * 最近对话
 * @param data {id, limit}
 */
export const sessionMessages = (data) => request({
    url: baseUrl + '/ai/session/messages',
    method: 'post',
    data
});

/**
 * 长期记忆
 * @param data {id}
 */
export const sessionMemories = (data) => request({
    url: baseUrl + '/ai/session/memories',
    method: 'post',
    data
});

/**
 * 最近调用记录
 * @param data {id, limit}
 */
export const sessionUsage = (data) => request({
    url: baseUrl + '/ai/session/usage',
    method: 'post',
    data
});

/**
 * 角色卡列表
 */
export const roleList = () => request({
    url: baseUrl + '/ai/role/list',
    method: 'post'
});

/**
 * 角色卡预览
 * @param data {name}
 */
export const rolePreview = (data) => request({
    url: baseUrl + '/ai/role/preview',
    method: 'post',
    data
});

/**
 * 重新扫描角色卡目录
 */
export const roleRescan = () => request({
    url: baseUrl + '/ai/role/rescan',
    method: 'post'
});

/**
 * 角色卡结构化编辑数据
 * @param data {name}
 */
export const roleDetail = (data) => request({
    url: baseUrl + '/ai/role/detail',
    method: 'post',
    data
});

/**
 * 保存 / 新建 / 预览角色卡
 * @param data {originalName, name, triggers, sections, baseMtime, baseHash, dryRun}
 */
export const roleSave = (data) => request({
    url: baseUrl + '/ai/role/save',
    method: 'post',
    data
});

/**
 * 复制角色卡
 * @param data {sourceName, targetName, overwrite}
 */
export const roleCopy = (data) => request({
    url: baseUrl + '/ai/role/copy',
    method: 'post',
    data
});

/**
 * 角色名检查 / 复制建议名
 * @param data {name, originalName, sourceName}
 */
export const roleNameCheck = (data) => request({
    url: baseUrl + '/ai/role/nameCheck',
    method: 'post',
    data
});

/**
 * 暂停 / 恢复会话
 * @param data {id, status: active|paused}
 */
export const sessionStatus = (data) => request({
    url: baseUrl + '/ai/session/status',
    method: 'post',
    data
});

/**
 * 切换会话角色；roleName 为空表示恢复默认解析
 * @param data {id, roleName}
 */
export const sessionRole = (data) => request({
    url: baseUrl + '/ai/session/role',
    method: 'post',
    data
});

/**
 * 重置会话历史
 * @param data {id}
 */
export const sessionReset = (data) => request({
    url: baseUrl + '/ai/session/reset',
    method: 'post',
    data
});

/**
 * 用量趋势
 * @param data {days}
 */
export const usageTrend = (data) => request({
    url: baseUrl + '/ai/usage/trend',
    method: 'post',
    data
});

/**
 * 用量分布
 * @param data {days}
 */
export const usageBreakdown = (data) => request({
    url: baseUrl + '/ai/usage/breakdown',
    method: 'post',
    data
});

/**
 * 提示词预览
 * @param data {selfId, scene, targetId, roleName, userName, userId, text, ambientText}
 */
export const promptPreview = (data) => request({
    url: baseUrl + '/ai/prompt/preview',
    method: 'post',
    data,
    timeout: 30 * 1000
});

