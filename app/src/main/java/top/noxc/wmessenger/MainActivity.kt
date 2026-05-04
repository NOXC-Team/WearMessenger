package top.noxc.wmessenger

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import top.noxc.wmessenger.ui.AddProxyScreen
import top.noxc.wmessenger.ui.ChatListScreen
import top.noxc.wmessenger.ui.ChatScreen
import top.noxc.wmessenger.ui.ContactsScreen
import top.noxc.wmessenger.ui.FrozenScreen
import top.noxc.wmessenger.ui.MenuScreen
import top.noxc.wmessenger.ui.SettingsScreen
import top.noxc.wmessenger.ui.QuitScreen
import top.noxc.wmessenger.ui.LoginScreen
import top.noxc.wmessenger.ui.ProxyListScreen
import top.noxc.wmessenger.ui.SearchScreen
import top.noxc.wmessenger.ui.GalleryPickerScreen
import top.noxc.wmessenger.ui.StorageScreen
import top.noxc.wmessenger.ui.AboutScreen
import top.noxc.wmessenger.ui.SecurityScreen
import top.noxc.wmessenger.ui.QRCodeLinkScreen
import top.noxc.wmessenger.ui.AppLockScreen
import top.noxc.wmessenger.ui.AppLockSetScreen
import top.noxc.wmessenger.ui.AppLockSettingsScreen
import top.noxc.wmessenger.ui.DevicesScreen
import top.noxc.wmessenger.core.FreezeInfo
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("wmessenger_prefs", android.content.Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language", "en-us") ?: "en-us"
        
        val locale = when (languageCode) {
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            "zh-TW" -> Locale.TRADITIONAL_CHINESE
            else -> Locale.US
        }
        Locale.setDefault(locale)
        
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            val viewModel: MainViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val chats by viewModel.chats.collectAsState()
            val messages by viewModel.messages.collectAsState()
            val isLoadingHistory by viewModel.isLoadingHistory.collectAsState()
            val currentChatId by viewModel.currentChatId.collectAsState()
            val canSend by viewModel.canSendMessages.collectAsState()
            val accounts by viewModel.accountList.collectAsState()
            val currentAccountIndex by viewModel.currentAccountIndex.collectAsState()
            val proxies by viewModel.proxyList.collectAsState()
            val connectionState by viewModel.connectionState.collectAsState()
            val contacts by viewModel.contacts.collectAsState()
            val searchResults by viewModel.searchResults.collectAsState()
            val searchUserResults by viewModel.searchUserResults.collectAsState()
            val mediaList by viewModel.mediaList.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val botCommands by viewModel.botCommands.collectAsState()
            val replyKeyboard by viewModel.replyKeyboard.collectAsState()
            val inlineKeyboard by viewModel.inlineKeyboard.collectAsState()
            val isFrozen by viewModel.isFrozen.collectAsState()
            val freezeInfo by viewModel.freezeInfo.collectAsState()
            val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
            val autoLockTimeout by viewModel.autoLockTimeout.collectAsState()
            val sessions by viewModel.sessions.collectAsState()
            val inactiveSessionTtlDays by viewModel.inactiveSessionTtlDays.collectAsState()
            val availableLanguages = viewModel.availableLanguages
            val languageChanged by viewModel.languageChanged.collectAsState()

            LaunchedEffect(languageChanged) {
                if (languageChanged) {
                    viewModel.clearLanguageChanged()
                    val intent = android.content.Intent(this@MainActivity, MainActivity::class.java)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finishAffinity()
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { granted ->
                val allGranted = granted.values.all { it }
                if (allGranted) {
                    viewModel.loadMedia()
                    viewModel.navigateTo(Screen.GALLERY_PICKER)
                }
            }

            fun requestMediaPermissions() {
                val perms = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.READ_MEDIA_IMAGES)
                    perms.add(Manifest.permission.READ_MEDIA_VIDEO)
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
                if (missing.isEmpty()) {
                    viewModel.loadMedia()
                    viewModel.navigateTo(Screen.GALLERY_PICKER)
                } else {
                    permissionLauncher.launch(missing.toTypedArray())
                }
            }

            var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }

            val photoLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { u ->
                    val chatId = currentChatId ?: return@let
                    contentResolver.takePersistableUriPermission(u, 0)
                    val inputStream = contentResolver.openInputStream(u)
                    if (inputStream != null) {
                        val tempFile = java.io.File(cacheDir, "upload_${System.currentTimeMillis()}")
                        tempFile.outputStream().use { out -> inputStream.copyTo(out) }
                        inputStream.close()
                        viewModel.sendPhoto(chatId, tempFile.absolutePath)
                    }
                }
            }

            val videoLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { u ->
                    val chatId = currentChatId ?: return@let
                    contentResolver.takePersistableUriPermission(u, 0)
                    val inputStream = contentResolver.openInputStream(u)
                    if (inputStream != null) {
                        val tempFile = java.io.File(cacheDir, "upload_${System.currentTimeMillis()}")
                        tempFile.outputStream().use { out -> inputStream.copyTo(out) }
                        inputStream.close()
                        viewModel.sendVideo(chatId, tempFile.absolutePath)
                    }
                }
            }

            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview()
            ) { bitmap ->
                bitmap?.let { b ->
                    val chatId = currentChatId ?: return@let
                    val tempFile = java.io.File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                    tempFile.outputStream().use { out ->
                        b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    viewModel.sendPhoto(chatId, tempFile.absolutePath)
                }
            }

            val WmColors = darkColors(
                primary = Color(0xFF2AABEE),
                primaryVariant = Color(0xFF1A5276),
                secondary = Color(0xFF2AABEE),
                secondaryVariant = Color(0xFF1A5276),
                surface = Color(0xFF1A1A1A),
                background = Color.Black
            )

            MaterialTheme(colors = WmColors) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                LaunchedEffect(isFrozen) {
                    if (isFrozen && currentScreen != Screen.FROZEN && currentScreen != Screen.LOGIN) {
                        viewModel.navigateTo(Screen.FROZEN)
                    }
                }

                when (currentScreen) {
                    Screen.LOGIN -> LoginScreen(
                        authType = uiState.authType,
                        qrLink = uiState.qrCodeLink,
                        phoneNumber = uiState.phoneNumber,
                        status = uiState.status,
                        isLoading = uiState.isLoading,
                        onPasswordSubmit = { viewModel.submitPassword(it) },
                        onCodeSubmit = { viewModel.submitCode(it) },
                        onPhoneNumberSubmit = { viewModel.submitPhoneNumber(it) },
                        onShowPhoneInput = { viewModel.showPhoneInput() },
                        onShowQrCode = { viewModel.showQrCode() },
                        onProxySettings = {
                            viewModel.loadProxies()
                            viewModel.navigateTo(Screen.PROXY_LIST)
                        },
                        onCancel = { viewModel.cancelLogin() },
                        onResetLogin = { viewModel.resetLogin() },
                        hasLoggedInAccount = accounts.any { it.loggedIn },
                        onSwipeBack = { viewModel.swipeBackFromLogin() },
                        onEmailSubmit = { viewModel.submitEmailAddress(it) },
                        onEmailCodeSubmit = { viewModel.submitEmailCode(it) },
                        onRegisterSubmit = { first, last -> viewModel.submitRegistration(first, last) }
                    )

                    Screen.CHAT_LIST -> ChatListScreen(
                        chats = chats,
                        savedScrollIndex = viewModel.chatListScrollIndex,
                        savedScrollOffset = viewModel.chatListScrollOffset,
                        onChatClick = { viewModel.openChat(it) },
                        onOpenMenu = { viewModel.navigateToMenu() },
                        onExit = { viewModel.showExitConfirm() },
                        onSaveScrollPosition = { index, offset ->
                            viewModel.chatListScrollIndex = index
                            viewModel.chatListScrollOffset = offset
                        }
                    )

                    Screen.MENU -> MenuScreen(
                        accounts = accounts,
                        currentAccountIndex = currentAccountIndex,
                        isAppLockEnabled = isAppLockEnabled,
                        onSwitchAccount = { viewModel.switchAccount(it) },
                        onAddAccount = { viewModel.addAccount() },
                        onLogoutAccount = { viewModel.logoutAccount(it) },
                        onContacts = {
                            viewModel.loadContacts()
                            viewModel.navigateTo(Screen.CONTACTS)
                        },
                        onSearch = { viewModel.navigateTo(Screen.SEARCH) },
                        onSettings = { viewModel.navigateTo(Screen.SETTING) },
                        onLockNow = { viewModel.lockNow() },
                        onSwipeUp = { viewModel.navigateBackToChatList() }
                    )

                    Screen.CONTACTS -> ContactsScreen(
                        contacts = contacts,
                        onContactClick = { viewModel.openChat(it) },
                        onBack = { viewModel.navigateBack() }
                    )

                    Screen.SEARCH -> SearchScreen(
                        userSearchResults = searchUserResults,
                        onSearchUsers = { viewModel.searchUsers(it) },
                        onChatClick = { viewModel.openChat(it) },
                        onBack = { viewModel.navigateBack() }
                    )

                    Screen.SETTING -> SettingsScreen(
                        currentLanguage = currentLanguage,
                        availableLanguages = availableLanguages,
                        onLanguageChange = { viewModel.setLanguage(it) },
                        onProxy = {
                            viewModel.loadProxies()
                            viewModel.navigateTo(Screen.PROXY_LIST)
                        },
                        onStorage = { viewModel.navigateTo(Screen.STORAGE) },
                        onSecurity = { viewModel.navigateTo(Screen.SECURITY) },
                        onAbout = { viewModel.navigateTo(Screen.ABOUT) },
                        onQrCodeLink = { viewModel.navigateTo(Screen.QR_CODE_LINK) },
                        onBack = { viewModel.navigateBack() }
                    )

                    Screen.SECURITY -> SecurityScreen(
                        onBack = { viewModel.navigateBack() },
                        onAppLockSettings = { viewModel.navigateTo(Screen.APP_LOCK_SETTINGS) },
                        onDevicesClick = {
                            viewModel.loadSessions()
                            viewModel.navigateTo(Screen.DEVICES)
                        }
                    )

                    Screen.APP_LOCK_SETTINGS -> AppLockSettingsScreen(
                        isAppLockEnabled = isAppLockEnabled,
                        autoLockTimeout = autoLockTimeout,
                        onBack = { viewModel.navigateBack() },
                        onAppLockToggle = { enabled ->
                            if (enabled) viewModel.setupAppLock("") else viewModel.disableAppLock()
                        },
                        onAppLockSet = { viewModel.navigateTo(Screen.APP_LOCK_SET) },
                        onAutoLockTimeoutChange = { viewModel.setAutoLockTimeout(it) }
                    )

                    Screen.CHAT -> {
                        val currentChatItem = currentChatId?.let { id ->
                            chats.find { it.id == id }
                        }
                        val chatTitle = currentChatItem?.title ?: "Chat"
                        ChatScreen(
                            chatTitle = chatTitle,
                            messages = messages,
                            canSend = canSend,
                            botCommands = botCommands,
                            replyKeyboard = replyKeyboard,
                            inlineKeyboard = inlineKeyboard,
                            isOnline = currentChatItem?.isOnline ?: false,
                            isTyping = currentChatItem?.isTyping ?: false,
                            isLoadingHistory = isLoadingHistory,
                            onBack = { viewModel.backToChatList() },
                            onSendMessage = { text ->
                                currentChatId?.let { viewModel.sendMessage(it, text) }
                            },
                            onLoadMore = {
                                currentChatId?.let { viewModel.loadMoreMessages(it) }
                            },
                            onInlineButtonClick = { chatId, messageId, data ->
                                viewModel.clickInlineButton(chatId, messageId, data)
                            },
                            onTyping = {
                                currentChatId?.let { viewModel.sendChatTyping(it) }
                            },
                            onAttachCamera = { cameraLauncher.launch(null) },
                            onAttachPhoto = {
                                requestMediaPermissions()
                            },
                            onAttachVideo = {
                                requestMediaPermissions()
                            }
                        )
                    }

                    Screen.GALLERY_PICKER -> GalleryPickerScreen(
                        mediaList = mediaList,
                        onMediaSelect = { item ->
                            currentChatId?.let { chatId ->
                                try {
                                    val uri = android.net.Uri.parse(item.uri)
                                    val inputStream = contentResolver.openInputStream(uri)
                                    if (inputStream != null) {
                                        val ext = if (item.isVideo) "mp4" else "jpg"
                                        val tempFile = java.io.File(cacheDir, "upload_${System.currentTimeMillis()}.$ext")
                                        tempFile.outputStream().use { out -> inputStream.copyTo(out) }
                                        inputStream.close()
                                        viewModel.sendMedia(chatId, tempFile.absolutePath, item.isVideo)
                                    }
                                } catch (_: Exception) {}
                            }
                            viewModel.navigateBack()
                        },
                        onBack = { viewModel.navigateBack() }
                    )

                    Screen.PROXY_LIST -> ProxyListScreen(
                        proxies = proxies,
                        connectionState = connectionState,
                        onEnable = { viewModel.enableProxy(it) },
                        onDisable = { viewModel.disableProxy() },
                        onRemove = { viewModel.removeProxy(it) },
                        onPing = { proxyId, callback -> viewModel.pingProxy(proxyId, callback) },
                        onAddProxy = { viewModel.navigateTo(Screen.PROXY_ADD) },
                        onBack = { viewModel.navigateBack() }
                    )

                    Screen.STORAGE -> StorageScreen(
                        onBack = { viewModel.navigateBack() }
                    )

                    Screen.ABOUT -> AboutScreen(
                        onBack = { viewModel.navigateBack() }
                    )

                    Screen.PROXY_ADD -> AddProxyScreen(
                        onAddSocks5 = { s, p, u, pw -> viewModel.addSocks5Proxy(s, p, u, pw) },
                        onAddHttp = { s, p, u, pw -> viewModel.addHttpProxy(s, p, u, pw) },
                        onAddMtproto = { s, p, sec -> viewModel.addMtprotoProxy(s, p, sec) },
                        onBack = {
                            viewModel.loadProxies()
                            viewModel.navigateBack()
                        }
                    )

                    Screen.QUIT -> QuitScreen(
                        onConfirm = { viewModel.exitApp() },
                        onCancel = { viewModel.navigateBack() }
                    )

                    Screen.FROZEN -> FrozenScreen(
                        freezeInfo = freezeInfo,
                        onOpenSpamBot = { viewModel.openSpamBot() },
                        onUnderstood = { viewModel.understoodFrozen() }
                    )

                    Screen.QR_CODE_LINK -> QRCodeLinkScreen(
                        link = uiState.qrCodeLink,
                        onBack = { viewModel.navigateBack() }
                    )

                    Screen.APP_LOCK -> AppLockScreen(
                        onUnlock = { viewModel.navigateBack() },
                        onVerifyPin = { viewModel.verifyAppLock(it) },
                        onBack = { viewModel.navigateBack() }
                    )

                    Screen.APP_LOCK_SET -> AppLockSetScreen(
                        onPinSet = { pin ->
                            viewModel.setupAppLock(pin)
                            viewModel.navigateBack()
                        },
                        onBack = { viewModel.navigateBack() }
                    )

                    Screen.DEVICES -> DevicesScreen(
                        sessions = sessions,
                        inactiveSessionTtlDays = inactiveSessionTtlDays,
                        onTerminateSession = { viewModel.terminateSession(it) },
                        onTerminateAllOther = { viewModel.terminateAllOtherSessions() },
                        onSetInactiveSessionTtl = { viewModel.setInactiveSessionTtl(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                }
            }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (viewModel as? MainViewModel)?.checkAppLock()
    }

    override fun onPause() {
        super.onPause()
        (viewModel as? MainViewModel)?.updateLastActiveTime()
    }

    override fun onDestroy() {
        super.onDestroy()
        (viewModel as? MainViewModel)?.closeAllManagers()
    }
}
