"use client"

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Calendar } from "@/components/ui/calendar"
import { Plus, Clock, Users, MapPin } from "lucide-react"
import { useState } from "react"

export function CalendarioContent() {
  const [date, setDate] = useState<Date | undefined>(new Date())

  const eventosHoy = [
    {
      id: 1,
      titulo: "Reunión con proveedor",
      hora: "09:00 AM",
      duracion: "1 hora",
      tipo: "Reunión",
      ubicacion: "Oficina Principal",
    },
    {
      id: 2,
      titulo: "Inventario mensual",
      hora: "02:00 PM",
      duracion: "3 horas",
      tipo: "Tarea",
      ubicacion: "Almacén",
    },
    {
      id: 3,
      titulo: "Llamada con cliente",
      hora: "04:30 PM",
      duracion: "30 min",
      tipo: "Llamada",
      ubicacion: "Virtual",
    },
  ]

  const proximosEventos = [
    {
      id: 4,
      titulo: "Presentación de ventas",
      fecha: "Mañana",
      hora: "10:00 AM",
      tipo: "Presentación",
    },
    {
      id: 5,
      titulo: "Capacitación del equipo",
      fecha: "Miércoles",
      hora: "09:00 AM",
      tipo: "Capacitación",
    },
    {
      id: 6,
      titulo: "Revisión financiera",
      fecha: "Viernes",
      hora: "03:00 PM",
      tipo: "Reunión",
    },
  ]

  const getTipoBadge = (tipo: string) => {
    const colors = {
      Reunión: "default",
      Tarea: "secondary",
      Llamada: "outline",
      Presentación: "destructive",
      Capacitación: "default",
    }
    return <Badge variant={colors[tipo as keyof typeof colors] as any}>{tipo}</Badge>
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold">Calendario</h1>
          <p className="text-muted-foreground">Gestiona tus eventos y citas</p>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          Nuevo Evento
        </Button>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        {/* Calendario */}
        <Card className="md:col-span-1">
          <CardHeader>
            <CardTitle>Calendario</CardTitle>
            <CardDescription>Selecciona una fecha</CardDescription>
          </CardHeader>
          <CardContent>
            <Calendar mode="single" selected={date} onSelect={setDate} className="rounded-md border" />
          </CardContent>
        </Card>

        {/* Eventos de hoy */}
        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle>Eventos de Hoy</CardTitle>
            <CardDescription>
              {new Date().toLocaleDateString("es-ES", {
                weekday: "long",
                year: "numeric",
                month: "long",
                day: "numeric",
              })}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {eventosHoy.map((evento) => (
                <div key={evento.id} className="flex items-start space-x-4 p-4 border rounded-lg">
                  <div className="flex-shrink-0">
                    <Clock className="h-5 w-5 text-muted-foreground mt-0.5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <h3 className="text-sm font-medium truncate">{evento.titulo}</h3>
                      {getTipoBadge(evento.tipo)}
                    </div>
                    <div className="mt-1 flex items-center text-sm text-muted-foreground">
                      <Clock className="h-3 w-3 mr-1" />
                      <span>
                        {evento.hora} • {evento.duracion}
                      </span>
                    </div>
                    <div className="mt-1 flex items-center text-sm text-muted-foreground">
                      <MapPin className="h-3 w-3 mr-1" />
                      <span>{evento.ubicacion}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Próximos eventos */}
      <Card>
        <CardHeader>
          <CardTitle>Próximos Eventos</CardTitle>
          <CardDescription>Eventos programados para esta semana</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {proximosEventos.map((evento) => (
              <div key={evento.id} className="p-4 border rounded-lg">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="font-medium">{evento.titulo}</h3>
                  {getTipoBadge(evento.tipo)}
                </div>
                <div className="text-sm text-muted-foreground">
                  <div className="flex items-center">
                    <Clock className="h-3 w-3 mr-1" />
                    <span>
                      {evento.fecha} • {evento.hora}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Resumen semanal */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Eventos Esta Semana</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">12</div>
            <p className="text-xs text-muted-foreground">+3 vs semana pasada</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Reuniones</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">8</div>
            <p className="text-xs text-muted-foreground">67% del total</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Horas Programadas</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">24h</div>
            <p className="text-xs text-muted-foreground">60% de la semana laboral</p>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
