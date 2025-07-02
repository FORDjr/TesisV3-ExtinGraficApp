"use client"

import { useState } from "react"
import { SidebarProvider, SidebarInset, SidebarTrigger } from "@/components/ui/sidebar"
import { Separator } from "@/components/ui/separator"
import { Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbPage } from "@/components/ui/breadcrumb"
import { AppSidebar } from "./components/app-sidebar"
import { DashboardContent } from "./components/dashboard-content"
import { InventarioContent } from "./components/inventario-content"
import { VentasContent } from "./components/ventas-content"
import { CalendarioContent } from "./components/calendario-content"

export default function App() {
  const [activeSection, setActiveSection] = useState("Dashboard")

  // Agregar función para manejar la navegación
  const handleNavigation = (section: string) => {
    setActiveSection(section)
    // Aquí podrías agregar navegación con Next.js router si lo necesitas
  }

  const renderContent = () => {
    switch (activeSection) {
      case "Dashboard":
        return <DashboardContent />
      case "Inventario":
        return <InventarioContent />
      case "Ventas":
        return <VentasContent />
      case "Calendario":
        return <CalendarioContent />
      default:
        return <DashboardContent />
    }
  }

  return (
    <SidebarProvider>
      <AppSidebar activeSection={activeSection} onNavigate={setActiveSection} />
      <SidebarInset>
        <header className="sticky top-0 flex h-16 shrink-0 items-center gap-2 border-b bg-background px-4">
          <SidebarTrigger className="-ml-1" />
          <Separator orientation="vertical" className="mr-2 h-4" />
          <Breadcrumb>
            <BreadcrumbList>
              <BreadcrumbItem>
                <BreadcrumbPage className="font-medium">{activeSection}</BreadcrumbPage>
              </BreadcrumbItem>
            </BreadcrumbList>
          </Breadcrumb>
        </header>
        <main className="flex-1 p-4 md:p-6">{renderContent()}</main>
      </SidebarInset>
    </SidebarProvider>
  )
}
