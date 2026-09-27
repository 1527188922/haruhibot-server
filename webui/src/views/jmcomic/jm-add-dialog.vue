<template>
  <el-dialog :title="title" :visible.sync="visibleProxy" width="560px" @closed="handleClosed">
    <div class="jm-add">
      <div class="jm-add-tip">
        支持一次粘贴多个：JM ID、漫画链接、或从 JM 页面复制的整段文本，会自动提取其中的 ID 并去重。
      </div>

      <div class="jm-add-toolbar">
        <el-button size="mini" icon="el-icon-document-copy" :loading="clipboardLoading" @click="readClipboard">读取剪贴板</el-button>
        <el-button size="mini" icon="el-icon-delete" :disabled="!rawText" @click="clearInput">清空</el-button>
        <span v-if="parsedIds.length > 0" class="jm-add-count">
          已识别 <b>{{parsedIds.length}}</b> 个 JM ID
        </span>
        <span v-else-if="rawText" class="jm-add-count jm-add-count--warn">未识别到有效的 JM ID</span>
      </div>

      <el-input
        v-model="rawText"
        type="textarea"
        :autosize="{ minRows: 4, maxRows: 10 }"
        placeholder="例如：&#10;422866&#10;https://18comic.vip/album/422866/&#10;JM529510"
        @paste.native="handlePaste"
      ></el-input>

      <div v-if="parsedIds.length > 0" class="jm-add-preview">
        <el-tag v-for="id in parsedIds" :key="`add-id-${id}`" size="mini" type="info" closable
                @close="removeId(id)">{{id}}</el-tag>
      </div>

      <!-- 单条时顺带提供收藏夹选择，避免"新增"和"收藏"两步操作 -->
      <div v-if="parsedIds.length === 1" class="jm-add-favorite">
        <span class="jm-add-favorite-label">同时收藏到</span>
        <el-select v-model="favoriteChoice" size="small" filterable allow-create default-first-option
                   clearable placeholder="不收藏" class="jm-add-favorite-select">
          <el-option v-for="item in favorites" :key="`add-fav-${item.id}`" :label="item.name" :value="item.name" />
        </el-select>
        <span class="jm-add-favorite-hint">留空则不收藏；输入不存在的名称会新建收藏夹</span>
      </div>

      <div v-if="results.length > 0" class="jm-add-result">
        <div v-for="item in results" :key="`add-res-${item.id}`" class="jm-add-result-item" :class="item.ok ? 'is-ok' : 'is-fail'">
          <i :class="item.ok ? 'el-icon-success' : 'el-icon-error'"></i>
          <span class="jm-add-result-id">{{item.id}}</span>
          <span class="jm-add-result-msg">{{item.message}}</span>
        </div>
      </div>
    </div>

    <span slot="footer">
      <el-button size="small" @click="visibleProxy = false">关闭</el-button>
      <el-button type="primary" size="small" :loading="submitting"
                 :disabled="parsedIds.length === 0" @click="submit">
        开始拉取{{parsedIds.length > 1 ? `（${parsedIds.length}）` : ''}}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { requestAlbum, listFavorites } from "@/api/jmcomic";
import { parseJmIds } from "@/util/jm-id";

