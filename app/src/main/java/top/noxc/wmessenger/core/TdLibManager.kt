package top.noxc.wmessenger.core

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import top.noxc.wmessenger.BuildConfig
import top.noxc.wmessenger.data.ProxyRepository
import top.noxc.wmessenger.ui.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TdLibManager(
    private val context: Context,
    private val accountIndex: Int = 0
) : Client.ResultHandler {

    companion object {
        private const val TAG = "TdLib"
        private val API_ID = BuildConfig.API_ID
        private val API_HASH = BuildConfig.API_HASH

        init {
            System.loadLibrary("tdjni")
        }
    }

    private var client: Client? = null
    private val proxyRepository = ProxyRepository(context)
    private var onUserNameReady: ((String) -> Unit)? = null
    private var closedLatch: CountDownLatch? = null
    private var onNewMessageForNotification: ((Long, Long, String, String, String, Boolean) -> Unit)? = null

    fun setOnUserNameReady(callback: (String) -> Unit) {
        onUserNameReady = callback
    }

    fun setOnNewMessageForNotification(callback: (chatId: Long, messageId: Long, chatTitle: String, senderName: String, messageText: String, isGroup: Boolean) -> Unit) {
        onNewMessageForNotification = callback
    }

    private val _authState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState.asStateFlow()

    private val _qrCodeLink = MutableStateFlow("")
    val qrCodeLink: StateFlow<String> = _qrCodeLink.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _connectionState = MutableStateFlow<String?>(null)
    val connectionState: StateFlow<String?> = _connectionState.asStateFlow()

    private val _chats = MutableStateFlow<List<ChatItem>>(emptyList())
    val chats: StateFlow<List<ChatItem>> = _chats.asStateFlow()

    private val _archivedChats = MutableStateFlow<List<ChatItem>>(emptyList())
    val archivedChats: StateFlow<List<ChatItem>> = _archivedChats.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageItem>>(emptyList())
    val messages: StateFlow<List<MessageItem>> = _messages.asStateFlow()

    private val _recoveryPin = MutableStateFlow<String?>(null)
    val recoveryPin: StateFlow<String?> = _recoveryPin.asStateFlow()
    private var recoveryPinTimestamp: Long = 0
    private val RECOVERY_PIN_EXPIRE_MS = 5 * 60 * 1000L

    private var _lastReadOutboxMessageId: Long = 0
    private val _currentChatId = MutableStateFlow<Long?>(null)
    val currentChatId: StateFlow<Long?> = _currentChatId.asStateFlow()

    private val _canSendMessages = MutableStateFlow(true)
    val canSendMessages: StateFlow<Boolean> = _canSendMessages.asStateFlow()

    private val _isFrozen = MutableStateFlow(false)
    val isFrozen: StateFlow<Boolean> = _isFrozen.asStateFlow()

    private val _freezeInfo = MutableStateFlow<FreezeInfo?>(null)
    val freezeInfo: StateFlow<FreezeInfo?> = _freezeInfo.asStateFlow()

    private val _proxyList = MutableStateFlow<List<ProxyItem>>(emptyList())
    val proxyList: StateFlow<List<ProxyItem>> = _proxyList.asStateFlow()

    private var _oldestMessageId: Long = 0
    private var _hasMoreMessages = true
    private var _isLoadingMessages = false
    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory
    private var _lastLoadWasLocal = false

    private val _sessions = MutableStateFlow<List<top.noxc.wmessenger.ui.SessionItem>>(emptyList())
    val sessions: StateFlow<List<top.noxc.wmessenger.ui.SessionItem>> = _sessions

    private val _inactiveSessionTtlDays = MutableStateFlow(365)
    val inactiveSessionTtlDays: StateFlow<Int> = _inactiveSessionTtlDays

    private val _userNames = mutableMapOf<Long, String>()
    private val _chatNames = mutableMapOf<Long, String>()
    private val _filePaths = mutableMapOf<Int, String>()
    private val _userOnlineStatus = mutableMapOf<Long, Boolean>()
    private val _userAvatarFileIds = mutableMapOf<Long, Int>()
    private val _chatUserIds = mutableMapOf<Long, Long>()
    private val _chatTypingStatus = mutableMapOf<Long, Boolean>()
    private val _chatTypes = mutableMapOf<Long, TdApi.ChatType>()

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ChatItem>>(emptyList())
    val searchResults: StateFlow<List<ChatItem>> = _searchResults.asStateFlow()

    private val _searchUserResults = MutableStateFlow<List<ChatItem>>(emptyList())
    val searchUserResults: StateFlow<List<ChatItem>> = _searchUserResults.asStateFlow()

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    private val _botCommands = MutableStateFlow<List<BotCommandItem>>(emptyList())
    val botCommands: StateFlow<List<BotCommandItem>> = _botCommands.asStateFlow()

    private val _replyKeyboard = MutableStateFlow<List<List<KeyboardButtonItem>>>(emptyList())
    val replyKeyboard: StateFlow<List<List<KeyboardButtonItem>>> = _replyKeyboard.asStateFlow()

    private val _inlineKeyboard = MutableStateFlow<Triple<Long, Long, List<List<InlineButtonItem>>>>(Triple(0, 0, emptyList()))
    val inlineKeyboard: StateFlow<Triple<Long, Long, List<List<InlineButtonItem>>>> = _inlineKeyboard.asStateFlow()

    private val filesDir = context.filesDir
    private val cacheDir = context.cacheDir

    fun init() {
        Client.execute(TdApi.SetLogVerbosityLevel(0))
        client = Client.create(this, null, null)
    }

    override fun onResult(result: TdApi.Object) {
        when (result.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                val state = (result as TdApi.UpdateAuthorizationState).authorizationState
                _authState.value = state
                handleAuthState(state)
            }

            TdApi.UpdateConnectionState.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateConnectionState
                _connectionState.value = formatConnectionState(update.state)
            }

            TdApi.UpdateFreezeState.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateFreezeState
                _isFrozen.value = update.isFrozen
                if (update.isFrozen) {
                    _freezeInfo.value = FreezeInfo(
                        freezingDate = update.freezingDate,
                        deletionDate = update.deletionDate,
                        appealLink = update.appealLink
                    )
                } else {
                    _freezeInfo.value = null
                }
            }

            TdApi.Error.CONSTRUCTOR -> {
                val err = result as TdApi.Error
                Log.e(TAG, "Error ${err.code}: ${err.message}")
                _error.value = "${err.code}: ${err.message}"
            }

            TdApi.UpdateNewChat.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateNewChat
                updateChat(update.chat)
            }

            TdApi.UpdateChatTitle.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateChatTitle
                updateChatTitle(update.chatId, update.title)
            }

            TdApi.UpdateChatPhoto.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateChatPhoto
                val smallFile = update.photo?.small
                val avatarFileId = smallFile?.id ?: 0
                var avatarLocalPath: String? = null
                if (avatarFileId != 0) {
                    val localPath = smallFile?.local?.path ?: ""
                    if (localPath.isNotEmpty()) {
                        _filePaths[avatarFileId] = localPath
                        avatarLocalPath = localPath
                    } else {
                        avatarLocalPath = _filePaths[avatarFileId]
                        downloadFile(avatarFileId)
                    }
                }
                val currentList = _chats.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == update.chatId }
                if (index >= 0) {
                    currentList[index] = currentList[index].copy(
                        avatarFileId = avatarFileId,
                        avatarLocalPath = avatarLocalPath
                    )
                    _chats.value = currentList
                }
            }

            TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateChatLastMessage
                updateChatLastMessage(update.chatId, update.lastMessage, update.positions)
            }

            TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateNewMessage
                addMessage(update.message)
                if (_currentChatId.value == update.message.chatId && !update.message.isOutgoing) {
                    client?.send(TdApi.ViewMessages(update.message.chatId, longArrayOf(update.message.id), TdApi.MessageSourceChatHistory(), true), null)
                }
                if (!update.message.isOutgoing) {
                    extractRecoveryPin(update.message)
                    triggerNotificationForMessage(update.message)
                }
            }

            TdApi.UpdateMessageContent.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateMessageContent
                updateMessageContent(update.chatId, update.messageId, update.newContent)
            }

            TdApi.UpdateDeleteMessages.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateDeleteMessages
                removeMessages(update.chatId, update.messageIds)
            }

            TdApi.UpdateUser.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateUser
                _userNames[update.user.id] = formatUserName(update.user)
                handleUserUpdate(update.user)
            }

            TdApi.UpdateUserStatus.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateUserStatus
                val isOnline = update.status is TdApi.UserStatusOnline
                _userOnlineStatus[update.userId] = isOnline
                refreshChatsWithUser(update.userId)
            }

            TdApi.UpdateChatAction.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateChatAction
                val isTyping = update.action !is TdApi.ChatActionCancel
                val wasTyping = _chatTypingStatus[update.chatId] == true
                _chatTypingStatus[update.chatId] = isTyping
                if (isTyping != wasTyping) {
                    refreshChatItem(update.chatId)
                }
            }

            TdApi.UpdateChatPosition.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateChatPosition
                when (update.position.list) {
                    is TdApi.ChatListMain -> updateChatPosition(update.chatId, update.position)
                    is TdApi.ChatListArchive -> updateArchivedChatPosition(update.chatId, update.position)
                }
            }

            TdApi.UpdateChatReadOutbox.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateChatReadOutbox
                if (_currentChatId.value == update.chatId) {
                    _lastReadOutboxMessageId = update.lastReadOutboxMessageId
                    updateMessagesReadState()
                }
            }

            TdApi.UpdateChatReadInbox.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateChatReadInbox
                val currentList = _chats.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == update.chatId }
                if (index >= 0) {
                    currentList[index] = currentList[index].copy(unreadCount = update.unreadCount)
                    _chats.value = currentList
                }
            }

            TdApi.UpdateFile.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateFile
                val file = update.file
                if (file.local.isDownloadingCompleted && file.local.path.isNotEmpty()) {
                    _filePaths[file.id] = file.local.path
                    refreshMessagesWithFile(file.id)
                    refreshAvatarWithFile(file.id)
                }
            }

            TdApi.User.CONSTRUCTOR -> {
                val user = result as TdApi.User
                _userNames[user.id] = formatUserName(user)
                handleUserUpdate(user)
                val isOnline = user.status is TdApi.UserStatusOnline
                _userOnlineStatus[user.id] = isOnline
                refreshChatsWithUser(user.id)
                refreshContactsWithUser(user.id)
            }

            TdApi.Chats.CONSTRUCTOR -> {
                val chatsResult = result as TdApi.Chats
                if (_isSearching) {
                    _isSearching = false
                    loadChatsByIds(chatsResult.chatIds)
                    val results = mutableListOf<ChatItem>()
                    for (chatId in chatsResult.chatIds) {
                        val chat = _chats.value.find { it.id == chatId }
                        if (chat != null) results.add(chat)
                    }
                    _searchResults.value = results
                } else {
                    loadChatsByIds(chatsResult.chatIds)
                }
            }

            TdApi.Users.CONSTRUCTOR -> {
                val usersResult = result as TdApi.Users
                val items = mutableListOf<ContactItem>()
                for (userId in usersResult.userIds) {
                    val name = _userNames[userId]
                    if (name != null) {
                        val avatarFileId = _userAvatarFileIds[userId] ?: 0
                        val avatarLocalPath = if (avatarFileId != 0) _filePaths[avatarFileId] else null
                        val isOnline = _userOnlineStatus[userId] == true
                        items.add(ContactItem(userId, name, avatarFileId, avatarLocalPath, isOnline))
                    }
                    client?.send(TdApi.GetUser(userId), this)
                }
                _contacts.value = items
            }

            TdApi.Chat.CONSTRUCTOR -> {
                val chat = result as TdApi.Chat
                updateChat(chat)
                if (_isSearchingUsers) {
                    _isSearchingUsers = false
                    _searchUserResults.value = listOf(toChatItem(chat))
                }
            }

            TdApi.Messages.CONSTRUCTOR -> {
                val messagesResult = result as TdApi.Messages
                val isFirstLoad = _oldestMessageId == 0L
                val wasLocalLoad = _lastLoadWasLocal
                val chatId = _currentChatId.value ?: 0
                val resultMessages = messagesResult.messages

                // Check if messages belong to current chat
                if (resultMessages.isNotEmpty() && resultMessages[0].chatId != chatId) {
                    _isLoadingMessages = false
                    _isLoadingHistory.value = false
                    return
                }

                val resultSize = resultMessages.size
                val newItems = resultMessages.map { msg -> toMessageItem(msg) }.reversed()

                if (isFirstLoad) {
                    _messages.value = newItems
                } else {
                    _messages.value = newItems + _messages.value
                }

                if (resultMessages.isNotEmpty()) {
                    _oldestMessageId = resultMessages.last().id
                }

                if (isFirstLoad && resultMessages.isNotEmpty()) {
                    val messageIds = resultMessages.map { it.id }.toLongArray()
                    client?.send(TdApi.ViewMessages(chatId, messageIds, TdApi.MessageSourceChatHistory(), true), null)
                }

                // Determine if there are more messages to load
                // TDLib returns 0 messages only when the end of history is reached
                _hasMoreMessages = resultSize > 0

                // Reset loading states
                _isLoadingMessages = false
                _isLoadingHistory.value = false

                // Handle local load fallback case: if local load returned nothing, try network load
                if (wasLocalLoad && isFirstLoad && resultSize == 0) {
                    _lastLoadWasLocal = false
                    _isLoadingMessages = true
                    client?.send(TdApi.GetChatHistory(chatId, _oldestMessageId, 0, 100, false), this)
                } else if (wasLocalLoad && isFirstLoad && resultSize > 0) {
                    // If we did a local load and got messages, also try to get more from network
                    _lastLoadWasLocal = false
                    _isLoadingMessages = true
                    _isLoadingHistory.value = true
                    client?.send(TdApi.GetChatHistory(chatId, _oldestMessageId, 0, 100, false), this)
                }
            }

            TdApi.Sessions.CONSTRUCTOR -> {
                val sessionsResult = result as TdApi.Sessions
                _inactiveSessionTtlDays.value = sessionsResult.inactiveSessionTtlDays
                _sessions.value = sessionsResult.sessions.map { session ->
                    top.noxc.wmessenger.ui.SessionItem(
                        id = session.id,
                        isCurrent = session.isCurrent,
                        deviceModel = session.deviceModel,
                        platform = session.platform,
                        applicationName = session.applicationName,
                        lastActiveDate = formatTimestamp(session.lastActiveDate),
                        canTerminate = !session.isCurrent
                    )
                }
            }

            TdApi.AddedProxy.CONSTRUCTOR -> {
                loadProxies()
            }

            TdApi.Message.CONSTRUCTOR -> {
            }

            TdApi.AddedProxies.CONSTRUCTOR -> {
                val proxiesResult = result as TdApi.AddedProxies
                _proxyList.value = proxiesResult.proxies.map { ap ->
                    val typeName = when (ap.proxy.type) {
                        is TdApi.ProxyTypeSocks5 -> "SOCKS5"
                        is TdApi.ProxyTypeHttp -> "HTTP"
                        is TdApi.ProxyTypeMtproto -> "MTProto"
                        else -> "Unknown"
                    }
                    ProxyItem(
                        id = ap.id,
                        server = ap.proxy.server,
                        port = ap.proxy.port,
                        type = typeName,
                        isEnabled = ap.isEnabled,
                        lastUsedDate = ap.lastUsedDate
                    )
                }
            }

            TdApi.Ok.CONSTRUCTOR -> {}

            TdApi.Message.CONSTRUCTOR -> {
                val message = result as TdApi.Message
                if (message.chatId == _currentChatId.value) {
                    val replyMarkup = message.replyMarkup
                    if (replyMarkup is TdApi.ReplyMarkupShowKeyboard) {
                        _replyKeyboard.value = replyMarkup.rows.map { row ->
                            row.map { btn -> KeyboardButtonItem(btn.text) }
                        }
                    } else if (replyMarkup is TdApi.ReplyMarkupRemoveKeyboard) {
                        _replyKeyboard.value = emptyList()
                    }
                    if (replyMarkup is TdApi.ReplyMarkupInlineKeyboard) {
                        _inlineKeyboard.value = Triple(
                            message.chatId,
                            message.id,
                            replyMarkup.rows.map { row ->
                                row.map { btn ->
                                    val type = btn.type
                                    InlineButtonItem(
                                        btn.text,
                                        when (type) {
                                            is TdApi.InlineKeyboardButtonTypeCallback -> type.data
                                            is TdApi.InlineKeyboardButtonTypeCallbackWithPassword -> type.data
                                            else -> null
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }

            else -> {}
        }
    }

    private fun formatConnectionState(state: TdApi.ConnectionState): String {
        return when (state.constructor) {
            TdApi.ConnectionStateWaitingForNetwork.CONSTRUCTOR -> "Waiting for network"
            TdApi.ConnectionStateConnectingToProxy.CONSTRUCTOR -> "Connecting to proxy"
            TdApi.ConnectionStateConnecting.CONSTRUCTOR -> "Connecting"
            TdApi.ConnectionStateUpdating.CONSTRUCTOR -> "Updating"
            TdApi.ConnectionStateReady.CONSTRUCTOR -> "Connected"
            else -> "Unknown"
        }
    }

    private fun handleAuthState(state: TdApi.AuthorizationState) {
        when (state.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                val databaseDir = filesDir.absolutePath + "/tdlib_$accountIndex"
                val filesDirectory = cacheDir.absolutePath + "/tdlib_$accountIndex"
                val deviceModel = android.os.Build.MODEL
                val systemVersion = android.os.Build.VERSION.RELEASE
                val appVersion = BuildConfig.VERSION_NAME
                client?.send(
                    TdApi.SetTdlibParameters(
                        false,
                        databaseDir,
                        filesDirectory,
                        null,
                        true,
                        true,
                        true,
                        true,
                        API_ID,
                        API_HASH,
                        "zh-hans",
                        deviceModel,
                        systemVersion,
                        appVersion
                    ),
                    this
                )
                restoreProxy()
            }

            TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR -> {
                val link = (state as TdApi.AuthorizationStateWaitOtherDeviceConfirmation).link
                if (link.isNotEmpty()) {
                    Log.d(TAG, "QR Code link received")
                    _qrCodeLink.value = link
                }
            }

            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                Log.d(TAG, "Authorization ready!")
                loadProxies()
                fetchCurrentUser()
            }

            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                Log.d(TAG, "Client closed")
                client = null
                closedLatch?.countDown()
            }

            else -> {}
        }
    }

    private fun restoreProxy() {
        val allProxies = proxyRepository.getAllProxies()
        val enabledProxy = allProxies.find { it.enabled } ?: return
        
        when (enabledProxy.type) {
            "SOCKS5" -> addSocks5Proxy(enabledProxy.server, enabledProxy.port, enabledProxy.username, enabledProxy.password)
            "HTTP" -> addHttpProxy(enabledProxy.server, enabledProxy.port, enabledProxy.username, enabledProxy.password)
            "MTProto" -> addMtprotoProxy(enabledProxy.server, enabledProxy.port, enabledProxy.secret)
        }
    }

    private fun fetchCurrentUser() {
        client?.send(TdApi.GetMe(), object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                if (result is TdApi.User) {
                    val name = listOf(result.firstName, result.lastName)
                        .filter { it.isNotEmpty() }
                        .joinToString(" ")
                    if (name.isNotEmpty()) {
                        onUserNameReady?.invoke(name)
                    }
                }
            }
        })
    }

    fun sendPhoneNumber(phoneNumber: String) {
        client?.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), this)
    }

    fun sendVerificationCode(code: String) {
        client?.send(TdApi.CheckAuthenticationCode(code), this)
    }

    fun sendPassword(password: String) {
        client?.send(TdApi.CheckAuthenticationPassword(password), this)
    }

    fun requestQrCode() {
        client?.send(TdApi.RequestQrCodeAuthentication(), this)
    }

    fun setEmailAddress(email: String) {
        client?.send(TdApi.SetAuthenticationEmailAddress(email), this)
    }

    fun checkEmailCode(code: String) {
        client?.send(TdApi.CheckAuthenticationEmailCode(TdApi.EmailAddressAuthenticationCode(code)), this)
    }

    fun registerUser(firstName: String, lastName: String) {
        client?.send(TdApi.RegisterUser(firstName, lastName, true), this)
    }

    fun registerPushNotifications(fcmToken: String) {
        val deviceToken = TdApi.DeviceTokenFirebaseCloudMessaging(fcmToken, false)
        client?.send(TdApi.RegisterDevice(deviceToken, longArrayOf()), this)
    }

    fun addSocks5Proxy(server: String, port: Int, username: String, password: String) {
        proxyRepository.saveProxy("SOCKS5", server, port, username, password, "")
        val proxy = TdApi.Proxy(server, port, TdApi.ProxyTypeSocks5(username, password))
        client?.send(TdApi.AddProxy(proxy, true), this)
    }

    fun addHttpProxy(server: String, port: Int, username: String, password: String) {
        proxyRepository.saveProxy("HTTP", server, port, username, password, "")
        val proxy = TdApi.Proxy(server, port, TdApi.ProxyTypeHttp(username, password, false))
        client?.send(TdApi.AddProxy(proxy, true), this)
    }

    fun addMtprotoProxy(server: String, port: Int, secret: String) {
        proxyRepository.saveProxy("MTProto", server, port, "", "", secret)
        val proxy = TdApi.Proxy(server, port, TdApi.ProxyTypeMtproto(secret))
        client?.send(TdApi.AddProxy(proxy, true), this)
    }

    fun enableProxy(proxyId: Int) {
        client?.send(TdApi.EnableProxy(proxyId), this)
    }

    fun disableProxy() {
        client?.send(TdApi.DisableProxy(), this)
    }

    fun removeProxy(proxyId: Int) {
        proxyRepository.removeProxy(proxyId)
        client?.send(TdApi.RemoveProxy(proxyId), this)
    }

    fun pingProxy(proxyId: Int, callback: (Int) -> Unit) {
        val proxy = getProxyById(proxyId)
        client?.send(TdApi.PingProxy(proxy), object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                if (result is TdApi.Seconds) {
                    callback((result.seconds * 1000).toInt())
                } else {
                    callback(-1)
                }
            }
        })
    }

    private fun getProxyById(proxyId: Int): TdApi.Proxy? {
        return _proxyList.value.find { it.id == proxyId }?.let { item ->
            val type = when (item.type) {
                "SOCKS5" -> TdApi.ProxyTypeSocks5("", "")
                "HTTP" -> TdApi.ProxyTypeHttp("", "", false)
                "MTProto" -> TdApi.ProxyTypeMtproto("")
                else -> TdApi.ProxyTypeSocks5("", "")
            }
            TdApi.Proxy(item.server, item.port, type)
        }
    }

    fun loadProxies() {
        client?.send(TdApi.GetProxies(), this)
    }

    fun logout() {
        client?.send(TdApi.LogOut()) {}
    }

    fun close() {
        val latch = CountDownLatch(1)
        closedLatch = latch
        client?.send(TdApi.Close()) {
            latch.countDown()
        }
        try {
            latch.await(3, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        client = null
        closedLatch = null
    }

    fun loadSessions() {
        client?.send(TdApi.GetActiveSessions(), this)
    }

    fun terminateSession(sessionId: Long) {
        client?.send(TdApi.TerminateSession(sessionId)) { result ->
            if (result is TdApi.Ok) {
                loadSessions()
            }
        }
    }

    fun terminateAllOtherSessions() {
        client?.send(TdApi.TerminateAllOtherSessions()) { result ->
            if (result is TdApi.Ok) {
                loadSessions()
            }
        }
    }

    fun setInactiveSessionTtl(days: Int) {
        client?.send(TdApi.SetInactiveSessionTtl(days)) { result ->
            if (result is TdApi.Ok) {
                loadSessions()
            }
        }
    }

    fun loadChats() {
        client?.send(TdApi.LoadChats(null, 100), this)
    }

    private fun loadChatsByIds(chatIds: LongArray) {
        chatIds.forEach { chatId ->
            client?.send(TdApi.GetChat(chatId), this)
        }
    }

    private fun updateChat(chat: TdApi.Chat) {
        val item = toChatItem(chat)
        val hasArchivePosition = chat.positions?.any { it.list is TdApi.ChatListArchive } == true
        val hasMainPosition = chat.positions?.any { it.list is TdApi.ChatListMain } == true

        if (hasArchivePosition) {
            val currentList = _archivedChats.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.id == chat.id }
            if (existingIndex >= 0) {
                currentList[existingIndex] = item
            } else {
                currentList.add(item)
            }
            currentList.sortByDescending { it.order }
            _archivedChats.value = currentList
        } else {
            val currentList = _archivedChats.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.id == chat.id }
            if (existingIndex >= 0) {
                currentList.removeAt(existingIndex)
                _archivedChats.value = currentList
            }
        }

        if (hasMainPosition) {
            val currentList = _chats.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.id == chat.id }
            if (existingIndex >= 0) {
                currentList[existingIndex] = item
            } else {
                currentList.add(item)
            }
            currentList.sortByDescending { it.order }
            _chats.value = currentList
        } else {
            val currentList = _chats.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.id == chat.id }
            if (existingIndex >= 0) {
                currentList.removeAt(existingIndex)
                _chats.value = currentList
            }
        }

        _chatNames[chat.id] = chat.title

        if (_currentChatId.value == chat.id) {
            val canSend = checkCanSend(chat)
            _canSendMessages.value = canSend
            _lastReadOutboxMessageId = chat.lastReadOutboxMessageId
            updateMessagesReadState()

            if (chat.type is TdApi.ChatTypePrivate) {
                val userId = (chat.type as TdApi.ChatTypePrivate).userId
                client?.send(TdApi.GetUser(userId), object : Client.ResultHandler {
                    override fun onResult(result: TdApi.Object) {
                        if (result is TdApi.User) {
                            if (result.type is TdApi.UserTypeBot) {
                                fetchBotCommands(chat.id, userId)
                            }
                            _userNames[result.id] = formatUserName(result)
                            handleUserUpdate(result)
                            _userOnlineStatus[result.id] = result.status is TdApi.UserStatusOnline
                            refreshChatsWithUser(result.id)
                        }
                    }
                })
            }

            if (chat.replyMarkupMessageId != 0L) {
                client?.send(TdApi.GetMessage(chat.id, chat.replyMarkupMessageId), this)
            }
        }
    }

    private fun handleUserUpdate(user: TdApi.User) {
        val smallFile = user.profilePhoto?.small
        val avatarFileId = smallFile?.id ?: 0
        if (avatarFileId != 0) {
            _userAvatarFileIds[user.id] = avatarFileId
            val localPath = smallFile?.local?.path ?: ""
            if (localPath.isNotEmpty()) {
                _filePaths[avatarFileId] = localPath
            } else if (!_filePaths.containsKey(avatarFileId)) {
                downloadFile(avatarFileId)
            }
        }
    }

    private fun refreshChatsWithUser(userId: Long) {
        val currentList = _chats.value.toMutableList()
        var changed = false
        for (i in currentList.indices) {
            val chatUserId = _chatUserIds[currentList[i].id]
            if (chatUserId == userId) {
                val isOnline = _userOnlineStatus[userId] == true
                if (currentList[i].isOnline != isOnline) {
                    currentList[i] = currentList[i].copy(isOnline = isOnline)
                    changed = true
                }
            }
        }
        if (changed) {
            _chats.value = currentList
        }
    }

    private fun refreshContactsWithUser(userId: Long) {
        val currentList = _contacts.value.toMutableList()
        var changed = false
        for (i in currentList.indices) {
            if (currentList[i].userId == userId) {
                val isOnline = _userOnlineStatus[userId] == true
                val avatarFileId = _userAvatarFileIds[userId] ?: 0
                val avatarLocalPath = if (avatarFileId != 0) _filePaths[avatarFileId] else null
                currentList[i] = currentList[i].copy(
                    isOnline = isOnline,
                    avatarFileId = avatarFileId,
                    avatarLocalPath = avatarLocalPath
                )
                changed = true
            }
        }
        if (changed) {
            _contacts.value = currentList
        }
    }

    private fun refreshChatItem(chatId: Long) {
        val currentList = _chats.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == chatId }
        if (index >= 0) {
            val isTyping = _chatTypingStatus[chatId] == true
            currentList[index] = currentList[index].copy(isTyping = isTyping)
            _chats.value = currentList
        }
    }

    private fun refreshAvatarWithFile(fileId: Int) {
        val localPath = _filePaths[fileId] ?: return
        val currentList = _chats.value.toMutableList()
        var changed = false
        for (i in currentList.indices) {
            if (currentList[i].avatarFileId == fileId && currentList[i].avatarLocalPath != localPath) {
                currentList[i] = currentList[i].copy(avatarLocalPath = localPath)
                changed = true
            }
        }
        if (changed) {
            _chats.value = currentList
        }
        val currentContacts = _contacts.value.toMutableList()
        var contactsChanged = false
        for (i in currentContacts.indices) {
            if (currentContacts[i].avatarFileId == fileId && currentContacts[i].avatarLocalPath != localPath) {
                currentContacts[i] = currentContacts[i].copy(avatarLocalPath = localPath)
                contactsChanged = true
            }
        }
        if (contactsChanged) {
            _contacts.value = currentContacts
        }
    }

    private fun checkCanSend(chat: TdApi.Chat): Boolean {
        return when (val type = chat.type) {
            is TdApi.ChatTypeSupergroup -> {
                if (type.isChannel) false
                else chat.permissions?.canSendBasicMessages ?: true
            }
            is TdApi.ChatTypeBasicGroup -> chat.permissions?.canSendBasicMessages ?: true
            else -> true
        }
    }

    private fun updateChatPosition(chatId: Long, position: TdApi.ChatPosition) {
        val currentList = _chats.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == chatId }
        if (position.order == 0L) {
            if (index >= 0) {
                currentList.removeAt(index)
                _chats.value = currentList
            }
            return
        }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(order = position.order)
        } else {
            client?.send(TdApi.GetChat(chatId), this)
            return
        }
        currentList.sortByDescending { it.order }
        _chats.value = currentList
    }

    private fun updateArchivedChatPosition(chatId: Long, position: TdApi.ChatPosition) {
        val currentList = _archivedChats.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == chatId }
        if (position.order == 0L) {
            if (index >= 0) {
                currentList.removeAt(index)
                _archivedChats.value = currentList
            }
            return
        }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(order = position.order)
        } else {
            client?.send(TdApi.GetChat(chatId), object : Client.ResultHandler {
                override fun onResult(result: TdApi.Object) {
                    if (result is TdApi.Chat) {
                        val item = toChatItem(result)
                        val updated = _archivedChats.value.toMutableList()
                        val existingIdx = updated.indexOfFirst { it.id == result.id }
                        if (existingIdx >= 0) {
                            updated[existingIdx] = item
                        } else {
                            updated.add(item)
                        }
                        updated.sortByDescending { it.order }
                        _archivedChats.value = updated
                    }
                }
            })
            return
        }
        currentList.sortByDescending { it.order }
        _archivedChats.value = currentList
    }

    fun loadArchivedChats() {
        client?.send(TdApi.LoadChats(TdApi.ChatListArchive(), 100), this)
    }

    fun muteAllChats() {
        val privateScope = TdApi.NotificationSettingsScopePrivateChats()
        val privateSettings = TdApi.ScopeNotificationSettings()
        privateSettings.muteFor = 2147483647
        client?.send(TdApi.SetScopeNotificationSettings(privateScope, privateSettings), null)

        val groupScope = TdApi.NotificationSettingsScopeGroupChats()
        val groupSettings = TdApi.ScopeNotificationSettings()
        groupSettings.muteFor = 2147483647
        client?.send(TdApi.SetScopeNotificationSettings(groupScope, groupSettings), null)

        val channelScope = TdApi.NotificationSettingsScopeChannelChats()
        val channelSettings = TdApi.ScopeNotificationSettings()
        channelSettings.muteFor = 2147483647
        client?.send(TdApi.SetScopeNotificationSettings(channelScope, channelSettings), null)
    }

    fun unmuteAllChats() {
        val privateScope = TdApi.NotificationSettingsScopePrivateChats()
        val privateSettings = TdApi.ScopeNotificationSettings()
        privateSettings.muteFor = 0
        client?.send(TdApi.SetScopeNotificationSettings(privateScope, privateSettings), null)

        val groupScope = TdApi.NotificationSettingsScopeGroupChats()
        val groupSettings = TdApi.ScopeNotificationSettings()
        groupSettings.muteFor = 0
        client?.send(TdApi.SetScopeNotificationSettings(groupScope, groupSettings), null)

        val channelScope = TdApi.NotificationSettingsScopeChannelChats()
        val channelSettings = TdApi.ScopeNotificationSettings()
        channelSettings.muteFor = 0
        client?.send(TdApi.SetScopeNotificationSettings(channelScope, channelSettings), null)
    }

    private fun updateChatTitle(chatId: Long, title: String) {
        val currentList = _chats.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == chatId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(title = title)
            _chats.value = currentList
        }
        _chatNames[chatId] = title
    }

    private fun updateChatLastMessage(chatId: Long, message: TdApi.Message?, positions: Array<TdApi.ChatPosition>) {
        val currentList = _chats.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == chatId }
        if (index >= 0) {
            var updated = currentList[index]
            if (message != null) {
                updated = updated.copy(lastMessage = getMessageText(message))
            }
            val mainPos = positions.find { it.list is TdApi.ChatListMain }
            if (mainPos != null) {
                updated = updated.copy(order = mainPos.order)
            }
            currentList[index] = updated
            currentList.sortByDescending { it.order }
            _chats.value = currentList
        }
    }

    fun openChat(chatId: Long) {
        _currentChatId.value?.let { oldChatId ->
            if (oldChatId != chatId) {
                client?.send(TdApi.CloseChat(oldChatId), null)
            }
        }
        _currentChatId.value = chatId
        _oldestMessageId = 0
        _hasMoreMessages = true
        _isLoadingMessages = true
        _isLoadingHistory.value = false
        _lastReadOutboxMessageId = 0
        _lastLoadWasLocal = true
        _botCommands.value = emptyList()
        _replyKeyboard.value = emptyList()
        _inlineKeyboard.value = Triple(0, 0, emptyList())
        _messages.value = emptyList()
        client?.send(TdApi.GetChat(chatId), this)
        client?.send(TdApi.OpenChat(chatId), null)
        client?.send(TdApi.GetChatHistory(chatId, 0, 0, 100, true), this)
    }

    fun closeChat() {
        _currentChatId.value?.let { chatId ->
            client?.send(TdApi.CloseChat(chatId), this)
        }
        _currentChatId.value = null
        _botCommands.value = emptyList()
        _replyKeyboard.value = emptyList()
        _inlineKeyboard.value = Triple(0, 0, emptyList())
    }

    fun loadMessages(chatId: Long) {
        if (_isLoadingMessages) return
        _isLoadingMessages = true
        _lastLoadWasLocal = true
        client?.send(TdApi.GetChatHistory(chatId, _oldestMessageId, 0, 50, true), this)
    }

    fun loadMoreMessages(chatId: Long) {
        if (!_hasMoreMessages || _isLoadingMessages || _currentChatId.value != chatId) return
        _isLoadingMessages = true
        _isLoadingHistory.value = true
        _lastLoadWasLocal = false
        // Load messages older than _oldestMessageId
        client?.send(TdApi.GetChatHistory(chatId, _oldestMessageId, 0, 100, false), this)
    }

    fun sendMessage(chatId: Long, text: String) {
        val inputContent = TdApi.InputMessageText(TdApi.FormattedText(text, emptyArray()), null, false)
        client?.send(TdApi.SendMessage(chatId, null, null, null, null, inputContent), this)
        client?.send(TdApi.SendChatAction(chatId, null, null, TdApi.ChatActionCancel()), null)
    }

    fun sendMessageReply(chatId: Long, replyToMessageId: Long, text: String) {
        val inputContent = TdApi.InputMessageText(TdApi.FormattedText(text, emptyArray()), null, false)
        val replyTo = TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0, "")
        client?.send(TdApi.SendMessage(chatId, null, replyTo, null, null, inputContent), this)
        client?.send(TdApi.SendChatAction(chatId, null, null, TdApi.ChatActionCancel()), null)
    }

    fun sendChatTyping(chatId: Long) {
        client?.send(TdApi.SendChatAction(chatId, null, null, TdApi.ChatActionTyping()), null)
    }

    fun deleteMessage(chatId: Long, messageId: Long) {
        client?.send(TdApi.DeleteMessages(chatId, longArrayOf(messageId), false), null)
    }

    private fun fetchBotCommands(chatId: Long, botUserId: Long) {
        client?.send(TdApi.GetUserFullInfo(botUserId), object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                if (result is TdApi.UserFullInfo) {
                    val botInfo = result.botInfo
                    if (botInfo != null && botInfo.commands.isNotEmpty()) {
                        _botCommands.value = botInfo.commands.map {
                            BotCommandItem(it.command, it.description)
                        }
                    }
                }
            }
        })
    }

    fun clickInlineButton(chatId: Long, messageId: Long, data: ByteArray) {
        client?.send(
            TdApi.GetCallbackQueryAnswer(
                chatId,
                messageId,
                TdApi.CallbackQueryPayloadData(data)
            ),
            this
        )
    }

    fun downloadFile(fileId: Int) {
        if (_filePaths.containsKey(fileId)) return
        client?.send(TdApi.DownloadFile(fileId, 32, 0, 0, true), this)
    }

    private var _isSearching = false
    private var _isSearchingUsers = false

    fun searchChats(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        _isSearching = true
        client?.send(TdApi.SearchChatsOnServer(query, 50), this)
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchUserResults.value = emptyList()
            return
        }
        _isSearchingUsers = true
        client?.send(TdApi.SearchPublicChat(query), this)
    }

    fun searchPublicChat(query: String, callback: (Long) -> Unit) {
        client?.send(TdApi.SearchPublicChat(query), object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                if (result is TdApi.Chat) {
                    callback(result.id)
                }
            }
        })
    }

    fun searchChatByUsername(username: String): Long? {
        var resultChatId: Long? = null
        val latch = CountDownLatch(1)
        client?.send(TdApi.SearchPublicChat(username), object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                if (result is TdApi.Chat) {
                    resultChatId = result.id
                }
                latch.countDown()
            }
        })
        latch.await(5, TimeUnit.SECONDS)
        return resultChatId
    }

    fun loadContacts() {
        client?.send(TdApi.GetContacts(), this)
    }

    fun sendPhoto(chatId: Long, path: String) {
        val photo = TdApi.InputMessagePhoto(
            TdApi.InputFileLocal(path),
            null,
            TdApi.InputFileLocal(path),
            IntArray(0),
            800,
            800,
            TdApi.FormattedText("", emptyArray()),
            false,
            null,
            false
        )
        client?.send(TdApi.SendMessage(chatId, null, null, null, null, photo), null)
    }

    fun sendVideo(chatId: Long, path: String) {
        val video = TdApi.InputMessageVideo(
            TdApi.InputFileLocal(path),
            null,
            TdApi.InputFileLocal(path),
            0,
            IntArray(0),
            0,
            800,
            800,
            false,
            TdApi.FormattedText("", emptyArray()),
            false,
            null,
            false
        )
        client?.send(TdApi.SendMessage(chatId, null, null, null, null, video), null)
    }

    fun loadMedia() {
        val items = mutableListOf<MediaItem>()
        try {
            val imageProjection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
            val imageCursor: Cursor? = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 100"
            )
            imageCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    ).toString()
                    val name = cursor.getString(nameCol) ?: "Photo"
                    items.add(MediaItem(uri, false, name))
                }
            }
        } catch (_: Exception) {
        }

        try {
            val videoProjection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME)
            val videoCursor: Cursor? = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC LIMIT 50"
            )
            videoCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    ).toString()
                    val name = cursor.getString(nameCol) ?: "Video"
                    items.add(MediaItem(uri, true, name))
                }
            }
        } catch (_: Exception) {
        }

        _mediaList.value = items
    }

    private fun addMessage(message: TdApi.Message) {
        if (_currentChatId.value != message.chatId) return
        val currentList = _messages.value.toMutableList()
        val item = toMessageItem(message)
        val existingIndex = currentList.indexOfFirst { it.id == message.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = item
        } else {
            currentList.add(item)
        }
        currentList.sortBy { it.date }
        _messages.value = currentList

        val replyMarkup = message.replyMarkup
        if (replyMarkup is TdApi.ReplyMarkupShowKeyboard) {
            _replyKeyboard.value = replyMarkup.rows.map { row ->
                row.map { btn -> KeyboardButtonItem(btn.text) }
            }
        } else if (replyMarkup is TdApi.ReplyMarkupRemoveKeyboard) {
            _replyKeyboard.value = emptyList()
        }
        if (replyMarkup is TdApi.ReplyMarkupInlineKeyboard) {
            _inlineKeyboard.value = Triple(
                message.chatId,
                message.id,
                replyMarkup.rows.map { row ->
                    row.map { btn ->
                        val type = btn.type
                        InlineButtonItem(
                            btn.text,
                            when (type) {
                                is TdApi.InlineKeyboardButtonTypeCallback -> type.data
                                is TdApi.InlineKeyboardButtonTypeCallbackWithPassword -> type.data
                                else -> null
                            }
                        )
                    }
                }
            )
        }
    }

    private fun updateMessageContent(chatId: Long, messageId: Long, newContent: TdApi.MessageContent) {
        if (_currentChatId.value != chatId) return
        val currentList = _messages.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(text = formatMessageContent(newContent))
            _messages.value = currentList
        }
    }

    private fun removeMessages(chatId: Long, messageIds: LongArray) {
        if (_currentChatId.value != chatId) return
        val currentList = _messages.value.toMutableList()
        currentList.removeAll { it.id in messageIds }
        _messages.value = currentList
    }

    private fun updateMessagesReadState() {
        val currentList = _messages.value.toMutableList()
        var changed = false
        for (i in currentList.indices) {
            val msg = currentList[i]
            if (msg.isOutgoing && msg.id <= _lastReadOutboxMessageId && !msg.isRead) {
                currentList[i] = msg.copy(isRead = true)
                changed = true
            }
        }
        if (changed) {
            _messages.value = currentList
        }
    }

    private fun refreshMessagesWithFile(fileId: Int) {
        val currentList = _messages.value.toMutableList()
        var changed = false
        for (i in currentList.indices) {
            val msg = currentList[i]
            if (msg.photoFileId == fileId || msg.videoFileId == fileId) {
                val path = _filePaths[fileId]
                currentList[i] = msg.copy(
                    photoLocalPath = if (msg.photoFileId == fileId) path else msg.photoLocalPath,
                    videoLocalPath = if (msg.videoFileId == fileId) path else msg.videoLocalPath
                )
                changed = true
            }
        }
        if (changed) {
            _messages.value = currentList
        }
    }

    private fun toChatItem(chat: TdApi.Chat): ChatItem {
        val mainPosition = chat.positions?.firstOrNull { it.list is TdApi.ChatListMain }
        val order = mainPosition?.order ?: 0L
        val userId = (chat.type as? TdApi.ChatTypePrivate)?.userId ?: 0L
        if (userId != 0L) {
            _chatUserIds[chat.id] = userId
        }
        _chatTypes[chat.id] = chat.type
        val smallFile = chat.photo?.small
        val avatarFileId = smallFile?.id ?: 0
        var avatarLocalPath: String? = null
        if (avatarFileId != 0) {
            val localPath = smallFile?.local?.path ?: ""
            if (localPath.isNotEmpty()) {
                _filePaths[avatarFileId] = localPath
                avatarLocalPath = localPath
            } else {
                avatarLocalPath = _filePaths[avatarFileId]
                downloadFile(avatarFileId)
            }
        }
        val isOnline = if (userId != 0L) _userOnlineStatus[userId] == true else false
        val isTyping = _chatTypingStatus[chat.id] == true
        return ChatItem(
            id = chat.id,
            title = chat.title,
            lastMessage = chat.lastMessage?.let { getMessageText(it) } ?: "",
            order = order,
            unreadCount = chat.unreadCount,
            avatarFileId = avatarFileId,
            avatarLocalPath = avatarLocalPath,
            isOnline = isOnline,
            isTyping = isTyping
        )
    }

    private fun toMessageItem(message: TdApi.Message): MessageItem {
        val senderName = resolveSenderName(message.senderId)
        var photoUrl: String? = null
        var photoFileId: Int? = null
        var photoLocalPath: String? = null
        var videoFileId: Int? = null
        var videoLocalPath: String? = null
        var videoWidth = 0
        var videoHeight = 0
        val text: String

        when (message.content.constructor) {
            TdApi.MessagePhoto.CONSTRUCTOR -> {
                val photoMsg = message.content as TdApi.MessagePhoto
                val sizes = photoMsg.photo.sizes
                var bestSize: TdApi.PhotoSize? = null
                if (sizes != null) {
                    for (s in sizes) {
                        if (bestSize == null || s.width * s.height > bestSize.width * bestSize.height) {
                            bestSize = s
                        }
                    }
                }
                if (bestSize != null) {
                    photoFileId = bestSize.photo.id
                    val localPath = bestSize.photo.local.path
                    if (localPath.isNotEmpty()) {
                        _filePaths[bestSize.photo.id] = localPath
                        photoLocalPath = localPath
                    } else {
                        downloadFile(bestSize.photo.id)
                    }
                    photoUrl = "[Photo ${bestSize.width}x${bestSize.height}]"
                } else {
                    photoUrl = "[Photo]"
                }
                text = photoMsg.caption?.text ?: ""
            }
            TdApi.MessageVideo.CONSTRUCTOR -> {
                val videoMsg = message.content as TdApi.MessageVideo
                videoWidth = videoMsg.video.width
                videoHeight = videoMsg.video.height
                videoFileId = videoMsg.video.video.id
                val localPath = videoMsg.video.video.local.path
                if (localPath.isNotEmpty()) {
                    _filePaths[videoMsg.video.video.id] = localPath
                    videoLocalPath = localPath
                } else {
                    downloadFile(videoMsg.video.video.id)
                }
                photoUrl = "[Video ${videoWidth}x${videoHeight}]"
                text = videoMsg.caption?.text ?: ""
            }
            else -> {
                text = formatMessageContent(message.content)
            }
        }

        return MessageItem(
            id = message.id,
            senderName = senderName,
            text = text,
            photoUrl = photoUrl,
            photoFileId = photoFileId,
            photoLocalPath = photoLocalPath,
            videoFileId = videoFileId,
            videoLocalPath = videoLocalPath,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            date = message.date,
            isOutgoing = message.isOutgoing,
            isRead = message.isOutgoing && message.id <= _lastReadOutboxMessageId
        )
    }

    private fun extractRecoveryPin(message: TdApi.Message) {
        val text = getMessageText(message)
        val regex = Regex("\\b(\\d{6})\\b")
        val match = regex.find(text)
        if (match != null) {
            _recoveryPin.value = match.groupValues[1]
            recoveryPinTimestamp = System.currentTimeMillis()
        }
    }

    private fun triggerNotificationForMessage(message: TdApi.Message) {
        if (onNewMessageForNotification == null) {
            Log.d(TAG, "Notification callback is null")
            return
        }

        val currentChatId = _currentChatId.value
        if (currentChatId == message.chatId) {
            Log.d(TAG, "Skipping notification for current chat ${message.chatId}")
            return
        }

        val chat = _chats.value.find { it.id == message.chatId }
        val chatType = _chatTypes[message.chatId]
        
        if (chat != null) {
            val isGroup = chatType !is TdApi.ChatTypePrivate
            val senderName = if (isGroup) resolveSenderName(message.senderId) else ""
            Log.d(TAG, "Sending notification: ${chat.title} - ${getMessageText(message)}")
            onNewMessageForNotification?.invoke(
                message.chatId,
                message.id,
                chat.title,
                senderName,
                getMessageText(message),
                isGroup
            )
        } else {
            client?.send(TdApi.GetChat(message.chatId), object : Client.ResultHandler {
                override fun onResult(result: TdApi.Object) {
                    if (result is TdApi.Chat) {
                        _chatTypes[message.chatId] = result.type
                        val isGroup = result.type !is TdApi.ChatTypePrivate
                        val senderName = if (isGroup) resolveSenderName(message.senderId) else ""
                        Log.d(TAG, "Sending notification (async): ${result.title} - ${getMessageText(message)}")
                        onNewMessageForNotification?.invoke(
                            message.chatId,
                            message.id,
                            result.title,
                            senderName,
                            getMessageText(message),
                            isGroup
                        )
                    }
                }
            })
        }
    }

    fun getValidRecoveryPin(): String? {
        val pin = _recoveryPin.value
        if (pin == null) return null
        if (System.currentTimeMillis() - recoveryPinTimestamp > RECOVERY_PIN_EXPIRE_MS) {
            _recoveryPin.value = null
            return null
        }
        return pin
    }

    private fun resolveSenderName(sender: TdApi.MessageSender?): String {
        return when (sender) {
            is TdApi.MessageSenderUser -> {
                _userNames[sender.userId] ?: run {
                    client?.send(TdApi.GetUser(sender.userId), this)
                    "User"
                }
            }
            is TdApi.MessageSenderChat -> {
                _chatNames[sender.chatId] ?: "Chat"
            }
            else -> ""
        }
    }

    private fun formatUserName(user: TdApi.User): String {
        val first = user.firstName ?: ""
        val last = user.lastName ?: ""
        return if (first.isNotEmpty() && last.isNotEmpty()) "$first $last"
        else if (first.isNotEmpty()) first
        else if (last.isNotEmpty()) last
        else user.phoneNumber ?: "User"
    }

    private fun formatTimestamp(timestamp: Int): String {
        if (timestamp == 0) return ""
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp.toLong() * 1000))
    }

    private fun getMessageText(message: TdApi.Message): String {
        return formatMessageContent(message.content)
    }

    private fun formatMessageContent(content: TdApi.MessageContent): String {
        return when (content.constructor) {
            TdApi.MessageText.CONSTRUCTOR -> (content as TdApi.MessageText).text.text
            TdApi.MessagePhoto.CONSTRUCTOR -> "[Photo]"
            TdApi.MessageVideo.CONSTRUCTOR -> "[Video]"
            TdApi.MessageVoiceNote.CONSTRUCTOR -> "[Voice]"
            TdApi.MessageAudio.CONSTRUCTOR -> "[Audio]"
            TdApi.MessageDocument.CONSTRUCTOR -> "[Document]"
            TdApi.MessageSticker.CONSTRUCTOR -> (content as TdApi.MessageSticker).sticker.emoji
            TdApi.MessageAnimation.CONSTRUCTOR -> "[GIF]"
            TdApi.MessageContact.CONSTRUCTOR -> "[Contact]"
            TdApi.MessageLocation.CONSTRUCTOR -> "[Location]"
            TdApi.MessageVideoNote.CONSTRUCTOR -> "[Video Note]"
            TdApi.MessagePoll.CONSTRUCTOR -> "[Poll]"
            TdApi.MessageDice.CONSTRUCTOR -> "[Dice]"
            else -> "[Message]"
        }
    }
}

