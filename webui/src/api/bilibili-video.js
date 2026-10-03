import request from '@/router/axios';
import { baseUrl } from '@/config/env';

const timeout = 60 * 1000;

/**
 * bilibili视频分页列表
 * @param data {bvid,title,ownerMid,ownerName,tag,currentPage,pageSize}
 */
export const search = (data) => request({
  url: baseUrl + '/bilibili/video/search',
  method: 'post',
  timeout,
  data
});

/**
 * 按bv号添加视频：文本里解析bv号后调用b站详情接口入库
 * @param data {text} 支持bv号/av号/视频链接/短链接
 */
export const add = (data) => request({
  url: baseUrl + '/bilibili/video/add',
  method: 'post',
  timeout,
  data
});

/**
 * 重新拉取视频详情
 * @param data {id}
 */
export const refresh = (data) => request({
  url: baseUrl + '/bilibili/video/refresh',
  method: 'post',
  timeout,
  data
});

/**
 * 下载视频到服务器本地（异步，返回值只是"已开始下载"）
 * @param data {id}
 */
export const download = (data) => request({
  url: baseUrl + '/bilibili/video/download',
  method: 'post',
  timeout,
  data
});

/**
 * 查询下载状态，key为视频记录id
 * @param data {ids: []} 不传ids时返回全部
 */
export const downloadStatus = (data) => request({
  url: baseUrl + '/bilibili/video/download/status',
  method: 'post',
  timeout,
  data
});

/**
 * 批量删除视频记录（本地已下载的视频文件保留）
 * @param data {ids: []}
 */
export const deleteBatch = (data) => request({
  url: baseUrl + '/bilibili/video/deleteBatch',
  method: 'post',
  timeout,
  data
});
