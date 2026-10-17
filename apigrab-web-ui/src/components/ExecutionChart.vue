<template>
  <div class="execution-chart">
    <el-row :gutter="20">
      <!-- 执行趋势图 -->
      <el-col :span="14">
        <el-card shadow="hover">
          <div slot="header">
            <span>执行趋势（近 7 天）</span>
          </div>
          <div class="chart-wrapper" ref="trendChart" style="height: 280px;"></div>
        </el-card>
      </el-col>
      <!-- 成功率饼图 -->
      <el-col :span="10">
        <el-card shadow="hover">
          <div slot="header">
            <span>执行结果分布</span>
          </div>
          <div class="chart-wrapper" ref="pieChart" style="height: 280px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
// 简易 Canvas 图表，不依赖第三方图表库，纯 Vue 实现
export default {
  name: 'ExecutionChart',
  props: {
    trendData: {
      type: Array,
      default: () => []
      // [{ date: '05-20', success: 10, failed: 2, total: 12 }, ...]
    },
    pieData: {
      type: Object,
      default: () => ({ success: 0, failed: 0, running: 0, partial: 0 })
    }
  },
  data() {
    return {
      trendCanvas: null,
      pieCanvas: null
    }
  },
  watch: {
    trendData: { handler() { this.drawTrendChart() }, deep: true },
    pieData: { handler() { this.drawPieChart() }, deep: true }
  },
  mounted() {
    this.$nextTick(() => {
      this.drawTrendChart()
      this.drawPieChart()
    })
  },
  methods: {
    drawTrendChart() {
      const container = this.$refs.trendChart
      if (!container) return
      container.innerHTML = ''
      const canvas = document.createElement('canvas')
      canvas.width = container.clientWidth || 500
      canvas.height = 280
      container.appendChild(canvas)
      const ctx = canvas.getContext('2d')
      const w = canvas.width
      const h = canvas.height

      const data = this.trendData.length > 0 ? this.trendData : this.getDefaultTrendData()

      // 标题区域
      const paddingTop = 30
      const paddingBottom = 40
      const paddingLeft = 50
      const paddingRight = 20
      const chartW = w - paddingLeft - paddingRight
      const chartH = h - paddingTop - paddingBottom

      // Y轴刻度
      const maxVal = Math.max(...data.map(d => d.total), 1)
      const ySteps = 5
      ctx.strokeStyle = '#e0e0e0'
      ctx.fillStyle = '#909399'
      ctx.font = '11px sans-serif'
      ctx.textAlign = 'right'
      for (let i = 0; i <= ySteps; i++) {
        const y = paddingTop + (chartH / ySteps) * i
        const val = Math.round(maxVal - (maxVal / ySteps) * i)
        ctx.beginPath()
        ctx.moveTo(paddingLeft, y)
        ctx.lineTo(w - paddingRight, y)
        ctx.stroke()
        ctx.fillText(val, paddingLeft - 8, y + 4)
      }

      // 柱状图
      const barGroupWidth = chartW / data.length
      const barWidth = Math.min(barGroupWidth * 0.3, 20)
      const gap = 4

      data.forEach((d, i) => {
        const x = paddingLeft + barGroupWidth * i + barGroupWidth / 2

        // 成功 - 蓝色
        const successH = (d.success / maxVal) * chartH
        ctx.fillStyle = '#409EFF'
        ctx.fillRect(x - barWidth - gap / 2, paddingTop + chartH - successH, barWidth, successH)

        // 失败 - 红色
        const failedH = (d.failed / maxVal) * chartH
        ctx.fillStyle = '#F56C6C'
        ctx.fillRect(x + gap / 2, paddingTop + chartH - failedH, barWidth, failedH)

        // X轴标签
        ctx.fillStyle = '#909399'
        ctx.textAlign = 'center'
        ctx.fillText(d.date, x, h - paddingBottom + 16)
      })

      // 图例
      ctx.font = '12px sans-serif'
      ctx.textAlign = 'left'
      ctx.fillStyle = '#409EFF'
      ctx.fillRect(w - 160, 8, 12, 12)
      ctx.fillStyle = '#303133'
      ctx.fillText('成功', w - 144, 18)
      ctx.fillStyle = '#F56C6C'
      ctx.fillRect(w - 90, 8, 12, 12)
      ctx.fillStyle = '#303133'
      ctx.fillText('失败', w - 74, 18)
    },

    drawPieChart() {
      const container = this.$refs.pieChart
      if (!container) return
      container.innerHTML = ''
      const canvas = document.createElement('canvas')
      canvas.width = container.clientWidth || 400
      canvas.height = 280
      container.appendChild(canvas)
      const ctx = canvas.getContext('2d')
      const w = canvas.width
      const h = canvas.height

      const data = this.pieData
      const total = (data.success || 0) + (data.failed || 0) + (data.running || 0) + (data.partial || 0)

      if (total === 0) {
        ctx.fillStyle = '#C0C4CC'
        ctx.font = '14px sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText('暂无执行数据', w / 2, h / 2)
        return
      }

      const cx = w / 2 - 60
      const cy = h / 2
      const radius = Math.min(cx, cy) - 20

      const slices = [
        { label: '成功', value: data.success || 0, color: '#67C23A' },
        { label: '失败', value: data.failed || 0, color: '#F56C6C' },
        { label: '运行中', value: data.running || 0, color: '#E6A23C' },
        { label: '部分成功', value: data.partial || 0, color: '#909399' }
      ].filter(s => s.value > 0)

      let startAngle = -Math.PI / 2
      slices.forEach(s => {
        const sliceAngle = (s.value / total) * 2 * Math.PI
        ctx.beginPath()
        ctx.moveTo(cx, cy)
        ctx.arc(cx, cy, radius, startAngle, startAngle + sliceAngle)
        ctx.closePath()
        ctx.fillStyle = s.color
        ctx.fill()
        startAngle += sliceAngle
      })

      // 中心白圆（环形效果）
      ctx.beginPath()
      ctx.arc(cx, cy, radius * 0.5, 0, 2 * Math.PI)
      ctx.fillStyle = '#fff'
      ctx.fill()

      // 中心文字
      ctx.fillStyle = '#303133'
      ctx.font = 'bold 18px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(total, cx, cy + 2)
      ctx.font = '11px sans-serif'
      ctx.fillStyle = '#909399'
      ctx.fillText('总执行', cx, cy + 18)

      // 右侧图例
      const legendX = w / 2 + 50
      let legendY = h / 2 - 30
      ctx.font = '12px sans-serif'
      ctx.textAlign = 'left'
      slices.forEach(s => {
        ctx.fillStyle = s.color
        ctx.fillRect(legendX, legendY, 12, 12)
        ctx.fillStyle = '#303133'
        const pct = Math.round(s.value / total * 100)
        ctx.fillText(`${s.label} (${s.value}, ${pct}%)`, legendX + 18, legendY + 10)
        legendY += 24
      })
    },

    getDefaultTrendData() {
      const data = []
      for (let i = 6; i >= 0; i--) {
        const d = new Date()
        d.setDate(d.getDate() - i)
        const dateStr = (d.getMonth() + 1).toString().padStart(2, '0') + '-' + d.getDate().toString().padStart(2, '0')
        data.push({ date: dateStr, success: 0, failed: 0, total: 0 })
      }
      return data
    }
  }
}
</script>

<style scoped>
.execution-chart {
  margin-top: 20px;
}
.chart-wrapper {
  width: 100%;
  position: relative;
}
</style>
