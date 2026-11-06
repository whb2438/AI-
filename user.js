import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || 0)
  const username = ref(localStorage.getItem('username') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')
  const integral = ref(Number(localStorage.getItem('integral')) || 0)
  const role = ref(localStorage.getItem('role') || 'USER')

  function setLoginData(data) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    nickname.value = data.nickname
    integral.value = data.integral
    role.value = data.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('username', data.username)
    localStorage.setItem('nickname', data.nickname)
    localStorage.setItem('integral', String(data.integral))
    localStorage.setItem('role', data.role)
  }

  function logout() {
    token.value = ''
    userId.value = 0
    username.value = ''
    nickname.value = ''
    integral.value = 0
    role.value = 'USER'
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('nickname')
    localStorage.removeItem('integral')
    localStorage.removeItem('role')
  }

  function isLoggedIn() {
    return token.value !== ''
  }

  function isAdmin() {
    return role.value === 'ADMIN'
  }

  return {
    token, userId, username, nickname, integral, role,
    setLoginData, logout, isLoggedIn, isAdmin,
  }
})
