package org.example.project.test

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

@Composable
fun TestConnectionScreen() {
    var resultado by remember { mutableStateOf("Presiona el botón para probar") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val httpClient = remember {
        HttpClient {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    // URLs simples para probar
    val urlsParaProbar = listOf(
        "http://192.168.1.24:8081",  // Tu IP Wi-Fi - puerto corregido
        "http://10.0.2.2:8081",      // Para emulador Android
        "http://localhost:8081"       // Para desarrollo local
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🔧 Prueba de Conexión Simple",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    resultado = "Probando conexiones...\n"

                    for (url in urlsParaProbar) {
                        try {
                            resultado += "🔍 Probando: $url\n"
                            val response = httpClient.get("$url/test")
                            val texto = response.bodyAsText()
                            resultado += "✅ ÉXITO: $texto\n\n"
                            break // Si funciona una, paramos
                        } catch (e: Exception) {
                            resultado += "❌ Falló: ${e.message}\n\n"
                        }
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("🚀 Probar Conexión Simple")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Resultado:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = resultado,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💡 Si funciona una URL:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("• Ya tienes conectividad básica ✅")
                Text("• Podemos proceder con la base de datos")

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "❌ Si todas fallan:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("• Problema de firewall en Windows")
                Text("• Dispositivos en redes diferentes")
                Text("• VPN no configurada correctamente")
            }
        }
    }
}
