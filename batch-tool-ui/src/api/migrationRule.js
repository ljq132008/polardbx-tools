import request from './index'

// 获取迁移规则列表
export function getMigrationRules(params) {
  return request({
    url: '/v1/migration-rules',
    method: 'get',
    params
  })
}

// 获取规则详情
export function getMigrationRule(id) {
  return request({
    url: `/v1/migration-rules/${id}`,
    method: 'get'
  })
}

// 创建迁移规则
export function createMigrationRule(data) {
  return request({
    url: '/v1/migration-rules',
    method: 'post',
    data
  })
}

// 更新迁移规则
export function updateMigrationRule(id, data) {
  return request({
    url: `/v1/migration-rules/${id}`,
    method: 'put',
    data
  })
}

// 删除迁移规则
export function deleteMigrationRule(id) {
  return request({
    url: `/v1/migration-rules/${id}`,
    method: 'delete'
  })
}

// 执行单个规则
export function executeMigrationRule(id) {
  return request({
    url: `/v1/migration-rules/${id}/execute`,
    method: 'post'
  })
}

// 预览命令行参数
export function previewCommand(id) {
  return request({
    url: `/v1/migration-rules/${id}/preview`,
    method: 'get'
  })
}

// 切换规则状态
export function toggleMigrationRuleStatus(id) {
  return request({
    url: `/v1/migration-rules/${id}/toggle`,
    method: 'post'
  })
}
