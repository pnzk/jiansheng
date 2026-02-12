import request from './request'

export const getUserProfile = () => {
  return request.get('/user/profile')
}

export const updateUserProfile = (data) => {
  return request.put('/user/profile', data)
}

export const changePassword = (data) => {
  return request.put('/user/password', data)
}

export const updatePrivacySettings = (data) => {
  return request.put('/user/privacy', data)
}

export const getCoachStudents = () => {
  return request.get('/user/coach/students')
}

export const handleCoachTodo = (data) => {
  return request.post('/user/coach/todos/handle', data)
}
