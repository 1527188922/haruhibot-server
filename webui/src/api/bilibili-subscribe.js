import request from '@/router/axios';
import { baseUrl } from '@/config/env';

const timeout = 60 * 1000;

/**
 * bilibili订阅列表
 * @param data {uid,selfId,uname,subType,enableStatus,offNotify,currentPage,pageSize}
 */
export const search = (data) => request({
    url: baseUrl + '/bilibili/subscribe/search',
    method: 'post',
    timeout,
    data
});

/**
 * 新增订阅
 * @param data {uid,subType,selfId,groupIds,friendIds,enableStatus,offNotify}
 */
export const add = (data) => request({
    url: baseUrl + '/bilibili/subscribe/add',
    method: 'post',
    timeout,
    data
});

/**
 * 修改订阅
 */
export const update = (data) => request({
    url: baseUrl + '/bilibili/subscribe/update',
    method: 'post',
    timeout,
    data
});

/**
 * 修改推送的群与好友
 * @param data {id,groupIds:[],friendIds:[]}
 */
export const updateTargets = (data) => request({
    url: baseUrl + '/bilibili/subscribe/updateTargets',
    method: 'post',
    timeout,
    data
});

export const deleteBatch = (data) => request({
    url: baseUrl + '/bilibili/subscribe/deleteBatch',
    method: 'post',
    timeout,
    data
});

/**
 * 查询可选的推送目标(群/好友)
 * @param data {selfId,type:'group'|'friend',keyword,ids:[],pageSize}
 */
export const targetList = (data) => request({
    url: baseUrl + '/bilibili/subscribe/target/list',
    method: 'post',
    timeout,
    data
});

/**
 * 直播推送定时任务信息(是否开启、cron)
 */
export const jobInfo = () => request({
    url: baseUrl + '/bilibili/subscribe/job/info',
    method: 'post',
    timeout
});
