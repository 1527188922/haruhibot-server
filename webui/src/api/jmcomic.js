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
