import request from './request'

export const getUserAchievements = () => {
  return request.get('/achievement/my')
}

export const checkAndUnlockAchievements = () => {
  return request.post('/achievement/check')
}

export const getAllAchievements = () => {
  return request.get('/achievement/all')
}
