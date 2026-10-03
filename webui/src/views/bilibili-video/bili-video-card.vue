<template>
  <!--
    bilibili视频卡片。
    播放/下载/刷新/删除都通过事件交给父组件处理，卡片只负责展示与交互。
  -->
  <div class="bili-card" :class="{'bili-card-downloaded': row.downloaded}">
    <div class="bili-cover" @click="$emit('open-video', row)">
      <img v-if="coverUrl" :src="coverUrl" referrerpolicy="no-referrer" alt="">
      <div v-else class="bili-cover-empty"><i class="el-icon-picture-outline"></i></div>
      <span v-if="row.duration" class="bili-cover-duration">{{durationText}}</span>
      <span class="bili-cover-flag" :class="flagClass">{{flagText}}</span>
    </div>

    <div class="bili-card-body">
      <div class="bili-title" :title="row.title">{{row.title}}</div>

      <div class="bili-meta-line">
        <a class="bili-bvid" :href="videoUrl" target="_blank" rel="noopener noreferrer">{{row.bvid}}</a>
<!--        <span class="bili-cid" :title="`cid：${row.cid}，av号：${row.avid || ''}`">cid:{{row.cid}}</span>-->
        <el-checkbox class="bili-card-select" :value="selected"
                     @change="$emit('select', row)"></el-checkbox>
      </div>

      <div class="bili-owner" :title="`点击进入b站个人主页(uid: ${row.ownerMid})`"
           @click="openSpace">
        <img v-if="row.ownerFace" class="bili-owner-face" :src="row.ownerFace"
             referrerpolicy="no-referrer" alt="">
        <i v-else class="el-icon-user-solid bili-owner-face bili-owner-face-icon"></i>
        <span class="bili-owner-name">{{row.ownerName || '未知作者'}}</span>
      </div>

      <div class="bili-tags">
        <el-tag v-for="tag in visibleTags" :key="tag" size="mini" type="success" :title="tag">{{tag}}</el-tag>
        <el-popover v-if="tagList.length > MAX_TAG_DISPLAY" placement="top" trigger="click" width="280">
          <div class="bili-tag-popover">
            <el-tag v-for="tag in tagList" :key="tag" size="mini" type="success">{{tag}}</el-tag>
          </div>
          <el-button slot="reference" type="text" size="mini">+{{tagList.length - MAX_TAG_DISPLAY}}</el-button>
        </el-popover>
        <span v-if="tagList.length === 0" class="bili-empty-text">无标签</span>
      </div>

      <div class="bili-times">
        <div title="视频发布时间(pubdate)">发布：{{formatTs(row.pubdate)}}</div>
        <div title="视频创建时间，即上传到b站的时间(ctime)">创建：{{formatTs(row.ctime)}}</div>
      </div>

      <!-- 下载不展示进度条，只展示状态 -->
      <div v-if="row.downloading" class="bili-downloading">
        <i class="el-icon-loading"></i>
        <span>正在下载到服务器…</span>
      </div>

      <div class="bili-actions">
        <el-tooltip content="视频下载到服务器本地" placement="top">
          <el-button v-if="!row.downloaded" type="primary" size="mini" plain icon="el-icon-download"
                     :loading="row.downloading" @click="$emit('download', row)">下载视频</el-button>
<!--          <el-tag v-else type="success" size="mini" effect="plain" class="bili-downloaded-tag">-->
<!--            <i class="el-icon-check"></i> 已下载-->
<!--          </el-tag>-->
        </el-tooltip>
        <el-button size="mini" plain icon="el-icon-refresh" @click="$emit('refresh', row)">刷新</el-button>
        <el-button size="mini" type="danger" plain icon="el-icon-delete" @click="$emit('delete', row)">删除</el-button>
      </div>

      <!-- 已下载时点击这里在页面内播放服务器上的视频 -->
      <div class="bili-file" :class="{'bili-file-playable': row.downloaded}"
           :title="row.downloaded ? `点击播放服务器上的视频：${row.videoFileName}` : `服务器本地文件：${row.videoFileName}（未下载）`"
           @click="onFileClick">
        <i :class="row.downloaded ? 'el-icon-video-play' : 'el-icon-document'"></i>
        <span class="bili-file-name">{{row.videoFileName}}</span>
      </div>

      <div v-if="failed" class="bili-download-error">
        <div class="bili-download-error-text" :title="row.downloadMessage">
          <i class="el-icon-warning-outline"></i>
          下载失败：{{row.downloadMessage}}
        </div>
        <el-button type="text" size="mini" icon="el-icon-refresh-right"
                   @click="$emit('retry', row)">重试</el-button>
      </div>
    </div>
  </div>
</template>

<script>
// 卡片上最多直接展示几个标签，剩下的收进popover
const MAX_TAG_DISPLAY = 3;

