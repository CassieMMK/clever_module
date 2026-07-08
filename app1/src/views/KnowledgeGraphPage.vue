<template>
  <div class="kg-page">
    <div class="toolbar">
      <div class="ctrl">
        <label>检索类型</label>
        <select v-model="searchType">
          <option value="auto">自动识别</option>
          <option value="author">作者</option>
          <option value="paper">论文</option>
        </select>
      </div>

      <div class="ctrl keyword">
        <label>关键词</label>
        <input
          v-model.trim="keyword"
          type="text"
          placeholder="输入作者名或论文名"
          @keyup.enter="searchGraph"
        />
      </div>

      <div class="actions">
        <button :disabled="loading" @click="searchGraph">查询</button>
        <button :disabled="loading" class="btn-secondary" @click="resetGraph">重置</button>
        <button :disabled="!graphHistory.length || loading || expanding" class="btn-secondary" @click="goBackStep">返回上一步</button>
        <button :disabled="!initialPayload || loading || expanding" class="btn-secondary" @click="backToInitial">回到查询结果</button>
        <button :disabled="rebuilding" class="btn-warning" @click="startRebuild">重建图谱</button>
      </div>

      <div class="ctrl switch">
        <label>相似作者</label>
        <input v-model="showSimilarAuthor" type="checkbox" />
      </div>
      <div class="ctrl switch">
        <label>相似论文</label>
        <input v-model="showSimilarPaper" type="checkbox" />
      </div>
    </div>

    <p v-if="errorText" class="err">{{ errorText }}</p>
    <p v-if="statusText" class="hint">{{ statusText }}</p>

    <div class="main">
      <div class="canvas-wrap">
        <div class="canvas-declare">
          声明：默认仅展示“作者-论文（撰写）”关系；作者相似/论文相似默认隐藏。点击节点后，按顶部相似开关可展示同类型相似实体；鼠标左键按住空白处可拖动画布，滚轮可缩放视图。
        </div>
        <div ref="graphRef" class="graph-canvas"></div>
      </div>

      <aside class="panel">
        <h3>节点详情</h3>
        <div v-if="activeNode">
          <p><strong>名称：</strong>{{ activeNode.display_name || activeNode.name }}</p>
          <p><strong>类型：</strong>{{ activeNode.category }}</p>
          <p v-if="activeNode.year"><strong>年份：</strong>{{ activeNode.year }}</p>
          <template v-if="activeNode.type === 'author'">
            <p><strong>所属机构：</strong>{{ getAttr(activeNode, 'org_name') || '-' }}</p>
            <p><strong>关联论文数：</strong>{{ getAttr(activeNode, 'paper_count') || 0 }}</p>
            <p><strong>省奖数量：</strong>{{ getAttr(activeNode, 'province_award_count') || 0 }}</p>
            <p><strong>国奖数量：</strong>{{ getAttr(activeNode, 'national_award_count') || 0 }}</p>
            <p><strong>团队数量：</strong>{{ getAttr(activeNode, 'group_count') || 0 }}</p>

            <div class="detail-block">
              <p class="detail-title">论文列表</p>
              <ul v-if="listFromAttr(activeNode, 'papers').length" class="detail-list">
                <li
                  v-for="(item, idx) in listFromAttr(activeNode, 'papers')"
                  :key="`paper_${activeNode.id}_${idx}`"
                >
                  {{ item.paper_name || '-' }} <span class="dim">({{ item.year || '未知' }})</span>
                </li>
              </ul>
              <p v-else class="dim">暂无</p>
            </div>

            <div class="detail-block">
              <p class="detail-title">省社科奖</p>
              <ul v-if="listFromAttr(activeNode, 'province_awards').length" class="detail-list">
                <li
                  v-for="(item, idx) in listFromAttr(activeNode, 'province_awards')"
                  :key="`paward_${activeNode.id}_${idx}`"
                >
                  {{ item.name || '-' }}
                  <span class="dim">｜{{ item.level || '等级未知' }}｜{{ item.year || '年份未知' }}</span>
                </li>
              </ul>
              <p v-else class="dim">暂无</p>
            </div>

            <div class="detail-block">
              <p class="detail-title">国家社科奖</p>
              <ul v-if="listFromAttr(activeNode, 'national_awards').length" class="detail-list">
                <li
                  v-for="(item, idx) in listFromAttr(activeNode, 'national_awards')"
                  :key="`naward_${activeNode.id}_${idx}`"
                >
                  {{ item.name || '-' }}
                  <span class="dim">｜{{ item.type_name || '类型未知' }}｜{{ item.category_name || '类别未知' }}｜{{ item.year || '年份未知' }}</span>
                </li>
              </ul>
              <p v-else class="dim">暂无</p>
            </div>

            <div class="detail-block">
              <p class="detail-title">社科团队</p>
              <ul v-if="listFromAttr(activeNode, 'groups').length" class="detail-list">
                <li
                  v-for="(item, idx) in listFromAttr(activeNode, 'groups')"
                  :key="`group_${activeNode.id}_${idx}`"
                >
                  {{ item.name || '-' }}
                  <span class="dim">｜{{ item.type_name || '类型未知' }}｜{{ item.start_year || '未知' }}-{{ item.end_year || '未知' }}</span>
                </li>
              </ul>
              <p v-else class="dim">暂无</p>
            </div>
          </template>
          <p class="tip">提示：单击节点可将该节点作为新中心继续展开；可用“返回上一步/回到查询结果”返回。</p>
        </div>
        <div v-else class="placeholder">单击左侧节点查看详情与继续展开。</div>
      </aside>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import * as echarts from 'echarts'

