<template>
  <div class="execution-log-detail">
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">执行详情</span>
        <div class="rc-card-header__actions">
          <el-button
            size="small"
            type="primary"
            :loading="refreshing"
            @click="refresh"
          >刷新</el-button>
          <el-button size="small" @click="$router.push('/monitor/history')">返回列表</el-button>
        </div>
      </div>

      <el-form label-width="120px" size="small" v-if="detail">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="执行 ID">{{ detail.id }}</el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="任务 ID">{{ detail.taskId }}</el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-tag :type="statusType(detail.status)" size="mini">{{ detail.status }}</el-tag>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="开始时间">{{ detail.startTime }}</el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束时间">{{ detail.endTime || '-' }}</el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="耗时">{{ detail.costMs }} ms</el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20" v-if="isRunning">
          <el-col :span="12">
            <el-form-item label="当前阶段">
              <el-tag type="warning" size="mini">{{ detail.currentStage ? stageLabel(detail.currentStage) : '-' }}</el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已处理条数">{{ detail.processedCount || 0 }}</el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="总记录">{{ detail.totalRecords }}</el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="成功">{{ detail.successCount }}</el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="失败">{{ detail.failCount }}</el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="采集页数">{{ detail.pageCount }}</el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="detail.errorMsg" label="错误信息">
          <el-input type="textarea" :rows="3" :value="detail.errorMsg" readonly style="color: #F56C6C;" />
        </el-form-item>
      </el-form>

      <!-- 分阶段明细 -->
      <el-divider content-position="left">分阶段明细</el-divider>
      <el-table :data="stages" border size="small" style="width: 100%;">
        <el-table-column label="阶段" min-width="120">
          <template slot-scope="{ row }">
            <el-tag size="mini">{{ stageLabel(row.stage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inputCount" label="输入" min-width="100" />
        <el-table-column prop="outputCount" label="输出" min-width="100" />
        <el-table-column prop="successCount" label="成功" min-width="100" />
        <el-table-column prop="failCount" label="失败" min-width="100" />
        <el-table-column label="耗时" min-width="120">
          <template slot-scope="{ row }">{{ row.costMs }} ms</template>
        </el-table-column>
      </el-table>

      <!-- 失败样本 -->
      <el-divider content-position="left">失败样本</el-divider>
      <el-table :data="failureSamples" border size="small" style="width: 100%;" max-height="300">
        <el-table-column label="阶段" width="120">
          <template slot-scope="{ row }">
            <el-tag size="mini" type="danger">{{ stageLabel(row.stage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="失败原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sourceRecord" label="源记录片段" min-width="300" show-overflow-tooltip />
      </el-table>

      <!-- 已入库数据预览 -->
      <el-divider content-position="left">已入库数据</el-divider>
      <el-button size="small" @click="loadData" :loading="dataLoading" style="margin-bottom: 10px;">查看入库数据</el-button>

      <template v-if="dataList.length > 0">
        <el-table :data="dataList" border size="small" max-height="400" style="width: 100%;">
          <el-table-column
            v-for="(val, key) in dataList[0]"
            :key="key"
            :prop="key"
            :label="key"
            min-width="120"
            show-overflow-tooltip
          />
        </el-table>
      </template>
    </el-card>
  </div>
</template>

<script>
import { getExecutionDetail, getExecutionData, getExecutionStages } from '@/api/execution'

export default {
  name: 'ExecutionLogDetail',
  data() {
    return {
      detail: null,
      stages: [],
      dataList: [],
      dataLoading: false,
      refreshing: false,
      pollTimer: null
    }
  },
  computed: {
    isRunning() {
      return this.detail && this.detail.status === 'RUNNING'
    },
    failureSamples() {
      const samples = []
      this.stages.forEach(stage => {
        if (!stage.failureSamples) return
        let parsed = stage.failureSamples
        if (typeof parsed === 'string') {
          try { parsed = JSON.parse(parsed) } catch { parsed = [] }
        }
        if (Array.isArray(parsed)) {
          parsed.forEach(item => samples.push({ stage: stage.stage, ...item }))
        }
      })
      return samples
    }
  },
  created() {
    this.loadDetail()
    this.loadStages()
  },
  beforeDestroy() {
    this.stopPolling()
  },
  methods: {
    async refresh() {
      this.refreshing = true
      try {
        await this.loadDetail()
        await this.loadStages()
      } finally {
        this.refreshing = false
      }
    },
    async loadDetail() {
      try {
        const res = await getExecutionDetail(this.$route.params.executionId)
        this.detail = res.data || res
        if (this.isRunning) {
          this.startPolling()
        } else {
          this.stopPolling()
          this.loadStages()
        }
      } catch {
        this.$message.error('加载执行详情失败')
      }
    },
    async loadStages() {
      try {
        const res = await getExecutionStages(this.$route.params.executionId)
        this.stages = res.data || []
      } catch {
        this.stages = []
      }
    },
    startPolling() {
      if (this.pollTimer) return
      this.pollTimer = setInterval(() => {
        this.loadDetail()
      }, 2000)
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    async loadData() {
      this.dataLoading = true
      try {
        const res = await getExecutionData(this.$route.params.executionId)
        this.dataList = res.data || []
      } catch {
        this.dataList = []
      } finally {
        this.dataLoading = false
      }
    },
    stageLabel(stage) {
      const map = { READ: '读取', DETAIL: '详情补全', TRANSFORM: '转换', WRITE: '写入', DONE: '完成' }
      return map[stage] || stage
    },
    statusType(s) {
      const map = { SUCCESS: 'success', FAILED: 'danger', RUNNING: 'warning', PARTIAL: 'warning' }
      return map[s] || 'info'
    }
  }
}
</script>
