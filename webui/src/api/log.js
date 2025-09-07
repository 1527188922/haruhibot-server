
import SSEClient from './sse-client';
import { baseUrl } from '@/config/env';
import { getToken, getUsername } from '@/util/auth';
import website from '@/config/website';

// 创建SSE客户端实例
export const createLogSSEClient = (initLine, handleMessage) => {
    // 配置请求头
    const headers = {};

    headers[website.Authorization] = getToken();
    headers[website.headerUserNameKey] = getUsername();

    // 创建客户端实例
    const sseClient = new SSEClient({
        url: `${baseUrl}/sys/log/tail`,
        params: { initLine },
        headers,
        onMessage: handleMessage,
        onOpen: () => {

        },
        onError: (error) => {

        },
        onClose: (reason) => {

        }
    });

    // 启动连接
    sseClient.connect();

    // 返回关闭方法
    return () => sseClient.close();
};