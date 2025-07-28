import request from '@/router/axios';
import { baseUrl } from '@/config/env';

export const search = (data) => request({
    url: baseUrl + '/dict/search',
    method: 'post',
    timeout:60 * 1000,
    data
});

export const refresh = () => request({
    url: baseUrl + `/dict/refresh`,
    method: 'post',
    timeout:60 * 1000
});