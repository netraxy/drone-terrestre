package com.clement.droneterrestre

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(ssid: String, onDisconnect: () -> Unit) {

    var serverIp by remember { mutableStateOf("192.168.4.1") }
    var serverPort by remember { mutableStateOf("8765") }
    var wsConnected by remember { mutableStateOf(false) }
    var wsError by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(true) }
    var invertY by remember { mutableStateOf(false) }

    var steerX by remember { mutableStateOf(0f) }   // -1..1
    var throttleY by remember { mutableStateOf(0f) } // -1..1

    var ws by remember { mutableStateOf<WebSocket?>(null) }
    val client = remember {
        OkHttpClient.Builder()
            .pingInterval(15, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    fun openWs() {
        wsError = null
        ws?.close(1000, "reconnect")
        val request = Request.Builder()
            .url("ws://${serverIp.trim()}:${serverPort.trim()}/")
            .build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                wsConnected = true
                wsError = null
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                wsConnected = false
                wsError = "Connexion WebSocket impossible : ${t.message}"
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                wsConnected = false
            }
        })
    }

    // Loop d'envoi 20Hz
    LaunchedEffect(wsConnected) {
        while (wsConnected) {
            val y = if (invertY) -throttleY else throttleY
            val json = JSONObject()
                .put("type", "control")
                .put("steering", (steerX * 1000).roundToInt() / 1000.0)
                .put("throttle", (y * 1000).roundToInt() / 1000.0)
            ws?.send(json.toString())
            delay(50)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ws?.close(1000, "leave")
            client.dispatcher.executorService.shutdown()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pilotage", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Text(ssid, fontSize = 11.sp, color = Color(0xFF888888))
                    }
                },
                actions = {
                    Surface(
                        color = if (wsConnected) Color(0xFF0A2A1A) else Color(0xFF2A1010),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (wsConnected) Color(0xFF22C55E) else Color(0xFFEF4444))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (wsConnected) "Connecté" else "Hors-ligne",
                                fontSize = 11.sp,
                                color = if (wsConnected) Color(0xFF86EFAC) else Color(0xFFF87171)
                            )
                        }
                    }
                    IconButton(onClick = {
                        ws?.close(1000, "user")
                        onDisconnect()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitter", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111111))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (showSettings || !wsConnected) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ADRESSE DU VÉHICULE", fontSize = 11.sp, color = Color(0xFF888888), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = serverIp,
                            onValueChange = { serverIp = it },
                            placeholder = { Text("IP (ex 192.168.4.1)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF2A2A2A)
                            )
                        )
                        OutlinedTextField(
                            value = serverPort,
                            onValueChange = { serverPort = it },
                            placeholder = { Text("Port") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF2A2A2A)
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { openWs(); showSettings = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (wsConnected) "Reconnecter" else "Connecter au véhicule") }
                    wsError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Color(0xFFF87171), fontSize = 12.sp)
                    }
                }
            }

            // Zone joysticks
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DIRECTION", fontSize = 10.sp, color = Color(0xFF666666), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Joystick(
                        size = 160.dp,
                        onMove = { x, _ -> steerX = x },
                        onRelease = { steerX = 0f }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "x: ${"%.2f".format(steerX)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VITESSE", fontSize = 10.sp, color = Color(0xFF666666), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Joystick(
                        size = 160.dp,
                        onMove = { _, y -> throttleY = y },
                        onRelease = { throttleY = 0f }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "y: ${"%.2f".format(throttleY)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
            }

            // Barre du bas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        steerX = 0f; throttleY = 0f
                        ws?.send(JSONObject().put("type", "stop").toString())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("⛔ ARRÊT", fontWeight = FontWeight.Bold) }

                OutlinedButton(
                    onClick = { invertY = !invertY },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (invertY) Color(0xFF60A5FA) else Color.White
                    ),
                    modifier = Modifier.height(50.dp)
                ) { Text(if (invertY) "Y inversé" else "Inverser Y") }
            }
        }
    }
}

@Composable
private fun Joystick(
    size: androidx.compose.ui.unit.Dp,
    onMove: (Float, Float) -> Unit,
    onRelease: () -> Unit
) {
    val density = LocalConfiguration.current.densityDpi / 160f
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) { size.toPx() }
    val maxRadius = sizePx / 2f * 0.7f

    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF1A1A1A))
            .border(2.dp, Color(0xFF2A2A2A), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(sizePx / 2, sizePx / 2)
                        val rel = offset - center
                        val d = hypot(rel.x, rel.y)
                        val clamped = if (d > maxRadius) rel * (maxRadius / d) else rel
                        knobOffset = clamped
                        onMove(clamped.x / maxRadius, -clamped.y / maxRadius)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = knobOffset + dragAmount
                        val d = hypot(newOffset.x, newOffset.y)
                        val clamped = if (d > maxRadius) newOffset * (maxRadius / d) else newOffset
                        knobOffset = clamped
                        onMove(clamped.x / maxRadius, -clamped.y / maxRadius)
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onRelease()
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF2A2A2A),
                radius = maxRadius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )
        }
        val knobSize = (sizePx * 0.35f / density).dp
        Box(
            modifier = Modifier
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        knobOffset.x.toInt(),
                        knobOffset.y.toInt()
                    )
                }
                .size(knobSize)
                .clip(CircleShape)
                .background(Color(0xFF3B82F6))
                .border(2.dp, Color(0xFF1E40AF), CircleShape)
        )
    }
}