export default {
  name: 'BiliVideoCard',
  props: {
    row: {
      type: Object,
      required: true
    },
    selected: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      MAX_TAG_DISPLAY
    }
  },
  computed: {
    coverUrl() {
      // b站图片是http的，https页面下会被浏览器拦截，统一换成https
      return this.row.pic ? String(this.row.pic).replace(/^http:/, 'https:') : null
    },
    videoUrl() {
      return `https://www.bilibili.com/video/${this.row.bvid}`
    },
    tagList() {
      if (!this.row.tag) {
        return []
      }
      return String(this.row.tag).split(/[,，]/).map(tag => tag.trim()).filter(tag => tag)
    },
    visibleTags() {
      return this.tagList.slice(0, MAX_TAG_DISPLAY)
    },
    flagText() {
      if (this.row.downloaded) {
        return '已下载'
      }
      if (this.row.downloading) {
        return '下载中'
      }
      return this.failed ? '下载失败' : '未下载'
    },
    flagClass() {
      if (this.row.downloaded) {
        return 'is-downloaded'
      }
      if (this.row.downloading) {
        return 'is-downloading'
      }
      return this.failed ? 'is-failed' : 'is-undownloaded'
    },
    failed() {
      return this.row.downloadState === 'fail' && !!this.row.downloadMessage
    },
    durationText() {
      return this.formatDuration(this.row.duration)
    }
  },
  methods: {
    openSpace() {
      if (this.row.ownerMid) {
        window.open(`https://space.bilibili.com/${this.row.ownerMid}`, '_blank')
      }
    },
    onFileClick() {
      if (this.row.downloaded) {
        this.$emit('play', this.row)
      } else {
        // this.$message.info('该视频还没有下载到服务器')
      }
    },
    formatTs(timestamp) {
      if (!timestamp) {
        return '-'
      }
      const value = String(timestamp).length === 10 ? Number(timestamp) * 1000 : Number(timestamp)
      return this.$dayjs(value).format('YYYY-MM-DD HH:mm:ss')
    },
    formatDuration(seconds) {
      const total = Math.max(0, Math.floor(Number(seconds) || 0))
      const hour = Math.floor(total / 3600)
      const minute = Math.floor((total % 3600) / 60)
      const second = total % 60
      const pad = value => String(value).padStart(2, '0')
      return hour > 0 ? `${hour}:${pad(minute)}:${pad(second)}` : `${pad(minute)}:${pad(second)}`
    }
  }
}
</script>

<style lang="scss" scoped>
.bili-card {
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  transition: box-shadow .2s;

  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, .1);
  }
}

.bili-card-downloaded {
  border-color: #b3e19d;
}

.bili-cover {
  position: relative;
  width: 100%;
  padding-top: 56.25%;
  background-color: #f5f7fa;
  cursor: pointer;

  img {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .bili-cover-empty {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #c0c4cc;
    font-size: 22px;
  }

  .bili-cover-duration {
    position: absolute;
    right: 6px;
    bottom: 6px;
    padding: 0 4px;
    font-size: 11px;
    line-height: 16px;
    color: #fff;
    background-color: rgba(0, 0, 0, .6);
    border-radius: 3px;
  }

  .bili-cover-flag {
    position: absolute;
    left: 6px;
    top: 6px;
    padding: 0 5px;
    font-size: 11px;
    line-height: 16px;
    border-radius: 3px;
    color: #fff;

    &.is-downloaded {
      background-color: rgba(103, 194, 58, .9);
    }

    &.is-downloading {
      background-color: rgba(64, 158, 255, .9);
    }

    &.is-failed {
      background-color: rgba(245, 108, 108, .9);
    }

    &.is-undownloaded {
      background-color: rgba(144, 147, 153, .85);
    }
  }
}

.bili-card-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 8px;
}

.bili-title {
  font-size: 13px;
  line-height: 18px;
  color: #303133;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  word-break: break-all;
  min-height: 36px;
}

.bili-meta-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  min-width: 0;

  .bili-bvid {
    color: #409eff;
    font-size: 12px;
    flex: none;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }

  .bili-cid {
    font-size: 12px;
    color: #c0c4cc;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    min-width: 0;
  }

  .bili-card-select {
    flex: none;
    margin-right: 0;
  }
}

.bili-owner {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  min-width: 0;
  cursor: pointer;

  .bili-owner-face {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    flex: none;
    object-fit: cover;
    background-color: #f5f7fa;
  }

  .bili-owner-face-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #c0c4cc;
    font-size: 12px;
  }

  .bili-owner-name {
    font-size: 12px;
    color: #606266;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &:hover {
      color: #409eff;
    }
  }
}

.bili-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;

  .el-tag {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .bili-empty-text {
    font-size: 12px;
    color: #c0c4cc;
  }
}

.bili-times {
  margin-top: 6px;
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}

/* 下载中：没有进度条，只给一个状态行 */
.bili-downloading {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 11px;
  color: #409eff;
}

.bili-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: auto;
  padding-top: 8px;

  .el-button {
    margin: 0;
    padding: 5px 7px;
  }

  .bili-downloaded-tag {
    line-height: 22px;
  }
}

.bili-file {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 11px;
  color: #c0c4cc;

  .bili-file-name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.bili-file-playable {
  color: #409eff;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

/* 下载失败：失败原因 + 重试，与JM任务抽屉里的失败任务保持一致 */
.bili-download-error {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  font-size: 12px;
  color: #f56c6c;

  .bili-download-error-text {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .el-button {
    flex: none;
    margin: 0;
    padding: 0 2px;
    color: #f56c6c;
  }
}

.bili-tag-popover {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
