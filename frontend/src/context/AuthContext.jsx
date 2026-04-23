import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { authApi } from '../services/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    const stored = localStorage.getItem('user')
    if (token && stored) {
      setUser(JSON.parse(stored))
    }
    setLoading(false)
  }, [])

  const login = useCallback(async (credentials) => {
    const { data } = await authApi.login(credentials)
    const userData = data.data
    localStorage.setItem('accessToken', userData.accessToken)
    localStorage.setItem('refreshToken', userData.refreshToken)
    const userObj = {
      email: userData.email,
      fullName: userData.fullName,
      role: userData.role,
      institutionId: userData.institutionId,
    }
    localStorage.setItem('user', JSON.stringify(userObj))
    setUser(userObj)
    return userObj
  }, [])

  const register = useCallback(async (payload) => {
    const { data } = await authApi.register(payload)
    const userData = data.data
    localStorage.setItem('accessToken', userData.accessToken)
    localStorage.setItem('refreshToken', userData.refreshToken)
    const userObj = {
      email: userData.email,
      fullName: userData.fullName,
      role: userData.role,
      institutionId: userData.institutionId,
    }
    localStorage.setItem('user', JSON.stringify(userObj))
    setUser(userObj)
    return userObj
  }, [])

  const logout = useCallback(async () => {
    try { await authApi.logout() } catch {}
    localStorage.clear()
    setUser(null)
  }, [])

  const isVerifier = user?.role === 'ROLE_VERIFIER' || user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_INSTITUTION_ADMIN'
  const isAdmin = user?.role === 'ROLE_ADMIN'

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isVerifier, isAdmin }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
