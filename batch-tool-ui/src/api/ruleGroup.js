import request from './index'

// 获取规则组列表
export function getRuleGroups(params) {
  return request({
    url: '/v1/rule-groups',
    method: 'get',
    params
  })
}

// 获取规则组详情
export function getRuleGroup(id) {
  return request({
    url: `/v1/rule-groups/${id}`,
    method: 'get'
  })
}

// 创建规则组
export function createRuleGroup(data) {
  return request({
    url: '/v1/rule-groups',
    method: 'post',
    data
  })
}

// 更新规则组
export function updateRuleGroup(id, data) {
  return request({
    url: `/v1/rule-groups/${id}`,
    method: 'put',
    data
  })
}

// 删除规则组
export function deleteRuleGroup(id) {
  return request({
    url: `/v1/rule-groups/${id}`,
    method: 'delete'
  })
}

// 执行规则组下所有规则（支持自定义并发度）
export function executeRuleGroup(id, concurrency = 1) {
  return request({
    url: `/v1/rule-groups/${id}/execute`,
    method: 'post',
    params: { concurrency }
  })
}

// 切换规则组状态
export function toggleRuleGroupStatus(id) {
  return request({
    url: `/v1/rule-groups/${id}/toggle`,
    method: 'post'
  })
}
