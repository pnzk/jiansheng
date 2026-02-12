import request from './request'

export const getAdminCoaches = () => request.get('/admin/coaches')

export const createAdminCoach = (data) => request.post('/admin/coaches', data)

export const updateAdminCoach = (coachId, data) => request.put(`/admin/coaches/${coachId}`, data)

export const deleteAdminCoach = (coachId) => request.delete(`/admin/coaches/${coachId}`)

export const getAdminStudents = () => request.get('/admin/students')

export const createAdminStudent = (data) => request.post('/admin/students', data)

export const updateAdminStudent = (studentId, data) => request.put(`/admin/students/${studentId}`, data)

export const deleteAdminStudent = (studentId) => request.delete(`/admin/students/${studentId}`)

export const assignStudentCoach = (studentId, coachId) =>
  request.put(`/admin/students/${studentId}/assign-coach`, { coachId })

