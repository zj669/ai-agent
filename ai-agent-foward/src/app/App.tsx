import { useEffect } from 'react'
import { AppRouter } from './router'

function App() {
  useEffect(() => {
    const root = document.documentElement
    root.classList.remove('dark')
    root.classList.add('light')
  }, [])

  return <AppRouter />
}

export default App
