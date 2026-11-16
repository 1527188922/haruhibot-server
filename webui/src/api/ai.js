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
