import request from '@/router/axios';
import { baseUrl } from '@/config/env';

export const search = (data) => request({
    url: baseUrl + '/chatRecord/search',
    method: 'post',
    timeout:5 * 60 * 1000,
    data
});


export const searchV2 = (data) => request({
    url: baseUrl + '/chatRecord/v2/search',
    method: 'post',
    timeout:5 * 60 * 1000,
    data
});

export const selectExtend = (data) => request({
    url: baseUrl + '/chatRecord/extend',
    method: 'post',
    timeout:60 * 1000,
    data
});

export const selectExtendV2 = (data) => request({
    url: baseUrl + '/chatRecord/v2/extend',
    method: 'post',
    timeout:60 * 1000,
    data
});