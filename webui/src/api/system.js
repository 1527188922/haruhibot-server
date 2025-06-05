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

/**
 *
 * @param data{path:绝对路径}
 * @returns {AxiosPromise}
 */
export const readFileContent = (data) => request({
    url: baseUrl + '/sys/readFileContent',
    method: 'post',
    data
});