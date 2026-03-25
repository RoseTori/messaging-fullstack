import { Component, type ReactNode, useEffect, useMemo, useRef, useState } from 'react'
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'


type UserStatus = 'ONLINE' | 'OFFLINE'
type ChatType = 'DIRECT' | 'GROUP'
type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM'
type ReceiptStatus = 'SENT' | 'DELIVERED' | 'READ'

type AuthResponse = { accessToken: string }
type User = { id: string; username: string; displayName: string; status: UserStatus }
type Chat = { id: string; type: ChatType; title: string | null; members: string[]; createdAt: string }
type Message = {
    id: string; chatId: string; senderId: string; type: MessageType
    cipherText: string; encryptedKey: string; nonce: string; algorithm: string
    metadata: string | null; clientMessageId: string | null; createdAt: string
}
type Receipt = { messageId: string; userId: string; status: ReceiptStatus; statusAt: string }
type TypingEvent = { chatId: string; userId: string; username: string; typing: boolean; expiresAt: string }
type PresenceEvent = { userId: string; online: boolean; timestamp: string }


declare global {
    interface Window { __APP_CONFIG__?: { apiBaseUrl?: string; wsUrl?: string } }
}

const defaultApiBaseUrl = window.location.origin
const defaultWsUrl = `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`
const config = {
    apiBaseUrl: window.__APP_CONFIG__?.apiBaseUrl ?? defaultApiBaseUrl,
    wsUrl: window.__APP_CONFIG__?.wsUrl ?? defaultWsUrl,
}


window.addEventListener('unhandledrejection', e => console.error('[Unhandled Rejection]', e.reason))
window.addEventListener('error', e => console.error('[Global Error]', e.message, e.error))


function randomId(): string {
    return typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : String(Date.now() + Math.random())
}

function initials(name: string): string {
    return (name || '?').split(' ').map(w => w[0] ?? '').join('').slice(0, 2).toUpperCase() || '?'
}

function sortByDate(msgs: Message[]): Message[] {
    return [...msgs].sort((a, b) => {
        const ta = a?.createdAt ? new Date(a.createdAt).getTime() : 0
        const tb = b?.createdAt ? new Date(b.createdAt).getTime() : 0
        return ta - tb
    })
}

function encodePayload(text: string) {
    return {
        cipherText: btoa(unescape(encodeURIComponent(text))),
        encryptedKey: btoa('test-key'),
        nonce: btoa(String(Date.now())),
        algorithm: 'TEST-BASE64',
        metadata: JSON.stringify({ preview: text.slice(0, 80) }),
    }
}

function decodePayload(msg: Message): string {
    try {
        if (msg.algorithm === 'TEST-BASE64') return decodeURIComponent(escape(atob(msg.cipherText)))
        return `[${msg.algorithm}] ${msg.cipherText}`
    } catch { return '[Unable to decode]' }
}

async function api<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
    const headers = new Headers(options.headers ?? {})
    if (!headers.has('Content-Type') && options.body) headers.set('Content-Type', 'application/json')
    if (token) headers.set('Authorization', `Bearer ${token}`)
    const res = await fetch(`${config.apiBaseUrl}${path}`, { ...options, headers })
    if (!res.ok) { const t = await res.text(); throw new Error(t || `HTTP ${res.status}`) }
    if (res.status === 204) return undefined as T
    return res.json() as Promise<T>
}


interface EBState { error: Error | null }

class ErrorBoundary extends Component<{ children: ReactNode }, EBState> {
    constructor(props: { children: ReactNode }) {
        super(props)
        this.state = { error: null }
    }
    static getDerivedStateFromError(error: Error): EBState { return { error } }
    componentDidCatch(error: Error, info: { componentStack: string }) {
        console.error('[ErrorBoundary]', error, info.componentStack)
    }
    render() {
        if (this.state.error) {
            return (
                <div className="crash-shell">
                    <div className="crash-card">
                        <div className="crash-title">// render error</div>
                        <div className="crash-message">{this.state.error.message}</div>
                        <button className="crash-btn" onClick={() => this.setState({ error: null })}>Dismiss</button>
                    </div>
                </div>
            )
        }
        return this.props.children
    }
}


