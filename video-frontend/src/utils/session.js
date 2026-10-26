const SESSION_KEY = 'videohub.session'

export const getSession = () => {
  try {
    const session = JSON.parse(localStorage.getItem(SESSION_KEY) || 'null')
    if (!session?.token || !session?.user) return null
    if (Number(session.expiresAt || 0) > 0 && Number(session.expiresAt) * 1000 <= Date.now()) {
      localStorage.removeItem(SESSION_KEY)
      return null
    }
    return session
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return null
  }
}

export const saveSession = (data) => {
  const session = {
    token: data.token,
    user: data.user,
    expiresAt: data.expiresAt,
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export const clearSession = () => localStorage.removeItem(SESSION_KEY)

export const getToken = () => getSession()?.token || ''

export const getCurrentUser = () => getSession()?.user || null

export const updateSessionUser = (user) => {
  const session = getSession()
  if (!session) return null
  const updated = { ...session, user }
  localStorage.setItem(SESSION_KEY, JSON.stringify(updated))
  return updated
}