export default {
  name: 'JmAddDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    // 传入后不再重复请求收藏夹列表
    favoriteOptions: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      rawText: '',
      removedIds: [],
      favoriteChoice: '',
      clipboardLoading: false,
      submitting: false,
      results: [],
      favorites: []
    }
  },
  computed: {
    visibleProxy: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    title() {
      return this.parsedIds.length > 1 ? `新增JM主记录（${this.parsedIds.length}）` : '新增JM主记录'
    },
    parsedIds() {
      return parseJmIds(this.rawText).filter(id => !this.removedIds.includes(id))
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.loadFavorites()
        // 打开即尝试读取剪贴板，一步到位
        this.tryAutoReadClipboard()
      }
    },
    favoriteOptions(value) {
      if (Array.isArray(value) && value.length > 0) {
        this.favorites = value
      }
    }
  },
  methods: {
    async loadFavorites() {
      if (this.favoriteOptions.length > 0) {
        this.favorites = this.favoriteOptions
        return
      }
      try {
        const { data: { code, data } } = await listFavorites()
        if (code === 200) {
          this.favorites = data || []
        }
      } catch (error) {
        // 收藏夹列表拿不到不影响新增，静默忽略
      }
    },
    /**
     * 自动读取剪贴板。
     * 浏览器要求用户手势或已授权才允许读取，失败时静默提示手动点按钮，不弹错误。
     */
    async tryAutoReadClipboard() {
      if (!navigator.clipboard || !navigator.clipboard.readText) {
        return
      }
      try {
        const text = await navigator.clipboard.readText()
        if (text && parseJmIds(text).length > 0 && !this.rawText) {
          this.applyClipboardText(text, true)
        }
      } catch (error) {
        // 未授权/非安全上下文：等用户点"读取剪贴板"
      }
    },
    async readClipboard() {
      if (!navigator.clipboard || !navigator.clipboard.readText) {
        return this.$message.warning('当前浏览器不支持读取剪贴板，请手动粘贴')
      }
      this.clipboardLoading = true
      try {
        const text = await navigator.clipboard.readText()
        if (!text) {
          return this.$message.warning('剪贴板是空的')
        }
        this.applyClipboardText(text)
      } catch (error) {
        this.$message.warning('读取剪贴板被拒绝，请手动粘贴')
      } finally {
        this.clipboardLoading = false
      }
    },
    applyClipboardText(text, silent) {
      const ids = parseJmIds(text)
      if (ids.length === 0) {
        if (!silent) {
          this.$message.warning('剪贴板里没有识别到 JM ID')
        }
        return
      }
      this.rawText = ids.join('\n')
      this.removedIds = []
      if (silent) {
        this.$message.success(`已从剪贴板识别 ${ids.length} 个 JM ID`)
      }
    },
    handlePaste() {
      // 粘贴意味着用户要重新开始，清掉之前手动移除的标记；
      // 放到 nextTick 里执行，确保晚于 v-model 对 rawText 的更新
      this.$nextTick(() => {
        this.removedIds = []
        this.results = []
      })
    },
    removeId(id) {
      this.removedIds.push(id)
    },
    clearInput() {
      this.rawText = ''
      this.removedIds = []
      this.results = []
    },
    handleClosed() {
      this.rawText = ''
      this.removedIds = []
      this.favoriteChoice = ''
      this.results = []
      this.submitting = false
    },
    async submit() {
      const ids = this.parsedIds
      if (ids.length === 0) {
        return
      }
      this.submitting = true
      this.results = []
      try {
        for (const id of ids) {
          try {
            const { data: { code, message } } = await requestAlbum(id)
            this.results.push({
              id,
              ok: code === 200,
              message: code === 200 ? '拉取成功' : (message || '拉取失败')
            })
          } catch (error) {
            this.results.push({ id, ok: false, message: this.errorMessage(error) })
          }
        }
        const okIds = this.results.filter(item => item.ok).map(item => item.id)
        const failCount = this.results.length - okIds.length
        if (okIds.length > 0 && this.favoriteChoice) {
          this.$emit('collect', { albumIds: okIds, favoriteName: this.favoriteChoice })
        }
        this.$emit('added', { successIds: okIds, failCount })
        if (okIds.length > 0) {
          this.$message.success(`拉取完成：成功 ${okIds.length} 个${failCount > 0 ? `，失败 ${failCount} 个` : ''}`)
        } else {
          this.$message.error('全部拉取失败')
        }
        // 成功后清空已完成的，方便继续添加下一批
        const failIds = this.results.filter(item => !item.ok).map(item => item.id)
        this.rawText = failIds.join('\n')
        this.removedIds = []
      } finally {
        this.submitting = false
      }
    },
    errorMessage(error) {
      if (error && error.data && error.data.message) {
        return error.data.message
      }
      return error && error.message ? error.message : '拉取异常'
    }
  }
}
</script>

<style lang="scss" scoped>
.jm-add {
  &-tip {
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
    margin-bottom: 8px;
  }

  &-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }

  &-count {
    font-size: 12px;
    color: #67c23a;

    &--warn {
      color: #e6a23c;
    }
  }

  &-preview {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 8px;
  }

  &-favorite {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 10px;
    font-size: 13px;

    &-label {
      flex: 0 0 auto;
    }

    &-select {
      flex: 0 1 220px;
      min-width: 0;
    }

    &-hint {
      flex: 1 1 100%;
      color: #909399;
      font-size: 12px;
    }
  }

  &-result {
    margin-top: 10px;
    max-height: 180px;
    overflow-y: auto;
    border-top: 1px solid #ebeef5;
    padding-top: 8px;

    &-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      line-height: 22px;

      &.is-ok {
        color: #67c23a;
      }

      &.is-fail {
        color: #f56c6c;
      }
    }

    &-id {
      font-weight: 600;
    }

    &-msg {
      color: #909399;
    }
  }
}

@media screen and (max-width: 768px) {
  .jm-add-favorite-select {
    flex: 1 1 100%;
  }
}
</style>
