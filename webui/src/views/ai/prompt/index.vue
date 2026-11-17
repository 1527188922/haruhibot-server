<template>
  <div class="ai-prompt">
    <basic-container>
      <div class="page-header">
        <div>
          <div class="page-title">提示词预览</div>
          <div class="page-desc">
            查看最终注入模型的 system 与 messages；不调用 API、不发送 QQ、不写会话。
          </div>
        </div>
        <el-tag type="info" effect="plain">只读预览</el-tag>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="24" :md="9">
          <div class="panel">
            <div class="panel-title">模拟输入</div>
            <el-form label-width="88px" size="small">
              <el-form-item label="场景">
                <el-radio-group v-model="form.scene">
                  <el-radio-button label="group">群聊</el-radio-button>
                  <el-radio-button label="private">私聊</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="机器人">
                <el-input v-model="form.selfId" placeholder="selfId，可留空"></el-input>
              </el-form-item>
              <el-form-item label="目标 ID">
                <el-input v-model="form.targetId" placeholder="群号 / QQ，可留空"></el-input>
              </el-form-item>
              <el-form-item label="角色">
                <el-select v-model="form.roleName" clearable placeholder="自动解析" style="width: 100%">
                  <el-option v-for="role in roles" :key="role.name" :label="role.name" :value="role.name"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="说话人">
                <el-input v-model="form.userName" placeholder="预览用户"></el-input>
              </el-form-item>
              <el-form-item label="模拟消息">
                <el-input v-model="form.text" type="textarea" :rows="4" placeholder="例如：在吗"></el-input>
              </el-form-item>
              <el-form-item label="环境上下文">
                <el-input v-model="form.ambientText"
                          type="textarea"
                          :rows="4"
                          placeholder="每行一条，例如：&#10;张三: 今天好累&#10;李四: 哈哈哈"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="el-icon-view" :loading="loading" @click="generate">生成预览</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-col>

        <el-col :xs="24" :sm="24" :md="15">
          <div v-if="result" class="panel">
            <div class="panel-toolbar">
              <div class="panel-title">预览结果</div>
              <el-button size="mini" icon="el-icon-document-copy" @click="copyMessages">复制 messages JSON</el-button>
            </div>

            <div class="summary-row">
              <el-tag size="small" type="info" effect="plain">估算 {{ result.estimatedTokens }} tokens</el-tag>
              <el-tag size="small" type="info" effect="plain">输入预算 {{ result.budget }}</el-tag>
              <el-tag size="small" :type="visionEnabled ? 'success' : 'info'" effect="plain">
                图片理解：{{ visionEnabled ? '开' : '关' }}
              </el-tag>
            </div>

            <el-tabs v-model="activeTab">
              <el-tab-pane label="System 分段" name="system">
                <el-collapse v-model="openSections">
                  <el-collapse-item v-for="(section, index) in result.sections"
                                    :key="index"
                                    :name="String(index)">
                    <template slot="title">
                      <span class="section-title">{{ section.title }}</span>
                      <span class="section-size">{{ section.content.length }} 字</span>
                    </template>
                    <pre class="prompt-content">{{ section.content }}</pre>
                  </el-collapse-item>
                </el-collapse>
              </el-tab-pane>

              <el-tab-pane label="Messages" name="messages">
                <div v-for="(message, index) in result.messages" :key="index" class="message-row">
                  <div class="message-role">
                    {{ message.role }}
                    <span v-if="message.name"> · {{ message.name }}</span>
                  </div>
                  <pre class="prompt-content">{{ message.content }}</pre>
                </div>
              </el-tab-pane>

              <el-tab-pane label="Token 估算" name="token">
                <el-table :data="tokenRows" size="small" border stripe>
                  <el-table-column prop="index" label="#" width="60" align="center"></el-table-column>
                  <el-table-column prop="role" label="角色" width="100"></el-table-column>
                  <el-table-column prop="chars" label="字符数" width="90" align="center"></el-table-column>
                  <el-table-column prop="tokens" label="估算 Token" width="110" align="center"></el-table-column>
                </el-table>
                <div class="token-total">
                  合计约 <b>{{ result.estimatedTokens }}</b> tokens，预算 <b>{{ result.budget }}</b>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>

          <div v-else class="panel empty-panel">
            填写左侧表单后点击「生成预览」
          </div>
        </el-col>
      </el-row>
    </basic-container>
  </div>
