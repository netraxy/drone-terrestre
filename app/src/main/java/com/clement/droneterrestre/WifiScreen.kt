package com.clement.droneterrestre

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onConnected: (String) -> Unit
) {
    val context = LocalContext.current
    val connector = remember { WifiConnector(context) }

    var networks by remember { mutableStateOf<List<WifiNet>>(emptyList()) }
    var selectedNet by remember { mutableStateOf<WifiNet?>(null) }
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        while (true) {
            scanning = true
            networks = connector.scan()
            scanning = false
            delay(5000)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Drone Terrestre", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Text("Connexion WiFi du véhicule", fontSize = 12.sp, color = Color(0xFF888888))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (hasPermission) {
                            scanning = true
                            networks = connector.scan()
                            scanning = false
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Rafraîchir", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111111))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Bouton PASSER
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onConnected("Manuel") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF60A5FA)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Passer — déjà connecté au WiFi manuellement")
            }

            if (!hasPermission) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = Color(0xFF2A1010),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Permission requise", color = Color(0xFFF87171), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "L'app a besoin de la localisation pour scanner les réseaux WiFi (obligation Android).",
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRequestPermission) { Text("Autoriser") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "RÉSEAUX DÉTECTÉS",
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                if (scanning) {
                    Text("Scan…", fontSize = 11.sp, color = Color(0xFF60A5FA))
                }
            }
            Spacer(Modifier.height(8.dp))

            if (networks.isEmpty() && hasPermission) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("Recherche en cours...", color = Color(0xFF666666))
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(networks) { net ->
                    NetworkRow(net = net, onClick = {
                        selectedNet = net
                        password = ""
                        errorMsg = null
                    })
                }
            }
        }
    }

    val net = selectedNet
    if (net != null) {
        AlertDialog(
            onDismissRequest = { if (!connecting) selectedNet = null },
            containerColor = Color(0xFF1A1A1A),
            title = { Text(net.ssid, color = Color.White) },
            text = {
                Column {
                    if (net.secured) {
                        Text(
                            "Entre le mot de passe du réseau",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Mot de passe") },
                            singleLine = true,
                            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                TextButton(onClick = { showPwd = !showPwd }) {
                                    Text(if (showPwd) "Cacher" else "Voir", fontSize = 12.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF2A2A2A)
                            )
                        )
                    } else {
                        Text(
                            "Réseau ouvert (sans mot de passe).",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                    }
                    errorMsg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Color(0xFFF87171), fontSize = 12.sp)
                    }
                    if (connecting) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Connexion... confirme dans le pop-up Android", fontSize = 12.sp, color = Color(0xFF60A5FA))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !connecting && (!net.secured || password.length >= 8),
                    onClick = {
                        connecting = true
                        errorMsg = null
                        connector.connect(
                            ssid = net.ssid,
                            password = password,
                            secured = net.secured,
                            isWpa3 = net.isWpa3,
                            onConnected = {
                                connecting = false
                                onConnected(net.ssid)
                            },
                            onError = { msg ->
                                connecting = false
                                errorMsg = msg
                            }
                        )
                    }
                ) { Text("Connecter") }
            },
            dismissButton = {
                TextButton(
                    enabled = !connecting,
                    onClick = { selectedNet = null }
                ) { Text("Annuler", color = Color(0xFF888888)) }
            }
        )
    }
}

@Composable
private fun NetworkRow(net: WifiNet, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Wifi,
            contentDescription = null,
            tint = Color(0xFF60A5FA),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(net.ssid, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                "${net.level} dBm · ${if (net.isWpa3) "WPA3" else if (net.secured) "WPA2" else "Ouvert"}",
                color = Color(0xFF888888),
                fontSize = 12.sp
            )
        }
        Icon(
            if (net.secured) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(18.dp)
        )
    }
}
