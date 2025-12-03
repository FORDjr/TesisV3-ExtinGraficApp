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
import org.example.project.preferredBaseUrl
import org.example.project.LOCAL_SERVER_URL

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

    // URLs actualizadas: túnel (preferred), localhost/emulador
    val urlsParaProbar = listOf(
        preferredBaseUrl(),            // Túnel actual
        LOCAL_SERVER_URL,              // Emulador / 10.0.2.2
        "http://10.0.2.2:8080",       // Emulador explícito
        "http://localhost:8080"       // Desktop local
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
                            resultado += "🔍 Probando: $url/health\n"
                            val response = httpClient.get("$url/health")
                            val code = response.status.value
                            if (code in 200..299) {
                                resultado += "✅ ÉXITO: HTTP $code\n\n"
                                break
                            } else {
                                resultado += "❌ Código HTTP: $code\n\n"
                            }
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
