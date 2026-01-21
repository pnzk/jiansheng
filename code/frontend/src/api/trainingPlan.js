import request from './request'

export const createTrainingPlan = (data) => {
  return request.post('/plan/create', data)
}

export const updateTrainingPlan = (planId, data) => {
  return request.put(`/plan/${planId}`, data)
}

export const deleteTrainingPlan = (planId) => {
  return request.delete(`/plan/${planId}`)
}

export const getStudentTrainingPlan = (studentId) => {
  return request.get(`/plan/student/${studentId}`)
}

export const getMyTrainingPlan = () => {
  return request.get('/plan/my')
}

export const getCoachTrainingPlans = () => {
  return request.get('/plan/coach')
}

export const updatePlanProgress = (planId, data) => {
  return request.put(`/plan/${planId}/progress`, data)
}
