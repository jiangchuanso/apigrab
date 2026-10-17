<template>
  <div class="api-response-panel">
    <!-- 失败结果：试跑结果与判定摘要照常可见，不因失败而隐藏 -->
    <el-alert
      v-if="result && result.success === false"
      type="error"
      :closable="false"
      show-icon
      style="margin-bottom: 12px;"
    >
      <template slot="title">试跑失败：{{ result.error || '未知错误' }}</template>
    </el-alert>

    <!-- 判定摘要（design C9）：常驻显示，不藏在页签里 -->
    <el-descriptions v-if="detection" :column="2" size="mini" border style="margin-bottom: 12px;">
      <el-descriptions-item label="响应头声明">
        {{ detection.contentType || '（响应未声明内容类型）' }}
      </el-descriptions-item>
      <el-descriptions-item label="判定结果">
        {{ detection.detectedRespType || '未能判定' }}
      </el-descriptions-item>
      <el-descriptions-item label="多行形态">
        {{ detection.detectedMultilines === null || detection.detectedMultilines === undefined
          ? '未能判定'
          : (detection.detectedMultilines ? '多行响应 (NDJSON)' : '单条响应') }}
      </el-descriptions-item>
      <el-descriptions-item label="本次使用">
        {{ detection.usedRespType || '（未设置）' }} / {{ detection.usedMultilines ? '多行' : '单条' }}
      </el-descriptions-item>
    </el-descriptions>

    <el-alert
      v-if="detection && detection.conflict"
      type="error"
      :closable="false"
      show-icon
      style="margin-bottom: 12px;"
    >
      <template slot="title">
        响应头判定为 {{ detection.detectedRespType }}，而本次使用的是 {{ detection.usedRespType }}。请确认是否改用判定结果。
      </template>
    </el-alert>

    <el-alert
      v-else-if="detection && !detection.detectedRespType"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 12px;"
    >
      <template slot="title">
        未能自动识别响应类型{{ detection.contentType ? `（响应头声明为 ${detection.contentType}，无法对应到已有格式）` : '（响应未声明内容类型）' }}，
        已保留当前设置，请手动确认。
      </template>
    </el-alert>

    <el-tabs v-model="activeTab">
      <!-- 结构树：**唯一**可点选配路径的视图。JSON 与 XML 都走这里（design P1） -->
      <el-tab-pane label="响应结构（点此配路径）" name="tree">
        <!-- 常驻诊断行：让「到底拿到了什么」一眼可见，避免再次陷入「看起来是空的」而无法定位 -->
        <div class="pane-diag">
          原始响应 {{ rawText ? rawText.length : 0 }} 字符
          ｜ 结构节点 {{ nodeCount }} 个
          ｜ 解析 {{ parseMode }}
        </div>

        <div v-if="!rawText" class="pane-empty">
          <el-alert type="warning" :closable="false" show-icon>
            <template slot="title">
              本次试跑没有拿到响应体（原始响应为空）。
              <template v-if="result && result.error">失败原因：{{ result.error }}</template>
              <template v-else>请确认接口是否正常返回内容。</template>
            </template>
          </el-alert>
        </div>
        <template v-else-if="!parsedTree">
          <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 10px;">
            <template slot="title">{{ treeUnavailableReason }}</template>
          </el-alert>
          <pre class="raw-text">{{ rawText }}</pre>
        </template>
        <template v-else>
          <div class="tree-current">
            <span class="tree-current-label">当前数据提取路径：</span>
            <code v-if="selectedPath">{{ selectedPath }}</code>
            <span v-else class="tree-current-empty">（未设置，点击下方节点即可填入）</span>
            <el-button v-if="selectedPath" type="text" size="mini" @click="onClearPath">清除</el-button>
          </div>
          <div class="pane-hint">
            点击下方节点即把路径填入「数据提取路径」；<strong>数组节点会自动带展平写法</strong>。
            带「不可选为路径」标记的节点不能作为记录路径。
            <span v-if="treeTruncated" class="pane-warn">（节点过多，已截断，仅显示部分结构）</span>
          </div>
          <!-- 纯 HTML 渲染，不使用树组件：层级由 flattenTree 计算、缩进由 padding 表达，
               整段结构可在 Node 中被断言；树组件的渲染在本环境无法验证 -->
          <div class="tree-flat tree-scroll">
            <div
              v-for="row in flatTree"
              :key="row.id"
              class="tree-row"
              :class="{ 'node-locked': !row.selectable }"
              :style="{ paddingLeft: (6 + row.depth * 18) + 'px' }"
              @click="onRowClick(row)"
            >
              <span class="tree-label">{{ row.label }}</span>
              <el-tag v-if="row.isArray" size="mini" type="warning" style="margin-left: 6px;">数组</el-tag>
              <el-tag v-if="!row.selectable" size="mini" type="info" style="margin-left: 6px;">不可选为路径</el-tag>
            </div>
          </div>
        </template>
      </el-tab-pane>

      <!-- 原始响应：仅供排障查看原始字节。**不放可点选视图**——否则会与「响应结构」页签
           形成两棵看起来一样、行为却不同的树，用户点错一棵会以为功能失效 -->
      <el-tab-pane label="原始响应文本" name="raw">
        <div class="pane-hint">
          未经处理的响应原文（排障查看空白字符、编码等问题用）。如需点选配置路径，请用「响应结构」页签。
        </div>
        <div v-if="!rawText" class="pane-empty">
          <el-alert type="info" title="本次响应无内容" :closable="false" />
        </div>
        <pre v-else class="raw-text raw-text-tall">{{ rawText }}</pre>
      </el-tab-pane>

      <el-tab-pane v-if="showParsedData" name="parsed">
        <template slot="label">
          解析数据
          <el-tag v-if="parsedStale" size="mini" type="warning" style="margin-left: 4px;">需重跑</el-tag>
        </template>

        <!-- 路径变更后，已有解析结果来自上一次试跑，必须显式声明，不能让用户误以为它对应当前路径 -->
        <el-alert
          v-if="parsedStale"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 10px;"
        >
          <template slot="title">
            「数据提取路径」已被改动，下方结果是<strong>上一次试跑</strong>的产物
            （当时生效路径：{{ usedPathLabel }}；当前路径：{{ currentPathLabel }}）。
          </template>
          <div style="margin-top: 8px;">
            <el-button type="primary" size="mini" :loading="testing" @click="$emit('retest')">
              用当前路径重新试跑
            </el-button>
            <span style="margin-left: 8px; font-size: 12px; color: #909399;">
              解析由后端按路径执行，需重新请求才能得到对应结果；不会自动请求，避免对目标接口造成意外调用。
            </span>
          </div>
        </el-alert>

        <div v-if="!result || !result.parsedData" class="pane-empty">
          <el-alert type="info" title="暂无解析数据" :closable="false" />
        </div>
        <template v-else>
          <div class="pane-hint">
            本次结果对应路径：<code>{{ usedPathLabel }}</code>
            ｜ 记录数：{{ parsedRecordsCount }}
          </div>
          <div class="pane-hint">
            <el-radio-group v-model="parsedViewMode" size="mini">
              <el-radio-button label="formatted">结构化</el-radio-button>
              <el-radio-button label="text">原始文本</el-radio-button>
            </el-radio-group>
          </div>
          <JsonDisplay
            v-if="parsedViewMode === 'formatted'"
            :data="result.parsedData"
            :deep="3"
            class="tree-scroll"
          />
          <pre v-else class="raw-text">{{ formattedParsedText }}</pre>
        </template>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import JsonDisplay from '@/utils/jsonDisplay'
