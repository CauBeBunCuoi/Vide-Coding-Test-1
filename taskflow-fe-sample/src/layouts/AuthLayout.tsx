import { Outlet } from 'react-router-dom'

export default function AuthLayout() {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4 py-8">
      <div className="w-full max-w-[400px]">
        <div className="text-center mb-8">
          <span className="text-2xl font-bold text-gray-900">TaskFlow</span>
        </div>
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-8">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
