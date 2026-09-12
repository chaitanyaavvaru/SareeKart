import { Navigate, useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';

/**
 * Route protection wrapper component.
 * Redirects unauthenticated users to /login, retaining redirect path in query.
 * Restricts admin-only routes.
 */
export default function ProtectedRoute({ children, adminOnly = false, allowedRoles = ['ADMIN', 'OWNER', 'MANAGER'] }) {
  const location = useLocation();
  const { isAuthenticated, user } = useSelector(state => state.auth);

  if (!isAuthenticated) {
    // Redirect to login page and store source path
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />;
  }

  if (adminOnly && !allowedRoles.includes(user?.role)) {
    // Redirect non-staff users to home
    return <Navigate to="/" replace />;
  }

  return children;
}