import { buildJsonTree, buildXmlTree, createNodeBudget, flattenTree } from '@/utils/responseTree'

export default {
  name: 'ApiResponsePanel',
  components: { JsonDisplay },
  props: {
    /** 试跑结果对象：{ success, error, rawResponse, parsedData, dataPath } */
    result: {
      type: Object,
      default: null
    },
    /** 判定摘要：{ contentType, detectedRespType, detectedMultilines, usedRespType, usedMultilines, conflict, autoFilled } */
    detection: {
      type: Object,
      default: null
    },
    /** 当前表单选择的响应类型，决定按 JSON 还是 XML 解析原始响应 */
    respType: {
      type: String,
      default: 'JSON'
    },
    /** 当前已填入的数据提取路径，用于在面板内即时回显点选结果（否则结果落在屏外，用户看不到反馈） */
    selectedPath: {
      type: String,
      default: ''
    },
    /** 是否有试跑正在进行，用于禁用「重新试跑」按钮 */
    testing: {
      type: Boolean,
      default: false
    },
    /**
     * 是否展示「解析数据」页签（api-config 的响应解析结果）。
     * 认证配置的 Token 测试只借本面板的点选能力，没有「按路径解析记录」的语义，
     * 传 false 可隐藏该页签；默认 true 以保持接口配置页行为不变（design D4）。
     */
    showParsedData: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      activeTab: 'tree',
      parsedViewMode: 'formatted'
    }
  },
  computed: {
    rawText() {
      const raw = this.result && this.result.rawResponse
      return raw === null || raw === undefined || raw === '' ? '' : String(raw)
    },
    isXml() {
      return this.respType === 'XML'
    },
    treeResult() {
      const text = this.rawText
      if (!text) return { nodes: [], truncated: false, error: null }
      const budget = createNodeBudget()
      try {
        if (this.isXml) {
          const doc = this.parseXmlDocument(text)
          if (!doc) return { nodes: [], truncated: false, error: null }
          const nodes = buildXmlTree(doc.documentElement, budget)
          return { nodes, truncated: budget.left <= 0, error: null }
        }
        const nodes = buildJsonTree(JSON.parse(text), budget)
        return { nodes, truncated: budget.left <= 0, error: null }
      } catch (e) {
        // 不再静默吞掉：解析异常必须可见，否则用户只会看到「空的」而无从定位
        return { nodes: [], truncated: false, error: e && e.message ? e.message : String(e) }
      }
    },
    parsedTree() {
      // 空数组在 JS 中为真值，必须归一为 null，否则「无法解析」分支不会命中
      const nodes = this.treeResult.nodes
      return nodes && nodes.length ? nodes : null
    },
    /** 展平为带层级的行，供纯 HTML 渲染（不使用树组件：其渲染在本环境无法验证且已多次故障） */
    flatTree() {
      return flattenTree(this.parsedTree)
    },
    nodeCount() {
      const nodes = this.treeResult.nodes
      if (!nodes || !nodes.length) return 0
      let count = 0
      const walk = n => {
        count += 1
        if (n.children) n.children.forEach(walk)
      }
      nodes.forEach(walk)
      return count
    },
    parseMode() {
      return this.isXml ? '按 XML 解析' : '按 JSON 解析'
    },
    usedPathLabel() {
      const p = this.result && this.result.dataPath
      return p ? p : '（留空，整份响应作为一条记录）'
    },
    currentPathLabel() {
      return this.selectedPath ? this.selectedPath : '（留空，整份响应作为一条记录）'
    },
    /**
     * 当前路径与「产出本次解析结果所用路径」是否不一致。
     * 解析由后端执行，改路径后不重新试跑就拿不到新结果——不提示会让用户把旧结果当成新路径的结果。
     */
    parsedStale() {
      if (!this.result || this.result.success !== true) return false
      return this.normalizePath(this.selectedPath) !== this.normalizePath(this.result.dataPath)
    },
    treeTruncated() {
      return this.treeResult.truncated
    },
    treeUnavailableReason() {
      const err = this.treeResult.error
      if (err) return `按当前响应类型解析失败：${err}。已降级为原始文本；可检查上方「响应类型」是否正确。`
      const head = this.isXml ? '响应不是可解析的 XML' : '响应不是可解析的 JSON'
      return `${head}，无法生成结构树。已降级为原始文本展示；如接口实际返回该格式，请检查上方「响应类型」。`
    },
    parsedRecordsCount() {
      const data = this.result && this.result.parsedData
      if (!data) return 0
      return Array.isArray(data) ? data.length : 1
    },
    formattedParsedText() {
      const data = this.result && this.result.parsedData
      try {
        return JSON.stringify(data, null, 2)
      } catch {
        return String(data)
      }
    }
  },
  methods: {
    /** 路径比较前的归一：空白/null/undefined 一律视为「留空」，避免 '  ' 与 '' 被当成不同路径 */
    normalizePath(value) {
      const text = value === null || value === undefined ? '' : String(value).trim()
      return text === '' ? null : text
    },
    /** 点选一行 → 交由父级决定如何落库；本组件不写表单（design C3） */
    onRowClick(row) {
      if (!row || row.selectable === false || !row.emitPath) return
      this.$emit('select-path', { path: row.emitPath, isArray: !!row.isArray })
    },
    onClearPath() {
      this.$emit('select-path', { path: '', isArray: false })
    },
    parseXmlDocument(text) {
      try {
        const doc = new DOMParser().parseFromString(text, 'application/xml')
        if (!doc || doc.getElementsByTagName('parsererror').length > 0) return null
        return doc
      } catch {
        return null
      }
    }
  }
}
</script>

