import request from './request'

export const getExerciseList = (params) => {
  return request.get('/exercise-reference/list', { params })
}

export const getAllExercises = () => {
  return request.get('/exercise-reference/all')
}

export const getExerciseById = (id) => {
  return request.get(`/exercise-reference/${id}`)
}

export const searchExercises = (keyword) => {
  return request.get('/exercise-reference/search', { params: { keyword } })
}

export const getExerciseTypes = () => {
  return request.get('/exercise-reference/types')
}

export const getBodyParts = () => {
  return request.get('/exercise-reference/body-parts')
}

export const getLevels = () => {
  return request.get('/exercise-reference/levels')
}

export const getExercisesByBodyPart = (bodyPart) => {
  return request.get(`/exercise-reference/by-body-part/${bodyPart}`)
}