</template>

<script>
import { promptPreview, roleList } from '@/api/ai';

export default {
  name: 'AiPrompt',
  data() {
    return {
      loading: false,
      roles: [],
      form: {
        scene: 'group',
        selfId: '',
        targetId: '',
        roleName: '',
        userName: '预览用户',
        text: '在吗',
        ambientText: ''
      },
      result: null,
      activeTab: 'system',
      openSections: ['0']
    };
  },
  computed: {
    visionEnabled() {
      return this.result && this.result.visionEnabled;
    },
    tokenRows() {
      if (!this.result || !this.result.messages) {
        return [];
      }
      return this.result.messages.map((message, index) => {
        const chars = (message.content || '').length;
        const images = message.imageUrls ? message.imageUrls.length : 0;
        const tokens = Math.max(chars ? 1 : 0, Math.ceil(chars * 0.75)) + images * 1024;
        return {
          index: index + 1,
          role: message.role,
          chars,
          tokens
        };
      });
    }
  },
  created() {
    this.loadRoles();
  },
  methods: {
    loadRoles() {
      roleList().then((res) => {
        this.roles = this.unwrap(res) || [];
      }).catch(() => {
        // 角色列表失败不阻塞预览
      });
    },
    generate() {
      if (!this.form.text || !this.form.text.trim()) {
        return this.$message.warning('模拟消息不能为空');
      }
      this.loading = true;
      promptPreview({
        selfId: this.toNumber(this.form.selfId),
        scene: this.form.scene,
        targetId: this.toNumber(this.form.targetId),
        roleName: this.form.roleName || undefined,
        userName: this.form.userName || undefined,
        text: this.form.text,
        ambientText: this.form.ambientText || undefined
      }).then((res) => {
        this.result = this.unwrap(res);
        this.activeTab = 'system';
        this.openSections = ['0'];
      }).catch((e) => {
        this.$message.error((e && e.message) || '生成预览失败');
      }).finally(() => {
        this.loading = false;
      });
    },
    copyMessages() {
      if (!this.result) {
        return;
      }
      const text = JSON.stringify(this.result.messages || [], null, 2);
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('已复制 messages JSON');
        }).catch(() => {
          this.fallbackCopy(text);
        });
      } else {
        this.fallbackCopy(text);
      }
    },
    fallbackCopy(text) {
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      try {
        document.execCommand('copy');
        this.$message.success('已复制 messages JSON');
      } catch (e) {
        this.$message.error('复制失败，请手动选择内容');
      } finally {
        document.body.removeChild(textarea);
      }
    },
    unwrap(res) {
      const body = res && res.data ? res.data : {};
      if (body.code !== 200) {
        throw new Error(body.message || '接口返回失败');
      }
      return body.data;
    },
    toNumber(value) {
      if (value === '' || value === null || value === undefined) {
        return undefined;
      }
      const number = Number(value);
      return Number.isFinite(number) ? number : undefined;
    }
  }
};
</script>

<style lang="scss" scoped>
.ai-prompt {
  .page-header {
    display: flex;
    align-items: center;
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

  .panel {
    margin-bottom: 16px;
    padding: 16px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 6px;
  }

  .panel-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .panel-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  .summary-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
  }

  .section-title {
    font-weight: 600;
    color: #303133;
  }

  .section-size {
    margin-left: 10px;
    font-size: 12px;
    color: #a8abb2;
  }

  .prompt-content {
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

  .message-row {
    margin-bottom: 12px;
  }

  .message-role {
    margin-bottom: 4px;
    font-size: 12px;
    font-weight: 600;
    color: #606266;
  }

  .token-total {
    margin-top: 12px;
    font-size: 13px;
    color: #606266;
  }

  .empty-panel {
    padding: 80px 0;
    text-align: center;
    font-size: 13px;
    color: #909399;
  }
}
</style>
