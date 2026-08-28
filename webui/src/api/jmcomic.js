import request from '@/router/axios';
import { baseUrl } from '@/config/env';

export const searchAlbums = (data) => request({
  url: baseUrl + '/jmcomic/manage/album/search',
  method: 'post',
  data
});

export const requestAlbum = (aid) => request({
  url: baseUrl + `/jmcomic/manage/album/request/${aid}`,
  method: 'post'
});

export const deleteAlbums = (data) => request({
  url: baseUrl + '/jmcomic/manage/album/deleteBatch',
  method: 'post',
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
