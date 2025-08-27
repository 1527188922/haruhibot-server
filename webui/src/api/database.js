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


export const executeSql = (data) => request({
    url: `${baseUrl}/sys/db/execute`,
    method: 'post',
    timeout:10 * 60 * 1000,
    data
});


export const getSqlCache = () => request({
    url: `${baseUrl}/sys/db/sql`,
    method: 'get',
    timeout:10 * 1000
});
export const saveSqlCache = (data) => request({
    url: `${baseUrl}/sys/db/sql`,
    method: 'post',
    timeout:10 * 1000,
    data
});