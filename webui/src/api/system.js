import request from '@/router/axios';
import { baseUrl } from '@/config/env';

/**
 *
 * @param action
 * @param data 对应接口的params字段
 * @param params {botId,async,echo,timeout}
 * @returns {AxiosPromise}
 */
export const botAction = (action,data,params) => request({
    url: `${baseUrl}/sys/bot/action/${action}`,
    method: 'post',
    params,
    data
})

/**
 *
 * @param data{parentPath}
 * @param rootType 1=服务器根目录 2=bot程序根目录
 * @returns {AxiosPromise}
 */
export const findFileNodes = (data,rootType = '1') => request({
    url: `${baseUrl}/sys/file/nodes?rootType=${rootType}`,
    method: 'post',
    data
});

/**
 *
 * @param data{path:绝对路径}
 * @returns {AxiosPromise}
 */
export const readFileContent = (data) => request({
    url: baseUrl + '/sys/file/readContent',
    method: 'post',
    data
})


export const deleteFile = (data,rootType,password) => request({
    url: `${baseUrl}/sys/file/delete?rootType=${rootType}&password=${password}`,
    method: 'post',
    data
})


/**
 *
 * @param data{path:绝对路径}
 * @returns {AxiosPromise}
 */
export const downloadFile = (data) =>request({
    url: baseUrl + '/sys/file/download',
    method: 'post',
    data,
    responseType:'blob'
})


export const saveFile = (data) => request({
    url: baseUrl + '/sys/file/save',
    method: 'post',
    data
})


export const botWsInfo = () => request({
    url: baseUrl + '/sys/botws/info',
    method: 'get'
})

export const botWsOperation = (command) => request({
    url: `${baseUrl}/sys/botws/opt?command=${command}`,
    method: 'post'
})