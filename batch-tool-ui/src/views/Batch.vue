<template>
  <div class="batch-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>命令执行</span>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #title>
          <strong>使用说明：</strong>输入完整的命令行参数执行批量操作
        </template>
        <div style="margin-top: 10px">
          <p><strong>导出示例：</strong>-h 127.0.0.1 -P 3306 -u root -p password -D test -o export -t my_table -d /data/export</p>
          <p><strong>导入示例：</strong>-h 127.0.0.1 -P 3306 -u root -p password -D test -o import -t my_table -f /data/import/data.csv</p>
        </div>
      </el-alert>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="命令参数" prop="commandArgs">
          <el-input
            v-model="form.commandArgs"
            type="textarea"
            :rows="4"
            placeholder="输入完整的命令行参数，如: -h 127.0.0.1 -P 3306 -u root -p password -D test -o export -t my_table -d /data/export"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleValidate" :loading="validating">
            <el-icon><Check /></el-icon>
            验证参数
          </el-button>
          <el-button type="success" @click="handleSubmit" :loading="submitting" :disabled="!validated">
            <el-icon><VideoPlay /></el-icon>
            执行任务
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">常用参数说明</el-divider>

      <el-table :data="paramDocs" border size="small">
        <el-table-column prop="param" label="参数" width="120" />
        <el-table-column prop="alias" label="别名" width="120" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="required" label="必填" width="80">
          <template #default="{ row }">
            <el-tag :type="row.required ? 'danger' : 'info'" size="small">
              {{ row.required ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 结果对话框 -->
    <el-dialog v-model="resultVisible" title="执行结果" width="500px">
      <el-result :icon="resultIcon" :title="resultTitle" :sub-title="resultMessage">
        <template #extra>
          <el-button type="primary" @click="viewJob" v-if="jobId">查看任务</el-button>
          <el-button @click="resultVisible = false">关闭</el-button>
        </template>
      </el-result>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { executeBatch, validateBatch } from '../api/job'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)
const validating = ref(false)
const validated = ref(false)
const resultVisible = ref(false)
const resultIcon = ref('success')
const resultTitle = ref('')
const resultMessage = ref('')
const jobId = ref('')

const form = reactive({
  commandArgs: ''
})

const rules = {
  commandArgs: [
    { required: true, message: '请输入命令行参数', trigger: 'blur' }
  ]
}

const paramDocs = [
  { param: '-h', alias: '--host', desc: '数据库主机地址', required: true },
  { param: '-P', alias: '--port', desc: '数据库端口，默认3306', required: false },
  { param: '-u', alias: '--user', desc: '数据库用户名', required: true },
  { param: '-p', alias: '--password', desc: '数据库密码', required: true },
  { param: '-D', alias: '--database', desc: '数据库名', required: true },
  { param: '-o', alias: '--operation', desc: '操作类型: export/import', required: true },
  { param: '-t', alias: '--table', desc: '表名', required: true },
  { param: '-d', alias: '--dir', desc: '导出目录(导出时)', required: false },
  { param: '-f', alias: '--file', desc: '导入文件(导入时)', required: false },
  { param: '-c', alias: '--charset', desc: '字符集，默认utf8', required: false },
  { param: '-s', alias: '--separator', desc: '字段分隔符，默认逗号', required: false },
  { param: '--where', alias: '', desc: 'WHERE条件', required: false },
  { param: '--batch-size', alias: '', desc: '批处理大小', required: false }
]

const handleValidate = async () => {
  if (!form.commandArgs.trim()) {
    ElMessage.warning('请输入命令行参数')
    return
  }

  validating.value = true
  try {
    await validateBatch({ commandArgs: form.commandArgs })
    validated.value = true
    ElMessage.success('参数验证通过')
  } catch (error) {
    validated.value = false
  } finally {
    validating.value = false
  }
}

const handleSubmit = async () => {
  if (!validated.value) {
    await handleValidate()
    if (!validated.value) return
  }

  submitting.value = true
  try {
    const res = await executeBatch({ commandArgs: form.commandArgs })
    resultIcon.value = 'success'
    resultTitle.value = '任务已创建'
    resultMessage.value = `任务ID: ${res.data}`
    jobId.value = res.data
    resultVisible.value = true
    validated.value = false
  } catch (error) {
    resultIcon.value = 'error'
    resultTitle.value = '创建失败'
    resultMessage.value = error.message || '未知错误'
    resultVisible.value = true
  } finally {
    submitting.value = false
  }
}

const handleReset = () => {
  form.commandArgs = ''
  validated.value = false
  formRef.value?.resetFields()
}

const viewJob = () => {
  resultVisible.value = false
  router.push('/')
}
</script>

<style scoped>
.batch-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
