import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { useLogout } from '@/features/auth/useAuth'
import { useClickOutside } from '@/hooks/useClickOutside'

export default function Navbar() {
  const { user } = useAuthStore()
  const logoutMutation = useLogout()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useClickOutside(menuRef, () => setMenuOpen(false))

  const initials = user?.username?.charAt(0)?.toUpperCase() ?? '?'

  return (
    <nav className="fixed top-0 left-0 right-0 h-14 bg-white border-b border-gray-200 flex items-center px-6 z-50">
      <Link to="/" className="text-lg font-bold text-gray-900 mr-auto select-none">
        TaskFlow
      </Link>
      <div className="relative" ref={menuRef}>
        <button
          onClick={() => setMenuOpen((v) => !v)}
          className="w-8 h-8 rounded-full bg-blue-600 text-white text-sm font-medium flex items-center justify-center hover:bg-blue-700 transition-colors"
          aria-label="User menu"
          aria-expanded={menuOpen}
        >
          {initials}
        </button>
        {menuOpen && (
          <div className="absolute right-0 top-10 w-44 bg-white border border-gray-200 rounded shadow-lg z-50 py-1">
            {user && (
              <div className="px-4 py-2 text-xs text-gray-500 border-b border-gray-100">
                {user.username}
              </div>
            )}
            <Link
              to="/profile"
              className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              onClick={() => setMenuOpen(false)}
            >
              Profile
            </Link>
            <button
              onClick={() => { setMenuOpen(false); logoutMutation.mutate() }}
              className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              disabled={logoutMutation.isPending}
            >
              Logout
            </button>
          </div>
        )}
      </div>
    </nav>
  )
}
