<template>
  <div class="execution-history">
    <el-card class="history-card">
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">执行历史</span>
        <span class="history-subtitle">按任务查看每次执行的结果与阶段统计</span>
      </div>

      <div class="history-body">
        <!-- 左侧：任务列表 -->
        <aside class="task-pane" aria-label="任务列表">
          <div class="task-pane__head">
            <div class="task-pane__title-row">
              <span class="task-pane__title">
                <i class="el-icon-s-grid" aria-hidden="true"></i>
                任务列表
              </span>
              <span class="task-pane__count">共 {{ taskTotal }} 个</span>
            </div>
            <el-input
              v-model="taskKeyword"
              size="small"
              clearable
              prefix-icon="el-icon-search"
              placeholder="搜索任务名称"
              aria-label="搜索任务名称"
              @keyup.enter.native="searchTasks"
              @change="searchTasks"
              @clear="searchTasks"
            />
          </div>

          <div class="task-pane__list" v-loading="taskLoading">
            <ul v-if="taskList.length > 0" role="list" class="task-list">
              <li
                v-for="item in taskList"
                :key="item.id"
                class="task-item"
                :class="{ 'is-active': item.id === selectedTaskId }"
                role="button"
                tabindex="0"
                :aria-pressed="item.id === selectedTaskId"
                @click="selectTask(item)"
                @keydown.enter.prevent="selectTask(item)"
                @keydown.space.prevent="selectTask(item)"
              >
                <span class="task-item__id">#{{ item.id }}</span>
                <span class="task-item__name" :title="item.name">{{ item.name }}</span>
                <i class="el-icon-arrow-right task-item__chevron" aria-hidden="true"></i>
              </li>
            </ul>
            <el-empty v-else description="暂无任务" :image-size="60" />
          </div>

          <div class="task-pane__foot">
            <el-pagination
              small
              background
              layout="sizes, prev, pager, next"
              :page-sizes="[10, 15, 20]"
              :current-page="taskPage"
              :page-size="taskPageSize"
              :total="taskTotal"
              @current-change="onTaskPageChange"
              @size-change="onTaskSizeChange"
            />
          </div>
        </aside>

        <!-- 右侧：执行历史 -->
        <section class="exec-pane">
          <template v-if="selectedTaskId">
            <div class="exec-bar">
              <div class="exec-bar__context">
                <i class="el-icon-s-flag exec-bar__icon" aria-hidden="true"></i>
                <span class="exec-bar__name">{{ selectedTaskName }}</span>
                <span class="exec-bar__id">#{{ selectedTaskId }}</span>
              </div>
              <el-button
                size="mini"
                round
                plain
                icon="el-icon-refresh"
                @click="refreshExecutions"
              >刷新</el-button>
            </div>

            <el-table
              :data="execList"
              border
              size="small"
              v-loading="execLoading"
              style="width: 100%;"
              empty-text="暂无执行记录"
            >
              <el-table-column prop="id" label="ID" width="60" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template slot-scope="{ row }">
                  <el-tag :type="statusType(row.status)" size="mini" effect="light">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalRecords" label="总记录" width="80" align="center" />
              <el-table-column prop="successCount" label="成功" width="80" align="center" />
              <el-table-column prop="failCount" label="失败" width="80" align="center" />
              <el-table-column prop="costMs" label="耗时(ms)" width="100" align="center" />
              <el-table-column prop="pageCount" label="采集页数" width="80" align="center" />
              <el-table-column prop="startTime" label="开始时间" min-width="160" />
              <el-table-column prop="endTime" label="结束时间" min-width="160" />
              <el-table-column label="操作" width="90" fixed="right">
                <template slot-scope="{ row }">
                  <el-button type="text" size="mini" @click="$router.push(`/monitor/log/${row.id}`)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="exec-pane__pager">
              <el-pagination
                background
                layout="total, sizes, prev, pager, next, jumper"
                :page-sizes="[5, 10, 20, 40]"
                :current-page="execPage"
                :page-size="execPageSize"
                :total="execTotal"
                @current-change="onExecPageChange"
                @size-change="onExecSizeChange"
              />
            </div>
          </template>

          <div v-else class="exec-pane__empty">
            <el-empty description="请从左侧选择一个任务，查看执行历史" :image-size="90" />
          </div>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getTaskList } from '@/api/task'