export default function App() {
    return <ErrorBoundary><AppInner /></ErrorBoundary>
}

function AppInner() {
    const [mode, setMode] = useState<'login' | 'register'>('login')
    const [username, setUsername] = useState('')
    const [displayName, setDisplayName] = useState('')
    const [password, setPassword] = useState('')
    const [token, setToken] = useState(() => localStorage.getItem('token') ?? '')
    const [me, setMe] = useState<User | null>(null)
    const [users, setUsers] = useState<User[]>([])
    const [chats, setChats] = useState<Chat[]>([])
    const [selectedChatId, setSelectedChatId] = useState('')
    const [messagesByChat, setMessagesByChat] = useState<Record<string, Message[]>>({})
    const [receiptsByChat, setReceiptsByChat] = useState<Record<string, Receipt[]>>({})
    const [typingByChat, setTypingByChat] = useState<Record<string, TypingEvent[]>>({})
    const [presence, setPresence] = useState<Record<string, boolean>>({})
    const [text, setText] = useState('')
    const [groupName, setGroupName] = useState('')
    const [selectedMembers, setSelectedMembers] = useState<string[]>([])
    const [error, setError] = useState('')
    const [connected, setConnected] = useState(false)

    const clientRef = useRef<Client | null>(null)
    const subscriptionsRef = useRef<StompSubscription[]>([])
    const typingTimerRef = useRef<number | null>(null)
    const pingTimerRef = useRef<number | null>(null)
    const messagesEndRef = useRef<HTMLDivElement | null>(null)

    const userMap = useMemo(() => Object.fromEntries(users.map(u => [u.id, u])), [users])
    const selectedChat = useMemo(() => chats.find(c => c.id === selectedChatId) ?? null, [chats, selectedChatId])
    const visibleMessages = selectedChatId ? messagesByChat[selectedChatId] ?? [] : []
    const visibleTyping = selectedChatId
        ? (typingByChat[selectedChatId] ?? []).filter(t => t.typing && t.userId !== me?.id && new Date(t.expiresAt).getTime() > Date.now())
        : []

    useEffect(() => { messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [visibleMessages.length])

    useEffect(() => {
        if (token) {
            localStorage.setItem('token', token)
            void bootstrap(token)
        } else {
            localStorage.removeItem('token')
            setMe(null); setUsers([]); setChats([]); setSelectedChatId('')
            disconnectStomp()
        }
        return () => { disconnectStomp() }
    }, [token])

    useEffect(() => {
        if (!selectedChatId || !token) return
        void loadHistory(selectedChatId, token)
    }, [selectedChatId, token])

    useEffect(() => {
        if (!selectedChatId || !me) return
        const unread = (messagesByChat[selectedChatId] ?? []).filter(m => m.senderId !== me.id)
        unread.forEach(m => sendReceipt(selectedChatId, m.id, 'READ'))
    }, [messagesByChat, selectedChatId, me])

    async function bootstrap(activeToken: string) {
        try {
            setError('')
            const [meData, userList, chatList] = await Promise.all([
                api<User>('/api/users/me', {}, activeToken),
                api<User[]>('/api/users', {}, activeToken),
                api<Chat[]>('/api/chats', {}, activeToken),
            ])
            setMe(meData)
            setUsers(userList)
            setChats(chatList)
            if (!selectedChatId && chatList[0]) setSelectedChatId(chatList[0].id)
            connectStomp(activeToken, meData.id, chatList)
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to load')
            setToken('')
        }
    }

    function disconnectStomp() {
        subscriptionsRef.current.forEach(s => s.unsubscribe())
        subscriptionsRef.current = []
        if (pingTimerRef.current) { window.clearInterval(pingTimerRef.current); pingTimerRef.current = null }
        if (typingTimerRef.current) { window.clearTimeout(typingTimerRef.current); typingTimerRef.current = null }
        clientRef.current?.deactivate()
        clientRef.current = null
        setConnected(false)
    }

    function connectStomp(activeToken: string, myUserId: string, currentChats: Chat[]) {
        disconnectStomp()
        const client = new Client({
            brokerURL: config.wsUrl,
            connectHeaders: { Authorization: `Bearer ${activeToken}` },
            reconnectDelay: 3000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            debug: () => {},
        })

        client.onConnect = () => {
            setConnected(true)
            const subs: StompSubscription[] = []

            subs.push(client.subscribe('/topic/presence', frame => {
                try {
                    const event = JSON.parse(frame.body) as PresenceEvent
                    setPresence(prev => ({ ...prev, [event.userId]: event.online }))
                } catch (e) { console.error('[STOMP presence]', e, frame.body) }
            }))

            subs.push(client.subscribe('/user/queue/messages', frame => {
                try {
                    const msg = JSON.parse(frame.body) as Message
                    upsertMessage(msg)
                    if (msg.senderId !== myUserId) sendReceipt(msg.chatId, msg.id, 'DELIVERED')
                } catch (e) { console.error('[STOMP /user/queue/messages]', e, frame.body) }
            }))

            subs.push(client.subscribe('/user/queue/ack', frame => {
                try {
                    const msg = JSON.parse(frame.body) as Message
                    upsertMessage(msg)
                } catch (e) { console.error('[STOMP /user/queue/ack]', e, frame.body) }
            }))

            currentChats.forEach(chat => subscribeToChat(client, chat.id, subs, myUserId))
            subscriptionsRef.current = subs

            client.publish({ destination: '/app/presence.ping', body: '{}' })
            pingTimerRef.current = window.setInterval(() => {
                client.publish({ destination: '/app/presence.ping', body: '{}' })
            }, 15000)
        }

        client.onWebSocketClose = () => setConnected(false)
        client.onStompError = frame => setError(frame.headers['message'] ?? frame.body ?? 'WebSocket error')
        client.activate()
        clientRef.current = client
    }

    function subscribeToChat(client: Client, chatId: string, subs: StompSubscription[], myUserId: string) {
        subs.push(client.subscribe(`/topic/chat.${chatId}`, (frame: IMessage) => {
            try {
                const msg = JSON.parse(frame.body) as Message
                upsertMessage(msg)
                if (msg.senderId !== myUserId) sendReceipt(chatId, msg.id, 'DELIVERED')
            } catch (e) { console.error(`[STOMP chat.${chatId}]`, e, frame.body) }
        }))

        subs.push(client.subscribe(`/topic/chat.${chatId}.typing`, (frame: IMessage) => {
            try {
                const event = JSON.parse(frame.body) as TypingEvent
                setTypingByChat(prev => {
                    const current = (prev[chatId] ?? []).filter(i => i.userId !== event.userId)
                    return { ...prev, [chatId]: [...current, event] }
                })
            } catch (e) { console.error(`[STOMP chat.${chatId}.typing]`, e, frame.body) }
        }))

        subs.push(client.subscribe(`/topic/chat.${chatId}.receipts`, (frame: IMessage) => {
            try {
                const receipt = JSON.parse(frame.body) as Receipt
                setReceiptsByChat(prev => {
                    const current = (prev[chatId] ?? []).filter(
                        r => !(r.messageId === receipt.messageId && r.userId === receipt.userId)
                    )
                    return { ...prev, [chatId]: [...current, receipt] }
                })
            } catch (e) { console.error(`[STOMP chat.${chatId}.receipts]`, e, frame.body) }
        }))
    }

    function upsertMessage(msg: Message) {
        setMessagesByChat(prev => {
            const current = prev[msg.chatId] ?? []
            if (current.some(m => m.id === msg.id || (m.clientMessageId && m.clientMessageId === msg.clientMessageId))) {
                return prev
            }
            return { ...prev, [msg.chatId]: sortByDate([...current, msg]) }
        })
    }

    async function loadHistory(chatId: string, activeToken: string) {
        try {
            const history = await api<Message[]>(`/api/chats/${chatId}/messages?size=50`, {}, activeToken)
            setMessagesByChat(prev => ({ ...prev, [chatId]: sortByDate(history) }))
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to load history')
        }
    }

    async function handleAuthSubmit(e: React.FormEvent) {
        e.preventDefault()
        try {
            setError('')
            const path = mode === 'login' ? '/api/auth/login' : '/api/auth/register'
            const body = mode === 'login' ? { username, password } : { username, displayName, password }
            const res = await api<AuthResponse>(path, { method: 'POST', body: JSON.stringify(body) })
            setToken(res.accessToken)
            setPassword('')
        } catch (e) { setError(e instanceof Error ? e.message : 'Authentication failed') }
    }

    async function createDirectChat(userId: string) {
        if (!token) return
        try {
            setError('')
            const chat = await api<Chat>('/api/chats', {
                method: 'POST',
                body: JSON.stringify({ type: 'DIRECT', title: null, memberIds: [userId] }),
            }, token)
            setChats(prev => prev.some(c => c.id === chat.id) ? prev : [chat, ...prev])
            setSelectedChatId(chat.id)
            if (clientRef.current?.connected && me) {
                subscribeToChat(clientRef.current, chat.id, subscriptionsRef.current, me.id)
            }
            await loadHistory(chat.id, token)
        } catch (e) { setError(e instanceof Error ? e.message : 'Failed to create chat') }
    }

    async function createGroupChat() {
        if (!token || selectedMembers.length === 0) return
        try {
            setError('')
            const chat = await api<Chat>('/api/chats', {
                method: 'POST',
                body: JSON.stringify({ type: 'GROUP', title: groupName || 'New Group', memberIds: selectedMembers }),
            }, token)
            setChats(prev => [chat, ...prev])
            setSelectedChatId(chat.id)
            setGroupName(''); setSelectedMembers([])
            if (clientRef.current?.connected && me) {
                subscribeToChat(clientRef.current, chat.id, subscriptionsRef.current, me.id)
            }
            await loadHistory(chat.id, token)
        } catch (e) { setError(e instanceof Error ? e.message : 'Failed to create group') }
    }

    function handleTyping(val: string) {
        setText(val)
        if (!selectedChatId || !clientRef.current?.connected) return
        const client = clientRef.current
        try {
            client.publish({
                destination: '/app/chat.typing',
                body: JSON.stringify({ chatId: selectedChatId, typing: true }),
            })
            if (typingTimerRef.current) window.clearTimeout(typingTimerRef.current)
            typingTimerRef.current = window.setTimeout(() => {
                try {
                    client.publish({
                        destination: '/app/chat.typing',
                        body: JSON.stringify({ chatId: selectedChatId, typing: false }),
                    })
                } catch (e) {
                    console.error('[STOMP publish typing:false]', e)
                }
            }, 1800)
        } catch (e) {
            console.error('[STOMP publish typing:true]', e)
            setError(e instanceof Error ? e.message : 'Failed to publish typing event')
        }
    }

    function sendReceipt(chatId: string, messageId: string, status: ReceiptStatus) {
        if (!clientRef.current?.connected) return
        clientRef.current.publish({
            destination: '/app/chat.receipt',
            body: JSON.stringify({ messageId, status }),
        })
    }

    function handleSend(e: React.FormEvent) {
        e.preventDefault()
        if (!selectedChatId || !text.trim() || !clientRef.current?.connected) return
        const client = clientRef.current
        try {
            const payload = encodePayload(text.trim())
            client.publish({
                destination: '/app/chat.send',
                body: JSON.stringify({ chatId: selectedChatId, type: 'TEXT', clientMessageId: randomId(), ...payload }),
            })
            setText('')
            if (typingTimerRef.current) { window.clearTimeout(typingTimerRef.current); typingTimerRef.current = null }
            client.publish({
                destination: '/app/chat.typing',
                body: JSON.stringify({ chatId: selectedChatId, typing: false }),
            })
        } catch (e) {
            console.error('[STOMP publish chat.send]', e)
            setError(e instanceof Error ? e.message : 'Failed to send message')
        }
    }

    async function blockUser(userId: string) {
        if (!token) return
        try {
            await api<void>(`/api/users/${userId}/block`, { method: 'POST' }, token)
            setError('User blocked')
        } catch (e) { setError(e instanceof Error ? e.message : 'Failed to block user') }
    }

    function logout() { setToken(''); setError('') }

    function getChatLabel(chat: Chat): string {
        const names = chat.members
            .filter(id => id !== me?.id)
            .map(id => userMap[id]?.displayName ?? id?.slice(0, 8) ?? '?')
        return chat.title ?? (chat.type === 'DIRECT' ? names[0] ?? 'Direct' : names.join(', '))
    }


    if (!token || !me) {
        return (
            <div className="auth-shell">
                <div className="auth-card">
                    <div className="auth-logo">Cipher</div>
                    <div className="auth-sub">Encrypted Messaging</div>
                    <div className="auth-tabs">
                        <button className={`auth-tab${mode === 'login' ? ' active' : ''}`} onClick={() => setMode('login')}>Login</button>
                        <button className={`auth-tab${mode === 'register' ? ' active' : ''}`} onClick={() => setMode('register')}>Register</button>
                    </div>
                    <form onSubmit={handleAuthSubmit} className="auth-form">
                        <div>
                            <label className="field-label">Username</label>
                            <input className="field-input" value={username} onChange={e => setUsername(e.target.value)} placeholder="enter username" autoComplete="username" />
                        </div>
                        {mode === 'register' && (
                            <div>
                                <label className="field-label">Display Name</label>
                                <input className="field-input" value={displayName} onChange={e => setDisplayName(e.target.value)} placeholder="your name" />
                            </div>
                        )}
                        <div>
                            <label className="field-label">Password</label>
                            <input className="field-input" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" autoComplete="current-password" />
                        </div>
                        <button type="submit" className="btn-primary">{mode === 'login' ? 'Sign In' : 'Create Account'}</button>
                    </form>
                    <div className="auth-meta">api → {config.apiBaseUrl} &nbsp;·&nbsp; ws → {config.wsUrl}</div>
                    {error && <div className="auth-error">{error}</div>}
                </div>
            </div>
        )
    }


    return (
        <div className="app-shell">

            <div className="col">
                <div className="me-card">
                    <div className="me-name">{me.displayName}</div>
                    <div className="me-handle">@{me.username}</div>
                    <div className="me-row">
            <span className={`ws-badge ${connected ? 'online' : 'offline'}`}>
              {connected ? '● connected' : '○ offline'}
            </span>
                        <button className="btn-ghost" onClick={logout}>logout</button>
                    </div>
                </div>
                <div className="col-header"><div className="col-title">// conversations</div></div>
                <div className="col-scroll">
                    {chats.map(chat => {
                        const label = getChatLabel(chat)
                        return (
                            <button key={chat.id} className={`chat-item${selectedChatId === chat.id ? ' active' : ''}`} onClick={() => setSelectedChatId(chat.id)}>
                                <div className="chat-avatar">{initials(label)}</div>
                                <div className="chat-info">
                                    <div className="chat-name">{label}</div>
                                    <div className="chat-type">{chat.type}</div>
                                </div>
                            </button>
                        )
                    })}
                </div>
            </div>

            <div className="col">
                {selectedChat ? (
                    <>
                        <div className="chat-header">
                            <div className="chat-header-avatar">{initials(getChatLabel(selectedChat))}</div>
                            <div>
                                <div className="chat-header-name">{getChatLabel(selectedChat)}</div>
                                <div className={`chat-header-sub${visibleTyping.length > 0 ? ' typing' : ''}`}>
                                    {visibleTyping.length > 0
                                        ? `${visibleTyping.map(t => t.username).join(', ')} typing...`
                                        : `${visibleMessages.length} messages`}
                                </div>
                            </div>
                        </div>

                        <div className="messages-wrap">
                            {visibleMessages.map(msg => {
                                const mine = msg.senderId === me.id
                                const receipts = (receiptsByChat[msg.chatId] ?? []).filter(r => r.messageId === msg.id)
                                const bestReceipt: ReceiptStatus = mine
                                    ? receipts.some(r => r.status === 'READ') ? 'READ'
                                        : receipts.some(r => r.status === 'DELIVERED') ? 'DELIVERED'
                                            : 'SENT'
                                    : 'SENT'
                                const senderName = mine ? 'you' : userMap[msg.senderId]?.displayName ?? msg.senderId?.slice(0, 8) ?? '?'
                                const time = msg.createdAt
                                    ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                                    : ''
                                return (
                                    <div key={msg.id} className={`msg-row ${mine ? 'mine' : 'theirs'}`}>
                                        <div className="msg-sender">{senderName}</div>
                                        <div className="msg-bubble">{decodePayload(msg)}</div>
                                        <div className="msg-footer">
                                            <span className="msg-time">{time}</span>
                                            {mine && <span className={`msg-receipt ${bestReceipt}`}>{bestReceipt.toLowerCase()}</span>}
                                        </div>
                                    </div>
                                )
                            })}
                            <div ref={messagesEndRef} />
                        </div>

                        <form className="composer" onSubmit={handleSend}>
                            <input
                                className="composer-input"
                                placeholder="type a message..."
                                value={text}
                                onChange={e => handleTyping(e.target.value)}
                            />
                            <button type="submit" className="composer-btn" disabled={!text.trim() || !connected}>send</button>
                        </form>
                    </>
                ) : (
                    <>
                        <div className="chat-header">
                            <div className="chat-header-name" style={{ color: '#2d4570' }}>Cipher</div>
                        </div>
                        <div className="empty-chat">
                            <div className="empty-chat-label">// select a conversation</div>
                        </div>
                    </>
                )}
            </div>

            <div className="col">
                <div className="col-header"><div className="col-title">// users</div></div>
                <div className="col-scroll">
                    {error && (
                        <div className={`notice ${error.toLowerCase().includes('block') ? 'info' : 'error'}`}>
                            {error}
                            <button onClick={() => setError('')} style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer', float: 'right', fontFamily: 'inherit', fontSize: 11 }}>✕</button>
                        </div>
                    )}
                    {users.filter(u => u.id !== me.id).map(user => (
                        <div key={user.id} className="user-card">
                            <div className="user-avatar">{initials(user.displayName)}</div>
                            <div className="user-info">
                                <div className="user-name">{user.displayName}</div>
                                <div className="user-handle">@{user.username}</div>
                            </div>
                            <div className="user-actions">
                                <span className={`presence-dot ${(presence[user.id] || user.status === 'ONLINE') ? 'online' : 'offline'}`} />
                                <button className="btn-sm" onClick={() => createDirectChat(user.id)}>msg</button>
                                <button className="btn-sm danger" onClick={() => blockUser(user.id)}>blk</button>
                            </div>
                        </div>
                    ))}
                </div>

                <div className="group-section">
                    <div className="group-title">// new group</div>
                    <input className="group-input" placeholder="group name..." value={groupName} onChange={e => setGroupName(e.target.value)} />
                    <div className="member-list">
                        {users.filter(u => u.id !== me.id).map(user => (
                            <label key={user.id} className="member-option">
                                <input
                                    type="checkbox"
                                    checked={selectedMembers.includes(user.id)}
                                    onChange={e => setSelectedMembers(prev =>
                                        e.target.checked ? [...prev, user.id] : prev.filter(id => id !== user.id)
                                    )}
                                />
                                {user.displayName}
                            </label>
                        ))}
                    </div>
                    <button className="btn-create" disabled={selectedMembers.length === 0} onClick={createGroupChat}>
                        create group ({selectedMembers.length})
                    </button>
                </div>
            </div>

        </div>
    )
}
