<template>
  <div class="ai-role-edit">
    <basic-container v-loading="pageLoading">
      <div class="page-header">
        <div>
          <div class="page-title">{{ isEdit ? '编辑角色卡' : '新建角色卡' }}</div>
          <div class="page-desc">
            按章节编辑，保存时自动拼装为 Markdown；角色名即文件名。
          </div>
        </div>
        <div class="page-actions">
          <el-button size="small" @click="goBack">返回</el-button>
          <el-button size="small" icon="el-icon-view" :loading="previewLoading" @click="preview">预览</el-button>
          <el-button v-if="isEdit" size="small" @click="openSaveAs">另存为新角色</el-button>
          <el-button size="small" type="primary" :loading="saving" :disabled="!canSave" @click="save">
            保存
          </el-button>
        </div>
      </div>

      <div class="panel meta-panel">
        <el-form label-width="90px" size="small">
          <el-form-item label="角色名">
            <el-input v-model="form.name"
                      :disabled="isEdit"
                      maxlength="30"
                      show-word-limit
                      placeholder="1~30 字符，不能包含路径字符"></el-input>
          </el-form-item>
          <el-form-item label="触发词">
            <div class="trigger-editor">
              <el-tag v-for="(word, index) in form.triggers"
                      :key="word + index"
                      closable
                      class="trigger-tag"
                      @close="removeTrigger(index)">
                {{ word }}
              </el-tag>
              <el-input v-model="triggerInput"
                        class="trigger-input"
                        placeholder="输入后回车添加"
                        @keyup.enter.native="addTrigger"></el-input>
            </div>
          </el-form-item>
          <el-form-item label="字符数">
            <span :class="{ 'over-limit': overLimit }">{{ charsApprox }} / {{ maxChars }}</span>
            <span class="muted">（按过滤后内容估算，保存时后端精确校验）</span>
          </el-form-item>
          <el-form-item v-if="form.warnings && form.warnings.length" label="警告">
            <el-alert type="warning" :closable="false">
              <div v-for="(warning, index) in form.warnings" :key="index" class="warning-line">- {{ warning }}</div>
            </el-alert>
          </el-form-item>
        </el-form>
      </div>

      <div class="section-toolbar">
        <div class="section-toolbar__title">章节</div>
        <div>
          <el-select v-model="newSectionTitle" size="small" clearable placeholder="选择默认章节" style="width: 180px">
            <el-option v-for="title in defaultTitles" :key="title" :label="title" :value="title"></el-option>
          </el-select>
          <el-button size="small" icon="el-icon-plus" @click="addSection">添加章节</el-button>
        </div>
      </div>

      <div v-for="(section, sIndex) in form.sections" :key="section.id" class="section-card">
        <div class="section-head">
          <el-input v-model="section.title"
                    size="small"
                    maxlength="40"
                    placeholder="章节标题"
                    class="section-title-input"></el-input>
          <el-select v-model="section.mode" size="small" class="mode-select">
            <el-option label="总是注入" value="base"></el-option>
            <el-option label="仅工具开启" value="tools"></el-option>
            <el-option label="仅视觉开启" value="vision"></el-option>
          </el-select>
          <div class="section-ops">
            <el-button size="mini" icon="el-icon-top" :disabled="sIndex === 0"
                       @click="moveSection(sIndex, -1)"></el-button>
            <el-button size="mini" icon="el-icon-bottom" :disabled="sIndex === form.sections.length - 1"
                       @click="moveSection(sIndex, 1)"></el-button>
            <el-button size="mini" type="danger" icon="el-icon-delete" @click="removeSection(sIndex)"></el-button>
          </div>
        </div>

        <div v-for="(line, lIndex) in section.lines" :key="lIndex" class="line-row">
          <el-input :value="line"
                    type="textarea"
                    :autosize="{ minRows: 1, maxRows: 6 }"
                    size="small"
                    placeholder="一行内容；列表请自行带 - "
                    @input="updateLine(section, lIndex, $event)"></el-input>
          <el-button size="mini" type="danger" plain icon="el-icon-delete"
                     @click="removeLine(section, lIndex)"></el-button>
        </div>

        <el-button size="mini" icon="el-icon-plus" @click="addLine(section)">追加一行</el-button>
      </div>

      <div class="footer-bar">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!canSave" @click="save">保存</el-button>
      </div>
    </basic-container>

    <el-dialog title="预览" :visible.sync="previewVisible" width="760px">
      <el-tabs v-model="previewTab">
        <el-tab-pane label="过滤后内容" name="filtered">
          <pre class="preview-content">{{ previewData.filteredContent }}</pre>
        </el-tab-pane>
        <el-tab-pane label="Markdown" name="markdown">
          <pre class="preview-content">{{ previewData.renderedMarkdown }}</pre>
        </el-tab-pane>
        <el-tab-pane :label="'校验(' + warningCount + ')'" name="warnings">
          <div v-if="!warningCount" class="empty-tip">没有警告</div>
          <ul v-else class="warning-list">
            <li v-for="(warning, index) in previewData.warnings" :key="index">{{ warning }}</li>
          </ul>
          <div class="token-line">估算约 {{ previewData.estimatedTokens || 0 }} tokens</div>
        </el-tab-pane>
      </el-tabs>
      <div slot="footer">
        <el-button size="small" @click="previewVisible = false">关闭</el-button>
      </div>
    </el-dialog>

    <el-dialog title="另存为新角色" :visible.sync="saveAsVisible" width="440px">
      <el-form label-width="90px" size="small">
        <el-form-item label="新角色名">
          <el-input v-model="saveAsName" maxlength="30" placeholder="1~30 字符"></el-input>
          <div v-if="saveAsMessage" class="field-error">{{ saveAsMessage }}</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="saveAsVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="saveAsLoading" @click="confirmSaveAs">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { roleDetail, roleSave, roleNameCheck } from '@/api/ai';

