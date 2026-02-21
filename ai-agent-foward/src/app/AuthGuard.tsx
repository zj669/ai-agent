import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { isAuthenticated } from './auth'

function RequireAuth() {
  const location = useLocation()

  if (!isAuthenticated()) {
    const redirect = encodeURIComponent(`${location.pathname}${location.search}`)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }

  return <Outlet />
}

export default RequireAuth
