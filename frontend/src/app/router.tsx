import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AppShell } from '../layouts/AppShell'
import { CustomizePage } from '../pages/CustomizePage'
import { HomePage } from '../pages/HomePage'
import { LandingPage } from '../pages/LandingPage'
import { StudyCreatePage } from '../pages/StudyCreatePage'
import { StudySearchPage } from '../pages/StudySearchPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <LandingPage />,
  },
  {
    path: '/app',
    element: <AppShell />,
    children: [
      { index: true, element: <Navigate to="/app/home" replace /> },
      { path: 'home', element: <HomePage /> },
      { path: 'studies', element: <StudySearchPage /> },
      { path: 'create', element: <StudyCreatePage /> },
      { path: 'customize', element: <CustomizePage /> },
    ],
  },
])