data class ChatItem(
    val id: Long,
    val title: String,
    val lastMessage: String,
    val order: Long,
    val unreadCount: Int,
    val avatarFileId: Int = 0,
    val avatarLocalPath: String? = null,
    val isOnline: Boolean = false,
    val isTyping: Boolean = false
)

data class MessageItem(
    val id: Long,
    val senderName: String,
    val text: String,
    val photoUrl: String?,
    val photoFileId: Int?,
    val photoLocalPath: String?,
    val videoFileId: Int?,
    val videoLocalPath: String?,
    val videoWidth: Int,
    val videoHeight: Int,
    val date: Int,
    val isOutgoing: Boolean,
    val isRead: Boolean
)

data class ProxyItem(
    val id: Int,
    val server: String,
    val port: Int,
    val type: String,
    val isEnabled: Boolean,
    val lastUsedDate: Int,
    val pingMs: Int = 0
)

data class ContactItem(
    val userId: Long,
    val name: String,
    val avatarFileId: Int = 0,
    val avatarLocalPath: String? = null,
    val isOnline: Boolean = false
)

sealed class ProxyState {
    object None : ProxyState()
    data class Added(val proxy: TdApi.Proxy) : ProxyState()
}

data class BotCommandItem(
    val command: String,
    val description: String
)

data class KeyboardButtonItem(
    val text: String
)

data class InlineButtonItem(
    val text: String,
    val callbackData: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InlineButtonItem) return false
        return text == other.text
    }

    override fun hashCode(): Int = text.hashCode()
}

data class FreezeInfo(
    val freezingDate: Int,
    val deletionDate: Int,
    val appealLink: String
)
