import request from './index'

// 获取任务列表（分页）
export function getJobs(params) {
  return request({
    url: '/v1/jobs',
    method: 'get',
    params
  })
}

// 获取任务详情
export function getJob(jobId) {
  return request({
    url: `/v1/jobs/${jobId}`,
    method: 'get'
  })
}

// 获取任务进度
export function getJobProgress(jobId) {
  return request({
    url: `/v1/jobs/${jobId}/progress`,
    method: 'get'
  })
}

// 取消任务
export function cancelJob(jobId) {
  return request({
    url: `/v1/jobs/${jobId}/cancel`,
    method: 'post'
  })
}

// 重试任务
export function retryJob(jobId) {
  return request({
    url: `/v1/jobs/${jobId}/retry`,
    method: 'post'
  })
}

// 删除任务
export function deleteJob(jobId) {
  return request({
    url: `/v1/jobs/${jobId}`,
    method: 'delete'
  })
}

// 清理历史任务
export function cleanJobs(days = 7) {
  return request({
    url: '/v1/jobs/clean',
    method: 'post',
    params: { days }
  })
}

// 获取任务日志
export function getJobLogs(jobId, params) {
  return request({
    url: `/v1/jobs/${jobId}/logs`,
    method: 'get',
    params
  })
}

// 获取任务执行详情
export function getJobDetails(jobId) {
  return request({
    url: `/v1/jobs/${jobId}/details`,
    method: 'get'
  })
}

// 获取任务规则快照列表
export function getJobRules(jobId) {
  return request({
    url: `/v1/jobs/${jobId}/rules`,
    method: 'get'
  })
}

// 执行命令行任务
export function executeBatch(data) {
  return request({
    url: '/v1/batch/execute',
    method: 'post',
    data
  })
}

// 验证命令行参数
export function validateBatch(data) {
  return request({
    url: '/v1/batch/validate',
    method: 'post',
    data
  })
}
