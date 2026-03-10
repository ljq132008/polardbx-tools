<template>
  <div class="jobs-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>任务列表</span>
          <div>
            <el-button type="primary" size="small" @click="fetchJobs">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
            <el-button type="danger" size="small" @click="handleClean">
              清理历史
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="等待中" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="queryParams.operationType" placeholder="全部" clearable style="width: 120px">
            <el-option label="导出" value="EXPORT" />
            <el-option label="导入" value="IMPORT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchJobs">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="jobs" v-loading="loading" stripe>
        <el-table-column prop="jobId" label="任务ID" width="200" />
        <el-table-column prop="jobName" label="任务名称" width="150" />
        <el-table-column prop="operationType" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.operationType === 'GROUP'" type="warning">
              规则组
            </el-tag>
            <el-tag v-else :type="row.operationType === 'EXPORT' ? 'success' : 'primary'">
              {{ row.operationType === 'EXPORT' ? '导出' : '导入' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceDatabase" label="源数据库" width="120" />
        <el-table-column prop="sourceTable" label="源表" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="180">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.progress || 0" 
              :status="getProgressStatus(row.status)"
              :stroke-width="15"
              text-inside
            />
          </template>
        </el-table-column>
        <el-table-column label="处理行数" width="140">
          <template #default="{ row }">
            <template v-if="row.operationType === 'GROUP'">
              <span>{{ row.completedRules || 0 }} / {{ row.totalRules || 0 }} 规则</span>
              <span v-if="row.failedRules > 0" style="color: #f56c6c; margin-left: 4px;">({{ row.failedRules }} 失败)</span>
            </template>
            <template v-else>
              {{ formatNumber(row.processedRows) }} / {{ formatNumber(row.totalRows) || '-' }}
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button
                v-if="row.status === 'RUNNING'"
                type="danger"
                size="small"
                @click="handleCancel(row.jobId)"
              >
                取消
              </el-button>
              <el-button
                v-if="row.status === 'FAILED' || row.status === 'CANCELLED'"
                type="warning"
                size="small"
                @click="handleRetry(row.jobId)"
              >
                重试
              </el-button>
              <el-button
                type="primary"
                size="small"
                @click="handleView(row)"
              >
                详情
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchJobs"
        @current-change="fetchJobs"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="70%">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="info">
          <el-descriptions v-if="currentJob" :column="2" border>
            <el-descriptions-item label="任务ID">{{ currentJob.jobId }}</el-descriptions-item>
            <el-descriptions-item label="任务名称">{{ currentJob.jobName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="操作类型">
              <el-tag v-if="currentJob.operationType === 'GROUP'" type="warning">规则组</el-tag>
              <el-tag v-else :type="currentJob.operationType === 'EXPORT' ? 'success' : 'primary'">
                {{ currentJob.operationType === 'EXPORT' ? '导出' : '导入' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(currentJob.status)">
                {{ getStatusText(currentJob.status) }}
              </el-tag>
            </el-descriptions-item>
            <!-- 规则组任务特有字段 -->
            <template v-if="currentJob.operationType === 'GROUP'">
              <el-descriptions-item label="并发度">{{ currentJob.concurrency || 1 }}</el-descriptions-item>
              <el-descriptions-item label="规则进度">
                {{ currentJob.completedRules || 0 }} / {{ currentJob.totalRules || 0 }}
                <span v-if="currentJob.failedRules > 0" style="color: #f56c6c;"> ({{ currentJob.failedRules }} 失败)</span>
              </el-descriptions-item>
            </template>
            <!-- 普通任务字段 -->
            <template v-else>
              <el-descriptions-item label="源数据库">{{ currentJob.sourceDatabase || '-' }}</el-descriptions-item>
              <el-descriptions-item label="源表">{{ currentJob.sourceTable || '-' }}</el-descriptions-item>
              <el-descriptions-item label="目标数据库">{{ currentJob.targetDatabase || '-' }}</el-descriptions-item>
              <el-descriptions-item label="目标表">{{ currentJob.targetTable || '-' }}</el-descriptions-item>
            </template>
            <el-descriptions-item label="进度">{{ currentJob.progress || 0 }}%</el-descriptions-item>
            <el-descriptions-item label="处理行数">
              <template v-if="currentJob.operationType === 'GROUP'">
                -
              </template>
              <template v-else>
                {{ formatNumber(currentJob.processedRows) }} / {{ formatNumber(currentJob.totalRows) || '-' }}
              </template>
            </el-descriptions-item>
            <el-descriptions-item label="速度">
              {{ currentJob.speed ? currentJob.speed + ' rows/s' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="耗时">{{ formatDuration(currentJob.duration) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentJob.createTime }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ currentJob.startTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ currentJob.endTime || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="currentJob.errorMsg" style="margin-top: 20px">
            <el-alert type="error" :closable="false" title="错误信息">
              <pre>{{ currentJob.errorMsg }}</pre>
            </el-alert>
          </div>

          <div v-if="currentJob.commandArgs" style="margin-top: 20px">
            <el-divider content-position="left">命令行参数</el-divider>
            <el-input
              type="textarea"
              :model-value="currentJob.commandArgs"
              :rows="3"
              readonly
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="执行日志" name="logs">
          <el-table :data="jobLogs" v-loading="logsLoading" max-height="400">
            <el-table-column prop="createTime" label="时间" width="180" />
            <el-table-column prop="level" label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="getLogLevelType(row.level)" size="small">
                  {{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="消息" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="currentJob && currentJob.operationType === 'GROUP'" label="规则执行详情" name="rules">
          <el-table :data="jobRules" v-loading="rulesLoading" max-height="400">
            <el-table-column prop="ruleName" label="规则名称" width="150" />
            <el-table-column label="源表" width="200">
              <template #default="{ row }">
                {{ row.sourceDatabase }}.{{ row.sourceTable }}
              </template>
            </el-table-column>
            <el-table-column label="目标表" width="200">
              <template #default="{ row }">
                <span v-if="row.targetDatabase">{{ row.targetDatabase }}.{{ row.targetTable }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="operationType" label="操作类型" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="进度" width="120">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.progress || 0"
                  :status="getProgressStatus(row.status)"
                  :stroke-width="10"
                />
              </template>
            </el-table-column>
            <el-table-column label="处理行数" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.processedRows) }} / {{ formatNumber(row.totalRows) || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="duration" label="耗时" width="100">
              <template #default="{ row }">
                {{ formatDuration(row.duration) }}
              </template>
            </el-table-column>
            <el-table-column prop="errorMsg" label="错误信息" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.errorMsg" style="color: #f56c6c;">{{ row.errorMsg }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { getJobs, cancelJob, retryJob, getJobLogs, cleanJobs, getJobRules } from '../api/job'
import { ElMessage, ElMessageBox } from 'element-plus'

const jobs = ref([])
const loading = ref(false)
const total = ref(0)
const detailVisible = ref(false)
const currentJob = ref(null)
const activeTab = ref('info')
const jobLogs = ref([])
const jobRules = ref([])
const logsLoading = ref(false)
const rulesLoading = ref(false)

const queryParams = reactive({
  page: 1,
  size: 20,
  status: '',
  operationType: ''
})

let timer = null

const fetchJobs = async () => {
  loading.value = true
  try {
    const res = await getJobs(queryParams)
    jobs.value = res.data.records || res.data
    total.value = res.data.total || jobs.value.length
  } catch (error) {
    console.error('Failed to fetch jobs:', error)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.status = ''
  queryParams.operationType = ''
  queryParams.page = 1
  fetchJobs()
}

const handleCancel = async (jobId) => {
  try {
    await ElMessageBox.confirm('确定要取消该任务吗？', '提示', {
      type: 'warning'
    })
    await cancelJob(jobId)
    ElMessage.success('任务已取消')
    fetchJobs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to cancel job:', error)
    }
  }
}

const handleRetry = async (jobId) => {
  try {
    const res = await retryJob(jobId)
    ElMessage.success('任务已重新创建: ' + res.data)
    fetchJobs()
  } catch (error) {
    console.error('Failed to retry job:', error)
  }
}

const handleView = async (job) => {
  currentJob.value = job
  detailVisible.value = true
  activeTab.value = 'info'

  // 加载日志
  logsLoading.value = true
  try {
    const res = await getJobLogs(job.jobId)
    jobLogs.value = res.data || []
  } catch (error) {
    jobLogs.value = []
  } finally {
    logsLoading.value = false
  }

  // 如果是规则组任务，加载规则快照
  if (job.operationType === 'GROUP') {
    rulesLoading.value = true
    try {
      const res = await getJobRules(job.jobId)
      jobRules.value = res.data || []
    } catch (error) {
      jobRules.value = []
    } finally {
      rulesLoading.value = false
    }
  }
}

const handleClean = async () => {
  try {
    await ElMessageBox.confirm('确定要清理7天前的历史任务吗？', '提示', {
      type: 'warning'
    })
    const res = await cleanJobs(7)
    ElMessage.success(`已清理 ${res.data} 条任务`)
    fetchJobs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to clean jobs:', error)
    }
  }
}

const getStatusType = (status) => {
  const map = {
    PENDING: 'info',
    RUNNING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    PENDING: '等待中',
    RUNNING: '执行中',
    COMPLETED: '已完成',
    FAILED: '失败',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

const getProgressStatus = (status) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  return undefined
}

const getLogLevelType = (level) => {
  const map = {
    INFO: 'info',
    WARN: 'warning',
    ERROR: 'danger',
    DEBUG: ''
  }
  return map[level] || ''
}

const formatNumber = (num) => {
  if (!num) return '0'
  return num.toLocaleString()
}

const formatDuration = (seconds) => {
  if (!seconds) return '-'
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
  return `${Math.floor(seconds / 3600)}时${Math.floor((seconds % 3600) / 60)}分${seconds % 60}秒`
}

onMounted(() => {
  fetchJobs()
  timer = setInterval(fetchJobs, 5000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.jobs-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 15px;
}
</style>
