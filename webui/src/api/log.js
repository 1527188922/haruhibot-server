import request from '@/router/axios';
import SSEClient from './sse-client';
import {baseUrl} from '@/config/env';
import {getToken, getUsername} from '@/util/auth';
import website from '@/config/website';

export const createLogSSEClient = (initLine, handleMessage) => {
    const headers = {};
    headers[website.Authorization] = getToken();
    headers[website.headerUserNameKey] = getUsername();

    return new SSEClient({
        url: `${baseUrl}/sys/log/tail`,
        params: {initLine},
        headers,
        onMessage: handleMessage,
        onOpen: () => {

        },
        onError: () => {

        },
        onClose: () => {

        }
    });
};

export const searchSystemLog = (data) => request({
    url: `${baseUrl}/sys/log/search`,
    method: 'post',
    timeout: 5 * 60 * 1000,
    data
});

export const enumList = (enumName) => request({
    url: `${baseUrl}/sys/enum/list?enumName=${enumName}`,
    method: 'get'
});
