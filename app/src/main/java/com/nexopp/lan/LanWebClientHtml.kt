package com.nexopp.lan

object LanWebClientHtml {

    fun getHtml(initialToken: String? = null): String {
        val safeInitialToken = initialToken?.replace("\"", "") ?: ""

        return """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NeXopp Connect — Conexión Local Wi-Fi</title>
    <style>
        :root {
            --bg-primary: #0b0f19;
            --bg-secondary: #131b2e;
            --bg-card: #1e293b;
            --border-color: #334155;
            --accent-blue: #38bdf8;
            --accent-green: #10b981;
            --accent-amber: #f59e0b;
            --accent-red: #ef4444;
            --text-primary: #f8fafc;
            --text-secondary: #94a3b8;
            --text-muted: #64748b;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }

        body {
            background-color: var(--bg-primary);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 24px 16px;
        }

        .container {
            width: 100%;
            max-width: 680px;
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 16px 20px;
            background: var(--bg-secondary);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
        }

        .logo-area {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .logo-badge {
            width: 38px;
            height: 38px;
            border-radius: 10px;
            background: linear-gradient(135deg, #2563eb, #38bdf8);
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 800;
            font-size: 18px;
            color: #ffffff;
            box-shadow: 0 2px 10px rgba(56, 189, 248, 0.35);
        }

        .brand-title {
            font-size: 19px;
            font-weight: 700;
            letter-spacing: -0.3px;
        }

        .brand-subtitle {
            font-size: 12px;
            color: var(--text-secondary);
        }

        .status-pill {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 6px 14px;
            border-radius: 9999px;
            font-size: 13px;
            font-weight: 600;
            background: rgba(148, 163, 184, 0.1);
            border: 1px solid var(--border-color);
            color: var(--text-secondary);
            transition: all 0.3s ease;
        }

        .status-pill.connected {
            background: rgba(16, 185, 129, 0.15);
            border-color: rgba(16, 185, 129, 0.4);
            color: var(--accent-green);
        }

        .status-pill.connecting {
            background: rgba(245, 158, 11, 0.15);
            border-color: rgba(245, 158, 11, 0.4);
            color: var(--accent-amber);
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: currentColor;
            display: inline-block;
        }

        .connected .status-dot {
            box-shadow: 0 0 10px var(--accent-green);
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0% { transform: scale(0.95); opacity: 0.8; }
            50% { transform: scale(1.2); opacity: 1; }
            100% { transform: scale(0.95); opacity: 0.8; }
        }

        .card {
            background: var(--bg-secondary);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 24px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
        }

        h2 {
            font-size: 17px;
            font-weight: 600;
            margin-bottom: 12px;
            color: var(--text-primary);
            display: flex;
            align-items: center;
            gap: 8px;
        }

        p {
            font-size: 14px;
            color: var(--text-secondary);
            line-height: 1.5;
        }

        .pairing-box {
            display: flex;
            flex-direction: column;
            gap: 14px;
            margin-top: 12px;
        }

        .input-row {
            display: flex;
            gap: 10px;
        }

        input[type="text"] {
            flex: 1;
            padding: 12px 16px;
            border-radius: 10px;
            border: 1px solid var(--border-color);
            background: var(--bg-card);
            color: var(--text-primary);
            font-size: 15px;
            outline: none;
            transition: border-color 0.2s;
        }

        input[type="text"]:focus {
            border-color: var(--accent-blue);
        }

        .code-input {
            text-transform: uppercase;
            font-family: monospace;
            font-weight: 700;
            font-size: 18px !important;
            letter-spacing: 2px;
            text-align: center;
        }

        button {
            cursor: pointer;
            padding: 12px 20px;
            border-radius: 10px;
            border: none;
            font-size: 14px;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            transition: all 0.2s ease;
        }

        .btn-primary {
            background: linear-gradient(135deg, #2563eb, #0284c7);
            color: white;
            box-shadow: 0 2px 10px rgba(37, 99, 235, 0.3);
        }

        .btn-primary:hover {
            opacity: 0.95;
            transform: translateY(-1px);
        }

        .btn-success {
            background: linear-gradient(135deg, #059669, #10b981);
            color: white;
            box-shadow: 0 2px 10px rgba(16, 185, 129, 0.3);
        }

        .btn-success:hover {
            opacity: 0.95;
            transform: translateY(-1px);
        }

        .btn-secondary {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
        }

        .btn-secondary:hover {
            background: #27354f;
        }

        .btn-danger {
            background: rgba(239, 68, 68, 0.15);
            border: 1px solid rgba(239, 68, 68, 0.3);
            color: var(--accent-red);
        }

        .btn-danger:hover {
            background: rgba(239, 68, 68, 0.25);
        }

        .action-row {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-top: 14px;
        }

        .log-container {
            background: #0b0f19;
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 14px;
            margin-top: 14px;
            max-height: 260px;
            overflow-y: auto;
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            font-size: 13px;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .log-entry {
            padding: 8px 12px;
            border-radius: 6px;
            background: rgba(30, 41, 59, 0.5);
            border-left: 3px solid var(--text-muted);
            display: flex;
            flex-direction: column;
            gap: 2px;
            word-break: break-word;
        }

        .log-entry.from-tablet {
            border-left-color: var(--accent-blue);
            background: rgba(56, 189, 248, 0.08);
        }

        .log-entry.from-pc {
            border-left-color: var(--accent-green);
            background: rgba(16, 185, 129, 0.08);
        }

        .log-entry.system {
            border-left-color: var(--accent-amber);
            background: rgba(245, 158, 11, 0.08);
        }

        .log-meta {
            font-size: 11px;
            color: var(--text-muted);
            display: flex;
            justify-content: space-between;
        }

        .log-text {
            font-size: 13px;
            font-weight: 500;
        }

        .info-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
            gap: 12px;
            margin-top: 12px;
        }

        .info-item {
            background: var(--bg-card);
            padding: 10px 14px;
            border-radius: 10px;
            border: 1px solid var(--border-color);
        }

        .info-label {
            font-size: 11px;
            color: var(--text-muted);
            text-transform: uppercase;
            font-weight: 600;
            letter-spacing: 0.5px;
        }

        .info-value {
            font-size: 14px;
            font-weight: 600;
            color: var(--text-primary);
            margin-top: 2px;
        }

        .hidden {
            display: none !important;
        }

        footer {
            margin-top: auto;
            padding-top: 20px;
            text-align: center;
            font-size: 12px;
            color: var(--text-muted);
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div class="logo-area">
                <div class="logo-badge">N</div>
                <div>
                    <div class="brand-title">NeXopp Connect</div>
                    <div class="brand-subtitle">Conexión Directa Wi-Fi Local (PoC)</div>
                </div>
            </div>
            <div id="statusPill" class="status-pill">
                <span class="status-dot"></span>
                <span id="statusText">Desconectado</span>
            </div>
        </header>

        <!-- CARD 1: EMPAREJAMIENTO MANUAL (Si no se entra por QR con token) -->
        <div id="pairingCard" class="card">
            <h2>🔑 Emparejamiento con Tablet</h2>
            <p>Introduce el código de 8 caracteres que aparece en la pantalla de NeXopp en tu tablet para autorizar la conexión local.</p>
            <div class="pairing-box">
                <div class="input-row">
                    <input type="text" id="pairingCodeInput" class="code-input" placeholder="XXXX-XXX" maxlength="9" autofocus>
                    <button class="btn-primary" onclick="submitPairingCode()">Conectar</button>
                </div>
                <div id="pairingError" style="color: var(--accent-red); font-size: 13px;" class="hidden"></div>
            </div>
        </div>

        <!-- CARD 2: ESTADO Y COMUNICACIÓN EN TIEMPO REAL -->
        <div id="connectedCard" class="card hidden">
            <h2>⚡ Sesión Activa NeXopp</h2>
            <p>Conexión directa establecida entre el navegador y la tablet a través de la red local Wi-Fi.</p>

            <div class="info-grid">
                <div class="info-item">
                    <div class="info-label">Dispositivo</div>
                    <div class="info-value">Tablet Android</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Canal</div>
                    <div class="info-value">WebSocket (LAN)</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Internet en PC</div>
                    <div class="info-value" id="internetStatusVal" style="color: var(--accent-green);">Activo ✓</div>
                </div>
            </div>

            <div style="margin-top: 20px;">
                <h2 style="font-size: 15px;">🧪 Pruebas de Comunicación Bidireccional</h2>
                <p>Envía mensajes de prueba a la tablet para comprobar la respuesta en tiempo real.</p>
                <div class="action-row">
                    <button class="btn-success" onclick="sendHelloFromPc()">
                        📤 Enviar HELLO_FROM_PC
                    </button>
                </div>

                <div class="input-row" style="margin-top: 12px;">
                    <input type="text" id="customMessageInput" placeholder="Escribe un mensaje personalizado para la tablet..." onkeydown="if(event.key==='Enter') sendCustomMessage()">
                    <button class="btn-secondary" onclick="sendCustomMessage()">Enviar</button>
                </div>
            </div>

            <div style="margin-top: 20px;">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <h2 style="font-size: 15px; margin-bottom: 0;">📜 Registro de Mensajes en Vivo</h2>
                    <button class="btn-secondary" style="padding: 4px 10px; font-size: 12px;" onclick="clearLogs()">Limpiar</button>
                </div>
                <div id="logList" class="log-container">
                    <!-- Logs dynamically added here -->
                </div>
            </div>

            <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
                <button class="btn-danger" onclick="disconnect()">Desconectar</button>
            </div>
        </div>

        <footer>
            NeXopp &bull; Red Local Directa &bull; Sin Cloud &bull; Sin Servidores Externos &bull; Coste 0 €
        </footer>
    </div>

    <script>
        let ws = null;
        let currentToken = "$safeInitialToken";

        const statusPill = document.getElementById('statusPill');
        const statusText = document.getElementById('statusText');
        const pairingCard = document.getElementById('pairingCard');
        const connectedCard = document.getElementById('connectedCard');
        const pairingCodeInput = document.getElementById('pairingCodeInput');
        const pairingError = document.getElementById('pairingError');
        const logList = document.getElementById('logList');
        const customMessageInput = document.getElementById('customMessageInput');

        function setStatus(state, text) {
            statusPill.className = 'status-pill ' + state;
            statusText.textContent = text;
        }

        function appendLog(type, sender, message) {
            const entry = document.createElement('div');
            entry.className = 'log-entry ' + type;
            const time = new Date().toLocaleTimeString();
            entry.innerHTML = 
                '<div class="log-meta">' +
                    '<span>' + escapeHtml(sender) + '</span>' +
                    '<span>' + time + '</span>' +
                '</div>' +
                '<div class="log-text">' + escapeHtml(message) + '</div>';
            logList.appendChild(entry);
            logList.scrollTop = logList.scrollHeight;
        }

        function clearLogs() {
            logList.innerHTML = '';
        }

        function escapeHtml(str) {
            return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }

        async function submitPairingCode() {
            const code = pairingCodeInput.value.trim();
            if (!code) {
                showPairingError('Por favor introduce el código de emparejamiento.');
                return;
            }
            pairingError.classList.add('hidden');
            setStatus('connecting', 'Validando código...');

            try {
                const response = await fetch('/api/pair?code=' + encodeURIComponent(code));
                const data = await response.json();
                if (data.success && data.sessionToken) {
                    currentToken = data.sessionToken;
                    connectWebSocket(currentToken);
                } else {
                    showPairingError('Código de emparejamiento incorrecto o expirado.');
                    setStatus('', 'Desconectado');
                }
            } catch (err) {
                showPairingError('No se pudo contactar con la tablet: ' + err.message);
                setStatus('', 'Desconectado');
            }
        }

        function showPairingError(msg) {
            pairingError.textContent = msg;
            pairingError.classList.remove('hidden');
        }

        function connectWebSocket(token) {
            if (ws) {
                try { ws.close(); } catch(e) {}
            }

            setStatus('connecting', 'Conectando...');
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = protocol + '//' + window.location.host + '/ws?token=' + encodeURIComponent(token);

            ws = new WebSocket(wsUrl);

            ws.onopen = function() {
                setStatus('connected', 'NeXopp Conectado');
                pairingCard.classList.add('hidden');
                connectedCard.classList.remove('hidden');
                appendLog('system', 'Sistema', 'Conexión WebSocket establecida con éxito con la Tablet.');
            };

            ws.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    if (data.type === 'HELLO_FROM_TABLET') {
                        appendLog('from-tablet', '📱 Tablet NeXopp', data.payload || 'HELLO_FROM_TABLET');
                    } else if (data.type === 'MESSAGE_FROM_TABLET') {
                        appendLog('from-tablet', '📱 Tablet NeXopp', data.payload);
                    } else if (data.type === 'STATUS') {
                        appendLog('system', 'Sistema', 'Estado: ' + data.payload);
                    } else {
                        appendLog('from-tablet', '📱 Tablet', event.data);
                    }
                } catch(e) {
                    appendLog('from-tablet', '📱 Tablet', event.data);
                }
            };

            ws.onclose = function(event) {
                setStatus('', 'Desconectado');
                appendLog('system', 'Sistema', 'Conexión cerrada por la tablet o red local.');
            };

            ws.onerror = function(error) {
                setStatus('', 'Error de conexión');
                appendLog('system', 'Error', 'Error en el socket de red local.');
            };
        }

        function sendHelloFromPc() {
            if (!ws || ws.readyState !== WebSocket.OPEN) {
                alert('No hay conexión activa con la tablet.');
                return;
            }
            const msg = {
                type: 'HELLO_FROM_PC',
                payload: 'HELLO_FROM_PC',
                timestamp: Date.now()
            };
            ws.send(JSON.stringify(msg));
            appendLog('from-pc', '💻 PC Navegador', 'HELLO_FROM_PC');
        }

        function sendCustomMessage() {
            const input = customMessageInput;
            const text = input.value.trim();
            if (!text) return;
            if (!ws || ws.readyState !== WebSocket.OPEN) {
                alert('No hay conexión activa con la tablet.');
                return;
            }
            const msg = {
                type: 'MESSAGE_FROM_PC',
                payload: text,
                timestamp: Date.now()
            };
            ws.send(JSON.stringify(msg));
            appendLog('from-pc', '💻 PC Navegador', text);
            input.value = '';
        }

        function disconnect() {
            if (ws) {
                try { ws.close(); } catch(e) {}
                ws = null;
            }
            setStatus('', 'Desconectado');
            connectedCard.classList.add('hidden');
            pairingCard.classList.remove('hidden');
            pairingCodeInput.value = '';
        }

        // Auto-connect if initialToken is provided in the URL
        window.addEventListener('DOMContentLoaded', () => {
            if (currentToken && currentToken.length > 0) {
                connectWebSocket(currentToken);
            }
        });
    </script>
</body>
</html>
        """.trimIndent()
    }
}
