package org.example.project.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.data.api.InventarioApiService

@Composable
fun NetworkDiagnosticScreen(
    apiService: InventarioApiService
) {
    var diagnosticResults by remember { mutableStateOf<List<Pair<String, Boolean>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var serverStatus by remember { mutableStateOf("Sin verificar") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🔧 Diagnóstico de Red",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Botón para probar conexiones
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    try {
                        // Probar verificación de conexión
                        val connectionWorking = apiService.verificarConexion()
                        serverStatus = if (connectionWorking) {
                            "✅ Conexión exitosa"
                        } else {
                            "❌ Sin conexión"
                        }

                        // Aquí agregaríamos los resultados de las pruebas individuales
                        // cuando implementemos el método de diagnóstico
                        diagnosticResults = listOf(
                            "Conexión general" to connectionWorking
                        )
                    } catch (e: Exception) {
                        serverStatus = "❌ Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Probando conexiones...")
            } else {
                Text("🔍 Probar todas las conexiones")
            }
        }

        // Estado del servidor
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Estado del Servidor:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = serverStatus,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Resultados de diagnóstico
        if (diagnosticResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Resultados de Conexión:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn {
                        items(diagnosticResults) { (url, isWorking) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (isWorking) "✅" else "❌",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // Información adicional
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💡 Soluciones sugeridas:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val suggestions = listOf(
                    "1. Asegúrate de que el servidor esté corriendo en tu PC",
                    "2. Verifica que tu celular y PC estén en la misma red",
                    "3. Comprueba que no hay firewall bloqueando el puerto 8080",
                    "4. Si usas VPN, verifica que ambos dispositivos estén conectados",
                    "5. Prueba desactivar temporalmente el firewall de Windows"
                )

                suggestions.forEach { suggestion ->
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
