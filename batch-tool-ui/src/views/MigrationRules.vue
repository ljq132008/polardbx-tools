<template>
  <div class="migration-rules-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>迁移规则管理</span>
          <el-button type="primary" size="small" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增规则
          </el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="规则组">
          <el-select v-model="queryParams.groupId" placeholder="全部" clearable style="width: 200px" @change="fetchRules">
            <el-option
              v-for="group in groups"
              :key="group.id"
              :label="group.groupName"
              :value="group.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 100px" @change="fetchRules">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="rules" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="ruleName" label="规则名称" width="150" />
        <el-table-column label="规则组" width="150">
          <template #default="{ row }">
            {{ getGroupName(row.groupId) }}
          </template>
        </el-table-column>
        <el-table-column prop="operationType" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.operationType === 'EXPORT' ? 'success' : 'primary'" size="small">
              {{ row.operationType === 'EXPORT' ? '导出' : '导入' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="源表" width="200">
          <template #default="{ row }">
            {{ row.sourceDatabase }}.{{ row.sourceTable }}
          </template>
        </el-table-column>
        <el-table-column label="目标表" width="200">
          <template #default="{ row }">
            <span v-if="row.targetDatabase">
              {{ row.targetDatabase }}.{{ row.targetTable || row.sourceTable }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button type="success" size="small" @click="handleExecute(row)" :disabled="row.status !== 1">
                执行
              </el-button>
              <el-button size="small" @click="handlePreview(row)">
                预览
              </el-button>
              <el-button type="primary" size="small" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button :type="row.status === 1 ? 'warning' : 'info'" size="small" @click="handleToggle(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchRules"
        @current-change="fetchRules"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="700px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="规则组" prop="groupId">
          <el-select v-model="form.groupId" placeholder="请选择规则组" style="width: 100%">
            <el-option
              v-for="group in groups"
              :key="group.id"
              :label="group.groupName"
              :value="group.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="操作类型" prop="operationType">
          <el-radio-group v-model="form.operationType">
            <el-radio label="EXPORT">导出</el-radio>
            <el-radio label="IMPORT">导入</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-divider content-position="left">源表配置</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="源数据库" prop="sourceDatabase">
              <el-input v-model="form.sourceDatabase" placeholder="留空则使用规则组配置" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="源表名" prop="sourceTable">
              <el-input v-model="form.sourceTable" placeholder="表名或正则匹配" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">目标配置</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="目标数据库">
              <el-input v-model="form.targetDatabase" placeholder="留空则与源数据库相同" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标表名">
              <el-input v-model="form.targetTable" placeholder="留空则与源表名相同" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">扩展参数</el-divider>

        <el-form-item label="Where条件">
          <el-input v-model="form.whereCondition" placeholder="WHERE条件（不需要WHERE关键字）" />
        </el-form-item>
        <el-form-item label="输出目录">
          <el-input v-model="form.outputDir" placeholder="导出目录路径" />
        </el-form-item>
        <el-form-item label="导入文件">
          <el-input v-model="form.inputFile" placeholder="导入文件路径" />
        </el-form-item>
        <el-form-item label="扩展参数">
          <el-input v-model="form.extraParams" type="textarea" :rows="2" placeholder="额外的命令行参数，如: --separator , --batch-size 1000" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog v-model="previewVisible" title="命令行预览" width="600px">
      <el-input
        type="textarea"
        :model-value="previewCommand"
        :rows="5"
        readonly
      />
      <template #footer>
        <el-button type="primary" @click="copyCommand">复制</el-button>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { 
  getMigrationRules, 
  createMigrationRule, 
  updateMigrationRule, 
  deleteMigrationRule,
  executeMigrationRule,
  toggleMigrationRuleStatus,
  previewCommand as previewRuleCommand
} from '../api/migrationRule'
import { getRuleGroups } from '../api/ruleGroup'
import { ElMessage, ElMessageBox } from 'element-plus'

const rules = ref([])
const groups = ref([])
const loading = ref(false)
const total = ref(0)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const previewCommand = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const currentId = ref(null)

const queryParams = reactive({
  page: 1,
  size: 20,
  groupId: null,
  status: null
})

const form = reactive({
  groupId: null,
  ruleName: '',
  operationType: 'EXPORT',
  sourceDatabase: '',
  sourceTable: '',
  targetDatabase: '',
  targetTable: '',
  whereCondition: '',
  outputDir: '',
  inputFile: '',
  extraParams: ''
})

const formRules = {
  groupId: [{ required: true, message: '请选择规则组', trigger: 'change' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  operationType: [{ required: true, message: '请选择操作类型', trigger: 'change' }],
  sourceTable: [{ required: true, message: '请输入源表名', trigger: 'blur' }]
}

const fetchGroups = async () => {
  try {
    const res = await getRuleGroups()
    groups.value = res.data || []
  } catch (error) {
    console.error('Failed to fetch groups:', error)
  }
}

const fetchRules = async () => {
  loading.value = true
  try {
    const res = await getMigrationRules(queryParams)
    rules.value = res.data?.records || res.data || []
    total.value = res.data?.total || rules.value.length
  } catch (error) {
    console.error('Failed to fetch rules:', error)
  } finally {
    loading.value = false
  }
}

const getGroupName = (groupId) => {
  const group = groups.value.find(g => g.id === groupId)
  return group ? group.groupName : '-'
}

const resetForm = () => {
  Object.assign(form, {
    groupId: null,
    ruleName: '',
    operationType: 'EXPORT',
    sourceDatabase: '',
    sourceTable: '',
    targetDatabase: '',
    targetTable: '',
    whereCondition: '',
    outputDir: '',
    inputFile: '',
    extraParams: ''
  })
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  currentId.value = null
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, {
    groupId: row.groupId,
    ruleName: row.ruleName,
    operationType: row.operationType,
    sourceDatabase: row.sourceDatabase || '',
    sourceTable: row.sourceTable,
    targetDatabase: row.targetDatabase || '',
    targetTable: row.targetTable || '',
    whereCondition: row.whereCondition || '',
    outputDir: row.outputDir || '',
    inputFile: row.inputFile || '',
    extraParams: row.extraParams || ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMigrationRule(currentId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createMigrationRule(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchRules()
  } catch (error) {
    console.error('Failed to submit:', error)
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该规则吗？', '提示', { type: 'warning' })
    await deleteMigrationRule(row.id)
    ElMessage.success('删除成功')
    fetchRules()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete:', error)
    }
  }
}

const handleToggle = async (row) => {
  try {
    await toggleMigrationRuleStatus(row.id)
    ElMessage.success('状态已更新')
    fetchRules()
  } catch (error) {
    console.error('Failed to toggle status:', error)
  }
}

const handleExecute = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要执行规则「${row.ruleName}」吗？`,
      '执行确认',
      { type: 'info' }
    )
    const res = await executeMigrationRule(row.id)
    ElMessage.success(`任务已创建: ${res.data}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to execute:', error)
    }
  }
}

const handlePreview = async (row) => {
  try {
    const res = await previewRuleCommand(row.id)
    previewCommand.value = res.data
    previewVisible.value = true
  } catch (error) {
    console.error('Failed to preview:', error)
  }
}

const copyCommand = () => {
  navigator.clipboard.writeText(previewCommand.value)
  ElMessage.success('已复制到剪贴板')
}

onMounted(() => {
  fetchGroups()
  fetchRules()
})
</script>

<style scoped>
.migration-rules-container {
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
