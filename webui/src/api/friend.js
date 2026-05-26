import request from '@/router/axios';
import { baseUrl } from '@/config/env';

export const search = (data) => request({
    url: baseUrl + '/friend/search',
    method: 'post',
    timeout:60 * 1000,
    data
});
