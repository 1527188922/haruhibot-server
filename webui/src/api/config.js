import request from '@/router/axios';
import { baseUrl } from '@/config/env';

/**
 * 全部配置（按配置文件分组）
 */
export const list = () => request({
    url: baseUrl + '/config/list',
    method: 'post',
    timeout: 30 * 1000
});

/**
 * 读取配置文件原始内容
 * @param data {fileName}
 */
export const fileContent = (data) => request({
    url: baseUrl + '/config/file/content',
    method: 'post',
    data
});

/**
 * 保存单个配置项
 * @param data {key, value}
 */
export const save = (data) => request({
    url: baseUrl + '/config/save',
    method: 'post',
    data
});

/**
 * 批量保存
 * @param data {items:[{key,value}]}
 */
export const batchSave = (data) => request({
    url: baseUrl + '/config/batchSave',
    method: 'post',
    data
});

/**
 * 重置为默认值
 * @param data {key}
 */
export const reset = (data) => request({
    url: baseUrl + '/config/reset',
    method: 'post',
    data
});

/**
 * 单key刷新（重新读取该key所在文件）
 * @param data {key}
 */
export const refresh = (data) => request({
    url: baseUrl + '/config/refresh',
    method: 'post',
    data
});

/**
 * 文件级刷新（重新读取整个文件）
 * @param data {fileName}
 */
export const refreshFile = (data) => request({
    url: baseUrl + '/config/refreshFile',
    method: 'post',
    data
});

/**
 * 重新加载全部配置文件
 */
export const refreshAll = () => request({
    url: baseUrl + '/config/refreshAll',
    method: 'post'
});
