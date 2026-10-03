<template>
  <!--
    左侧分组列表：按作者 / 按标签 两种浏览方式。
    交互与JM漫画的收藏夹tab一致：左侧选一项，右侧只展示这一组的视频。
  -->
  <div class="bili-group-panel">
    <div class="bili-group-head">
      <el-radio-group v-model="modeProxy" size="mini" class="bili-group-mode">
        <el-radio-button label="owner">按作者</el-radio-button>
        <el-radio-button label="tag">按标签</el-radio-button>
      </el-radio-group>
      <el-button type="text" size="mini" icon="el-icon-refresh" :loading="loading"
                 title="刷新分组列表" @click="$emit('refresh')"></el-button>
    </div>

    <el-input v-if="items.length > 8" v-model.trim="keyword" class="bili-group-search" size="mini"
              clearable :placeholder="mode === 'owner' ? '搜索作者' : '搜索标签'"></el-input>

    <div v-loading="loading" class="bili-group-list">
      <div class="bili-group-item" :class="{'bili-group-item--active': activeKey === ALL_KEY}"
           @click="selectAll">
        <i class="el-icon-s-grid bili-group-item-icon"></i>
        <span class="bili-group-item-name">全部视频</span>
        <span class="bili-group-item-count">{{total}}</span>
      </div>

      <template v-if="mode === 'owner'">
        <div v-for="item in filteredItems" :key="`owner-${item.ownerMid}`"
             class="bili-group-item"
             :class="{'bili-group-item--active': activeKey === ownerKey(item)}"
             :title="`${item.ownerName || item.ownerMid}（${item.videoCount}）`"
             @click="selectOwner(item)">
          <img v-if="item.ownerFace" class="bili-group-item-avatar" :src="fixUrl(item.ownerFace)"
               referrerpolicy="no-referrer" alt="">
          <i v-else class="el-icon-user-solid bili-group-item-avatar bili-group-item-icon"></i>
          <span class="bili-group-item-name">{{item.ownerName || item.ownerMid}}</span>
          <span class="bili-group-item-count">{{item.videoCount}}</span>
        </div>
      </template>

      <template v-else>
        <div v-for="item in filteredItems" :key="`tag-${item.tag}`"
             class="bili-group-item"
             :class="{'bili-group-item--active': activeKey === tagKey(item)}"
             :title="`${item.tag}（${item.videoCount}）`"
             @click="selectTag(item)">
          <i class="el-icon-price-tag bili-group-item-icon"></i>
          <span class="bili-group-item-name">{{item.tag}}</span>
          <span class="bili-group-item-count">{{item.videoCount}}</span>
        </div>
      </template>

      <div v-if="!loading && filteredItems.length === 0" class="bili-group-empty">
        {{keyword ? '没有匹配的分组' : (mode === 'owner' ? '暂无作者' : '暂无标签')}}
      </div>
    </div>
  </div>
</template>

<script>
export const ALL_KEY = 'all';
export const OWNER_PREFIX = 'owner-';
export const TAG_PREFIX = 'tag-';

export default {
  name: 'BiliGroupPanel',
  props: {
    /**
     * owner / tag
     */
    mode: {
      type: String,
      default: 'owner'
    },
    authors: {
      type: Array,
      default: () => []
    },
    tags: {
      type: Array,
      default: () => []
    },
    /**
     * 当前选中的分组key，all 表示全部
     */
    activeKey: {
      type: String,
      default: ALL_KEY
    },
    total: {
      type: Number,
      default: 0
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      ALL_KEY,
      keyword: ''
    }
  },
  computed: {
    modeProxy: {
      get() {
        return this.mode
      },
      set(value) {
        this.$emit('update:mode', value)
      }
    },
    items() {
      return this.mode === 'owner' ? this.authors : this.tags
    },
    filteredItems() {
      if (!this.keyword) {
        return this.items
      }
      const keyword = this.keyword.toLowerCase()
      return this.items.filter(item => {
        const name = this.mode === 'owner' ? (item.ownerName || String(item.ownerMid)) : item.tag
        return String(name).toLowerCase().includes(keyword)
      })
    }
  },
  methods: {
    ownerKey(item) {
      return OWNER_PREFIX + item.ownerMid
    },
    tagKey(item) {
      return TAG_PREFIX + item.tag
    },
    fixUrl(url) {
      return url ? String(url).replace(/^http:/, 'https:') : url
    },
    selectAll() {
      this.$emit('select', {key: ALL_KEY})
    },
    selectOwner(item) {
      this.$emit('select', {
        key: this.ownerKey(item),
        ownerMid: item.ownerMid,
        ownerName: item.ownerName,
        ownerFace: item.ownerFace
      })
    },
    selectTag(item) {
      this.$emit('select', {
        key: this.tagKey(item),
        tag: item.tag
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.bili-group-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  min-width: 0;
}

.bili-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  margin-bottom: 6px;

  .bili-group-mode {
    flex: 1;
    min-width: 0;

    ::v-deep .el-radio-button__inner {
      padding: 5px 8px;
    }
  }
}

.bili-group-search {
  margin-bottom: 6px;
}

.bili-group-list {
  flex: 1;
  min-height: 200px;
  max-height: 720px;
  overflow-y: auto;
}

.bili-group-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;

  &:hover {
    background: #f5f7fa;
  }

  &--active {
    background: #ecf5ff;
    color: #409eff;
  }

  &-avatar {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    flex: none;
    object-fit: cover;
    background-color: #f5f7fa;
  }

  &-icon {
    width: 20px;
    height: 20px;
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #909399;
    font-size: 14px;
  }

  &-name {
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &-count {
    flex: 0 0 auto;
    color: #909399;
    font-size: 12px;
  }
}

.bili-group-empty {
  color: #909399;
  font-size: 12px;
  padding: 8px;
  text-align: center;
}

/**
 * 移动端：左侧竖列收成顶部横向条，先把纵向空间还给卡片
 */
@media screen and (max-width: 768px) {
  .bili-group-list {
    display: flex;
    flex-wrap: nowrap;
    gap: 6px;
    min-height: 0;
    max-height: none;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    padding-bottom: 4px;
  }

  .bili-group-item {
    flex: 0 0 auto;
    border: 1px solid #ebeef5;
  }
}
</style>
