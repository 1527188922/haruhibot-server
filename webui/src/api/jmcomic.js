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
 * @param data {ids: [], collected: true|false}
 */
export const collectAlbums = (data) => request({
  url: baseUrl + '/jmcomic/manage/album/collect',
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
