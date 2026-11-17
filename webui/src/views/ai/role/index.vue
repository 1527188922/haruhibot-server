<template>
  <div class="ai-role">
    <basic-container>
      <div class="page-header">
        <div>
          <div class="page-title">角色卡</div>
          <div class="page-desc">
            按章节结构化编辑，保存后写入运行目录 <code>aichat/roles/*.md</code>；文件名即角色名。
          </div>
        </div>
        <div>
          <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
          <el-button size="small" icon="el-icon-refresh-right" :loading="rescanLoading" @click="rescan">
            重新扫描
          </el-button>
          <el-button size="small" type="primary" icon="el-icon-plus" @click="createRole">新建角色卡</el-button>
        </div>
      </div>

      <div v-loading="loading" class="role-grid">
        <div v-for="row in rows" :key="row.name" class="role-card">
          <div class="role-card__head">
            <div class="role-card__identity">
              <div class="role-card__avatar">{{ row.name ? row.name.charAt(0) : '?' }}</div>
              <div class="role-card__title-wrap">
                <div class="role-card__name">{{ row.name }}</div>
                <div class="role-card__file">{{ row.fileName }}</div>
              </div>
            </div>
            <el-tag size="mini"
                    :type="row.warnings && row.warnings.length ? 'warning' : 'success'"
                    effect="plain">
              {{ row.warnings && row.warnings.length ? row.warnings.length + ' 警告' : '正常' }}
            </el-tag>
          </div>

          <div class="role-card__meta">
            <span>{{ row.chars }} 字符</span>
            <span>{{ row.usedBySessions }} 个会话使用</span>
          </div>

          <div class="role-card__triggers">
            <el-tag v-for="word in row.triggerWords" :key="word" size="mini" effect="plain" class="word-tag">
              {{ word }}
            </el-tag>
            <span v-if="!row.triggerWords || !row.triggerWords.length" class="muted">无触发词</span>
          </div>

          <div class="role-card__footer">
            <span class="role-card__time">{{ row.updatedAtText || '-' }}</span>
            <div class="role-card__actions">
              <el-button size="mini" type="text" @click="open(row)">预览</el-button>
              <el-button size="mini" type="text" @click="editRole(row)">编辑</el-button>
              <el-button size="mini" type="text" @click="openCopy(row)">复制</el-button>
            </div>
          </div>
        </div>

        <div v-if="!loading && !rows.length" class="empty-tip">
          暂无角色卡，点击右上角「新建角色卡」开始。
        </div>
      </div>

      <div class="table-tip">
        提示：这里显示的是按当前工具 / 视觉开关过滤后的角色卡内容；字符数与最终注入量一致。
      </div>
    </basic-container>

    <el-drawer :visible.sync="drawer" size="60%" direction="rtl" :with-header="false">
      <div v-loading="previewLoading" class="drawer-body">
        <template v-if="preview">
          <div class="drawer-header">
            <div>
              <div class="drawer-title">{{ preview.name }}</div>
              <div class="drawer-sub">
                {{ preview.fileName }} · {{ preview.chars }} 字符 · 约 {{ preview.estimatedTokens }} tokens ·
                {{ preview.updatedAtText || '-' }}
              </div>
            </div>
            <el-button size="small" @click="drawer = false">关闭</el-button>
          </div>

          <div class="mode-tags">
            <el-tag size="mini" :type="preview.toolsEnabled ? 'success' : 'info'" effect="plain">
              工具小节：{{ preview.toolsEnabled ? '注入' : '跳过' }}
            </el-tag>
            <el-tag size="mini" :type="preview.visionEnabled ? 'success' : 'info'" effect="plain">
              视觉小节：{{ preview.visionEnabled ? '注入' : '跳过' }}
            </el-tag>
          </div>

          <div class="trigger-row">
            <span class="label">触发词：</span>
            <el-tag v-for="word in preview.triggerWords" :key="word" size="mini" effect="plain" class="word-tag">
              {{ word }}
            </el-tag>
            <span v-if="!preview.triggerWords || !preview.triggerWords.length" class="muted">无</span>
          </div>

          <el-alert v-if="preview.warnings && preview.warnings.length"
                    title="Lint 警告"
                    type="warning"
                    :closable="false"
                    class="warning-box">
            <div v-for="(warning, index) in preview.warnings" :key="index" class="warning-line">
              - {{ warning }}
            </div>
          </el-alert>

          <div class="content-title">过滤后内容</div>
          <pre class="role-content">{{ preview.content }}</pre>
        </template>
        <div v-else class="empty-tip">没有找到角色卡</div>
      </div>
    </el-drawer>

    <el-dialog title="复制角色卡" :visible.sync="copyDialogVisible" :fullscreen="isMobile" append-to-body width="460px">
      <el-form label-width="90px" size="small">
        <el-form-item label="源角色">
          <span>{{ copySource }}</span>
        </el-form-item>
        <el-form-item label="新角色名">
          <el-input v-model="copyTarget" placeholder="新角色名"></el-input>
          <div v-if="copyMessage" class="dialog-tip error">{{ copyMessage }}</div>
        </el-form-item>
        <el-form-item label="">
          <el-checkbox v-model="copyAndEdit">复制后立即编辑</el-checkbox>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="copyDialogVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="copyLoading" @click="confirmCopy">复制</el-button>
      </div>
    </el-dialog>

    <el-drawer :visible.sync="editorVisible"
               :size="isMobile ? '100%' : '60%'"
               direction="rtl"
               :with-header="false"
               :before-close="beforeEditorClose"
               append-to-body
               custom-class="role-editor-drawer">
      <ai-role-editor v-if="editorVisible"
                      ref="roleEditor"
                      :key="editorKey"
                      :role-name="editorRoleName"
                      @request-close="closeEditorWithConfirm"
                      @saved="onEditorSaved" />
    </el-drawer>
  </div>
</template>

<script>
import { roleList, rolePreview, roleRescan, roleCopy, roleNameCheck } from '@/api/ai';
import AiRoleEditor from './edit.vue';

export default {
  name: 'AiRole',
  components: {
    AiRoleEditor
  },
  data() {
    return {
      loading: false,
      rescanLoading: false,
      rows: [],
      drawer: false,
      previewLoading: false,
      preview: null,
      copyDialogVisible: false,
      copySource: '',
      copyTarget: '',
      copyAndEdit: false,
      copyLoading: false,
      copyMessage: '',
      editorVisible: false,
      editorRoleName: '',
      editorKey: 0,
      windowWidth: window.innerWidth
    };
  },
  computed: {
    isMobile() {
      return this.windowWidth <= 768;
    }
  },
  mounted() {
    window.addEventListener('resize', this.onWindowResize);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.onWindowResize);
  },
  created() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      roleList().then(({ data: { code, message, data } }) => {
        if (code !== 200) {
          return this.$message.error(message || '加载失败');
        }
        this.rows = data || [];
      }).catch(() => {
        this.$message.error('加载角色卡失败');
      }).finally(() => {
        this.loading = false;
      });
    },
    rescan() {
      this.rescanLoading = true;
      roleRescan().then(({ data: { code, message, data } }) => {
        if (code !== 200) {
          return this.$message.error(message || '重新扫描失败');
        }
        this.rows = data || [];
        this.$message.success('已重新扫描角色卡目录');
      }).catch(() => {
        this.$message.error('重新扫描失败');
      }).finally(() => {
        this.rescanLoading = false;
      });
    },
    open(row) {
      this.drawer = true;
      this.previewLoading = true;
      this.preview = null;
      rolePreview({ name: row.name }).then(({ data: { code, message, data } }) => {
        if (code !== 200) {
          return this.$message.error(message || '加载预览失败');
        }
        this.preview = data;
      }).catch(() => {
        this.$message.error('加载角色卡预览失败');
      }).finally(() => {
        this.previewLoading = false;
      });
    },
    createRole() {
      this.openEditor('');
    },
    editRole(row) {
      this.openEditor(row.name);
    },
    openEditor(name) {
      this.editorRoleName = name || '';
      this.editorKey += 1;
      this.editorVisible = true;
    },
    onWindowResize() {
      this.windowWidth = window.innerWidth;
    },
    closeEditorWithConfirm() {
      const editor = this.$refs.roleEditor;
      if (!editor || !editor.isDirty()) {
        this.editorVisible = false;
        return;
      }
      this.$confirm('当前有未保存的修改，确定关闭？', '提示', {
        confirmButtonText: '关闭',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.editorVisible = false;
      }).catch(() => {
      });
    },
    beforeEditorClose(done) {
      const editor = this.$refs.roleEditor;
      if (!editor || !editor.isDirty()) {
        return done();
      }
      this.$confirm('当前有未保存的修改，确定关闭？', '提示', {
        confirmButtonText: '关闭',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => done()).catch(() => {
      });
    },
    onEditorSaved(detail) {
      if (detail && detail.name) {
        this.editorRoleName = detail.name;
      }
      this.load();
    },
    openCopy(row) {
      this.copySource = row.name;
      this.copyTarget = row.name + '-副本';
      this.copyAndEdit = false;
      this.copyMessage = '';
      this.copyDialogVisible = true;
      roleNameCheck({ sourceName: row.name }).then((res) => {
        const body = res && res.data ? res.data : {};
        if (body.code === 200 && body.data && body.data.suggested) {
          this.copyTarget = body.data.suggested;
        }
      }).catch(() => {
        // 建议名失败时保留默认值
      });
    },
    confirmCopy() {
      const target = (this.copyTarget || '').trim();
      if (!target) {
        this.copyMessage = '请输入新角色名';
        return;
      }
      this.copyLoading = true;
      this.copyMessage = '';
      roleCopy({
        sourceName: this.copySource,
        targetName: target,
        overwrite: false
      }).then((res) => {
        const body = res && res.data ? res.data : {};
        if (body.code !== 200) {
          this.copyMessage = body.message || '复制失败';
          return;
        }
        const detail = body.data || {};
        this.$message.success('已创建：' + (detail.name || target));
        this.copyDialogVisible = false;
        this.load();
        if (this.copyAndEdit) {
          this.openEditor(detail.name || target);
        }
      }).catch((e) => {
        this.copyMessage = (e && e.message) || '复制失败';
      }).finally(() => {
        this.copyLoading = false;
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.ai-role {
  .page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  .page-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .page-desc {
    margin-top: 4px;
    font-size: 12px;
    color: #909399;

    code {
      padding: 1px 4px;
      background: #f5f7fa;
      border-radius: 3px;
    }
  }

  .role-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;
    min-height: 80px;
  }

  .role-card {
    display: flex;
    flex-direction: column;
    padding: 16px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    transition: box-shadow 0.2s, transform 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
    }
  }

  .role-card__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
  }

  .role-card__identity {
    display: flex;
    min-width: 0;
    margin-right: 10px;
  }

  .role-card__avatar {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    margin-right: 10px;
    font-size: 18px;
    font-weight: 600;
    color: #fff;
    background: linear-gradient(135deg, #79bbff, #409eff);
    border-radius: 10px;
  }

  .role-card__title-wrap {
    min-width: 0;
  }

  .role-card__name {
    font-size: 16px;
    font-weight: 600;
    line-height: 1.3;
    color: #303133;
    word-break: break-all;
  }

  .role-card__file {
    margin-top: 2px;
    font-size: 12px;
    color: #909399;
    word-break: break-all;
  }

  .role-card__meta {
    display: flex;
    gap: 16px;
    margin: 14px 0 10px;
    font-size: 12px;
    color: #606266;
  }

  .role-card__triggers {
    flex: 1;
    min-height: 28px;
  }

  .role-card__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 14px;
    padding-top: 12px;
    border-top: 1px solid #f2f3f5;
  }

  .role-card__time {
    font-size: 12px;
    color: #a8abb2;
  }

  .role-card__actions {
    flex-shrink: 0;
  }

  .role-card__actions .el-button + .el-button {
    margin-left: 4px;
  }

  .role-grid .empty-tip {
    grid-column: 1 / -1;
  }

  .word-tag {
    margin: 0 6px 4px 0;
  }

  .table-tip {
    margin-top: 12px;
    font-size: 12px;
    color: #909399;
  }

  .drawer-body {
    min-height: 100%;
    padding: 20px;
    box-sizing: border-box;
  }

  .drawer-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding-bottom: 16px;
    border-bottom: 1px solid #ebeef5;
  }

  .drawer-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .drawer-sub {
    margin-top: 6px;
    font-size: 12px;
    color: #909399;
  }

  .dialog-tip {
    margin-top: 6px;
    font-size: 12px;
    color: #909399;

    &.error {
      color: #f56c6c;
    }
  }

  .mode-tags {
    margin: 14px 0 8px;
  }

  .trigger-row {
    margin-bottom: 12px;
    font-size: 13px;
  }

  .label {
    color: #606266;
  }

  .muted {
    color: #a8abb2;
  }

  .warning-box {
    margin-bottom: 16px;
  }

  .warning-line {
    font-size: 12px;
    line-height: 1.8;
  }

  .content-title {
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .role-content {
    margin: 0;
    padding: 14px;
    max-height: 60vh;
    overflow: auto;
    font-family: Consolas, Monaco, 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.7;
    color: #303133;
    white-space: pre-wrap;
    word-break: break-word;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 6px;
  }

  .empty-tip {
    padding: 32px 0;
    text-align: center;
    font-size: 13px;
    color: #909399;
  }

  @media (max-width: 768px) {
    .role-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>

<style lang="scss">
.role-editor-drawer .el-drawer__body {
  padding: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
</style>
