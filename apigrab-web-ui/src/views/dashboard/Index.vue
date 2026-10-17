<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #409EFF;">
              <i class="el-icon-connection" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.apiConfigCount || 0 }}</div>
              <div class="stat-label">接口配置</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #67C23A;">
              <i class="el-icon-bank-card" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.datasourceCount || 0 }}</div>
              <div class="stat-label">数据源</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #E6A23C;">
              <i class="el-icon-s-operation" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.taskCount || 0 }}</div>
              <div class="stat-label">采集任务</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: stats.successRate >= 90 ? '#67C23A' : '#F56C6C' }">
              <i class="el-icon-document" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.successRate || 0 }}%</div>
              <div class="stat-label">执行成功率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 执行图表 -->
    <execution-chart :trend-data="trendData" :pie-data="pieData" />

    <!-- 近期执行记录 -->
    <el-card style="margin-top: 20px;">
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">近期执行记录</span>
        <div class="rc-card-header__actions">
          <el-button size="mini" @click="$router.push('/monitor/history')">查看全部</el-button>
        </div>
      </div>
      <el-table :data="recentExecutions" size="small" border style="width: 100%;" v-loading="loading">
        <el-table-column label="任务名称" min-width="160">
          <template slot-scope="{ row }">{{ row.taskName || `#${row.taskId}` }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="statusType(row.status)" size="mini">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalRecords" label="总记录" width="80" align="center" />
        <el-table-column prop="successCount" label="成功" width="80" align="center" />
        <el-table-column prop="failCount" label="失败" width="80" align="center" />
        <el-table-column prop="pageCount" label="页数" width="80" align="center" />
        <el-table-column prop="costMs" label="耗时(ms)" width="100" align="center" />
        <el-table-column label="执行时间" width="160">
          <template slot-scope="{ row }">{{ row.createTime || row.startTime }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getDashboardStatistics } from '@/api/dashboard'
import { getExecutionList } from '@/api/execution'
import ExecutionChart from '@/components/ExecutionChart.vue'

export default {
  name: 'Dashboard',
  components: { ExecutionChart },
  data() {
    return {
      loading: false,
      stats: {},
      recentExecutions: [],
      trendData: [],
      pieData: { success: 0, failed: 0, running: 0, partial: 0 }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getDashboardStatistics()
        this.stats = res.data || {}
        // 后端分页参数名为 pageNum/pageSize；本列表仅展示最近 10 条
        const execRes = await getExecutionList({ pageNum: 1, pageSize: 10 })
        const execList = execRes.data?.list || execRes.data || []
        this.recentExecutions = execList

        // 构建图表数据
        this.buildChartData(execList, this.stats)
      } catch (e) {
        // 后端可能还没启动，显示空数据
        this.recentExecutions = []
      } finally {
        this.loading = false
      }
    },
    buildChartData(execList, stats) {
      // 饼图数据（与后端统计口径一致：成功/失败/运行中/部分成功）
      this.pieData = {
        success: stats.successCount || 0,
        failed: stats.failedCount || 0,
        running: stats.runningCount || 0,
        partial: stats.partialCount || 0
      }

      // 趋势数据 - 近7天
      const trendMap = {}
      for (let i = 6; i >= 0; i--) {
        const d = new Date()
        d.setDate(d.getDate() - i)
        const key = (d.getMonth() + 1).toString().padStart(2, '0') + '-' + d.getDate().toString().padStart(2, '0')
        trendMap[key] = { date: key, success: 0, failed: 0, total: 0 }
      }

      // 按日期聚合执行结果
      ;(execList || []).forEach(item => {
        const createTime = item.startTime || item.createTime
        if (!createTime) return
        const d = new Date(createTime)
        const key = (d.getMonth() + 1).toString().padStart(2, '0') + '-' + d.getDate().toString().padStart(2, '0')
        if (trendMap[key]) {
          trendMap[key].total++
          if (item.status === 'SUCCESS') trendMap[key].success++
          else if (item.status === 'FAILED') trendMap[key].failed++
        }
      })

      this.trendData = Object.values(trendMap)
    },
    statusType(status) {
      const map = { SUCCESS: 'success', FAILED: 'danger', RUNNING: 'warning', PARTIAL: 'warning' }
      return map[status] || 'info'
    }
  }
}
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
}
.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}
.stat-icon i {
  font-size: 28px;
  color: #fff;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}
</style>
