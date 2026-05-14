package top.noxc.wmessenger

import android.app.Application
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import top.noxc.wmessenger.BuildConfig
import top.noxc.wmessenger.core.ChatItem
import top.noxc.wmessenger.ui.MediaItem
import top.noxc.wmessenger.core.ContactItem
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import top.noxc.wmessenger.core.MessageItem
import top.noxc.wmessenger.core.ProxyItem
import java.util.Locale
import top.noxc.wmessenger.core.TdLibManager
import top.noxc.wmessenger.core.BotCommandItem
import top.noxc.wmessenger.core.KeyboardButtonItem
import top.noxc.wmessenger.core.InlineButtonItem
import top.noxc.wmessenger.core.FreezeInfo
import top.noxc.wmessenger.core.WearNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "wmessenger_prefs"
        private const val KEY_ACCOUNT_COUNT = "account_count"
        private const val KEY_CURRENT_ACCOUNT = "current_account"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_APP_LOCK_PASSWORD = "app_lock_password"
        private const val KEY_APP_LOCK_SALT = "app_lock_salt"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_WRONG_ATTEMPT_COUNT = "app_lock_wrong_attempts"
        private const val KEY_LOCK_UNTIL_TIMESTAMP = "app_lock_until_timestamp"
        private const val KEY_CLEAR_DATA_ON_10_WRONG = "clear_data_on_10_wrong"
        private const val KEY_RECOVERY_ATTEMPTS = "recovery_attempts"
        private const val KEY_RECOVERY_COOLDOWN_END = "recovery_cooldown_end"
        private const val KEY_LOGOUT_COOLDOWN_END = "logout_cooldown_end"
        private const val KEY_DENSITY_SCALE = "density_scale"
        private const val KEY_EXPERIMENTS_UNLOCKED = "experiments_unlocked"
        private const val KEY_EXP_QUICK_REPLY = "exp_quick_reply"
        private const val KEY_EXP_LIGHT_MODE = "exp_light_mode"
        private const val KEY_EXP_MUTE_ALL = "exp_mute_all"
        private const val KEY_EXP_NOTIFICATIONS = "exp_notifications"
        private const val KEY_EXP_AVATAR_CLEAR = "exp_avatar_clear"
        private const val KEY_EXP_DOUBLE_SWIPE_EXIT = "exp_double_swipe_exit"
        private const val KEY_MUTE_ALL_ENABLED = "mute_all_enabled"
        private const val KEY_LIGHT_MODE_ACTIVE = "light_mode_active"
        private const val MAX_ACCOUNTS = 99
        private const val LOGOUT_COOLDOWN_FILE = "logout_cooldown.dat"
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, 0)

    var chatListScrollIndex = 0
    var chatListScrollOffset = 0

    fun isAppLocked(): Boolean {
        if (!_isAppLockEnabled.value || _appLockPassword.value.isEmpty()) return false
        val timeout = prefs.getLong(KEY_AUTO_LOCK_TIMEOUT, 0L)
        if (timeout == 0L) return false
        return System.currentTimeMillis() - lastActiveTime > timeout
    }

    private val _scrollToTopTrigger = MutableStateFlow(0)
    val scrollToTopTrigger: StateFlow<Int> = _scrollToTopTrigger.asStateFlow()

    fun scrollToTop() {
        _scrollToTopTrigger.value++
    }

    private val _accountList = MutableStateFlow<List<AccountInfo>>(emptyList())
    val accountList: StateFlow<List<AccountInfo>> = _accountList.asStateFlow()

    private val _currentAccountIndex = MutableStateFlow(0)
    val currentAccountIndex: StateFlow<Int> = _currentAccountIndex.asStateFlow()

    private val _currentLanguage = MutableStateFlow(
        prefs.getString(KEY_LANGUAGE, "en-us") ?: "en-us"
    )
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    val availableLanguages = listOf(
        LanguageItem("en-us", "English (US)"),
        LanguageItem("zh-CN", "简体中文"),
        LanguageItem("zh-TW", "繁體中文")
    )

    private val managers = mutableMapOf<Int, TdLibManager>()
    private lateinit var notificationManager: WearNotificationManager

    private val currentManager: TdLibManager?
        get() = managers[_currentAccountIndex.value]

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var isResettingAuth = false
    private var preferPhoneInput = false

    private val _currentScreen = MutableStateFlow(Screen.LOGIN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val navStack = mutableListOf<Screen>()

    private val _chats = MutableStateFlow<List<ChatItem>>(emptyList())
    val chats: StateFlow<List<ChatItem>> = _chats.asStateFlow()

    private val _archivedChats = MutableStateFlow<List<ChatItem>>(emptyList())
    val archivedChats: StateFlow<List<ChatItem>> = _archivedChats.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageItem>>(emptyList())
    val messages: StateFlow<List<MessageItem>> = _messages.asStateFlow()

    private val _currentChatId = MutableStateFlow<Long?>(null)
    val currentChatId: StateFlow<Long?> = _currentChatId.asStateFlow()

    private val _canSendMessages = MutableStateFlow(true)
    val canSendMessages: StateFlow<Boolean> = _canSendMessages.asStateFlow()

    private val _proxyList = MutableStateFlow<List<ProxyItem>>(emptyList())
    val proxyList: StateFlow<List<ProxyItem>> = _proxyList.asStateFlow()

    private val _connectionState = MutableStateFlow<String?>(null)
    val connectionState: StateFlow<String?> = _connectionState.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ChatItem>>(emptyList())
    val searchResults: StateFlow<List<ChatItem>> = _searchResults.asStateFlow()

    private val _searchUserResults = MutableStateFlow<List<ChatItem>>(emptyList())
    val searchUserResults: StateFlow<List<ChatItem>> = _searchUserResults.asStateFlow()

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    private fun hashPassword(password: String, salt: String): String {
        val salted = salt + password + salt
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(hash)
        } else {
            android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
        }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(bytes)
        } else {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    private val _botCommands = MutableStateFlow<List<BotCommandItem>>(emptyList())
    val botCommands: StateFlow<List<BotCommandItem>> = _botCommands.asStateFlow()

    private val _replyKeyboard = MutableStateFlow<List<List<KeyboardButtonItem>>>(emptyList())
    val replyKeyboard: StateFlow<List<List<KeyboardButtonItem>>> = _replyKeyboard.asStateFlow()

    private val _inlineKeyboard = MutableStateFlow<Triple<Long, Long, List<List<InlineButtonItem>>>>(Triple(0, 0, emptyList()))
    val inlineKeyboard: StateFlow<Triple<Long, Long, List<List<InlineButtonItem>>>> = _inlineKeyboard.asStateFlow()

    private val _isFrozen = MutableStateFlow(false)
    val isFrozen: StateFlow<Boolean> = _isFrozen.asStateFlow()

    private val _freezeInfo = MutableStateFlow<FreezeInfo?>(null)
    val freezeInfo: StateFlow<FreezeInfo?> = _freezeInfo.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
    )
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _appLockPassword = MutableStateFlow(
        prefs.getString(KEY_APP_LOCK_PASSWORD, "") ?: ""
    )
    private val _appLockSalt = MutableStateFlow(
        prefs.getString(KEY_APP_LOCK_SALT, "") ?: ""
    )

    init {
        if (_isAppLockEnabled.value && _appLockPassword.value.isNotEmpty() && _appLockSalt.value.isEmpty()) {
            val salt = generateSalt()
            val hash = hashPassword(_appLockPassword.value, salt)
            prefs.edit()
                .putString(KEY_APP_LOCK_PASSWORD, hash)
                .putString(KEY_APP_LOCK_SALT, salt)
                .apply()
            _appLockPassword.value = hash
            _appLockSalt.value = salt
        }
    }

    private val _autoLockTimeout = MutableStateFlow(
        prefs.getInt(KEY_AUTO_LOCK_TIMEOUT, 0)
    )
    val autoLockTimeout: StateFlow<Int> = _autoLockTimeout.asStateFlow()

    private val _wrongAttemptCount = MutableStateFlow(
        prefs.getInt(KEY_WRONG_ATTEMPT_COUNT, 0)
    )
    val wrongAttemptCount: StateFlow<Int> = _wrongAttemptCount.asStateFlow()

    private val _lockUntilTimestamp = MutableStateFlow(
        prefs.getLong(KEY_LOCK_UNTIL_TIMESTAMP, 0)
    )
    val lockUntilTimestamp: StateFlow<Long> = _lockUntilTimestamp.asStateFlow()

    private val _clearDataOn10Wrong = MutableStateFlow(
        prefs.getBoolean(KEY_CLEAR_DATA_ON_10_WRONG, false)
    )
    val clearDataOn10Wrong: StateFlow<Boolean> = _clearDataOn10Wrong.asStateFlow()

    private val _densityScale = MutableStateFlow(
        prefs.getFloat(KEY_DENSITY_SCALE, 1.0f)
    )
    val densityScale: StateFlow<Float> = _densityScale.asStateFlow()

    private val _experimentsUnlocked = MutableStateFlow(
        prefs.getBoolean(KEY_EXPERIMENTS_UNLOCKED, false)
    )
    val experimentsUnlocked: StateFlow<Boolean> = _experimentsUnlocked.asStateFlow()

    private val _expQuickReply = MutableStateFlow(
        prefs.getBoolean(KEY_EXP_QUICK_REPLY, false)
    )
    val expQuickReply: StateFlow<Boolean> = _expQuickReply.asStateFlow()

    private val _expLightMode = MutableStateFlow(
        prefs.getBoolean(KEY_EXP_LIGHT_MODE, false)
    )
    val expLightMode: StateFlow<Boolean> = _expLightMode.asStateFlow()

    private val _expMuteAll = MutableStateFlow(
        prefs.getBoolean(KEY_EXP_MUTE_ALL, false)
    )
    val expMuteAll: StateFlow<Boolean> = _expMuteAll.asStateFlow()

    private val _expNotifications = MutableStateFlow(
        prefs.getBoolean(KEY_EXP_NOTIFICATIONS, false)
    )
    val expNotifications: StateFlow<Boolean> = _expNotifications.asStateFlow()

    private val _expAvatarClear = MutableStateFlow(
        prefs.getBoolean(KEY_EXP_AVATAR_CLEAR, false)
    )
    val expAvatarClear: StateFlow<Boolean> = _expAvatarClear.asStateFlow()

    private val _expDoubleSwipeExit = MutableStateFlow(
        prefs.getBoolean(KEY_EXP_DOUBLE_SWIPE_EXIT, false)
    )
    val expDoubleSwipeExit: StateFlow<Boolean> = _expDoubleSwipeExit.asStateFlow()

    private val _muteAllEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_MUTE_ALL_ENABLED, false)
    )
    val muteAllEnabled: StateFlow<Boolean> = _muteAllEnabled.asStateFlow()

    private val _lightModeActive = MutableStateFlow(
        prefs.getBoolean(KEY_LIGHT_MODE_ACTIVE, false)
    )
    val isLightMode: StateFlow<Boolean> = _lightModeActive.asStateFlow()

    private val _recoveryPin = MutableStateFlow<String?>(null)
    val recoveryPin: StateFlow<String?> = _recoveryPin.asStateFlow()

    private val _sessions = MutableStateFlow<List<top.noxc.wmessenger.ui.SessionItem>>(emptyList())
    val sessions: StateFlow<List<top.noxc.wmessenger.ui.SessionItem>> = _sessions.asStateFlow()

    private val _inactiveSessionTtlDays = MutableStateFlow(180)
    val inactiveSessionTtlDays: StateFlow<Int> = _inactiveSessionTtlDays.asStateFlow()

    var lastActiveTime: Long = 0L
        private set

    var pendingReply: Triple<Long, String, Boolean>? = null

    init {
        notificationManager = WearNotificationManager(getApplication())
        if (BuildConfig.DEBUG && !_experimentsUnlocked.value) {
            _experimentsUnlocked.value = true
            prefs.edit().putBoolean(KEY_EXPERIMENTS_UNLOCKED, true).apply()
        }
        initFcmToken()
        loadAccountList()
        if (_accountList.value.isEmpty()) {
            addAccount()
        } else {
            val savedIndex = prefs.getInt(KEY_CURRENT_ACCOUNT, 0)
            switchAccount(savedIndex.coerceAtMost(_accountList.value.size - 1))
        }
        val hasAnyLoggedIn = _accountList.value.any { it.loggedIn }
        if (!hasAnyLoggedIn) {
            _currentScreen.value = Screen.WELCOME
        }
    }

    private fun loadAccountList() {
        val count = prefs.getInt(KEY_ACCOUNT_COUNT, 0)
        val list = mutableListOf<AccountInfo>()
        for (i in 0 until count) {
            val name = prefs.getString("account_name_$i", "账号 ${i + 1}") ?: "账号 ${i + 1}"
            val loggedIn = prefs.getBoolean("account_logged_in_$i", false)
            list.add(AccountInfo(i, name, loggedIn))
        }
        _accountList.value = list
    }

    fun addAccount(): Boolean {
        val currentCount = _accountList.value.size
        if (currentCount >= MAX_ACCOUNTS) return false
        val index = if (currentCount == 0) 0 else {
            val maxIndex = _accountList.value.maxOf { it.index }
            maxIndex + 1
        }
        val accountName = "账号 ${index + 1}"
        val newList = _accountList.value.toMutableList()
        newList.add(AccountInfo(index, accountName, false))
        _accountList.value = newList
        prefs.edit()
            .putInt(KEY_ACCOUNT_COUNT, newList.size)
            .putString("account_name_$index", accountName)
            .putBoolean("account_logged_in_$index", false)
            .apply()
        switchAccount(index)
        return true
    }

    fun cancelAddAccount() {
        val currentIndex = _currentAccountIndex.value
        val accountInfo = _accountList.value.find { it.index == currentIndex } ?: return
        if (!accountInfo.loggedIn && _accountList.value.size > 1) {
            removeAccount(currentIndex)
            val firstAccount = _accountList.value.firstOrNull() ?: return
            switchAccount(firstAccount.index)
        }
    }

    fun updateAccountName(index: Int, name: String) {
        prefs.edit().putString("account_name_$index", name).apply()
        val list = _accountList.value.toMutableList()
        val idx = list.indexOfFirst { it.index == index }
        if (idx >= 0) {
            list[idx] = list[idx].copy(name = name)
            _accountList.value = list
        }
    }

    fun switchAccount(index: Int) {
        val accountInfo = _accountList.value.find { it.index == index } ?: return
        val oldIndex = _currentAccountIndex.value

        if (oldIndex != index) {
            managers[oldIndex]?.closeChat()
        }

        _currentAccountIndex.value = index
        prefs.edit().putInt(KEY_CURRENT_ACCOUNT, index).apply()
        
        _uiState.value = UiState()
        navStack.clear()
        _chats.value = emptyList()
        _messages.value = emptyList()
        _currentChatId.value = null
        _canSendMessages.value = true
        _proxyList.value = emptyList()
        _connectionState.value = null
        _isFrozen.value = false
        _freezeInfo.value = null

        if (managers.containsKey(index)) {
            val existingManager = managers[index]!!
            val currentState = existingManager.authState.value
            val isReady = currentState is TdApi.AuthorizationStateReady
            if (isReady) {
                _chats.value = existingManager.chats.value
                _messages.value = existingManager.messages.value
                _currentChatId.value = existingManager.currentChatId.value
                _canSendMessages.value = existingManager.canSendMessages.value
                _proxyList.value = existingManager.proxyList.value
                _connectionState.value = existingManager.connectionState.value
                existingManager.setOnNewMessageForNotification { chatId, messageId, chatTitle, senderName, messageText, isGroup ->
                    if (_expNotifications.value && !_muteAllEnabled.value) {
                        notificationManager.notifyMessage(
                            notificationId = kotlin.math.abs((chatId % Int.MAX_VALUE).toInt()),
                            chatId = chatId,
                            accountIndex = index,
                            chatTitle = chatTitle,
                            senderName = senderName,
                            messageText = messageText,
                            isGroup = isGroup,
                            isLocked = isAppLocked()
                        )
                    }
                }
                if (_isAppLockEnabled.value && _appLockPassword.value.isNotEmpty() && lastActiveTime == 0L) {
                    navigateTo(Screen.APP_LOCK)
                } else {
                    navigateTo(Screen.CHAT_LIST)
                }
                existingManager.loadChats()
                return
            } else if (currentState != null) {
                navigateTo(Screen.LOGIN)
                return
            } else {
                existingManager.close()
                managers.remove(index)
            }
        }

        val manager = TdLibManager(getApplication(), index)
        managers[index] = manager
        manager.setOnUserNameReady { name ->
            viewModelScope.launch {
                updateAccountName(index, name)
            }
        }
        manager.setOnNewMessageForNotification { chatId, messageId, chatTitle, senderName, messageText, isGroup ->
            if (_expNotifications.value && !_muteAllEnabled.value) {
                notificationManager.notifyMessage(
                    notificationId = kotlin.math.abs((chatId % Int.MAX_VALUE).toInt()),
                    chatId = chatId,
                    accountIndex = index,
                    chatTitle = chatTitle,
                    senderName = senderName,
                    messageText = messageText,
                    isGroup = isGroup,
                    isLocked = isAppLocked()
                )
            }
        }
        manager.init()
        observeManager(manager)
        navigateTo(Screen.LOGIN)
    }

    fun cancelLogin() {
        val currentIndex = _currentAccountIndex.value
        val accountInfo = _accountList.value.find { it.index == currentIndex } ?: return
        if (!accountInfo.loggedIn && _accountList.value.size > 1) {
            managers[currentIndex]?.close()
            managers.remove(currentIndex)
            val newList = _accountList.value.filter { it.index != currentIndex }.toMutableList()
            _accountList.value = newList
            prefs.edit()
                .putInt(KEY_ACCOUNT_COUNT, newList.size)
                .remove("account_name_$currentIndex")
                .remove("account_logged_in_$currentIndex")
                .apply()
            val firstAccount = _accountList.value.firstOrNull() ?: return
            switchAccount(firstAccount.index)
        } else {
            navigateTo(Screen.CHAT_LIST)
        }
    }

    fun removeAccount(index: Int) {
        managers[index]?.close()
        managers.remove(index)
        val newList = _accountList.value.filter { it.index != index }
        _accountList.value = newList
        prefs.edit()
            .putInt(KEY_ACCOUNT_COUNT, newList.size)
            .remove("account_name_$index")
            .remove("account_logged_in_$index")
            .apply()
        if (newList.isEmpty()) {
            addAccount()
        } else if (_currentAccountIndex.value == index) {
            switchAccount(newList.first().index)
        }
    }

    fun logoutAccount(index: Int) {
        val manager = managers[index] ?: return
        manager.logout()
        managers.remove(index)
        prefs.edit()
            .putBoolean("account_logged_in_$index", false)
            .apply()
        _accountList.value = _accountList.value.map {
            if (it.index == index) it.copy(loggedIn = false) else it
        }
        if (_accountList.value.none { it.loggedIn }) {
            prefs.edit()
                .putBoolean(KEY_APP_LOCK_ENABLED, false)
                .remove(KEY_APP_LOCK_PASSWORD)
                .remove(KEY_APP_LOCK_SALT)
                .apply()
            _isAppLockEnabled.value = false
            _appLockPassword.value = ""
            _appLockSalt.value = ""
        }
        if (_currentAccountIndex.value == index) {
            _uiState.value = UiState()
            navStack.clear()
            navigateTo(Screen.LOGIN)
            val newManager = TdLibManager(getApplication(), index)
            managers[index] = newManager
            newManager.setOnUserNameReady { name ->
                viewModelScope.launch {
                    updateAccountName(index, name)
                }
            }
            newManager.init()
            observeManager(newManager)
        }
    }

    private fun observeManager(manager: TdLibManager) {
        viewModelScope.launch {
            manager.authState.collect { state ->
                if (manager !== currentManager) return@collect
                state?.let { onAuthStateChanged(it) }
            }
        }

        viewModelScope.launch {
            manager.chats.collect { list ->
                if (manager !== currentManager) return@collect
                _chats.value = list
            }
        }

        viewModelScope.launch {
            manager.archivedChats.collect { list ->
                if (manager !== currentManager) return@collect
                _archivedChats.value = list
            }
        }

        viewModelScope.launch {
            manager.messages.collect { list ->
                if (manager !== currentManager) return@collect
                _messages.value = list
            }
        }

        viewModelScope.launch {
            manager.currentChatId.collect { id ->
                if (manager !== currentManager) return@collect
                _currentChatId.value = id
            }
        }

        viewModelScope.launch {
            manager.canSendMessages.collect { can ->
                if (manager !== currentManager) return@collect
                _canSendMessages.value = can
            }
        }

        viewModelScope.launch {
            manager.proxyList.collect { list ->
                if (manager !== currentManager) return@collect
                _proxyList.value = list
            }
        }

        viewModelScope.launch {
            manager.contacts.collect { list ->
                if (manager !== currentManager) return@collect
                _contacts.value = list
            }
        }

        viewModelScope.launch {
            manager.searchResults.collect { list ->
                if (manager !== currentManager) return@collect
                _searchResults.value = list
            }
        }

        viewModelScope.launch {
            manager.searchUserResults.collect { list ->
                if (manager !== currentManager) return@collect
                _searchUserResults.value = list
            }
        }

        viewModelScope.launch {
            manager.mediaList.collect { list ->
                if (manager !== currentManager) return@collect
                _mediaList.value = list
            }
        }

        viewModelScope.launch {
            manager.qrCodeLink.collect { link ->
                if (manager !== currentManager) return@collect
                if (link.isNotEmpty() && (_uiState.value.authType == AuthType.QR_CODE || _uiState.value.authType == AuthType.LOADING)) {
                    _uiState.value = _uiState.value.copy(
                        qrCodeLink = link,
                        status = "Scan QR Code"
                    )
                }
            }
        }

        viewModelScope.launch {
            manager.botCommands.collect { list ->
                if (manager !== currentManager) return@collect
                _botCommands.value = list
            }
        }

        viewModelScope.launch {
            manager.replyKeyboard.collect { list ->
                if (manager !== currentManager) return@collect
                _replyKeyboard.value = list
            }
        }

        viewModelScope.launch {
            manager.inlineKeyboard.collect { triple ->
                if (manager !== currentManager) return@collect
                _inlineKeyboard.value = triple
            }
        }

        viewModelScope.launch {
            manager.isFrozen.collect { frozen ->
                if (manager !== currentManager) return@collect
                _isFrozen.value = frozen
            }
        }

        viewModelScope.launch {
            manager.freezeInfo.collect { info ->
                if (manager !== currentManager) return@collect
                _freezeInfo.value = info
            }
        }

        viewModelScope.launch {
            manager.recoveryPin.collect { pin ->
                if (manager !== currentManager) return@collect
                if (pin != null) {
                    _recoveryPin.value = pin
                }
            }
        }

        viewModelScope.launch {
            manager.error.collect { err ->
                if (manager !== currentManager) return@collect
                if (err != null) {
                    val currentAuthState = currentManager?.authState?.value
                    val recoverType = when {
                        currentAuthState is TdApi.AuthorizationStateWaitPhoneNumber -> if (preferPhoneInput) AuthType.PHONE_INPUT else AuthType.QR_CODE
                        currentAuthState is TdApi.AuthorizationStateWaitCode -> AuthType.VERIFICATION_CODE
                        currentAuthState is TdApi.AuthorizationStateWaitPassword -> AuthType.PASSWORD
                        currentAuthState is TdApi.AuthorizationStateWaitEmailAddress -> AuthType.EMAIL_INPUT
                        currentAuthState is TdApi.AuthorizationStateWaitEmailCode -> AuthType.EMAIL_CODE
                        currentAuthState is TdApi.AuthorizationStateWaitRegistration -> AuthType.REGISTRATION
                        else -> AuthType.ERROR
                    }
                    _uiState.value = _uiState.value.copy(
                        authType = recoverType,
                        status = "Error: $err",
                        isLoading = false,
                        error = err
                    )
                }
            }
        }

        viewModelScope.launch {
            manager.connectionState.collect { connState ->
                if (manager !== currentManager) return@collect
                _connectionState.value = connState
                if (connState != null) {
                    _uiState.value = _uiState.value.copy(connectionState = connState)
                }
            }
        }
    }

    private fun onAuthStateChanged(state: TdApi.AuthorizationState) {
        when (state.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                _uiState.value = _uiState.value.copy(
                    authType = AuthType.LOADING,
                    status = "Connecting to Telegram...",
                    isLoading = true
                )
            }

            TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR -> {
                val link = (state as TdApi.AuthorizationStateWaitOtherDeviceConfirmation).link
                _uiState.value = _uiState.value.copy(
                    authType = AuthType.QR_CODE,
                    qrCodeLink = link,
                    status = "Scan QR Code",
                    isLoading = false
                )
            }

            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                if (preferPhoneInput) {
                    _uiState.value = _uiState.value.copy(
                        authType = AuthType.PHONE_INPUT,
                        status = "Enter Phone Number",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        authType = AuthType.QR_CODE,
                        status = "Requesting QR Code...",
                        isLoading = true
                    )
                    currentManager?.requestQrCode()
                }
            }

            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                val codeInfo = state as TdApi.AuthorizationStateWaitCode
                _uiState.value = _uiState.value.copy(
                    authType = AuthType.VERIFICATION_CODE,
                    phoneNumber = codeInfo.codeInfo.phoneNumber,
                    status = "Enter Verification Code",
                    isLoading = false
                )
            }

            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                _uiState.value = _uiState.value.copy(
                    authType = AuthType.PASSWORD,
                    status = "Enter Two-Step Password",
                    isLoading = false
                )
            }

            TdApi.AuthorizationStateWaitEmailAddress.CONSTRUCTOR -> {
                _uiState.value = _uiState.value.copy(
                    authType = AuthType.EMAIL_INPUT,
                    status = "Enter Email Address",
                    isLoading = false
                )
            }

            TdApi.AuthorizationStateWaitEmailCode.CONSTRUCTOR -> {
                _uiState.value = _uiState.value.copy(
                    authType = AuthType.EMAIL_CODE,
                    status = "Enter Email Code",
                    isLoading = false
                )
            }

            TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR -> {
                _uiState.value = _uiState.value.copy(
                    authType = AuthType.REGISTRATION,
                    status = "Register Account",
                    isLoading = false
                )
            }

            TdApi.AuthorizationStateWaitPremiumPurchase.CONSTRUCTOR -> {
                _uiState.value = _uiState.value.copy(
                    authType = AuthType.ERROR,
                    status = "Premium purchase required. Please use the official Telegram app to log in first.",
                    isLoading = false
                )
            }

            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                _uiState.value = _uiState.value.copy(
                    authType = AuthType.READY,
                    status = "Logged in!",
                    isLoading = false
                )
                val idx = _currentAccountIndex.value
                prefs.edit().putBoolean("account_logged_in_$idx", true).apply()
                _accountList.value = _accountList.value.map {
                    if (it.index == idx) it.copy(loggedIn = true) else it
                }
                if (_isAppLockEnabled.value && _appLockPassword.value.isNotEmpty() && lastActiveTime == 0L) {
                    navigateTo(Screen.APP_LOCK)
                } else {
                    navigateTo(Screen.CHAT_LIST)
                }
                currentManager?.loadChats()
            }

            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                if (isResettingAuth) {
                    isResettingAuth = false
                    managers.remove(_currentAccountIndex.value)
                    val newManager = TdLibManager(getApplication(), _currentAccountIndex.value)
                    managers[_currentAccountIndex.value] = newManager
                    newManager.setOnUserNameReady { name ->
                        viewModelScope.launch {
                            updateAccountName(_currentAccountIndex.value, name)
                        }
                    }
                    newManager.init()
                    observeManager(newManager)
                    return
                }
                _uiState.value = _uiState.value.copy(
                    authType = AuthType.ERROR,
                    status = "Connection closed",
                    isLoading = false
                )
                val idx = _currentAccountIndex.value
                prefs.edit().putBoolean("account_logged_in_$idx", false).apply()
                _accountList.value = _accountList.value.map {
                    if (it.index == idx) it.copy(loggedIn = false) else it
                }
                navigateTo(Screen.LOGIN)
            }

            else -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        managers.values.forEach { it.close() }
    }

    fun showPhoneInput() {
        preferPhoneInput = true
        val currentState = currentManager?.authState?.value
        if (currentState is TdApi.AuthorizationStateWaitOtherDeviceConfirmation) {
            isResettingAuth = true
            _uiState.value = _uiState.value.copy(
                authType = AuthType.LOADING,
                status = "Resetting authentication...",
                isLoading = true
            )
            currentManager?.logout()
        } else {
            _uiState.value = _uiState.value.copy(
                authType = AuthType.PHONE_INPUT,
                status = "Enter Phone Number",
                isLoading = false
            )
        }
    }

    fun showQrCode() {
        preferPhoneInput = false
        val currentState = currentManager?.authState?.value
        if (currentState is TdApi.AuthorizationStateWaitOtherDeviceConfirmation) {
            _uiState.value = _uiState.value.copy(
                authType = AuthType.QR_CODE,
                qrCodeLink = currentState.link,
                status = "Scan QR Code",
                isLoading = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                authType = AuthType.QR_CODE,
                status = "Requesting QR Code...",
                isLoading = true
            )
            currentManager?.requestQrCode()
        }
    }

    fun submitPhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(
            status = "Sending verification code...",
            isLoading = true
        )
        currentManager?.sendPhoneNumber(phoneNumber)
    }

    fun submitPassword(password: String) {
        _uiState.value = _uiState.value.copy(
            status = "Verifying password...",
            isLoading = true
        )
        currentManager?.sendPassword(password)
    }

    fun submitCode(code: String) {
        _uiState.value = _uiState.value.copy(
            status = "Verifying code...",
            isLoading = true
        )
        currentManager?.sendVerificationCode(code)
    }

    fun submitEmailAddress(email: String) {
        _uiState.value = _uiState.value.copy(
            status = "Sending email code...",
            isLoading = true
        )
        currentManager?.setEmailAddress(email)
    }

    fun submitEmailCode(code: String) {
        _uiState.value = _uiState.value.copy(
            status = "Verifying email code...",
            isLoading = true
        )
        currentManager?.checkEmailCode(code)
    }

    fun submitRegistration(firstName: String, lastName: String) {
        _uiState.value = _uiState.value.copy(
            status = "Registering...",
            isLoading = true
        )
        currentManager?.registerUser(firstName, lastName)
    }

    fun resetLogin() {
        isResettingAuth = true
        _uiState.value = _uiState.value.copy(
            authType = AuthType.LOADING,
            status = "Resetting...",
            isLoading = true
        )
        currentManager?.logout()
    }

    fun navigateTo(screen: Screen) {
        navStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun swipeBackFromLogin() {
        navigateTo(Screen.QUIT)
    }

    fun swipeBackFromWelcome() {
        navigateTo(Screen.QUIT)
    }

    fun navigateBack() {
        if (navStack.isNotEmpty()) {
            _currentScreen.value = navStack.removeAt(navStack.size - 1)
        }
    }

    fun navigateToMenu() {
        _currentScreen.value = Screen.MENU
    }

    fun navigateBackToChatList() {
        navStack.removeAll { it != Screen.CHAT_LIST }
        _currentScreen.value = Screen.CHAT_LIST
    }

    fun openChat(chatId: Long) {
        currentManager?.openChat(chatId)
        navigateTo(Screen.CHAT)
    }

    fun backToChatList() {
        currentManager?.closeChat()
        navigateBack()
    }

    fun sendMessage(chatId: Long, text: String) {
        currentManager?.sendMessage(chatId, text)
    }

    fun sendMessageReply(chatId: Long, replyToMessageId: Long, text: String) {
        currentManager?.sendMessageReply(chatId, replyToMessageId, text)
    }

    fun sendChatTyping(chatId: Long) {
        currentManager?.sendChatTyping(chatId)
    }

    fun clickInlineButton(chatId: Long, messageId: Long, data: ByteArray) {
        currentManager?.clickInlineButton(chatId, messageId, data)
    }

    fun loadMoreMessages(chatId: Long) {
        currentManager?.loadMoreMessages(chatId)
    }

    fun loadArchivedChats() {
        currentManager?.loadArchivedChats()
    }

    fun openArchivedChat(chatId: Long) {
        currentManager?.openChat(chatId)
        navigateTo(Screen.CHAT)
    }

    fun unlockExperiments() {
        _experimentsUnlocked.value = true
        prefs.edit().putBoolean(KEY_EXPERIMENTS_UNLOCKED, true).apply()
    }

    fun setExpQuickReply(enabled: Boolean) {
        _expQuickReply.value = enabled
        prefs.edit().putBoolean(KEY_EXP_QUICK_REPLY, enabled).apply()
    }

    fun setExpLightMode(enabled: Boolean) {
        _expLightMode.value = enabled
        prefs.edit().putBoolean(KEY_EXP_LIGHT_MODE, enabled).apply()
    }

    fun setExpMuteAll(enabled: Boolean) {
        _expMuteAll.value = enabled
        prefs.edit().putBoolean(KEY_EXP_MUTE_ALL, enabled).apply()
        if (!enabled) {
            _muteAllEnabled.value = false
            prefs.edit().putBoolean(KEY_MUTE_ALL_ENABLED, false).apply()
        }
    }

    fun setExpNotifications(enabled: Boolean) {
        _expNotifications.value = enabled
        prefs.edit().putBoolean(KEY_EXP_NOTIFICATIONS, enabled).apply()
        if (!enabled) {
            _muteAllEnabled.value = false
            prefs.edit().putBoolean(KEY_MUTE_ALL_ENABLED, false).apply()
            currentManager?.unmuteAllChats()
            notificationManager.cancelAll()
        }
    }

    fun setExpAvatarClear(enabled: Boolean) {
        _expAvatarClear.value = enabled
        prefs.edit().putBoolean(KEY_EXP_AVATAR_CLEAR, enabled).apply()
    }

    fun setExpDoubleSwipeExit(enabled: Boolean) {
        _expDoubleSwipeExit.value = enabled
        prefs.edit().putBoolean(KEY_EXP_DOUBLE_SWIPE_EXIT, enabled).apply()
    }

    fun disableAllExperiments() {
        _expQuickReply.value = false
        _expLightMode.value = false
        _expMuteAll.value = false
        _expNotifications.value = false
        _expAvatarClear.value = false
        _expDoubleSwipeExit.value = false
        _muteAllEnabled.value = false
        _experimentsUnlocked.value = false
        prefs.edit()
            .putBoolean(KEY_EXP_QUICK_REPLY, false)
            .putBoolean(KEY_EXP_LIGHT_MODE, false)
            .putBoolean(KEY_EXP_MUTE_ALL, false)
            .putBoolean(KEY_EXP_NOTIFICATIONS, false)
            .putBoolean(KEY_EXP_AVATAR_CLEAR, false)
            .putBoolean(KEY_EXP_DOUBLE_SWIPE_EXIT, false)
            .putBoolean(KEY_MUTE_ALL_ENABLED, false)
            .putBoolean(KEY_EXPERIMENTS_UNLOCKED, false)
            .apply()
        currentManager?.unmuteAllChats()
        notificationManager.cancelAll()
    }

    fun setMuteAllEnabled(enabled: Boolean) {
        _muteAllEnabled.value = enabled
        prefs.edit().putBoolean(KEY_MUTE_ALL_ENABLED, enabled).apply()
        if (enabled) {
            currentManager?.muteAllChats()
            notificationManager.cancelAll()
        } else {
            currentManager?.unmuteAllChats()
        }
    }

    fun toggleLightMode() {
        _lightModeActive.value = !_lightModeActive.value
        prefs.edit().putBoolean(KEY_LIGHT_MODE_ACTIVE, _lightModeActive.value).apply()
    }

    val isLoadingHistory: StateFlow<Boolean>
        get() = currentManager?.isLoadingHistory ?: MutableStateFlow(false)

    fun searchChats(query: String) {
        currentManager?.searchChats(query)
    }

    fun searchUsers(query: String) {
        currentManager?.searchUsers(query)
    }

    fun loadContacts() {
        currentManager?.loadContacts()
    }

    fun loadMedia() {
        currentManager?.loadMedia()
    }

    fun sendMedia(chatId: Long, path: String, isVideo: Boolean) {
        if (isVideo) {
            currentManager?.sendVideo(chatId, path)
        } else {
            currentManager?.sendPhoto(chatId, path)
        }
    }

    fun sendPhoto(chatId: Long, path: String) {
        currentManager?.sendPhoto(chatId, path)
    }

    fun sendVideo(chatId: Long, path: String) {
        currentManager?.sendVideo(chatId, path)
    }

    fun openSpamBot() {
        currentManager?.searchPublicChat("SpamBot") { chatId ->
            viewModelScope.launch {
                currentManager?.openChat(chatId)
                _currentChatId.value = chatId
                _messages.value = currentManager?.messages?.value ?: emptyList()
                _currentScreen.value = Screen.CHAT
                navStack.clear()
                navStack.add(Screen.CHAT_LIST)
            }
        }
    }

    fun understoodFrozen() {
        navigateBack()
    }

    fun closeAllManagers() {
        managers.values.forEach { it.close() }
        managers.clear()
    }

    fun setupAppLock(password: String) {
        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, true)
            .putString(KEY_APP_LOCK_PASSWORD, hash)
            .putString(KEY_APP_LOCK_SALT, salt)
            .apply()
        _isAppLockEnabled.value = true
        _appLockPassword.value = hash
        _appLockSalt.value = salt
        lastActiveTime = 0L
    }

    fun disableAppLock() {
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, false)
            .remove(KEY_APP_LOCK_PASSWORD)
            .remove(KEY_APP_LOCK_SALT)
            .apply()
        _isAppLockEnabled.value = false
        _appLockPassword.value = ""
        _appLockSalt.value = ""
    }

    fun verifyAppLock(password: String): Boolean {
        val hash = hashPassword(password, _appLockSalt.value)
        if (hash == _appLockPassword.value) {
            val cooldownFile = java.io.File(getApplication<android.app.Application>().filesDir, "logout_cooldown.dat")
            cooldownFile.delete()
            prefs.edit()
                .putInt(KEY_RECOVERY_ATTEMPTS, 0)
                .putLong(KEY_RECOVERY_COOLDOWN_END, 0)
                .apply()
            return true
        }
        return false
    }

    fun saveWrongAttemptCount(count: Int) {
        prefs.edit().putInt(KEY_WRONG_ATTEMPT_COUNT, count).apply()
        _wrongAttemptCount.value = count
    }

    fun saveLockUntilTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LOCK_UNTIL_TIMESTAMP, timestamp).apply()
        _lockUntilTimestamp.value = timestamp
    }

    fun setClearDataOn10Wrong(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLEAR_DATA_ON_10_WRONG, enabled).apply()
        _clearDataOn10Wrong.value = enabled
    }

    fun clearAppData() {
        val activity = getApplication<Application>()
        val activityManager = activity.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        activityManager.clearApplicationUserData()
    }

    fun getRecoveryAttempts(): Int {
        return prefs.getInt(KEY_RECOVERY_ATTEMPTS, 0)
    }

    fun getRecoveryCooldownEnd(): Long {
        return prefs.getLong(KEY_RECOVERY_COOLDOWN_END, 0)
    }

    fun getLogoutCooldownEnd(): Long {
        val app = getApplication<Application>()
        val file = java.io.File(app.filesDir, LOGOUT_COOLDOWN_FILE)
        return if (file.exists()) {
            try {
                file.readText().toLong()
            } catch (e: Exception) {
                0
            }
        } else {
            0
        }
    }

    private fun saveLogoutCooldownEnd(endTime: Long) {
        val app = getApplication<Application>()
        val file = java.io.File(app.filesDir, LOGOUT_COOLDOWN_FILE)
        file.writeText(endTime.toString())
    }

    fun resetRecoveryAttempts() {
        prefs.edit()
            .putInt(KEY_RECOVERY_ATTEMPTS, 0)
            .putLong(KEY_RECOVERY_COOLDOWN_END, 0)
            .apply()
    }

    fun incrementRecoveryAttempts() {
        val current = prefs.getInt(KEY_RECOVERY_ATTEMPTS, 0)
        prefs.edit().putInt(KEY_RECOVERY_ATTEMPTS, current + 1).apply()
    }

    fun setRecoveryCooldown(seconds: Long) {
        prefs.edit().putLong(KEY_RECOVERY_COOLDOWN_END, System.currentTimeMillis() + seconds * 1000).apply()
    }

    fun setLogoutCooldown(seconds: Long) {
        prefs.edit().putLong(KEY_LOGOUT_COOLDOWN_END, System.currentTimeMillis() + seconds * 1000).apply()
    }

    fun logoutAllAccounts(): Boolean {
        val cooldownEnd = getLogoutCooldownEnd()
        if (System.currentTimeMillis() < cooldownEnd) {
            return false
        }
        val newCooldownEnd = System.currentTimeMillis() + 3 * 24 * 3600 * 1000
        saveLogoutCooldownEnd(newCooldownEnd)
        val wasAppLockEnabled = _isAppLockEnabled.value
        val appLockPassword = _appLockPassword.value
        val appLockSalt = _appLockSalt.value
        val currentLang = _currentLanguage.value
        val editor = prefs.edit()
        editor.clear()
        editor.putString(KEY_LANGUAGE, currentLang)
        if (wasAppLockEnabled && appLockPassword.isNotEmpty()) {
            editor.putBoolean(KEY_APP_LOCK_ENABLED, true)
            editor.putString(KEY_APP_LOCK_PASSWORD, appLockPassword)
            editor.putString(KEY_APP_LOCK_SALT, appLockSalt)
        }
        editor.apply()
        if (wasAppLockEnabled && appLockPassword.isNotEmpty()) {
            _isAppLockEnabled.value = true
            _appLockPassword.value = appLockPassword
            _appLockSalt.value = appLockSalt
        }
        managers.values.forEach { it.close() }
        managers.clear()
        _accountList.value = emptyList()
        _currentAccountIndex.value = 0
        _chats.value = emptyList()
        _archivedChats.value = emptyList()
        _messages.value = emptyList()
        _currentChatId.value = null
        _wrongAttemptCount.value = 0
        _lockUntilTimestamp.value = 0
        _languageChanged.value = true
        return true
    }

    fun sendRecoveryToBot() {
        viewModelScope.launch {
            val result = currentManager?.searchChatByUsername("@WearMessenger_Bot")
            if (result != null) {
                currentManager?.sendMessage(result, "/recovery")
            }
        }
    }

    fun verifyRecoveryPin(pin: String): Boolean {
        if (pin.length != 6) return false
        
        val attempts = getRecoveryAttempts()
        if (attempts >= 3) {
            setRecoveryCooldown(7 * 24 * 3600)
            return false
        }
        
        val expectedPin = _recoveryPin.value
        if (expectedPin == null || pin != expectedPin) {
            incrementRecoveryAttempts()
            if (attempts + 1 >= 3) {
                setRecoveryCooldown(7 * 24 * 3600)
            }
            return false
        }
        
        prefs.edit()
            .remove(KEY_APP_LOCK_PASSWORD)
            .putBoolean(KEY_APP_LOCK_ENABLED, false)
            .putInt(KEY_WRONG_ATTEMPT_COUNT, 0)
            .putLong(KEY_LOCK_UNTIL_TIMESTAMP, 0)
            .putInt(KEY_RECOVERY_ATTEMPTS, 0)
            .putLong(KEY_RECOVERY_COOLDOWN_END, 0)
            .apply()
        _isAppLockEnabled.value = false
        _appLockPassword.value = ""
        _wrongAttemptCount.value = 0
        _lockUntilTimestamp.value = 0
        _recoveryPin.value = null
        
        return true
    }

    fun setAutoLockTimeout(timeoutSeconds: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, timeoutSeconds).apply()
        _autoLockTimeout.value = timeoutSeconds
    }

    fun lockNow() {
        if (_isAppLockEnabled.value && _appLockPassword.value.isNotEmpty()) {
            navigateTo(Screen.APP_LOCK)
        }
    }

    fun checkAppLock() {
        if (_isAppLockEnabled.value && _appLockPassword.value.isNotEmpty()) {
            if (_currentScreen.value == Screen.APP_LOCK) return
            val timeout = _autoLockTimeout.value
            val elapsed = (System.currentTimeMillis() - lastActiveTime) / 1000
            if (timeout == 0 || elapsed >= timeout) {
                navigateTo(Screen.APP_LOCK)
            }
        }
    }

    fun updateLastActiveTime() {
        lastActiveTime = System.currentTimeMillis()
    }

    fun loadSessions() {
        currentManager?.loadSessions()
        viewModelScope.launch {
            currentManager?.sessions?.collect { _sessions.value = it }
        }
        viewModelScope.launch {
            currentManager?.inactiveSessionTtlDays?.collect { _inactiveSessionTtlDays.value = it }
        }
    }

    fun terminateSession(sessionId: Long) {
        currentManager?.terminateSession(sessionId)
    }

    fun terminateAllOtherSessions() {
        currentManager?.terminateAllOtherSessions()
    }

    fun setInactiveSessionTtl(days: Int) {
        currentManager?.setInactiveSessionTtl(days)
    }

    fun logout() {
        val idx = _currentAccountIndex.value
        currentManager?.logout()
        prefs.edit().putBoolean("account_logged_in_$idx", false).apply()
        _accountList.value = _accountList.value.map {
            if (it.index == idx) it.copy(loggedIn = false) else it
        }
        if (_accountList.value.none { it.loggedIn }) {
            prefs.edit()
                .putBoolean(KEY_APP_LOCK_ENABLED, false)
                .remove(KEY_APP_LOCK_PASSWORD)
                .apply()
            _isAppLockEnabled.value = false
            _appLockPassword.value = ""
        }
        navigateTo(Screen.LOGIN)
    }

    fun exitApp() {
        closeAllManagers()
        val activity = getApplication<android.app.Application>()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun initFcmToken() {
        if (!isGooglePlayServicesAvailable()) return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "FCM Token: $token")
                currentManager?.registerPushNotifications(token)
            } else {
                Log.w("FCM", "Fetching FCM token failed", task.exception)
            }
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        return try {
            com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(getApplication()) == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    fun showExitConfirm() {
        navigateTo(Screen.QUIT)
    }

    fun addSocks5Proxy(server: String, port: Int, username: String, password: String) {
        currentManager?.addSocks5Proxy(server, port, username, password)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            currentManager?.loadProxies()
        }
    }

    fun addHttpProxy(server: String, port: Int, username: String, password: String) {
        currentManager?.addHttpProxy(server, port, username, password)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            currentManager?.loadProxies()
        }
    }

    fun addMtprotoProxy(server: String, port: Int, secret: String) {
        currentManager?.addMtprotoProxy(server, port, secret)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            currentManager?.loadProxies()
        }
    }

    fun enableProxy(proxyId: Int) {
        currentManager?.enableProxy(proxyId)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            currentManager?.loadProxies()
        }
    }

    fun disableProxy() {
        currentManager?.disableProxy()
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            currentManager?.loadProxies()
        }
    }

    fun removeProxy(proxyId: Int) {
        currentManager?.removeProxy(proxyId)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            currentManager?.loadProxies()
        }
    }

    fun pingProxy(proxyId: Int, callback: (Int) -> Unit) {
        currentManager?.pingProxy(proxyId, callback)
    }

    fun setDensityScale(scale: Float) {
        prefs.edit().putFloat(KEY_DENSITY_SCALE, scale).apply()
        _densityScale.value = scale
    }

    fun loadProxies() {
        currentManager?.loadProxies()
    }

    private val _languageChanged = MutableStateFlow(false)
    val languageChanged: StateFlow<Boolean> = _languageChanged.asStateFlow()

    private val _restartRequested = MutableStateFlow(false)
    val restartRequested: StateFlow<Boolean> = _restartRequested.asStateFlow()

    fun setLanguage(code: String) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
        _currentLanguage.value = code

        val locale = when (code) {
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            "zh-TW" -> Locale.TRADITIONAL_CHINESE
            else -> Locale.US
        }
        Locale.setDefault(locale)

        val config = Configuration(getApplication<Application>().resources.configuration)
        config.setLocale(locale)
        getApplication<Application>().resources.updateConfiguration(config, getApplication<Application>().resources.displayMetrics)

        _languageChanged.value = true
    }

    fun clearLanguageChanged() {
        _languageChanged.value = false
    }

    fun setDensityScaleAndRestart(scale: Float) {
        prefs.edit().putFloat(KEY_DENSITY_SCALE, scale).apply()
        _densityScale.value = scale
        _restartRequested.value = true
    }

    fun clearRestartRequested() {
        _restartRequested.value = false
    }
}

data class UiState(
    val authType: AuthType = AuthType.LOADING,
    val qrCodeLink: String = "",
    val phoneNumber: String = "",
    val status: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val connectionState: String? = null
)

enum class AuthType {
    LOADING, QR_CODE, PHONE_INPUT, PASSWORD, VERIFICATION_CODE, EMAIL_INPUT, EMAIL_CODE, REGISTRATION, READY, ERROR
}

enum class Screen {
    WELCOME, LOGIN, CHAT_LIST, CHAT, MENU, SETTING, WELCOME_SETTING, SECURITY, PROXY_LIST, PROXY_ADD, STORAGE, ABOUT, CONTACTS, SEARCH, GALLERY_PICKER, QUIT, FROZEN, APP_LOCK, APP_LOCK_SET, APP_LOCK_SETTINGS, DEVICES, ARCHIVED_CHATS, UI_SETTINGS, EXPERIMENTS
}

data class AccountInfo(
    val index: Int,
    val name: String,
    val loggedIn: Boolean
)

data class LanguageItem(
    val code: String,
    val displayName: String
)