import { getExecutionList } from '@/api/execution'

export default {
  name: 'ExecutionHistory',
  data() {
    return {
      // 左侧任务列表（独立分页状态）
      taskList: [],
      taskTotal: 0,
      taskPage: 1,
      taskPageSize: 10,
      taskKeyword: '',
      taskLoading: false,
      selectedTaskId: null,
      selectedTaskName: '',
      // 右侧执行历史（独立分页状态）
      execList: [],
      execTotal: 0,
      execPage: 1,
      execPageSize: 10,
      execLoading: false,
      // 请求序号：用于丢弃过期响应，避免快速切换任务时右侧展示错乱
      execRequestSeq: 0
    }
  },
  created() {
    this.loadTasks()
  },
  methods: {
    async loadTasks() {
      this.taskLoading = true
      try {
        // 后端分页参数名为 pageNum/pageSize
        const params = { pageNum: this.taskPage, pageSize: this.taskPageSize }
        if (this.taskKeyword) params.name = this.taskKeyword
        const res = await getTaskList(params)
        this.taskList = res.data?.list || res.data || []
        this.taskTotal = res.data?.total || this.taskList.length
      } catch {
        this.taskList = []
        this.taskTotal = 0
      } finally {
        this.taskLoading = false
      }
      this.syncSelection()
    },
    /**
     * 任务列表刷新后的选中态维护：原选中仍在新列表则保留；否则选中首项（最新创建，
     * 后端按 id 倒序）；无任务则清空选中与右侧。
     */
    syncSelection() {
      const current = this.taskList.find(t => t.id === this.selectedTaskId)
      if (current) {
        this.selectedTaskName = current.name
        return
      }
      if (this.taskList.length > 0) {
        this.selectTask(this.taskList[0])
      } else {
        this.selectedTaskId = null
        this.selectedTaskName = ''
        this.execList = []
        this.execTotal = 0
        this.execPage = 1
      }
    },
    selectTask(task) {
      if (!task) return
      this.selectedTaskId = task.id
      this.selectedTaskName = task.name
      this.execPage = 1
      this.loadExecutions()
    },
    async loadExecutions() {
      if (!this.selectedTaskId) return
      const taskId = this.selectedTaskId
      const seq = ++this.execRequestSeq
      this.execLoading = true
      try {
        const res = await getExecutionList({
          taskId,
          pageNum: this.execPage,
          pageSize: this.execPageSize
        })
        // 丢弃过期响应：选中任务或请求序号已变化
        if (seq !== this.execRequestSeq || taskId !== this.selectedTaskId) return
        this.execList = res.data?.list || res.data || []
        this.execTotal = res.data?.total || this.execList.length
      } catch {
        if (seq !== this.execRequestSeq) return
        this.execList = []
        this.execTotal = 0
      } finally {
        if (seq === this.execRequestSeq) this.execLoading = false
      }
    },
    searchTasks() {
      this.taskPage = 1
      this.loadTasks()
    },
    onTaskPageChange(p) {
      this.taskPage = p
      this.loadTasks()
    },
    onTaskSizeChange(size) {
      this.taskPageSize = size
      this.taskPage = 1
      this.loadTasks()
    },
    refreshExecutions() {
      this.loadExecutions()
    },
    onExecPageChange(p) {
      this.execPage = p
      this.loadExecutions()
    },
    onExecSizeChange(size) {
      this.execPageSize = size
      this.execPage = 1
      this.loadExecutions()
    },
    statusType(s) {
      const map = { SUCCESS: 'success', FAILED: 'danger', RUNNING: 'warning', PARTIAL: 'warning' }
      return map[s] || 'info'
    }
  }
}
</script>

