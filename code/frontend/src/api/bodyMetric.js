import request from './request'

export const addBodyMetric = (data) => {
  return request.post('/bodymetric/add', data)
}

export const getBodyMetricHistory = (params) => {
  return request.get('/bodymetric/history', { params })
}

export const getLatestBodyMetric = () => {
  return request.get('/bodymetric/latest')
}

export const calculateBMI = (height, weight) => {
  return request.get('/bodymetric/bmi', { params: { height, weight } })
}
