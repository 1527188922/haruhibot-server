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
