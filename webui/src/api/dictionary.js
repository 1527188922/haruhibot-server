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

export const deleteBatch = (data) => request({
    url: baseUrl + '/dict/deleteBatch',
    method: 'post',
    data
});

export const add = (data) => request({
    url: baseUrl + '/dict/add',
    method: 'post',
    data
});

export const update = (data) => request({
    url: baseUrl + '/dict/update',
    method: 'post',
    data
});