<style scoped>
.execution-history {
  color: var(--rc-text);
}

/* 头部：标题 + 一句说明，弱化于标题 */
.history-subtitle {
  font-size: var(--rc-text-sm);
  color: var(--rc-text-muted);
}

/* 卡片体不再留白：由左右两栏各自负责内边距，实现贴边的主从布局 */
.history-card >>> .el-card__body {
  padding: 0;
}

.history-body {
  display: flex;
  align-items: stretch;
  min-height: 560px;
}

/* ---------------- 左侧任务列表 ---------------- */
.task-pane {
  display: flex;
  flex-direction: column;
  width: 300px;
  min-width: 300px;
  background: var(--rc-surface-sunken);
  border-right: 1px solid var(--rc-border);
}

.task-pane__head {
  padding: var(--rc-space-4);
  border-bottom: 1px solid var(--rc-border);
  display: flex;
  flex-direction: column;
  gap: var(--rc-space-3);
}
.task-pane__title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.task-pane__title {
  font-size: var(--rc-text-base);
  font-weight: 600;
  color: var(--rc-text);
}
.task-pane__title i {
  color: var(--rc-primary);
  margin-right: var(--rc-space-1);
}
.task-pane__count {
  font-size: var(--rc-text-xs);
  color: var(--rc-text-muted);
}

.task-pane__list {
  flex: 1;
  overflow: auto;
  min-height: 120px;
}
.task-list {
  margin: 0;
  padding: var(--rc-space-2) 0;
}
.task-item {
  display: flex;
  align-items: center;
  gap: var(--rc-space-2);
  padding: var(--rc-space-3) var(--rc-space-4);
  cursor: pointer;
  color: var(--rc-text-secondary);
  font-size: var(--rc-text-sm);
  border-left: 3px solid transparent;
  transition: background-color var(--rc-transition), color var(--rc-transition),
    border-color var(--rc-transition);
}
.task-item:hover {
  background: var(--rc-primary-soft);
  color: var(--rc-text);
}
.task-item.is-active {
  background: var(--rc-primary-soft);
  color: var(--rc-primary);
  border-left-color: var(--rc-primary);
}
.task-item__id {
  flex-shrink: 0;
  font-family: var(--rc-font-mono);
  font-size: var(--rc-text-xs);
  color: var(--rc-text-muted);
}
.task-item.is-active .task-item__id {
  color: var(--rc-primary);
}
.task-item__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-item__chevron {
  flex-shrink: 0;
  font-size: var(--rc-text-xs);
  color: var(--rc-primary);
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity var(--rc-transition), transform var(--rc-transition);
}
.task-item:hover .task-item__chevron,
.task-item.is-active .task-item__chevron {
  opacity: 1;
  transform: translateX(0);
}

.task-pane__foot {
  padding: var(--rc-space-2) var(--rc-space-3);
  border-top: 1px solid var(--rc-border);
  display: flex;
  justify-content: center;
}

/* ---------------- 右侧执行历史 ---------------- */
.exec-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: var(--rc-space-5);
  overflow: auto;
}

/* 当前任务上下文条：把「我在看哪个任务」明确出来 */
.exec-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--rc-space-3);
  padding: var(--rc-space-3) var(--rc-space-4);
  margin-bottom: var(--rc-space-4);
  background: var(--rc-surface-sunken);
  border: 1px solid var(--rc-border);
  border-radius: var(--rc-radius-sm);
}
.exec-bar__context {
  display: flex;
  align-items: center;
  min-width: 0;
}
.exec-bar__icon {
  color: var(--rc-primary);
  margin-right: var(--rc-space-2);
}
.exec-bar__name {
  font-weight: 600;
  color: var(--rc-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.exec-bar__id {
  flex-shrink: 0;
  margin-left: var(--rc-space-2);
  font-family: var(--rc-font-mono);
  font-size: var(--rc-text-xs);
  color: var(--rc-text-muted);
}

.exec-pane__pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--rc-space-4);
}

.exec-pane__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
