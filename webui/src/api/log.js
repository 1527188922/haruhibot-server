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
        onError: (error) => {

        },
        onClose: (reason) => {

        }
    });
};