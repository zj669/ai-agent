import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import { bootApp } from './app/boot'
import './index.css'

bootApp()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
)
