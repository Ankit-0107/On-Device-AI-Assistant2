package com.example.offlineassistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.arm.aichat.InferenceEngine
import com.arm.aichat.InferenceEngineProvider
import com.example.offlineassistant.service.AssistantNotificationService
import com.example.offlineassistant.ui.theme.OfflineAssistantTheme
import com.example.offlineassistant.voice.SpeechRecognizerManager
import com.example.offlineassistant.voice.TextToSpeechManager
import com.example.offlineassistant.voice.SpeechResultListener
import com.example.offlineassistant.voice.VoiceInputButton
import kotlinx.coroutines.launch
import java.io.File

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class MainActivity : ComponentActivity() {

    private lateinit var engine: InferenceEngine
    private lateinit var permissionManager: PermissionManager
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var speechRecognizerManager: SpeechRecognizerManager

    private var onModelStatusChanged: ((String, Boolean) -> Unit)? = null
    
    private val modelPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                loadSelectedModel(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Go edge-to-edge for a modern, immersive look
        enableEdgeToEdge()

        engine = InferenceEngineProvider.get(applicationContext)
        permissionManager = PermissionManager(this)
        ttsManager = TextToSpeechManager(this)
        speechRecognizerManager = SpeechRecognizerManager(this)

        permissionManager.requestPermissions { granted ->
            if (granted) {
                AssistantNotificationService.start(this)
            }
        }

        setContent {
            OfflineAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AssistantScreen(
                        onSelectModel = {
                            modelPicker.launch(arrayOf("*/*"))
                        },
                        onSendMessage = { message, onToken, onFinished ->
                            generateResponse(message, onToken, onFinished)
                        },
                        onStatusCallbackReady = { callback ->
                            onModelStatusChanged = callback
                        },
                        onRunBenchmark = { onResult ->
                            runBenchmark(onResult)
                        },
                        commandParser = AssistantCommandParser(),
                        toolExecutor = AssistantToolExecutor(applicationContext),
                        speechRecognizerManager = speechRecognizerManager,
                        ttsManager = ttsManager
                    )
                }
            }
        }
        
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }
    
    private fun handleIntent(intent: Intent) {
        val startVoice = intent.getBooleanExtra("EXTRA_START_VOICE", false)
        val shortcutAction = intent.getStringExtra("shortcut_action")
        
        if (startVoice) {
            lifecycleScope.launch {
                kotlinx.coroutines.delay(500)
                speechRecognizerManager.startListening()
            }
        }
        
        shortcutAction?.let {
            val cmd = when(it) {
                "take_note" -> "take a note"
                "set_alarm" -> "set alarm for 7:00 am"
                "make_call" -> "make a call"
                else -> null
            }
            if (cmd != null) {
                val action = AssistantCommandParser().parse(cmd)
                if (action != null) {
                    AssistantToolExecutor(this).execute(action)
                }
            }
        }
    }

    private fun loadSelectedModel(uri: Uri) {
        onModelStatusChanged?.invoke("Loading model (copying file)...", false)
        lifecycleScope.launch {
            try {
                val modelFile = copyModelToInternalStorage(applicationContext, uri)
                onModelStatusChanged?.invoke("Loading model (initializing engine)...", false)
                engine.loadModel(modelFile.absolutePath)
                engine.setSystemPrompt(
                    "You are a helpful offline AI assistant. Answer clearly and concisely."
                )
                onModelStatusChanged?.invoke("Model Ready", true)
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("MainActivity", "Error loading model", e)
                onModelStatusChanged?.invoke("Error: ${e.message?.take(30)}...", false)
            }
        }
    }

    private var generationJob: kotlinx.coroutines.Job? = null

    private fun generateResponse(
        message: String,
        onToken: (String) -> Unit,
        onFinished: (String) -> Unit
    ) {
        // Cancel previous generation if user sends a new prompt early
        generationJob?.cancel()

        generationJob = lifecycleScope.launch {
            val fullResponse = StringBuilder()
            try {
                engine.sendUserPrompt(message)
                    .collect { token ->
                        fullResponse.append(token)
                        onToken(token)
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellation (user sent a new prompt)
            } catch (e: Exception) {
                onToken("\nError: ${e.message}")
            } finally {
                onFinished(fullResponse.toString())
                if (ttsManager.isEnabled && fullResponse.isNotBlank()) {
                    ttsManager.speak(fullResponse.toString())
                }
            }
        }
    }

    private fun runBenchmark(onResult: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                val sb = java.lang.StringBuilder()
                sb.append("=== Task 2: Throughput ===\n")
                sb.append(engine.bench(pp = 64, tg = 5, pl = 1, nr = 1))
                sb.append("\n\n=== Task 3: Latency (TTFT) ===\n")
                sb.append("[10 tokens]\n").append(engine.bench(pp = 10, tg = 1, pl = 1, nr = 1)).append("\n")
                sb.append("[50 tokens]\n").append(engine.bench(pp = 50, tg = 1, pl = 1, nr = 1)).append("\n")
                sb.append("[100 tokens]\n").append(engine.bench(pp = 100, tg = 1, pl = 1, nr = 1))
                onResult(sb.toString())
            } catch (e: Exception) {
                onResult("Benchmark failed: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.destroy()
        speechRecognizerManager.destroy()
        ttsManager.shutdown()
        AssistantNotificationService.stop(this)
    }
}

suspend fun copyModelToInternalStorage(context: Context, uri: Uri): File = kotlinx.coroutines.Dispatchers.IO.let { ioDispatcher ->
    kotlinx.coroutines.withContext(ioDispatcher) {
        val modelFile = File(context.filesDir, "gemma-model.gguf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            modelFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        modelFile
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    onSelectModel: () -> Unit,
    onSendMessage: (String, (String) -> Unit, (String) -> Unit) -> Unit,
    onStatusCallbackReady: ((String, Boolean) -> Unit) -> Unit,
    onRunBenchmark: ((String) -> Unit) -> Unit,
    commandParser: AssistantCommandParser,
    toolExecutor: AssistantToolExecutor,
    speechRecognizerManager: SpeechRecognizerManager,
    ttsManager: TextToSpeechManager
) {
    var userMessage by remember { mutableStateOf("") }
    var modelStatus by remember { mutableStateOf("No model selected") }
    var modelLoaded by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    
    var isListening by remember { mutableStateOf(false) }
    var audioLevel by remember { mutableFloatStateOf(0f) }

    var ttsEnabled by remember { mutableStateOf(ttsManager.isEnabled) }
    var showMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    
    val conversation = remember {
        mutableStateListOf(
            ChatMessage("Hello! Tap the folder icon to load a GGUF model, then we can chat or I can help you with tasks.", false)
        )
    }

    onStatusCallbackReady { status, loaded ->
        modelStatus = status
        modelLoaded = loaded
    }
    
    DisposableEffect(speechRecognizerManager) {
        val listener = object : SpeechResultListener {
            override fun onPartialResult(text: String) {
                userMessage = text
            }
            override fun onFinalResult(text: String) {
                userMessage = text
                if (text.isNotBlank()) {
                    sendMessage(
                        text, conversation, commandParser, toolExecutor, 
                        { isGenerating = true }, 
                        { msg, tk, fin -> onSendMessage(msg, tk, fin) },
                        { isGenerating = false }
                    )
                    userMessage = ""
                }
            }
            override fun onError(errorMessage: String) {
                // handle silently or show subtle toast
            }
            override fun onListeningStarted() {
                isListening = true
            }
            override fun onListeningStopped() {
                isListening = false
                audioLevel = 0f
            }
            override fun onRmsChanged(rmsDb: Float) {
                audioLevel = ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
            }
        }
        speechRecognizerManager.listener = listener
        onDispose { speechRecognizerManager.listener = null }
    }

    // Scroll to bottom when conversation changes
    LaunchedEffect(conversation.size, isGenerating) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Assistant", 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        AnimatedVisibility(visible = modelStatus.isNotBlank()) {
                            Text(
                                text = modelStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (modelLoaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        ttsEnabled = !ttsEnabled
                        ttsManager.isEnabled = ttsEnabled
                    }) {
                        Icon(
                            imageVector = if (ttsEnabled) Icons.Rounded.GraphicEq else Icons.Default.SettingsVoice,
                            contentDescription = "Toggle TTS",
                            tint = if (ttsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSelectModel, enabled = !isGenerating) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = "Select Model")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Run Benchmark") },
                                onClick = {
                                    showMenu = false
                                    isGenerating = true
                                    conversation.add(ChatMessage("Running benchmark (10 iterations)...", false))
                                    val index = conversation.lastIndex
                                    onRunBenchmark { result ->
                                        conversation[index] = conversation[index].copy(text = result)
                                        isGenerating = false
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                enabled = modelLoaded && !isGenerating
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(conversation) { message ->
                    ChatBubble(message = message)
                }
                
                if (isGenerating) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 16.dp,
                                    bottomEnd = 16.dp,
                                    bottomStart = 16.dp
                                ),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Thinking...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Area
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // crucial for edge-to-edge
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = userMessage,
                        onValueChange = { userMessage = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask or command...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ),
                        maxLines = 4,
                        trailingIcon = {
                            if (userMessage.isBlank()) {
                                VoiceInputButton(
                                    isListening = isListening,
                                    onTap = {
                                        if (isListening) speechRecognizerManager.stopListening()
                                        else speechRecognizerManager.startListening()
                                    },
                                    audioLevel = audioLevel,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    )
                    
                    AnimatedVisibility(
                        visible = userMessage.isNotBlank(),
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp, bottom = 4.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(enabled = modelLoaded && !isGenerating) {
                                    val msg = userMessage.trim()
                                    sendMessage(
                                        msg, conversation, commandParser, toolExecutor, 
                                        { isGenerating = true }, 
                                        { m, tk, fin -> onSendMessage(m, tk, fin) },
                                        { isGenerating = false }
                                    )
                                    userMessage = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun sendMessage(
    message: String,
    conversation: MutableList<ChatMessage>,
    commandParser: AssistantCommandParser,
    toolExecutor: AssistantToolExecutor,
    onStartGenerating: () -> Unit,
    sendPrompt: (String, (String) -> Unit, (String) -> Unit) -> Unit,
    onFinishedGenerating: () -> Unit
) {
    conversation.add(ChatMessage(text = message, isUser = true))

    val action = commandParser.parse(message)
    if (action != null) {
        val result = toolExecutor.execute(action)
        conversation.add(ChatMessage(text = result ?: "I couldn't perform that action.", isUser = false))
        return
    }

    onStartGenerating()
    conversation.add(ChatMessage(text = "", isUser = false))
    val assistantMessageIndex = conversation.lastIndex

    sendPrompt(
        message,
        { token ->
            conversation[assistantMessageIndex] = conversation[assistantMessageIndex].copy(
                text = conversation[assistantMessageIndex].text + token
            )
        },
        { _ ->
            onFinishedGenerating()
        }
    )
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = if (isUser) 0.dp else 1.dp
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 22.sp
                ),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                textAlign = if (isUser) TextAlign.End else TextAlign.Start
            )
        }
    }
}
