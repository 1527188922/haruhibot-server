import request from '@/router/axios';
import { baseUrl } from '@/config/env';

export const search = (data) => request({
    url: baseUrl + '/group/search',
    method: 'post',
    timeout:60 * 1000,
    data
});

/**
 *
 * @param params{botId}
 * @returns {AxiosPromise}
 */
export const refresh = (params) => request({
    url: baseUrl + `/group/refresh`,
    method: 'post',
    timeout:60 * 1000,
    params
});


export const codeNameList = (data) => request({
    url: baseUrl + `/group/list`,
    method: 'post',
    timeout:60 * 1000,
    data
});

/**
 * 群成员分页查询
 * @param data{selfId,groupId,userId,nickname,card,leftFlag,currentPage,pageSize}
 */
export const searchMember = (data) => request({
    url: baseUrl + '/group/member/search',
    method: 'post',
    timeout:60 * 1000,
    data
});

/**
 * 刷新指定群的群成员
 * @param params{botId,groupId}
 */
export const refreshMember = (params) => request({
    url: baseUrl + '/group/member/refresh',
    method: 'post',
    timeout:60 * 1000,
    params
});