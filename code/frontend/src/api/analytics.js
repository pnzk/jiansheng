import request from './request'

export const getDashboardStatistics = () => {
  return request.get('/analytics/dashboard')
}

export const getUserBehaviorAnalysis = (params) => {
  return request.get('/analytics/behavior', { params })
}

export const getFitnessEffectAnalysis = (params) => {
  return request.get('/analytics/fitness-effect', { params })
}

export const getLeaderboard = (type, limit = 10) => {
  return request.get('/analytics/leaderboard', { params: { type, limit } })
}

export const getPeakHourWarning = () => {
  return request.get('/analytics/peak-hour')
}

export const getEquipmentUsage = () => {
  return request.get('/analytics/equipment-usage')
}
