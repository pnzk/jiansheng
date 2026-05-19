import request from './request'

export const getDashboardStatistics = (params) => {
  return request.get('/analytics/dashboard', { params })
}

export const getUserBehaviorAnalysis = (params) => {
  return request.get('/analytics/behavior', { params })
}

export const getFitnessEffectAnalysis = (params) => {
  return request.get('/analytics/fitness-effect', { params })
}

export const getLeaderboard = (type, limit = 10, params = {}) => {
  return request.get('/analytics/leaderboard', { params: { type, limit, ...params } })
}

export const getPeakHourWarning = (params) => {
  return request.get('/analytics/peak-hour', { params })
}

export const getEquipmentUsage = (params) => {
  return request.get('/analytics/equipment-usage', { params })
}

export const getCoachWorkload = () => {
  return request.get('/analytics/coach-workload')
}

export const getCoachDashboard = () => {
  return request.get('/analytics/coach-dashboard')
}

export const getCoachStudentReport = (params) => {
  return request.get('/analytics/coach-student-report', { params })
}

export const getHourlyActivity = (params) => {
  return request.get('/analytics/hourly-activity', { params })
}

export const getHourlyHeatmap = (params) => {
  return request.get('/analytics/hourly-heatmap', { params })
}

export const getExercisePreference = (params) => {
  return request.get('/analytics/exercise-preference', { params })
}

export const getCoachStudentExerciseRecords = (params) => {
  return request.get('/analytics/coach-student-exercise-records', { params })
}

export const getCoachStudentBodyMetrics = (params) => {
  return request.get('/analytics/coach-student-body-metrics', { params })
}
