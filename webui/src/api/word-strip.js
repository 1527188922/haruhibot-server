import request from '@/router/axios';
import { baseUrl } from '@/config/env';

export const search = (data) => request({
    url: baseUrl + '/wordStrip/search',
    method: 'post',
    data
});


export const refresh = () => request({
    url: baseUrl + '/wordStrip/refresh',
    method: 'post'
});

export const deleteBatch = (data) => request({
    url: baseUrl + '/wordStrip/deleteBatch',
    method: 'post',
    data
});

