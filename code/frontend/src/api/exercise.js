import request from './request'

export const addExerciseRecord = (data) => {
  return request.post('/exercise/add', data)
}

export const getUserExerciseRecords = (params) => {
  return request.get('/exercise/records', { params })
}

export const getExerciseStatistics = (params) => {
  return request.get('/exercise/statistics', { params })
}

export const deleteExerciseRecord = (recordId) => {
  return request.delete(`/exercise/${recordId}`)
}
