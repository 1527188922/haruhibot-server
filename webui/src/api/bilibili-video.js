import request from '@/router/axios';
import { baseUrl } from '@/config/env';

const timeout = 60 * 1000;

/**
 * bilibili视频分页列表
 * @param data {bvid,title,ownerMid,ownerName,tag,tagExact,currentPage,pageSize}
 */
export const search = (data) => request({
  url: baseUrl + '/bilibili/video/search',
  method: 'post',
  timeout,
  data
});

/**
 * 所有视频作者（附带视频数），左侧"按作者"分组列表
 */
export const authors = () => request({
  url: baseUrl + '/bilibili/video/authors',
  method: 'post',
  timeout,
  data: {}
});

/**
 * 所有标签（附带视频数），左侧"按标签"分组列表
 */
export const tags = () => request({
  url: baseUrl + '/bilibili/video/tags',
  method: 'post',
  timeout,
  data: {}
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
 * 下载视频到服务器本地（异步，同一个bvid+cid只会有一个任务）
 * 进度通过WebSocket主题 bilibili.video.download 推送
 * @param data {id}
 */
export const download = (data) => request({
  url: baseUrl + '/bilibili/video/download',
  method: 'post',
  timeout,
  data
});

/**
 * 下载任务快照，WebSocket未连通时轮询兜底
 */
export const downloadTasks = () => request({
  url: baseUrl + '/bilibili/video/download/tasks',
  method: 'post',
  timeout,
  data: {}
});

/**
 * 批量删除视频
 * @param data {ids: [], deleteData: 是否删除数据库记录, deleteFile: 是否删除本地视频文件}
 *             两个勾选项互相独立，勾了哪个删哪个
 */
export const deleteBatch = (data) => request({
  url: baseUrl + '/bilibili/video/deleteBatch',
  method: 'post',
  timeout,
  data
});
