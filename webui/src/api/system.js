import request from '@/router/axios';
import { baseUrl } from '@/config/env';

/**
 *
 * @param data{parentPath}
 * @returns {AxiosPromise}
 */
export const findFileNodes = (data) => request({
    url: baseUrl + '/sys/fileNodes',
    method: 'post',
    data
});