export default {
  name: 'KnowledgeGraphPage',
  data () {
    return {
      keyword: '',
      searchType: 'auto',
      showSimilarAuthor: false,
      showSimilarPaper: false,
      loading: false,
      expanding: false,
      rebuilding: false,
      errorText: '',
      statusText: '',
      rawPayload: { nodes: [], edges: [], metadata: {} },
      activeNode: null,
      chartInstance: null,
      initialPayload: null,
      graphHistory: []
    }
  },
  mounted () {
    this.initChart()
    this.searchGraph()
    window.addEventListener('resize', this.onResize)
  },
  beforeUnmount () {
    window.removeEventListener('resize', this.onResize)
    if (this.chartInstance) {
      this.chartInstance.dispose()
      this.chartInstance = null
    }
  },
  methods: {
    getAttr (node, key) {
      const attrs = node && node.attributes
      if (!attrs || typeof attrs !== 'object') return ''
      return attrs[key]
    },
    listFromAttr (node, key) {
      const value = this.getAttr(node, key)
      return Array.isArray(value) ? value : []
    },
    parseError (e) {
      return (e.response && e.response.data && e.response.data.detail && e.response.data.detail.message) ||
        (e.response && e.response.data && e.response.data.message) ||
        e.message ||
        '请求失败'
    },
    clonePayload (payload) {
      try {
        return JSON.parse(JSON.stringify(payload || { nodes: [], edges: [], metadata: {} }))
      } catch (_) {
        return { nodes: [], edges: [], metadata: {} }
      }
    },
    initChart () {
      const dom = this.$refs.graphRef
      if (!dom) return
      this.chartInstance = echarts.init(dom)
      this.chartInstance.on('click', (params) => {
        if (this.loading || this.expanding) return
        if (!params || params.dataType !== 'node') return
        const nodeId = params.data && params.data.id
        if (!nodeId) return
        const node = (this.rawPayload.nodes || []).find(n => n.id === nodeId)
        if (!node) return
        this.activeNode = node
        this.expandFromNode(nodeId, node.type)
      })
    },
    async searchGraph () {
      this.loading = true
      this.errorText = ''
      this.statusText = ''
      try {
        const { data } = await axios.get('/api/kg/query', {
          params: {
            keyword: this.keyword,
            search_type: this.searchType,
            max_nodes: 300,
            include_similar: false
          }
        })
        this.rawPayload = data || { nodes: [], edges: [], metadata: {} }
        this.initialPayload = this.clonePayload(this.rawPayload)
        this.graphHistory = []
        this.activeNode = null
        this.renderGraph(true)
        const hitMode = this.rawPayload.metadata && this.rawPayload.metadata.hit_mode
        const queryType = this.rawPayload.metadata && this.rawPayload.metadata.query_type
        this.statusText = `命中方式：${hitMode || 'unknown'} ｜ 识别类型：${queryType || 'unknown'}`
      } catch (e) {
        this.errorText = this.parseError(e)
        if (e && e.response && e.response.status === 409) {
          this.statusText = '图谱数据版本已升级，请先点击“重建图谱”再查询。'
        }
      } finally {
        this.loading = false
      }
    },
    similarEnabledByType (entityType) {
      if (entityType === 'author') return this.showSimilarAuthor
      if (entityType === 'paper') return this.showSimilarPaper
      return false
    },
    async expandFromNode (centerId, entityType = '') {
      if (this.expanding) return
      this.graphHistory.push({
        payload: this.clonePayload(this.rawPayload),
        activeNode: this.activeNode ? { ...this.activeNode } : null,
        statusText: this.statusText || ''
      })
      this.expanding = true
      try {
        const includeSimilar = this.similarEnabledByType(entityType)
        const { data } = await axios.get('/api/kg/subgraph', {
          params: {
            center_id: centerId,
            depth: 1,
            max_nodes: 300,
            include_similar: includeSimilar
          }
        })
        this.rawPayload = data || { nodes: [], edges: [], metadata: {} }
        this.activeNode = (this.rawPayload.nodes || []).find(n => n.id === centerId) || this.activeNode
        this.renderGraph(false)
        const meta = this.rawPayload.metadata || {}
        if (meta.truncated) {
          this.statusText = '结果较大，已按上限裁剪显示（max_nodes=300）'
        }
      } catch (e) {
        if (this.graphHistory.length) this.graphHistory.pop()
        this.errorText = this.parseError(e)
        if (e && e.response && e.response.status === 409) {
          this.statusText = '图谱数据版本已升级，请先点击“重建图谱”再查询。'
        }
      } finally {
        this.expanding = false
      }
    },
    goBackStep () {
      if (!this.graphHistory.length) return
      const previous = this.graphHistory.pop()
      this.rawPayload = this.clonePayload(previous.payload)
      this.activeNode = previous.activeNode || null
      this.statusText = previous.statusText || ''
      this.errorText = ''
      this.renderGraph(false)
    },
    backToInitial () {
      if (!this.initialPayload) return
      this.rawPayload = this.clonePayload(this.initialPayload)
      this.activeNode = null
      this.errorText = ''
      this.graphHistory = []
      this.renderGraph(true)
    },
    async startRebuild () {
      this.rebuilding = true
      this.errorText = ''
      this.statusText = '正在启动重建任务...'
      try {
        const startResp = await axios.post('/api/kg/rebuild/start')
        const taskId = startResp && startResp.data && startResp.data.task_id
        if (!taskId) {
          this.statusText = '重建任务已启动'
          return
        }
        await this.pollRebuildStatus(taskId)
      } catch (e) {
        this.errorText = this.parseError(e)
      } finally {
        this.rebuilding = false
      }
    },
    async pollRebuildStatus (taskId) {
      for (let i = 0; i < 120; i += 1) {
        const { data } = await axios.get('/api/kg/rebuild/status', { params: { task_id: taskId } })
        const status = data && data.status
        const stage = data && data.stage
        const progress = data && data.progress
        this.statusText = `重建中：${stage || 'unknown'} ${progress || 0}%`
        if (status === 'success') {
          this.statusText = '重建完成，已刷新图谱'
          await this.searchGraph()
          return
        }
        if (status === 'failed') {
          throw new Error((data && data.message) || '重建失败')
        }
        await new Promise(resolve => setTimeout(resolve, 1000))
      }
      throw new Error('重建状态轮询超时')
    },
    resetGraph () {
      this.keyword = ''
      this.searchType = 'auto'
      this.errorText = ''
      this.statusText = ''
      this.searchGraph()
    },
    renderGraph (fitView = false) {
      if (!this.chartInstance) return
      const option = this.buildChartOption()
      this.chartInstance.setOption(option, true)
      if (fitView) this.chartInstance.resize()
    },
    buildChartOption () {
      const nodes = this.rawPayload.nodes || []
      const edges = this.rawPayload.edges || []
      const graphDom = this.$refs.graphRef
      const canvasWidth = (graphDom && graphDom.clientWidth) || 1200
      const canvasHeight = (graphDom && graphDom.clientHeight) || 760

      const MAX_RENDER_NODES = 220
      const clipped = nodes.length > MAX_RENDER_NODES
      const renderNodes = clipped ? nodes.slice(0, MAX_RENDER_NODES) : nodes
      const renderIdSet = new Set(renderNodes.map(n => n.id))
      const renderEdges = edges.filter(e => renderIdSet.has(e.source || e.from) && renderIdSet.has(e.target || e.to))
      const renderNodeMap = {}
      renderNodes.forEach(n => {
        renderNodeMap[n.id] = n
      })

      const similarInfoByNode = {}
      renderEdges.forEach((edge) => {
        const isSimilar = edge.type === 'similar' || edge.line_style === 'dashed'
        if (!isSimilar) return
        const src = edge.source || edge.from
        const dst = edge.target || edge.to
        if (!src || !dst) return
        if (similarInfoByNode[dst]) return
        const srcNode = renderNodeMap[src] || {}
        const score = typeof edge.similarity_score === 'number' ? edge.similarity_score.toFixed(3) : '-'
        const basis = edge.similarity_basis_text || ''
        similarInfoByNode[dst] = {
          sourceName: srcNode.display_name || srcNode.name || src,
          score,
          basis
        }
      })
      if (clipped) {
        this.statusText = `节点过多已限流渲染前 ${MAX_RENDER_NODES} 个，请继续缩小范围后查看详情。`
      }

      const authors = renderNodes.filter(n => n.type === 'author')
      const papers = renderNodes.filter(n => n.type === 'paper')
      const positions = {}
      const centerY = Math.round(canvasHeight * 0.55)
      const leftX = Math.round(canvasWidth * 0.26)
      const rightX = Math.round(canvasWidth * 0.74)

      const placeList = (list, x, baseY) => {
        if (!list.length) return
        const availableHeight = Math.max(320, canvasHeight - 220)
        const gap = Math.max(20, Math.min(54, Math.floor(availableHeight / Math.max(list.length, 1))))
        const startY = baseY - Math.floor((list.length - 1) * gap / 2)
        list.forEach((node, idx) => {
          positions[node.id] = { x, y: startY + idx * gap }
        })
      }
      placeList(authors, leftX, centerY)
      placeList(papers, rightX, centerY)

      const chartNodes = renderNodes.map((node) => {
        const point = positions[node.id] || { x: Math.round(canvasWidth * 0.5), y: centerY }
        const isAuthor = node.type === 'author'
        const text = String(node.display_name || node.name || '-')
        const attrs = node.attributes || {}
        const authorSuffix = isAuthor
          ? `\n省奖:${attrs.province_award_count || 0} 国奖:${attrs.national_award_count || 0}`
          : ''
        const valueNode = {
          ...node,
          _similarInfo: similarInfoByNode[node.id] || null
        }
        return {
          id: node.id,
          name: text.length > 22 ? `${text.slice(0, 22)}...` : text,
          x: point.x,
          y: point.y,
          symbol: 'roundRect',
          symbolSize: isAuthor ? [168, 54] : [210, 52],
          itemStyle: {
            color: isAuthor ? '#2f7cf6' : '#18a572',
            borderColor: isAuthor ? '#9ec5ff' : '#9be4cc',
            borderWidth: 1.2,
            shadowColor: 'rgba(0,0,0,0.25)',
            shadowBlur: 8
          },
          label: {
            show: true,
            color: '#ffffff',
            fontSize: 12,
            lineHeight: 15,
            formatter: `${text.length > 22 ? `${text.slice(0, 22)}...` : text}${authorSuffix}`
          },
          value: valueNode
        }
      })

      const chartEdges = renderEdges.map((edge) => {
        const src = edge.source || edge.from
        const dst = edge.target || edge.to
        const isSimilar = edge.type === 'similar' || edge.line_style === 'dashed'
        return {
          source: src,
          target: dst,
          value: edge,
          lineStyle: {
            color: isSimilar ? '#f39c12' : '#b7c9e3',
            width: isSimilar ? 1.4 : 1.6,
            type: isSimilar ? 'dashed' : 'solid',
            opacity: isSimilar ? 0.75 : 0.6,
            curveness: 0.06
          }
        }
      })

      return {
        backgroundColor: 'transparent',
        animation: false,
        tooltip: {
          trigger: 'item',
          confine: true,
          textStyle: {
            fontSize: 12,
            lineHeight: 18
          },
          extraCssText: 'max-width: 520px; white-space: normal; word-break: break-word; line-height: 1.5;',
          position: (point, params, dom, rect, size) => {
            const viewWidth = size && size.viewSize ? size.viewSize[0] : 1200
            const viewHeight = size && size.viewSize ? size.viewSize[1] : 760
            const boxWidth = size && size.contentSize ? size.contentSize[0] : 260
            const boxHeight = size && size.contentSize ? size.contentSize[1] : 80

            let x = (point && point[0] ? point[0] : 0) + 28
            let y = (point && point[1] ? point[1] : 0) - boxHeight / 2

            if (x + boxWidth > viewWidth - 8) x = viewWidth - boxWidth - 8
            if (y + boxHeight > viewHeight - 8) y = viewHeight - boxHeight - 8
            if (x < 8) x = 8
            if (y < 8) y = 8
            return [x, y]
          },
          formatter: (params) => {
            if (params.dataType === 'node') {
              const n = params.data && params.data.value ? params.data.value : {}
              const base = `${n.display_name || n.name || '-'}<br/>类型：${n.category || n.type || '-'}`
              const sim = n._similarInfo
              if (!sim) return base
              return `${base}<br/>与“${sim.sourceName}”相似（${sim.score}）<br/>${sim.basis || ''}`
            }
            const isSimilar = params.data && params.data.lineStyle && params.data.lineStyle.type === 'dashed'
            if (!isSimilar) return '关系：撰写'
            return '关系：相似（悬停相似节点查看依据）'
          }
        },
        series: [
          {
            type: 'graph',
            coordinateSystem: null,
            layout: 'none',
            roam: true,
            scaleLimit: {
              min: 0.55,
              max: 2.2
            },
            nodeScaleRatio: 0.7,
            draggable: false,
            focusNodeAdjacency: true,
            edgeSymbol: ['none', 'arrow'],
            edgeSymbolSize: [0, 8],
            edgeLabel: {
              show: true,
              position: 'end',
              rotate: false,
              distance: 10,
              color: '#ffd27d',
              fontSize: 12,
              backgroundColor: 'rgba(13, 26, 40, 0.72)',
              borderRadius: 4,
              padding: [2, 5],
              formatter: (params) => {
                const edge = params && params.data && params.data.value ? params.data.value : {}
                const isSimilar = edge.type === 'similar' || edge.line_style === 'dashed'
                if (!isSimilar) return ''
                const score = typeof edge.similarity_score === 'number' ? edge.similarity_score.toFixed(3) : '-'
                return `相似 ${score}`
              }
            },
            data: chartNodes,
            links: chartEdges,
            lineStyle: {
              opacity: 0.6
            },
            emphasis: {
              focus: 'adjacency',
              lineStyle: { width: 2.2, opacity: 0.95 }
            }
          }
        ]
      }
    },
    onResize () {
      if (!this.chartInstance) return
      this.chartInstance.resize()
    }
  }
}
</script>

