import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AppProvider } from './app/AppProvider'
import { RouterProvider } from 'react-router-dom'
import { router } from './app/router'
import './styles/global.scss'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppProvider>
      <RouterProvider router={router} />
    </AppProvider>
  </StrictMode>,
)
