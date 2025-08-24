import request from '@/router/axios';
import { baseUrl } from '@/config/env';

export const databaseInfoNode = (data) => request({
    url: `${baseUrl}/sys/db/info`,
    method: 'post',
    timeout:60 * 1000,
    data
});



export const databaseDDL = (tableName) => request({
    url: `${baseUrl}/sys/db/ddl?tableName=${tableName}`,
    method: 'get',
    timeout:60 * 1000
});