<style scoped lang="scss">
.kg-page {
  color: #e8f0fa;
  background: #081a2d;
  min-height: 100%;
  padding: 14px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-end;
  padding: 12px;
  background: rgba(10, 33, 56, 0.92);
  border: 1px solid rgba(121, 173, 226, 0.32);
  border-radius: 10px;
  margin-bottom: 10px;
}

.ctrl {
  min-width: 180px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ctrl.keyword {
  min-width: 280px;
}

.ctrl.switch {
  min-width: 110px;
  align-items: center;
}

.ctrl label {
  font-size: 12px;
  color: #9fc2e8;
}

.ctrl input,
.ctrl select {
  height: 34px;
  border: 1px solid rgba(121, 173, 226, 0.35);
  border-radius: 6px;
  padding: 0 10px;
  background: rgba(7, 25, 43, 0.94);
  color: #e8f0fa;
}

.actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
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

.btn-secondary {
  background: linear-gradient(135deg, #7f8c8d, #5d6d6f);
}

.btn-warning {
  background: linear-gradient(135deg, #d68910, #b9770e);
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.err {
  color: #ff8f8f;
  margin: 0 0 8px;
}

.hint {
  color: #ffd27d;
  margin: 0 0 8px;
}

.main {
  display: grid;
  grid-template-columns: 7fr 3fr;
  gap: 12px;
}

.canvas-wrap {
  position: relative;
  background: linear-gradient(180deg, rgba(8, 28, 48, 0.96), rgba(6, 20, 36, 0.96));
  border: 1px solid rgba(121, 173, 226, 0.24);
  border-radius: 10px;
  min-height: 760px;
  overflow: hidden;
}

.canvas-declare {
  position: absolute;
  left: 12px;
  right: 12px;
  top: 10px;
  z-index: 2;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(3, 14, 26, 0.74);
  border: 1px solid rgba(121, 173, 226, 0.25);
  color: #b9d7f6;
  font-size: 12px;
  line-height: 1.45;
  pointer-events: none;
}

.graph-canvas {
  width: 100%;
  height: 760px;
}

.panel {
  background: linear-gradient(180deg, rgba(9, 30, 52, 0.95), rgba(7, 24, 42, 0.95));
  border: 1px solid rgba(121, 173, 226, 0.24);
  border-radius: 10px;
  padding: 12px;
  max-height: 760px;
  overflow: auto;
}

.tip {
  color: #9fc2e8;
}

.detail-block {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid rgba(121, 173, 226, 0.2);
}

.detail-title {
  margin: 0 0 6px;
  color: #d7e9ff;
  font-weight: 600;
}

.detail-list {
  margin: 0;
  padding-left: 18px;
}

.detail-list li {
  margin: 4px 0;
  line-height: 1.4;
}

.dim {
  color: #9fb9d3;
}

.placeholder {
  color: #9fb9d3;
  line-height: 1.8;
}

@media (max-width: 1320px) {
  .main {
    grid-template-columns: 1fr;
  }

  .graph-canvas {
    height: 680px;
  }

  .panel {
    max-height: none;
  }
}
</style>
