<template>
  <div class="rule-groups-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>规则组管理</span>
          <el-button type="primary" size="small" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增规则组
          </el-button>
        </div>
      </template>

      <el-table :data="groups" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="groupName" label="规则组名称" width="150" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="源数据库" width="200">
          <template #default="{ row }">
            {{ row.sourceHost }}:{{ row.sourcePort }}/{{ row.sourceDatabase }}
          </template>
        </el-table-column>
        <el-table-column label="目标数据库" width="200">
          <template #default="{ row }">
            <span v-if="row.targetHost">
              {{ row.targetHost }}:{{ row.targetPort }}/{{ row.targetDatabase }}
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
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button type="success" size="small" @click="handleExecute(row)" :disabled="row.status !== 1">
                执行
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
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则组' : '新增规则组'" width="700px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="规则组名称" prop="groupName">
          <el-input v-model="form.groupName" placeholder="请输入规则组名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>

        <el-divider content-position="left">源数据库配置</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="主机地址" prop="sourceHost">
              <el-input v-model="form.sourceHost" placeholder="127.0.0.1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="端口" prop="sourcePort">
              <el-input-number v-model="form.sourcePort" :min="1" :max="65535" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="sourceUser">
              <el-input v-model="form.sourceUser" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="sourcePassword">
              <el-input v-model="form.sourcePassword" type="password" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="数据库" prop="sourceDatabase">
          <el-input v-model="form.sourceDatabase" placeholder="数据库名" />
        </el-form-item>

        <el-divider content-position="left">目标数据库配置（可选）</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="主机地址">
              <el-input v-model="form.targetHost" placeholder="留空则与源数据库相同" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="端口">
              <el-input-number v-model="form.targetPort" :min="1" :max="65535" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="form.targetUser" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码">
              <el-input v-model="form.targetPassword" type="password" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="数据库">
          <el-input v-model="form.targetDatabase" placeholder="目标数据库名" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { 
  getRuleGroups, 
  createRuleGroup, 
  updateRuleGroup, 
  deleteRuleGroup, 
  executeRuleGroup,
  toggleRuleGroupStatus 
} from '../api/ruleGroup'
import { ElMessage, ElMessageBox } from 'element-plus'

const groups = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const currentId = ref(null)

const form = reactive({
  groupName: '',
  description: '',
  sourceHost: '127.0.0.1',
  sourcePort: 3306,
  sourceUser: '',
  sourcePassword: '',
  sourceDatabase: '',
  targetHost: '',
  targetPort: 3306,
  targetUser: '',
  targetPassword: '',
  targetDatabase: ''
})

const rules = {
  groupName: [{ required: true, message: '请输入规则组名称', trigger: 'blur' }],
  sourceHost: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  sourcePort: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  sourceUser: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  sourcePassword: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  sourceDatabase: [{ required: true, message: '请输入数据库名', trigger: 'blur' }]
}

const fetchGroups = async () => {
  loading.value = true
  try {
    const res = await getRuleGroups()
    groups.value = res.data || []
  } catch (error) {
    console.error('Failed to fetch groups:', error)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    groupName: '',
    description: '',
    sourceHost: '127.0.0.1',
    sourcePort: 3306,
    sourceUser: '',
    sourcePassword: '',
    sourceDatabase: '',
    targetHost: '',
    targetPort: 3306,
    targetUser: '',
    targetPassword: '',
    targetDatabase: ''
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
    groupName: row.groupName,
    description: row.description,
    sourceHost: row.sourceHost,
    sourcePort: row.sourcePort,
    sourceUser: row.sourceUser,
    sourcePassword: '', // 密码不回显
    sourceDatabase: row.sourceDatabase,
    targetHost: row.targetHost || '',
    targetPort: row.targetPort || 3306,
    targetUser: row.targetUser || '',
    targetPassword: '',
    targetDatabase: row.targetDatabase || ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateRuleGroup(currentId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createRuleGroup(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchGroups()
  } catch (error) {
    console.error('Failed to submit:', error)
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该规则组吗？相关的迁移规则也会被删除', '提示', {
      type: 'warning'
    })
    await deleteRuleGroup(row.id)
    ElMessage.success('删除成功')
    fetchGroups()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete:', error)
    }
  }
}

const handleToggle = async (row) => {
  try {
    await toggleRuleGroupStatus(row.id)
    ElMessage.success('状态已更新')
    fetchGroups()
  } catch (error) {
    console.error('Failed to toggle status:', error)
  }
}

const handleExecute = async (row) => {
  try {
    const { value: concurrency } = await ElMessageBox.prompt(
      `请输入并发执行数（1-10），规则组「${row.groupName}」下的规则将并发执行`,
      '执行确认',
      {
        confirmButtonText: '执行',
        cancelButtonText: '取消',
        inputPattern: /^([1-9]|10)$/,
        inputErrorMessage: '请输入1-10之间的整数',
        inputValue: '1',
        inputPlaceholder: '并发数（1-10）'
      }
    )
    const res = await executeRuleGroup(row.id, parseInt(concurrency))
    ElMessage.success(`已创建任务，并发度: ${concurrency}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to execute:', error)
    }
  }
}

onMounted(() => {
  fetchGroups()
})
</script>

<style scoped>
.rule-groups-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
