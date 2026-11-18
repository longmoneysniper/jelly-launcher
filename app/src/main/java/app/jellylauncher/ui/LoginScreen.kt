package app.jellylauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    initialServer: String,
    error: String?,
    busy: Boolean,
    canCancel: Boolean,
    onSubmit: (server: String, username: String, password: String) -> Unit,
    onCancel: () -> Unit,
) {
    var server by remember { mutableStateOf(initialServer) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (canCancel) BackHandler(onBack = onCancel)

    val canSubmit = !busy && server.isNotBlank() && username.isNotBlank()
    val submit = { if (canSubmit) onSubmit(server, username, password) }

    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(Modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Connect to Jellyfin", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Enter your server address and the account to use. Only a sign-in token is stored on this TV, not your password.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                label = { Text("Server address (e.g. 192.168.1.20:8096)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )

            if (error != null) {
                Text(error, color = Color(0xFFFF6B6B), fontSize = 14.sp)
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { submit() }, enabled = canSubmit) {
                    Text(if (busy) "Connecting…" else "Connect")
                }
                if (canCancel) {
                    OutlinedButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}
