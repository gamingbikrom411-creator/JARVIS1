package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.GlowButton
import com.example.ui.components.HudCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val theme by viewModel.activeTheme.collectAsState()
    val allProfiles by viewModel.allUserProfiles.collectAsState()
    val activeProfile by viewModel.userProfile.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()

    var isAddingUser by remember { mutableStateOf(false) }
    var newUserNameInput by remember { mutableStateOf("") }
    var newUserTitleInput by remember { mutableStateOf("Sir") }

    var isAddingNote by remember { mutableStateOf(false) }
    var newNoteCategory by remember { mutableStateOf("PREFERENCE") }
    var newNoteFact by remember { mutableStateOf("") }

    var isEditingName by remember { mutableStateOf(false) }
    var editingNameValue by remember { mutableStateOf(activeProfile?.userName ?: "Sir") }

    BackHandler {
        viewModel.navigateTo(ScreenDestination.DASHBOARD)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(16.dp)
    ) {
        // --- 1. Header Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(ScreenDestination.DASHBOARD) },
                modifier = Modifier.testTag("memory_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = theme.primaryColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MULTI-USER MEMORY ARCHIVE",
                    color = theme.primaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "PERMANENT SESSION RECALL // ${allProfiles.size} PROFILES",
                    color = theme.textMutedColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(
                onClick = { viewModel.triggerManualBackupSync() },
                modifier = Modifier.testTag("sync_backup_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    tint = theme.primaryColor
                )
            }
        }

        // --- 2. Multi-User Profile Switcher Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allProfiles.forEach { profile ->
                val isActive = profile.userId == activeProfile?.userId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isActive) theme.primaryColor.copy(alpha = 0.25f)
                            else theme.surfaceColor
                        )
                        .border(
                            width = if (isActive) 1.5.dp else 0.8.dp,
                            color = if (isActive) theme.primaryColor else theme.textMutedColor.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.switchActiveUser(profile.userId) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isActive) Color(0xFF00E676) else theme.textMutedColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile.userName,
                            color = if (isActive) theme.primaryColor else theme.textColor,
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Add Profile Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.surfaceVariantColor.copy(alpha = 0.6f))
                    .border(0.8.dp, theme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable { isAddingUser = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add User",
                        tint = theme.primaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NEW USER",
                        color = theme.primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- 3. Active User Dossier & Session Backup Badge ---
        HudCard(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = theme.primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACTIVE PROFILE: ${activeProfile?.userName ?: "Sir"}",
                            color = theme.textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = if (isEditingName) "CANCEL" else "RENAME",
                        color = theme.primaryColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (isEditingName) editingNameValue = activeProfile?.userName ?: "Sir"
                            isEditingName = !isEditingName
                        }
                    )
                }

                if (isEditingName) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editingNameValue,
                        onValueChange = { editingNameValue = it },
                        label = { Text("Update Name", color = theme.textMutedColor, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textColor,
                            unfocusedTextColor = theme.textColor,
                            focusedBorderColor = theme.primaryColor,
                            unfocusedBorderColor = theme.textMutedColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    GlowButton(
                        text = "SAVE IDENTITY",
                        theme = theme,
                        onClick = {
                            if (editingNameValue.isNotBlank()) {
                                viewModel.setUserName(editingNameValue.trim())
                                isEditingName = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Title: ${activeProfile?.formalTitle ?: "Sir"} | Lang: ${activeProfile?.preferredLanguage ?: "Hinglish"}",
                            color = theme.textMutedColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Interactions: ${activeProfile?.totalInteractions ?: 0}",
                            color = theme.textMutedColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Backup Status Banner
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.surfaceVariantColor.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$backupStatus // RESTORE-GUARDED",
                        color = Color(0xFF00E676),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- 4. Add Custom Note / Memory Trigger Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = theme.primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RECORDED FACTS (${memories.size})",
                    color = theme.primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "+ ADD FACT",
                    color = theme.primaryColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { isAddingNote = !isAddingNote }
                )

                if (memories.isNotEmpty()) {
                    Text(
                        text = "PURGE",
                        color = Color(0xFFFF5252),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { viewModel.clearAllMemoriesForActiveUser() }
                    )
                }
            }
        }

        // Add Note Input Accordion
        AnimatedVisibility(visible = isAddingNote) {
            HudCard(
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Category Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("PREFERENCE", "PROJECT", "INSTRUCTION", "TASK_HISTORY", "NOTE").forEach { cat ->
                            val isCatSelected = newNoteCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCatSelected) theme.primaryColor else theme.surfaceVariantColor)
                                    .clickable { newNoteCategory = cat }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isCatSelected) Color.Black else theme.textMutedColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newNoteFact,
                        onValueChange = { newNoteFact = it },
                        label = { Text("Fact / Note (e.g. 'Prefers dark theme', 'Working on Stark OS')", color = theme.textMutedColor, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textColor,
                            unfocusedTextColor = theme.textColor,
                            focusedBorderColor = theme.primaryColor,
                            unfocusedBorderColor = theme.textMutedColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    GlowButton(
                        text = "COMMIT TO PERMANENT MEMORY",
                        theme = theme,
                        onClick = {
                            if (newNoteFact.isNotBlank()) {
                                viewModel.addCustomMemory(newNoteCategory, newNoteFact)
                                newNoteFact = ""
                                isAddingNote = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- 5. Memories List ---
        if (memories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No permanent memories logged for ${activeProfile?.userName ?: "this profile"}.\nSpeak or add notes (e.g., 'My name is ...', 'I prefer Hindi', 'My project is ...') and Jarvis will retain them permanently across all sessions.",
                    color = theme.textMutedColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(memories, key = { it.id }) { memory ->
                    HudCard(
                        theme = theme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(theme.primaryColor.copy(alpha = 0.2f))
                                            .border(0.6.dp, theme.primaryColor, RoundedCornerShape(3.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = memory.category,
                                            color = theme.primaryColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                        .format(Date(memory.timestamp))
                                    Text(
                                        text = dateStr,
                                        color = theme.textMutedColor,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = memory.fact,
                                    color = theme.textColor,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteMemory(memory.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = theme.textMutedColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialog: Create New User Profile ---
    if (isAddingUser) {
        AlertDialog(
            onDismissRequest = { isAddingUser = false },
            title = {
                Text(
                    text = "NEW USER PROFILE",
                    color = theme.primaryColor,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUserNameInput,
                        onValueChange = { newUserNameInput = it },
                        label = { Text("User Name (e.g. Tony, Pepper, Midul)", color = theme.textMutedColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textColor,
                            unfocusedTextColor = theme.textColor,
                            focusedBorderColor = theme.primaryColor,
                            unfocusedBorderColor = theme.textMutedColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newUserTitleInput,
                        onValueChange = { newUserTitleInput = it },
                        label = { Text("Title (Sir / Boss / Ma'am)", color = theme.textMutedColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textColor,
                            unfocusedTextColor = theme.textColor,
                            focusedBorderColor = theme.primaryColor,
                            unfocusedBorderColor = theme.textMutedColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUserNameInput.isNotBlank()) {
                            viewModel.createNewUserProfile(newUserNameInput.trim(), newUserTitleInput.trim())
                            newUserNameInput = ""
                            isAddingUser = false
                        }
                    }
                ) {
                    Text("CREATE & ACTIVATE", color = theme.primaryColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingUser = false }) {
                    Text("CANCEL", color = theme.textMutedColor, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = theme.surfaceColor
        )
    }
}