let sectionSeed = 0;

const DEFAULT_TITLES = [
  '你是谁',
  '说话风格',
  '性格与反应',
  '回复示例',
  '群聊习惯',
  '禁止事项'
];

function newSection(title, lines) {
  return {
    id: ++sectionSeed,
    title: title || '',
    mode: 'base',
    lines: lines && lines.length ? lines : ['']
  };
}

function defaultSections() {
  return DEFAULT_TITLES.map(title => newSection(title, ['']));
}

export default {
  name: 'AiRoleEdit',
  data() {
    return {
      pageLoading: false,
      saving: false,
      previewLoading: false,
      saveAsLoading: false,
      form: {
        originalName: '',
        name: '',
        triggers: [],
        sections: [],
        warnings: []
      },
      baseMtime: 0,
      baseHash: '',
      maxChars: 6000,
      triggerInput: '',
      newSectionTitle: '',
      defaultTitles: DEFAULT_TITLES,
      initialSnapshot: '',
      previewVisible: false,
      previewTab: 'filtered',
      previewData: {},
      saveAsVisible: false,
      saveAsName: '',
      saveAsMessage: ''
    };
  },
  computed: {
    isEdit() {
      return !!this.form.originalName;
    },
    canSave() {
      return !!this.form.name && this.form.name.trim().length > 0
        && this.form.sections.some(section => (section.title || '').trim()
          && (section.lines || []).some(line => (line || '').trim()));
    },
    overLimit() {
      return this.charsApprox > this.maxChars;
    },
    charsApprox() {
      let chars = 0;
      this.form.sections.forEach(section => {
        const title = (section.title || '').trim();
        const lines = (section.lines || []).filter(line => (line || '').trim());
        if (!title || !lines.length) {
          return;
        }
        chars += ('## ' + title).length + 1;
        if (section.mode === 'tools' || section.mode === 'vision') {
          chars += 5;
        }
        lines.forEach(line => {
          chars += line.trim().length + 1;
        });
        chars += 1;
      });
      return chars;
    },
    warningCount() {
      return this.previewData.warnings ? this.previewData.warnings.length : 0;
    }
  },
  created() {
    const name = this.$route.query.name;
    if (name) {
      this.load(name);
    } else {
      this.initNew();
    }
  },
  beforeRouteLeave(to, from, next) {
    if (this.saving || this.saveAsLoading || !this.isDirty()) {
      return next();
    }
    this.$confirm('当前有未保存的修改，确定离开？', '提示', {
      confirmButtonText: '离开',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => next()).catch(() => next(false));
  },
  methods: {
    load(name) {
      this.pageLoading = true;
      roleDetail({ name }).then((res) => {
        const body = this.body(res);
        if (body.code !== 200) {
          this.$message.error(body.message || '加载角色卡失败');
          return;
        }
        const data = body.data;
        if (!data) {
          this.$message.error('角色卡不存在：' + name);
          this.$router.replace('/ai/role');
          return;
        }
        this.form = {
          originalName: data.name,
          name: data.name,
          triggers: data.triggers || [],
          warnings: data.warnings || [],
          sections: (data.sections && data.sections.length ? data.sections : [newSection('', [''])])
            .map(section => ({
              id: ++sectionSeed,
              title: section.title || '',
              mode: section.mode || 'base',
              lines: section.lines && section.lines.length ? section.lines : ['']
            }))
        };
        this.baseMtime = data.mtime || 0;
        this.baseHash = data.hash || '';
        this.maxChars = data.maxChars || 6000;
        this.initialSnapshot = this.snapshot();
      }).catch((e) => {
        this.$message.error((e && e.message) || '加载角色卡失败');
      }).finally(() => {
        this.pageLoading = false;
      });
    },
    initNew() {
      this.form = {
        originalName: '',
        name: '',
        triggers: [],
        warnings: [],
        sections: defaultSections()
      };
      this.baseMtime = 0;
      this.baseHash = '';
      this.maxChars = 6000;
      this.initialSnapshot = this.snapshot();
    },
    save() {
      if (!this.canSave) {
        return this.$message.warning('至少需要一个有内容的章节');
      }
      if (this.overLimit) {
        return this.$message.warning('角色卡内容可能超过上限，请先预览校验');
      }
      this.saving = true;
      roleSave(this.buildPayload(false)).then((res) => {
        const body = this.body(res);
        if (body.code !== 200) {
          return this.handleSaveError(body.message);
        }
        this.applySaved(body.data);
      }).catch((e) => {
        this.handleSaveError((e && e.message) || '保存失败');
      }).finally(() => {
        this.saving = false;
      });
    },
    handleSaveError(message) {
      this.$message.error(message || '保存失败');
      if (message && message.indexOf('外部修改') >= 0) {
        this.$confirm('角色卡已被外部修改，是否重新加载？', '保存冲突', {
          confirmButtonText: '重新加载',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.load(this.form.originalName || this.form.name);
        }).catch(() => {
        });
      }
    },
    preview() {
      if (!this.canSave) {
        return this.$message.warning('至少需要一个有内容的章节');
      }
      this.previewLoading = true;
      roleSave(this.buildPayload(true)).then((res) => {
        const body = this.body(res);
        if (body.code !== 200) {
          return this.$message.error(body.message || '预览失败');
        }
        this.previewData = body.data || {};
        this.previewTab = 'filtered';
        this.previewVisible = true;
      }).catch((e) => {
        this.$message.error((e && e.message) || '预览失败');
      }).finally(() => {
        this.previewLoading = false;
      });
    },
    openSaveAs() {
      const source = this.form.name || '新角色';
      this.saveAsVisible = true;
      this.saveAsMessage = '';
      this.saveAsName = source + '-副本';
      roleNameCheck({ sourceName: source }).then((res) => {
        const body = this.body(res);
        if (body.code === 200 && body.data && body.data.suggested) {
          this.saveAsName = body.data.suggested;
        }
      }).catch(() => {
        // 保留默认建议名
      });
    },
    confirmSaveAs() {
      const name = (this.saveAsName || '').trim();
      if (!name) {
        this.saveAsMessage = '请输入新角色名';
        return;
      }
      this.saveAsLoading = true;
      this.saveAsMessage = '';
      const payload = Object.assign({}, this.buildPayload(false), {
        originalName: '',
        name,
        baseMtime: 0,
        baseHash: ''
      });
      roleSave(payload).then((res) => {
        const body = this.body(res);
        if (body.code !== 200) {
          this.saveAsMessage = body.message || '保存失败';
          return;
        }
        this.saveAsVisible = false;
        this.applySaved(body.data);
      }).catch((e) => {
        this.saveAsMessage = (e && e.message) || '保存失败';
      }).finally(() => {
        this.saveAsLoading = false;
      });
    },
    applySaved(data) {
      const detail = data && data.detail ? data.detail : null;
      if (!detail) {
        return;
      }
      const changedName = this.form.originalName !== detail.name;
      this.form.originalName = detail.name;
      this.form.name = detail.name;
      this.form.triggers = detail.triggers || [];
      this.form.warnings = detail.warnings || [];
      this.form.sections = (detail.sections && detail.sections.length ? detail.sections : this.form.sections)
        .map(section => ({
          id: ++sectionSeed,
          title: section.title || '',
          mode: section.mode || 'base',
          lines: section.lines && section.lines.length ? section.lines : ['']
        }));
      this.baseMtime = detail.mtime || 0;
      this.baseHash = detail.hash || '';
      this.maxChars = detail.maxChars || this.maxChars;
      this.initialSnapshot = this.snapshot();
      this.$message.success(data.saved ? '角色卡已保存' : '角色卡已更新');
      if (changedName) {
        this.$router.replace({ path: '/ai/role/edit', query: { name: detail.name } });
      }
    },
    buildPayload(dryRun) {
      return {
        originalName: this.form.originalName,
        name: (this.form.name || '').trim(),
        triggers: this.form.triggers,
        sections: this.form.sections.map(section => ({
          title: (section.title || '').trim(),
          mode: section.mode,
          lines: (section.lines || []).map(line => (line || '').trim()).filter(Boolean)
        })),
        baseMtime: this.baseMtime,
        baseHash: this.baseHash,
        dryRun
      };
    },
    snapshot() {
      return JSON.stringify({
        name: this.form.name,
        triggers: this.form.triggers,
        sections: this.form.sections.map(section => ({
          title: section.title,
          mode: section.mode,
          lines: section.lines
        }))
      });
    },
    isDirty() {
      return this.snapshot() !== this.initialSnapshot;
    },
    addTrigger() {
      const value = (this.triggerInput || '').trim();
      if (!value) {
        return;
      }
      if (value.length > 20) {
        return this.$message.warning('触发词不能超过 20 字符');
      }
      if (this.form.triggers.length >= 10) {
        return this.$message.warning('最多 10 个触发词');
      }
      if (!this.form.triggers.includes(value)) {
        this.form.triggers.push(value);
      }
      this.triggerInput = '';
    },
    removeTrigger(index) {
      this.form.triggers.splice(index, 1);
    },
    addSection() {
      const title = (this.newSectionTitle || '').trim();
      this.form.sections.push(newSection(title, ['']));
      this.newSectionTitle = '';
    },
    removeSection(index) {
      this.form.sections.splice(index, 1);
    },
    moveSection(index, offset) {
      const target = index + offset;
      if (target < 0 || target >= this.form.sections.length) {
        return;
      }
      const list = this.form.sections;
      const temp = list[index];
      list.splice(index, 1);
      list.splice(target, 0, temp);
    },
    addLine(section) {
      section.lines.push('');
    },
    removeLine(section, index) {
      if (section.lines.length <= 1) {
        this.$set(section.lines, 0, '');
      } else {
        section.lines.splice(index, 1);
      }
    },
    updateLine(section, index, value) {
      this.$set(section.lines, index, value);
    },
    goBack() {
      this.$router.push('/ai/role');
    },
    body(res) {
      return res && res.data ? res.data : {};
    }
  }
};
</script>

<style lang="scss" scoped>
.ai-role-edit {
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
  }

  .page-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .panel {
    padding: 16px;
    margin-bottom: 16px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 6px;
  }

  .meta-panel {
    padding-bottom: 0;
  }

  .trigger-editor {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
  }

  .trigger-tag {
    margin-right: 0;
  }

  .trigger-input {
    width: 180px;
  }

  .over-limit {
    color: #f56c6c;
    font-weight: 600;
  }

  .muted {
    margin-left: 8px;
    font-size: 12px;
    color: #a8abb2;
  }

  .warning-line {
    font-size: 12px;
    line-height: 1.8;
  }

  .section-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  .section-toolbar__title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .section-card {
    padding: 16px;
    margin-bottom: 16px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 6px;
  }

  .section-head {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }

  .section-title-input {
    flex: 1;
    margin-right: 10px;
  }

  .mode-select {
    width: 130px;
    margin-right: 10px;
  }

  .section-ops {
    flex-shrink: 0;
  }

  .line-row {
    display: flex;
    align-items: flex-start;
    margin-bottom: 8px;
  }

  .line-row .el-textarea {
    flex: 1;
    margin-right: 8px;
  }

  .footer-bar {
    padding: 12px 0 24px;
    text-align: right;
  }

  .preview-content {
    margin: 0;
    padding: 12px;
    max-height: 420px;
    overflow: auto;
    font-family: Consolas, Monaco, 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.7;
    color: #303133;
    white-space: pre-wrap;
    word-break: break-word;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 4px;
  }

  .warning-list {
    margin: 0;
    padding-left: 18px;
    font-size: 13px;
    line-height: 1.9;
    color: #e6a23c;
  }

  .token-line {
    margin-top: 12px;
    font-size: 12px;
    color: #909399;
  }

  .field-error {
    margin-top: 6px;
    font-size: 12px;
    color: #f56c6c;
  }

  .empty-tip {
    padding: 32px 0;
    text-align: center;
    font-size: 13px;
    color: #909399;
  }

  @media (max-width: 768px) {
    .page-header {
      flex-direction: column;
    }

    .page-actions {
      margin-top: 12px;
      justify-content: flex-start;
    }

    .section-head {
      flex-wrap: wrap;
    }

    .section-title-input {
      flex: 1 1 100%;
      margin-bottom: 8px;
      margin-right: 0;
    }
  }
}
</style>
