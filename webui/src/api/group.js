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