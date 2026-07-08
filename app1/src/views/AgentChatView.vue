<template>
  <div class="agent-page">
    <div class="toolbar">
      <div class="toolbar-title">智能分析助手</div>
      <div class="actions">
        <button :disabled="sending" @click="clearChat">清空会话</button>
      </div>
    </div>

    <div class="main">
      <section class="chat-panel">
        <div class="chat-title">对话区</div>
        <div ref="messageBox" class="message-box">
          <div v-for="(msg, idx) in messages" :key="`msg_${idx}`" :class="['msg', msg.role]">
            <div class="bubble" v-html="msg.html"></div>
          </div>
          <div v-if="sending" class="msg assistant">
            <div class="bubble">正在分析，请稍候...</div>
          </div>
        </div>
        <div class="input-row">
          <textarea
            v-model.trim="userInput"
            placeholder="请输入你的问题，例如：请分析四川省社会科学院近年的课题方向。"
            @keydown.ctrl.enter.prevent="sendMessage"
          />
          <button :disabled="sending || !userInput" @click="sendMessage">发送</button>
        </div>
      </section>

      <aside class="right-panel">
        <div class="panel-title">报告 Markdown（可编辑）</div>
        <textarea
          v-model="reportDraft"
          class="report-editor"
          placeholder="当接口返回 markdown_report 时会自动填入，可手动编辑。"
        />
        <div class="panel-actions">
          <button :disabled="exporting || !reportDraft" @click="downloadWord">下载 Word</button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { marked } from 'marked'

export default {
  name: 'AgentChatView',
  data () {
    return {
      userInput: '',
      sending: false,
      exporting: false,
      messages: [
        {
          role: 'assistant',
          html: marked.parse('你好，我是社科智能分析助手（v2）。')
        }
      ],
      reportDraft: ''
    }
  },
  methods: {
    formatMarkdown (text) {
      const raw = String(text || '')
      return marked.parse(raw || '')
    },
    appendMessage (role, content) {
      this.messages.push({
        role,
        html: this.formatMarkdown(content)
      })
      this.$nextTick(() => {
        const box = this.$refs.messageBox
        if (box) box.scrollTop = box.scrollHeight
      })
    },
    extractAssistantText (data) {
      if (!data || typeof data !== 'object') return '请求失败：响应为空'
      const errorMsg = data.error && (data.error.message || data.error.code)
      if (errorMsg) return `请求失败：${errorMsg}`
      return data.assistant_message ||
        (data.result && data.result.markdown_report) ||
        (data.result && data.result.analysis) ||
        '未返回可展示内容'
    },
    async sendMessage () {
      const message = this.userInput
      if (!message || this.sending) return
      this.sending = true
      this.userInput = ''
      this.appendMessage('user', message)
      try {
        const { data } = await axios.post('/api/agent/chat/v2', {
          session_id: 'demo',
          message,
          context: {
            output_type: 'report'
          }
        })
        const assistantText = this.extractAssistantText(data)
        this.appendMessage('assistant', assistantText)
        const markdownReport = data && data.result && data.result.markdown_report
        if (markdownReport) this.reportDraft = markdownReport
      } catch (e) {
        const msg = (e.response && e.response.data && e.response.data.detail) || e.message || '请求失败'
        this.appendMessage('assistant', `请求异常：${msg}`)
      } finally {
        this.sending = false
      }
    },
    async downloadWord () {
      if (!this.reportDraft || this.exporting) return
      this.exporting = true
      try {
        const resp = await axios.post('/api/agent/export-word', {
          markdown: this.reportDraft
        }, {
          responseType: 'blob'
        })
        const blob = new Blob([resp.data], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = 'analysis-report.docx'
        document.body.appendChild(a)
        a.click()
        a.remove()
        window.URL.revokeObjectURL(url)
      } catch (e) {
        const msg = (e.response && e.response.data && e.response.data.detail) || e.message || '导出失败'
        this.appendMessage('assistant', `导出 Word 失败：${msg}`)
      } finally {
        this.exporting = false
      }
    },
    clearChat () {
      this.messages = [
        {
          role: 'assistant',
          html: marked.parse('会话已清空。你可以继续提问。')
        }
      ]
      this.reportDraft = ''
    }
  }
}
</script>

<style scoped lang="scss">
.agent-page {
  min-height: calc(100vh - 120px);
  color: #e8f0fa;
}

.toolbar {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid rgba(121, 173, 226, 0.3);
  border-radius: 10px;
  background: rgba(10, 33, 56, 0.9);
}

.toolbar-title {
  font-size: 16px;
  font-weight: 700;
  color: #d9ebff;
}

.actions {
  margin-left: auto;
}

button {
  height: 34px;
  border: none;
  border-radius: 6px;
  padding: 0 12px;
  background: linear-gradient(135deg, #2f7cf6, #1f61cc);
  color: #fff;
  cursor: pointer;
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.main {
  display: grid;
  grid-template-columns: 6.5fr 3.5fr;
  gap: 12px;
}

.chat-panel,
.right-panel {
  border: 1px solid rgba(121, 173, 226, 0.24);
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(8, 28, 48, 0.96), rgba(6, 20, 36, 0.96));
}

.chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 760px;
}

.chat-title,
.panel-title {
  padding: 10px 12px;
  font-weight: 600;
  border-bottom: 1px solid rgba(121, 173, 226, 0.2);
}

.message-box {
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.msg {
  display: flex;
  margin-bottom: 10px;
}

.msg.user {
  justify-content: flex-end;
}

.msg.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 88%;
  padding: 10px 12px;
  border-radius: 8px;
  line-height: 1.6;
  word-break: break-word;
  overflow-wrap: anywhere;
  white-space: normal;
}

.msg.user .bubble {
  background: rgba(47, 124, 246, 0.24);
  border: 1px solid rgba(112, 167, 255, 0.35);
}

.msg.assistant .bubble {
  background: rgba(9, 39, 70, 0.85);
  border: 1px solid rgba(121, 173, 226, 0.3);
}

.input-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid rgba(121, 173, 226, 0.2);
}

.input-row textarea {
  min-height: 90px;
  resize: vertical;
  border: 1px solid rgba(121, 173, 226, 0.3);
  border-radius: 8px;
  background: rgba(7, 25, 43, 0.95);
  color: #e8f0fa;
  padding: 10px;
  line-height: 1.55;
}

.right-panel {
  padding-bottom: 12px;
}

.report-editor {
  width: calc(100% - 24px);
  margin: 10px 12px;
  height: 300px;
  border: 1px solid rgba(121, 173, 226, 0.3);
  border-radius: 8px;
  background: rgba(7, 25, 43, 0.95);
  color: #e8f0fa;
  padding: 10px;
  line-height: 1.6;
  resize: vertical;
}

.panel-actions {
  padding: 0 12px;
}

@media (max-width: 1320px) {
  .main {
    grid-template-columns: 1fr;
  }
}
</style>