<style scoped>
.api-response-panel {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
  background: #fff;
}
.pane-hint {
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}
.pane-diag {
  margin-bottom: 8px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
}
.pane-warn {
  color: #e6a23c;
}
.pane-empty {
  padding: 4px 0;
}
.tree-scroll {
  max-height: 320px;
  overflow: auto;
}
.tree-flat {
  padding: 4px 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.tree-row {
  display: flex;
  align-items: center;
  padding-top: 3px;
  padding-bottom: 3px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}
.tree-row:hover {
  background: #ecf5ff;
}
.tree-row.node-locked {
  color: #c0c4cc;
  cursor: default;
}
.tree-row.node-locked:hover {
  background: transparent;
}
.raw-text {
  max-height: 320px;
  overflow: auto;
  margin: 0;
  padding: 8px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
.raw-text-tall {
  max-height: 420px;
}
.tree-current {
  margin-bottom: 6px;
  padding: 6px 8px;
  background: #f4f8ff;
  border: 1px solid #d6e4ff;
  border-radius: 4px;
  font-size: 12px;
}
.tree-current-label {
  color: #909399;
}
.tree-current code {
  color: #409eff;
  font-family: 'Courier New', monospace;
  word-break: break-all;
}
.tree-current-empty {
  color: #c0c4cc;
}
.tree-node {
  display: inline-flex;
  align-items: center;
}
.tree-label {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}
.node-locked {
  color: #c0c4cc;
}
</style>
