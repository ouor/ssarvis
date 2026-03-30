import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AuthProvider } from './app/providers/AuthProvider'
import './styles/globals.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <div className="app-root">
        <App />
      </div>
    </AuthProvider>
  </StrictMode>,
)
