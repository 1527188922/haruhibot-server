import request from '@/router/axios';
import { baseUrl } from '@/config/env';

export const searchAlbums = (data) => request({
  url: baseUrl + '/jmcomic/manage/album/search',
  method: 'post',
  data
});

/**
 * 全部JM标签(已去重)，用于标签下拉候选
 */
export const allTags = () => request({
  url: baseUrl + '/jmcomic/manage/tags',
  method: 'get'
});

/**
 * 全部JM作者(已去重)，用于作者下拉候选
 */
export const allAuthors = () => request({
  url: baseUrl + '/jmcomic/manage/authors',
  method: 'get'
});

export const requestAlbum = (aid) => request({
  url: baseUrl + `/jmcomic/manage/album/request/${aid}`,
  method: 'post'
});

/**
 * JM在线搜索(调用JM服务器/search接口)
 * @param data {name: 关键字, sort: mr|mv|mp|tf, page: 页码(从1开始)}
 */
export const searchOnlineAlbums = (data) => request({
  url: baseUrl + '/jmcomic/manage/album/searchOnline',
  method: 'post',
  timeout: 30 * 1000,
  data
});

/**
 * JM在线搜索历史(按时间倒序，条数由 jm.search.history.limit 控制，不含结果快照)
 */
export const searchOnlineHistory = () => request({
  url: baseUrl + '/jmcomic/manage/album/searchOnline/history',
  method: 'get'
});

/**
 * JM在线搜索历史详情，含当时那一页的结果快照
 */
export const searchOnlineHistoryDetail = (id) => request({
  url: baseUrl + `/jmcomic/manage/album/searchOnline/history/${id}`,
  method: 'get'
});

/**
 * 删除JM在线搜索历史
 * @param data {ids: [], clearAll: 是否清空}
 */
export const deleteSearchOnlineHistory = (data) => request({
  url: baseUrl + '/jmcomic/manage/album/searchOnline/history/delete',
  method: 'post',
  data
});

export const downloadAlbum = (aid) => request({
  url: baseUrl + `/jmcomic/manage/album/download/${aid}`,
  timeout:60 * 1000,
  method: 'post'
});

export const generateAlbumZip = (aid) => request({
  url: baseUrl + `/jmcomic/manage/album/generateZip/${aid}`,
  timeout:60 * 1000,
  method: 'post'
});

export const generateAlbumPdf = (aid) => request({
  url: baseUrl + `/jmcomic/manage/album/generatePdf/${aid}`,
  timeout:60 * 1000,
  method: 'post'
});

export const deleteAlbums = (data) => request({
  url: baseUrl + '/jmcomic/manage/album/deleteBatch',
  method: 'post',
  data
});

/**
 * 收藏/取消收藏JM主记录
 * @param data {ids: [], collected: true|false,
 *              favoriteId?: 收藏夹id, favoriteName?: 收藏夹名称(不存在时自动新建),
 *              favoriteIds?: 多选收藏夹id列表, favoriteNames?: 多选收藏夹名称列表}
 */
export const collectAlbums = (data) => request({
  url: baseUrl + '/jmcomic/manage/album/collect',
  method: 'post',
  data
});

/* ==================== 收藏夹 ==================== */

/**
 * 收藏夹列表，含每个收藏夹下的漫画数量
 */
export const listFavorites = () => request({
  url: baseUrl + '/jmcomic/manage/favorite/list',
  method: 'get'
});

/**
 * 新建收藏夹
 * @param data {name: 收藏夹名称}
 */
export const createFavorite = (data) => request({
  url: baseUrl + '/jmcomic/manage/favorite/create',
  method: 'post',
  data
});

/**
 * 重命名收藏夹（默认收藏夹不支持）
 * @param data {id: 收藏夹id, name: 新名称}
 */
export const renameFavorite = (data) => request({
  url: baseUrl + '/jmcomic/manage/favorite/rename',
  method: 'post',
  data
});

/**
 * 删除收藏夹（默认收藏夹不支持）
 * 被删收藏夹下的漫画若不再属于其他收藏夹，会自动回落到默认收藏夹
 */
export const deleteFavorite = (id) => request({
  url: baseUrl + `/jmcomic/manage/favorite/delete/${id}`,
  method: 'post'
});

/**
 * 把漫画加入收藏夹（可多选收藏夹），收藏夹不存在时按名称新建
 * @param data {albumIds: [], favoriteId?, favoriteName?, favoriteIds?: [], favoriteNames?: []}
 */
export const addAlbumsToFavorite = (data) => request({
  url: baseUrl + '/jmcomic/manage/favorite/album/add',
  method: 'post',
  data
});

/**
 * 更改漫画所属的收藏夹：以传入的收藏夹集合为准，缺的补上、多的移出
 * （集合为空表示从所有收藏夹移出）
 * @param data {albumIds: [], favoriteIds?: [], favoriteNames?: []}
 */
export const saveAlbumFavorites = (data) => request({
  url: baseUrl + '/jmcomic/manage/favorite/album/save',
  method: 'post',
  data
});

/**
 * 把漫画移出收藏夹；不传 favoriteId 表示从所有收藏夹移出
 * @param data {albumIds: [], favoriteId?: 收藏夹id}
 */
export const removeAlbumsFromFavorite = (data) => request({
  url: baseUrl + '/jmcomic/manage/favorite/album/remove',
  method: 'post',
  data
});

export const deleteAllFile = (data) => request({
  url: baseUrl + '/jmcomic/manage/album/deleteAllFile',
  method: 'post',
  timeout:60 * 1000,
  data
});

export const searchChapterImages = (data) => request({
  url: baseUrl + '/jmcomic/manage/chapter-image/search',
  method: 'post',
  data
});

export const requestChapterImages = (data) => request({
  url: baseUrl + '/jmcomic/manage/chapter-image/request',
  method: 'post',
  data
});

export const deleteChapterImages = (data) => request({
  url: baseUrl + '/jmcomic/manage/chapter-image/deleteBatch',
  method: 'post',
  data
});

/**
 * 内存中的JM任务(运行中/排队中/最近完成)，不持久化，重启即清空
 */
export const listJmTasks = () => request({
  url: baseUrl + '/jmcomic/manage/task/list',
  method: 'get'
});

/**
 * 取消排队中的任务(正在执行的任务不支持取消)
 * @param taskId 任务id，来自任务列表
 */
export const cancelJmTask = (taskId) => request({
  url: baseUrl + `/jmcomic/manage/task/cancel/${taskId}`,
  method: 'post'
});

/**
 * 取消全部排队中的任务
 */
export const cancelQueuedJmTasks = () => request({
  url: baseUrl + '/jmcomic/manage/task/cancelQueued',
  method: 'post'
});

/**
 * 重试失败的任务：新建一个同JM同动作的任务提交，
 * 下载漫画会从失败的那一话开始续传
 * @param taskId 失败任务的id，来自任务列表
 */
export const retryJmTask = (taskId) => request({
  url: baseUrl + `/jmcomic/manage/task/retry/${taskId}`,
  method: 'post'
});
