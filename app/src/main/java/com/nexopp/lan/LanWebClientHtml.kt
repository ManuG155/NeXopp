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
    <title>NeXopp — Biblioteca y Editor Web Local</title>
    <style>
        :root {
            --bg-base: #090d16;
            --bg-surface: #111827;
            --bg-card: #1f293d;
            --bg-card-hover: #27354f;
            --border: #374151;
            --border-light: #4b5563;
            --primary: #38bdf8;
            --primary-hover: #0ea5e9;
            --success: #10b981;
            --warning: #f59e0b;
            --danger: #ef4444;
            --text: #f9fafb;
            --text-secondary: #9ca3af;
            --text-muted: #6b7280;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            -webkit-font-smoothing: antialiased;
        }

        body {
            background-color: var(--bg-base);
            color: var(--text);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
        }

        /* Top Header Bar */
        header {
            height: 60px;
            background: var(--bg-surface);
            border-bottom: 1px solid var(--border);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 24px;
            position: sticky;
            top: 0;
            z-index: 100;
        }

        .header-left {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .brand-badge {
            width: 34px;
            height: 34px;
            border-radius: 8px;
            background: linear-gradient(135deg, #2563eb, #38bdf8);
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 800;
            font-size: 16px;
            color: #fff;
            box-shadow: 0 2px 8px rgba(56, 189, 248, 0.4);
        }

        .brand-title {
            font-size: 18px;
            font-weight: 700;
            letter-spacing: -0.3px;
        }

        .header-right {
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 5px 12px;
            border-radius: 9999px;
            font-size: 12px;
            font-weight: 600;
            background: rgba(156, 163, 175, 0.1);
            border: 1px solid var(--border);
            color: var(--text-secondary);
        }

        .status-badge.connected {
            background: rgba(16, 185, 129, 0.15);
            border-color: rgba(16, 185, 129, 0.4);
            color: var(--success);
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: currentColor;
            display: inline-block;
        }

        .connected .status-dot {
            box-shadow: 0 0 8px var(--success);
        }

        /* Buttons */
        button {
            cursor: pointer;
            border: none;
            outline: none;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            font-size: 13px;
            font-weight: 600;
            border-radius: 8px;
            padding: 8px 14px;
            transition: all 0.15s ease;
        }

        .btn-primary {
            background: var(--primary);
            color: #0b1120;
        }

        .btn-primary:hover {
            background: var(--primary-hover);
        }

        .btn-secondary {
            background: var(--bg-card);
            border: 1px solid var(--border);
            color: var(--text);
        }

        .btn-secondary:hover {
            background: var(--bg-card-hover);
            border-color: var(--border-light);
        }

        .btn-icon {
            padding: 7px;
            border-radius: 6px;
            background: transparent;
            color: var(--text-secondary);
        }

        .btn-icon:hover {
            background: rgba(255, 255, 255, 0.08);
            color: var(--text);
        }

        .btn-danger {
            background: rgba(239, 68, 68, 0.15);
            border: 1px solid rgba(239, 68, 68, 0.3);
            color: var(--danger);
        }

        .btn-danger:hover {
            background: rgba(239, 68, 68, 0.25);
        }

        /* Main Containers */
        .view-container {
            flex: 1;
            display: flex;
            flex-direction: column;
        }

        /* --- LIBRARY VIEW --- */
        .library-content {
            max-width: 1200px;
            width: 100%;
            margin: 0 auto;
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        .library-toolbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 12px;
        }

        .search-box {
            position: relative;
            width: 280px;
        }

        .search-box input {
            width: 100%;
            padding: 9px 12px 9px 34px;
            border-radius: 8px;
            border: 1px solid var(--border);
            background: var(--bg-surface);
            color: var(--text);
            font-size: 13px;
            outline: none;
        }

        .search-box input:focus {
            border-color: var(--primary);
        }

        .search-icon {
            position: absolute;
            left: 10px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-muted);
            font-size: 14px;
        }

        /* Breadcrumbs & Folder Pills */
        .breadcrumbs {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
            color: var(--text-secondary);
        }

        .crumb-link {
            cursor: pointer;
            color: var(--primary);
            font-weight: 500;
        }

        .crumb-link:hover {
            text-decoration: underline;
        }

        .folders-row {
            display: flex;
            align-items: center;
            gap: 10px;
            overflow-x: auto;
            padding-bottom: 4px;
        }

        .folder-chip {
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 7px 14px;
            border-radius: 20px;
            background: var(--bg-surface);
            border: 1px solid var(--border);
            font-size: 13px;
            color: var(--text);
            white-space: nowrap;
            transition: all 0.2s;
        }

        .folder-chip:hover {
            background: var(--bg-card);
            border-color: var(--border-light);
        }

        .folder-dot {
            width: 10px;
            height: 10px;
            border-radius: 50%;
        }

        /* Notebook Grid */
        .notebook-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
            gap: 20px;
            margin-top: 10px;
        }

        .notebook-card {
            background: var(--bg-surface);
            border: 1px solid var(--border);
            border-radius: 12px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
            cursor: pointer;
            position: relative;
        }

        .notebook-card:hover {
            transform: translateY(-2px);
            border-color: var(--border-light);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
        }

        .card-cover {
            height: 100px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 32px;
            color: rgba(255, 255, 255, 0.9);
            position: relative;
        }

        .card-body {
            padding: 14px;
            display: flex;
            flex-direction: column;
            gap: 6px;
            flex: 1;
        }

        .card-title {
            font-size: 15px;
            font-weight: 600;
            color: var(--text);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .card-meta {
            font-size: 12px;
            color: var(--text-secondary);
            display: flex;
            justify-content: space-between;
        }

        .card-actions {
            display: flex;
            gap: 6px;
            margin-top: 8px;
            padding-top: 8px;
            border-top: 1px solid rgba(255, 255, 255, 0.06);
        }

        .card-actions button {
            flex: 1;
            padding: 5px 8px;
            font-size: 12px;
        }

        /* Empty State */
        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: var(--text-muted);
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 12px;
        }

        /* --- EDITOR VIEW --- */
        .editor-container {
            display: flex;
            flex-direction: column;
            height: calc(100vh - 60px);
            overflow: hidden;
        }

        .editor-topbar {
            height: 52px;
            background: var(--bg-surface);
            border-bottom: 1px solid var(--border);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 16px;
            gap: 12px;
            z-index: 10;
        }

        .editor-topbar-left {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .doc-title-badge {
            font-size: 14px;
            font-weight: 600;
            color: var(--text);
            max-width: 200px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .save-indicator {
            font-size: 11px;
            color: var(--success);
            display: flex;
            align-items: center;
            gap: 4px;
        }

        .editor-tools {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .tool-btn {
            padding: 6px 12px;
            border-radius: 6px;
            background: transparent;
            color: var(--text-secondary);
            border: 1px solid transparent;
        }

        .tool-btn:hover {
            background: rgba(255, 255, 255, 0.05);
            color: var(--text);
        }

        .tool-btn.active {
            background: rgba(56, 189, 248, 0.15);
            border-color: rgba(56, 189, 248, 0.4);
            color: var(--primary);
        }

        .color-swatches {
            display: flex;
            align-items: center;
            gap: 5px;
            padding: 0 6px;
            border-left: 1px solid var(--border);
            border-right: 1px solid var(--border);
        }

        .swatch {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            cursor: pointer;
            border: 2px solid transparent;
            transition: transform 0.1s;
        }

        .swatch:hover {
            transform: scale(1.15);
        }

        .swatch.selected {
            border-color: #ffffff;
            box-shadow: 0 0 6px rgba(255, 255, 255, 0.6);
        }

        .width-control {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 12px;
            color: var(--text-secondary);
        }

        .width-control input[type="range"] {
            width: 70px;
            cursor: pointer;
        }

        .editor-topbar-right {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .page-nav {
            display: flex;
            align-items: center;
            gap: 6px;
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 6px;
            padding: 2px 6px;
        }

        .page-indicator {
            font-size: 12px;
            font-weight: 600;
            padding: 0 6px;
            color: var(--text);
            min-width: 70px;
            text-align: center;
        }

        /* Canvas Scroll Area */
        .canvas-viewport {
            flex: 1;
            overflow: auto;
            background: #060910;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 30px;
            position: relative;
        }

        .canvas-wrapper {
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.6);
            border-radius: 4px;
            overflow: hidden;
            background: #ffffff;
            position: relative;
        }

        canvas {
            display: block;
            touch-action: none;
            cursor: text;
        }

        .text-editor-overlay {
            position: absolute;
            z-index: 20;
            background: rgba(255, 255, 255, 0.98);
            border: 2px dashed var(--primary);
            border-radius: 6px;
            box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
            padding: 6px 8px;
            display: flex;
            flex-direction: column;
            min-width: 160px;
        }

        #textEditorInput {
            width: 100%;
            border: none;
            outline: none;
            background: transparent;
            resize: none;
            font-family: "Liberation Sans", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            line-height: 1.25;
            overflow: hidden;
            padding: 2px;
        }

        .text-editor-actions {
            display: flex;
            justify-content: flex-end;
            gap: 6px;
            margin-top: 6px;
            padding-top: 4px;
            border-top: 1px solid rgba(0, 0, 0, 0.08);
        }

        .btn-text-action {
            border: none;
            border-radius: 4px;
            padding: 3px 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 11px;
            font-weight: bold;
            cursor: pointer;
        }

        .btn-text-action.confirm {
            background: #10b981;
            color: #ffffff;
        }

        .btn-text-action.cancel {
            background: #ef4444;
            color: #ffffff;
        }

        /* Modals */
        .modal-overlay {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0, 0, 0, 0.7);
            backdrop-filter: blur(4px);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 1000;
        }

        .modal-card {
            background: var(--bg-surface);
            border: 1px solid var(--border);
            border-radius: 14px;
            padding: 24px;
            width: 100%;
            max-width: 440px;
            display: flex;
            flex-direction: column;
            gap: 16px;
            box-shadow: 0 16px 36px rgba(0, 0, 0, 0.4);
        }

        .modal-title {
            font-size: 17px;
            font-weight: 700;
        }

        .form-group {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .form-group label {
            font-size: 12px;
            font-weight: 600;
            color: var(--text-secondary);
        }

        .form-group input, .form-group select {
            padding: 10px 12px;
            border-radius: 8px;
            border: 1px solid var(--border);
            background: var(--bg-card);
            color: var(--text);
            font-size: 14px;
            outline: none;
        }

        .form-group input:focus, .form-group select:focus {
            border-color: var(--primary);
        }

        .modal-actions {
            display: flex;
            justify-content: flex-end;
            gap: 10px;
            margin-top: 8px;
        }

        .hidden {
            display: none !important;
        }
    </style>
</head>
<body>
    <header>
        <div class="header-left">
            <div class="brand-badge">N</div>
            <div>
                <div class="brand-title">NeXopp</div>
            </div>
        </div>
        <div class="header-right">
            <div id="statusPill" class="status-badge">
                <span class="status-dot"></span>
                <span id="statusText">Conectando...</span>
            </div>
        </div>
    </header>

    <!-- VIEW 1: PAIRING VIEW (If opened without token) -->
    <div id="pairingView" class="view-container hidden" style="align-items: center; justify-content: center; padding: 20px;">
        <div class="modal-card" style="max-width: 400px; text-align: center;">
            <div class="modal-title">Conectar con NeXopp Tablet</div>
            <p style="font-size: 13px; color: var(--text-secondary);">
                Introduce el código de 8 caracteres que aparece en la pantalla de NeXopp en tu tablet.
            </p>
            <div class="form-group" style="margin-top: 8px;">
                <input type="text" id="pairingCodeInput" placeholder="XXXX-XXX" style="font-family: monospace; font-size: 20px; font-weight: 700; letter-spacing: 2px; text-align: center; text-transform: uppercase;" maxlength="9">
            </div>
            <div id="pairingError" style="color: var(--danger); font-size: 12px;" class="hidden"></div>
            <button class="btn-primary" style="margin-top: 10px;" onclick="submitPairingCode()">Entrar a Biblioteca</button>
        </div>
    </div>

    <!-- VIEW 2: LIBRARY VIEW -->
    <div id="libraryView" class="view-container">
        <div class="library-content">
            <div class="library-toolbar">
                <div class="breadcrumbs" id="breadcrumbs">
                    <span class="crumb-link" onclick="navigateToFolder(null)">Biblioteca</span>
                </div>
                <div style="display: flex; gap: 10px; align-items: center;">
                    <div class="search-box">
                        <span class="search-icon">🔍</span>
                        <input type="text" id="searchInput" placeholder="Buscar cuadernos..." oninput="filterNotebooks()">
                    </div>
                    <button class="btn-secondary" onclick="openCreateSubjectModal()">+ Carpeta</button>
                    <button class="btn-primary" onclick="openCreateNotebookModal()">+ Cuaderno</button>
                </div>
            </div>

            <!-- Subfolders row -->
            <div id="foldersRow" class="folders-row"></div>

            <!-- Notebooks Grid -->
            <div id="notebookGrid" class="notebook-grid"></div>

            <div id="emptyState" class="empty-state hidden">
                <span style="font-size: 40px;">📓</span>
                <p>No hay cuadernos en esta carpeta.</p>
                <button class="btn-primary" onclick="openCreateNotebookModal()">Crear primer cuaderno</button>
            </div>
        </div>
    </div>

    <!-- VIEW 3: EDITOR VIEW -->
    <div id="editorView" class="view-container hidden">
        <div class="editor-container">
            <div class="editor-topbar">
                <div class="editor-topbar-left">
                    <button class="btn-secondary" style="padding: 5px 10px;" onclick="closeDocumentAndReturn()">
                        ← Biblioteca
                    </button>
                    <span id="editorDocTitle" class="doc-title-badge">Cuaderno</span>
                    <span id="saveStatus" class="save-indicator">✓ Sincronizado</span>
                </div>

                <div class="editor-tools">
                    <button id="toolText" class="tool-btn active" onclick="setTool('text')">🔤 Texto</button>
                    <button id="toolPen" class="tool-btn" onclick="setTool('pen')">✏️ Pluma</button>
                    <button id="toolHighlighter" class="tool-btn" onclick="setTool('highlighter')">🖍️ Subrayador</button>
                    <button id="toolEraser" class="tool-btn" onclick="setTool('eraser')">🧹 Borrador</button>

                    <div class="color-swatches" id="colorSwatches">
                        <div class="swatch selected" style="background-color: #1e293b;" onclick="setColor(0xFF1E293B, this)"></div>
                        <div class="swatch" style="background-color: #2563eb;" onclick="setColor(0xFF2563EB, this)"></div>
                        <div class="swatch" style="background-color: #dc2626;" onclick="setColor(0xFFDC2626, this)"></div>
                        <div class="swatch" style="background-color: #16a34a;" onclick="setColor(0xFF16A34A, this)"></div>
                        <div class="swatch" style="background-color: #9333ea;" onclick="setColor(0xFF9333EA, this)"></div>
                        <div class="swatch" style="background-color: #ea580c;" onclick="setColor(0xFFEA580C, this)"></div>
                    </div>

                    <div class="width-control">
                        <span>Grosor:</span>
                        <input type="range" id="widthSlider" min="0.5" max="8.0" step="0.5" value="1.5" oninput="updateWidth(this.value)">
                        <span id="widthValue" style="min-width: 24px;">1.5</span>
                    </div>
                </div>

                <div class="editor-topbar-right">
                    <div class="page-nav">
                        <button class="btn-icon" onclick="prevPage()">‹</button>
                        <span id="pageIndicator" class="page-indicator">1 / 1</span>
                        <button class="btn-icon" onclick="nextPage()">›</button>
                    </div>
                    <button class="btn-secondary" style="padding: 5px 8px; font-size: 12px;" onclick="addPagePrompt()">+ Pág</button>
                    <button class="btn-primary" style="padding: 5px 12px;" onclick="saveDocumentExplicit()">Guardar</button>
                </div>
            </div>

            <div class="canvas-viewport" id="canvasViewport">
                <div class="canvas-wrapper" id="canvasWrapper">
                    <canvas id="pageCanvas"></canvas>
                    <div id="textEditorOverlay" class="text-editor-overlay hidden">
                        <textarea id="textEditorInput" placeholder="Escribe con el teclado..." rows="1" spellcheck="false"></textarea>
                        <div class="text-editor-actions">
                            <button class="btn-text-action confirm" title="Guardar (Ctrl+Enter o clic fuera)" onclick="commitTextEditor()">✓ Guardar</button>
                            <button class="btn-text-action cancel" title="Cancelar (Esc)" onclick="cancelTextEditor()">✕ Cancelar</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- MODAL: CREAR CUADERNO -->
    <div id="createNotebookModal" class="modal-overlay hidden">
        <div class="modal-card">
            <div class="modal-title">Nuevo Cuaderno</div>
            <div class="form-group">
                <label>Nombre del cuaderno</label>
                <input type="text" id="newNotebookName" placeholder="Ej. Álgebra Lineal" autofocus>
            </div>
            <div class="form-group">
                <label>Plantilla de página</label>
                <select id="newNotebookTemplate">
                    <option value="ruled" selected>Rayado</option>
                    <option value="graph">Cuadriculado</option>
                    <option value="dotted">Puntos</option>
                    <option value="plain">Liso / Blanco</option>
                </select>
            </div>
            <div class="form-group">
                <label>Color de portada</label>
                <select id="newNotebookColor">
                    <option value="2005034" selected>Azul Marino</option>
                    <option value="417606">Verde Esmeralda</option>
                    <option value="8591427">Vino Tinto</option>
                    <option value="8138002">Ámbar / Óxido</option>
                    <option value="3621201">Gris Pizarra</option>
                    <option value="4988245">Púrpura Profundo</option>
                </select>
            </div>
            <div class="modal-actions">
                <button class="btn-secondary" onclick="closeModals()">Cancelar</button>
                <button class="btn-primary" onclick="submitCreateNotebook()">Crear</button>
            </div>
        </div>
    </div>

    <!-- MODAL: CREAR CARPETA -->
    <div id="createSubjectModal" class="modal-overlay hidden">
        <div class="modal-card">
            <div class="modal-title">Nueva Carpeta</div>
            <div class="form-group">
                <label>Nombre de la carpeta</label>
                <input type="text" id="newSubjectName" placeholder="Ej. Semestre 1">
            </div>
            <div class="form-group">
                <label>Color</label>
                <select id="newSubjectColor">
                    <option value="4280391410" selected>Azul</option>
                    <option value="4283215696">Verde</option>
                    <option value="4293467747">Ámbar</option>
                    <option value="4290772986">Rojo</option>
                    <option value="4288424424">Púrpura</option>
                </select>
            </div>
            <div class="modal-actions">
                <button class="btn-secondary" onclick="closeModals()">Cancelar</button>
                <button class="btn-primary" onclick="submitCreateSubject()">Crear</button>
            </div>
        </div>
    </div>

    <script>
        let ws = null;
        let currentToken = "$safeInitialToken";

        // Application State
        let subjects = [];
        let notebooks = [];
        let tags = [];
        let currentSubjectId = null;

        // Current open document state
        let currentDoc = null;
        let currentPageIndex = 0;

        // Editor Drawing / Text State
        let currentTool = 'text'; // 'text', 'pen', 'highlighter', 'eraser'
        let currentColor = 0xFF1E293B; // ARGB
        let currentWidthPt = 1.5;
        let isDrawing = false;
        let currentStrokePoints = [];
        let activeTextPt = null;

        // DOM elements
        const pairingView = document.getElementById('pairingView');
        const libraryView = document.getElementById('libraryView');
        const editorView = document.getElementById('editorView');
        const statusPill = document.getElementById('statusPill');
        const statusText = document.getElementById('statusText');
        const breadcrumbs = document.getElementById('breadcrumbs');
        const foldersRow = document.getElementById('foldersRow');
        const notebookGrid = document.getElementById('notebookGrid');
        const emptyState = document.getElementById('emptyState');
        const searchInput = document.getElementById('searchInput');

        const canvas = document.getElementById('pageCanvas');
        const ctx = canvas.getContext('2d');
        const canvasWrapper = document.getElementById('canvasWrapper');
        const pageIndicator = document.getElementById('pageIndicator');
        const editorDocTitle = document.getElementById('editorDocTitle');
        const saveStatus = document.getElementById('saveStatus');

        function setStatus(connected, text) {
            if (connected) {
                statusPill.className = 'status-badge connected';
                statusText.textContent = text || 'Conectado a Tablet ●';
            } else {
                statusPill.className = 'status-badge';
                statusText.textContent = text || 'Desconectado';
            }
        }

        // --- WebSocket Connection ---
        function connectWebSocket(token) {
            if (ws) {
                try { ws.close(); } catch(e) {}
            }
            setStatus(false, 'Conectando...');
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = protocol + '//' + window.location.host + '/ws?token=' + encodeURIComponent(token);

            ws = new WebSocket(wsUrl);

            ws.onopen = function() {
                setStatus(true, 'Conectado a Tablet ●');
                pairingView.classList.add('hidden');
                libraryView.classList.remove('hidden');
                ws.send(JSON.stringify({ type: 'GET_LIBRARY' }));
            };

            ws.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    handleIncomingMessage(data);
                } catch(e) {
                    console.error('Error parseando mensaje:', e);
                }
            };

            ws.onclose = function() {
                stopDocSyncPolling();
                setStatus(false, 'Desconectado de Tablet');
                saveStatus.textContent = '⚠️ Desconectado';
                saveStatus.style.color = 'var(--danger)';
            };

            ws.onerror = function() {
                setStatus(false, 'Error de conexión');
            };
        }

        async function submitPairingCode() {
            const input = document.getElementById('pairingCodeInput');
            const code = input.value.trim();
            const err = document.getElementById('pairingError');
            if (!code) return;
            err.classList.add('hidden');

            try {
                const res = await fetch('/api/pair?code=' + encodeURIComponent(code));
                const data = await res.json();
                if (data.success && data.sessionToken) {
                    currentToken = data.sessionToken;
                    connectWebSocket(currentToken);
                } else {
                    err.textContent = 'Código incorrecto o expirado.';
                    err.classList.remove('hidden');
                }
            } catch(e) {
                err.textContent = 'Error de red local: ' + e.message;
                err.classList.remove('hidden');
            }
        }

        function handleIncomingMessage(msg) {
            if (msg.payload && typeof msg.payload === 'string' && msg.payload.startsWith('{')) {
                try {
                    const inner = JSON.parse(msg.payload);
                    if (inner.type) msg = inner;
                } catch(e) {}
            }

            switch(msg.type) {
                case 'LIBRARY_DATA':
                    subjects = msg.subjects || [];
                    notebooks = msg.notebooks || [];
                    tags = msg.tags || [];
                    renderLibrary();
                    break;

                case 'DOCUMENT_DATA':
                    if (currentDoc && currentDoc.notebookId === msg.notebookId) {
                        // Reconciling update from server polling
                        if (msg.version > (currentDoc.version || 0)) {
                            currentDoc.pages = msg.pages;
                            currentDoc.version = msg.version;
                            renderCurrentPage();
                            showSavedStatus('Sincronizado ✓');
                        }
                    } else {
                        loadDocumentIntoEditor(msg);
                    }
                    break;

                case 'STROKE_ADDED':
                    if (currentDoc && msg.notebookId === currentDoc.notebookId && msg.pageIndex === currentPageIndex) {
                        appendStrokeToCurrentPage(msg.stroke);
                        if (msg.version && msg.version > (currentDoc.version || 0)) {
                            currentDoc.version = msg.version;
                        }
                    }
                    break;

                case 'STROKE_ACK':
                case 'TEXT_ACK':
                    if (currentDoc && msg.notebookId === currentDoc.notebookId) {
                        if (msg.version && msg.version > (currentDoc.version || 0)) {
                            currentDoc.version = msg.version;
                        }
                        showSavedStatus('Sincronizado ✓');
                    }
                    break;

                case 'DOCUMENT_UP_TO_DATE':
                    // Server confirmed document version is current
                    break;

                case 'STROKES_ERASED':
                    if (currentDoc && msg.notebookId === currentDoc.notebookId && msg.pageIndex === currentPageIndex) {
                        if (msg.page) {
                            currentDoc.pages[currentPageIndex] = msg.page;
                            if (msg.version !== undefined) currentDoc.version = msg.version;
                            renderCurrentPage();
                            showSavedStatus('Sincronizado ✓');
                        }
                    }
                    break;

                case 'TEXT_ADDED':
                    if (currentDoc && msg.notebookId === currentDoc.notebookId && msg.pageIndex === currentPageIndex) {
                        appendElementToCurrentPage(msg.text);
                        if (msg.version && msg.version > (currentDoc.version || 0)) {
                            currentDoc.version = msg.version;
                        }
                    }
                    break;

                case 'TABLET_DOCUMENT_CHANGED':
                    if (currentDoc && msg.notebookId === currentDoc.notebookId) {
                        if (!currentDoc.version || msg.version > currentDoc.version) {
                            currentDoc.pages = msg.pages;
                            currentDoc.version = msg.version;
                            renderCurrentPage();
                            showSavedStatus('Sincronizado desde Tablet ✓');
                        }
                    }
                    break;

                case 'DOCUMENT_SAVED':
                    showSavedStatus('Guardado en Tablet ✓');
                    break;

                case 'ERROR':
                    alert('Aviso de NeXopp: ' + msg.message);
                    break;
            }
        }

        // --- Library UI Logic ---
        function renderLibrary() {
            renderBreadcrumbs();
            renderFolders();
            renderNotebookGrid();
        }

        function renderBreadcrumbs() {
            let html = '<span class="crumb-link" onclick="navigateToFolder(null)">Biblioteca</span>';
            if (currentSubjectId) {
                const chain = [];
                let curr = subjects.find(s => s.id === currentSubjectId);
                while (curr) {
                    chain.unshift(curr);
                    curr = curr.parentId ? subjects.find(s => s.id === curr.parentId) : null;
                }
                chain.forEach(folder => {
                    html += ' <span>/</span> <span class="crumb-link" onclick="navigateToFolder(\'' + folder.id + '\')">' + escapeHtml(folder.name) + '</span>';
                });
            }
            breadcrumbs.innerHTML = html;
        }

        function renderFolders() {
            const subfolders = subjects.filter(s => (s.parentId || null) === (currentSubjectId || null));
            if (subfolders.length === 0) {
                foldersRow.innerHTML = '';
                return;
            }
            let html = '';
            subfolders.forEach(s => {
                const count = notebooks.filter(n => n.subjectId === s.id).length;
                const hexColor = '#' + (s.color & 0xFFFFFF).toString(16).padStart(6, '0');
                html += '<div class="folder-chip" onclick="navigateToFolder(\'' + s.id + '\')">' +
                    '<span class="folder-dot" style="background-color:' + hexColor + '"></span>' +
                    '<span>' + escapeHtml(s.name) + '</span>' +
                    '<span style="font-size:11px;color:var(--text-muted);">' + count + '</span>' +
                '</div>';
            });
            foldersRow.innerHTML = html;
        }

        function renderNotebookGrid() {
            const query = (searchInput.value || '').trim().toLowerCase();
            const filtered = notebooks.filter(n => {
                const matchFolder = currentSubjectId ? n.subjectId === currentSubjectId : true;
                const matchQuery = query ? n.name.toLowerCase().includes(query) : true;
                return matchFolder && matchQuery;
            });

            if (filtered.length === 0) {
                notebookGrid.innerHTML = '';
                emptyState.classList.remove('hidden');
                return;
            }

            emptyState.classList.add('hidden');
            let html = '';
            filtered.forEach(nb => {
                const coverColor = '#' + (nb.coverColor & 0xFFFFFF).toString(16).padStart(6, '0');
                const dateStr = nb.lastModified ? new Date(nb.lastModified).toLocaleDateString() : '';
                html += '<div class="notebook-card" onclick="openNotebook(\'' + nb.id + '\', \'' + nb.fileName + '\')">' +
                    '<div class="card-cover" style="background-color:' + coverColor + ';">' +
                        '📓' +
                    '</div>' +
                    '<div class="card-body">' +
                        '<div class="card-title">' + escapeHtml(nb.name) + '</div>' +
                        '<div class="card-meta">' +
                            '<span>' + (nb.pageCount || 1) + ' págs</span>' +
                            '<span>' + dateStr + '</span>' +
                        '</div>' +
                        '<div class="card-actions" onclick="event.stopPropagation()">' +
                            '<button class="btn-secondary" onclick="openNotebook(\'' + nb.id + '\', \'' + nb.fileName + '\')">Abrir</button>' +
                            '<button class="btn-secondary" onclick="renameNotebookPrompt(\'' + nb.id + '\', \'' + escapeHtml(nb.name) + '\')">Renombrar</button>' +
                            '<button class="btn-danger" onclick="deleteNotebookPrompt(\'' + nb.id + '\')">✕</button>' +
                        '</div>' +
                    '</div>' +
                '</div>';
            });
            notebookGrid.innerHTML = html;
        }

        function navigateToFolder(id) {
            currentSubjectId = id;
            renderLibrary();
        }

        function filterNotebooks() {
            renderNotebookGrid();
        }

        function openNotebook(notebookId, fileName) {
            if (!ws || ws.readyState !== WebSocket.OPEN) return;
            setStatus(true, 'Abriendo cuaderno...');
            ws.send(JSON.stringify({
                type: 'OPEN_DOCUMENT',
                notebookId: notebookId,
                fileName: fileName
            }));
        }

        function renameNotebookPrompt(id, oldName) {
            const newName = prompt('Nuevo nombre del cuaderno:', oldName);
            if (newName && newName.trim() && newName.trim() !== oldName) {
                ws.send(JSON.stringify({
                    type: 'RENAME_NOTEBOOK',
                    notebookId: id,
                    newName: newName.trim()
                }));
            }
        }

        function deleteNotebookPrompt(id) {
            if (confirm('¿Mover cuaderno a la papelera? Podrás restaurarlo en la tablet.')) {
                ws.send(JSON.stringify({
                    type: 'DELETE_NOTEBOOK',
                    notebookId: id
                }));
            }
        }

        function openCreateNotebookModal() {
            document.getElementById('newNotebookName').value = '';
            document.getElementById('createNotebookModal').classList.remove('hidden');
        }

        function openCreateSubjectModal() {
            document.getElementById('newSubjectName').value = '';
            document.getElementById('createSubjectModal').classList.remove('hidden');
        }

        function closeModals() {
            document.getElementById('createNotebookModal').classList.add('hidden');
            document.getElementById('createSubjectModal').classList.add('hidden');
        }

        function submitCreateNotebook() {
            const name = document.getElementById('newNotebookName').value.trim();
            const template = document.getElementById('newNotebookTemplate').value;
            const color = parseInt(document.getElementById('newNotebookColor').value);
            closeModals();

            ws.send(JSON.stringify({
                type: 'CREATE_NOTEBOOK',
                name: name || 'Sin título',
                subjectId: currentSubjectId || '',
                coverColor: color,
                template: template
            }));
        }

        function submitCreateSubject() {
            const name = document.getElementById('newSubjectName').value.trim();
            const color = parseInt(document.getElementById('newSubjectColor').value);
            if (!name) return;
            closeModals();

            ws.send(JSON.stringify({
                type: 'CREATE_SUBJECT',
                name: name,
                color: color,
                parentId: currentSubjectId || null
            }));
        }

        // --- Editor Canvas & Drawing Engine ---
        let docSyncInterval = null;

        function startDocSyncPolling() {
            stopDocSyncPolling();
            docSyncInterval = setInterval(checkDocumentVersion, 2500);
        }

        function stopDocSyncPolling() {
            if (docSyncInterval) {
                clearInterval(docSyncInterval);
                docSyncInterval = null;
            }
        }

        function checkDocumentVersion() {
            if (!currentDoc || !ws || ws.readyState !== WebSocket.OPEN) {
                return;
            }
            ws.send(JSON.stringify({
                type: 'CHECK_DOCUMENT_VERSION',
                notebookId: currentDoc.notebookId,
                version: currentDoc.version || 1
            }));
        }

        function loadDocumentIntoEditor(docData) {
            currentDoc = docData;
            currentPageIndex = 0;

            editorDocTitle.textContent = docData.title || 'Cuaderno';
            showSavedStatus('Sincronizado ✓');

            libraryView.classList.add('hidden');
            editorView.classList.remove('hidden');

            renderCurrentPage();
            startDocSyncPolling();
        }

        function closeDocumentAndReturn() {
            stopDocSyncPolling();
            if (activeTextPt) {
                commitTextEditor();
            }
            editorView.classList.add('hidden');
            libraryView.classList.remove('hidden');
            currentDoc = null;
        }

        function renderCurrentPage() {
            if (!currentDoc || !currentDoc.pages || currentDoc.pages.length === 0) return;
            const page = currentDoc.pages[currentPageIndex];
            if (!page) return;

            pageIndicator.textContent = (currentPageIndex + 1) + ' / ' + currentDoc.pages.length;

            const ptWidth = page.width || 595.276;
            const ptHeight = page.height || 841.890;

            // Scale to physical pixels on canvas
            const scale = 1.4; // 1.4x scale for crisp reading on PC
            const pixelRatio = window.devicePixelRatio || 1;

            canvas.width = ptWidth * scale * pixelRatio;
            canvas.height = ptHeight * scale * pixelRatio;

            canvas.style.width = (ptWidth * scale) + 'px';
            canvas.style.height = (ptHeight * scale) + 'px';

            ctx.save();
            ctx.scale(scale * pixelRatio, scale * pixelRatio);

            // 1. Draw Page Background
            drawPageBackground(ctx, page, ptWidth, ptHeight);

            // 2. Draw Existing Layers, Strokes and Text
            if (page.layers) {
                page.layers.forEach(layer => {
                    if (layer.elements) {
                        layer.elements.forEach(elem => {
                            if (elem.type === 'stroke') {
                                drawStroke(ctx, elem);
                            } else if (elem.type === 'text') {
                                drawText(ctx, elem);
                            }
                        });
                    }
                });
            }

            ctx.restore();
        }

        function drawPageBackground(ctx, page, w, h) {
            ctx.fillStyle = '#ffffff';
            ctx.fillRect(0, 0, w, h);

            const bg = page.background || { style: 'ruled' };
            const style = bg.style || 'ruled';

            ctx.lineWidth = 0.5;
            ctx.strokeStyle = '#dbeafe'; // Light blue lines

            if (style === 'ruled') {
                const lineSpacing = 24.0;
                for (let y = lineSpacing * 2; y < h - lineSpacing; y += lineSpacing) {
                    ctx.beginPath();
                    ctx.moveTo(0, y);
                    ctx.lineTo(w, y);
                    ctx.stroke();
                }
                // Vertical margin
                ctx.strokeStyle = '#fecdd3'; // Faint pink margin
                ctx.beginPath();
                ctx.moveTo(72.0, 0);
                ctx.lineTo(72.0, h);
                ctx.stroke();
            } else if (style === 'graph') {
                const gridSize = 14.17; // ~5mm
                ctx.strokeStyle = '#e2e8f0';
                for (let x = 0; x < w; x += gridSize) {
                    ctx.beginPath();
                    ctx.moveTo(x, 0);
                    ctx.lineTo(x, h);
                    ctx.stroke();
                }
                for (let y = 0; y < h; y += gridSize) {
                    ctx.beginPath();
                    ctx.moveTo(0, y);
                    ctx.lineTo(w, y);
                    ctx.stroke();
                }
            } else if (style === 'dotted') {
                const dotSize = 18.0;
                ctx.fillStyle = '#cbd5e1';
                for (let x = dotSize; x < w; x += dotSize) {
                    for (let y = dotSize; y < h; y += dotSize) {
                        ctx.beginPath();
                        ctx.arc(x, y, 0.7, 0, Math.PI * 2);
                        ctx.fill();
                    }
                }
            }
        }

        function drawStroke(ctx, stroke) {
            if (!stroke.points || stroke.points.length < 2) return;

            const isHighlighter = stroke.tool === 'highlighter';
            const color = stroke.color;
            const alpha = isHighlighter ? 0.35 : (((color >> 24) & 0xFF) / 255.0 || 1.0);
            const r = (color >> 16) & 0xFF;
            const g = (color >> 8) & 0xFF;
            const b = color & 0xFF;

            ctx.strokeStyle = 'rgba(' + r + ',' + g + ',' + b + ',' + alpha + ')';
            ctx.fillStyle = ctx.strokeStyle;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';

            const pts = stroke.points;
            ctx.beginPath();
            ctx.moveTo(pts[0].x, pts[0].y);

            for (let i = 1; i < pts.length; i++) {
                ctx.lineWidth = pts[i].w || 1.5;
                ctx.lineTo(pts[i].x, pts[i].y);
            }
            ctx.stroke();
        }

        function drawText(ctx, textElem) {
            if (!textElem || !textElem.content) return;
            const color = textElem.color !== undefined ? textElem.color : 0xFF000000;
            const alpha = (((color >> 24) & 0xFF) / 255.0) || 1.0;
            const r = (color >> 16) & 0xFF;
            const g = (color >> 8) & 0xFF;
            const b = color & 0xFF;
            ctx.fillStyle = 'rgba(' + r + ',' + g + ',' + b + ',' + alpha + ')';

            const size = textElem.size || 14.0;
            ctx.font = size + 'px "Liberation Sans", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
            ctx.textBaseline = 'top';

            const lines = String(textElem.content).split('\n');
            const lineHeight = size * 1.25;
            for (let i = 0; i < lines.length; i++) {
                ctx.fillText(lines[i], textElem.x, textElem.y + (i * lineHeight));
            }
        }

        function appendElementToCurrentPage(elem) {
            const page = currentDoc.pages[currentPageIndex];
            if (!page) return;
            if (!page.layers || page.layers.length === 0) {
                page.layers = [{ elements: [] }];
            }
            page.layers[page.layers.length - 1].elements.push(elem);
            renderCurrentPage();
        }

        function appendStrokeToCurrentPage(stroke) {
            appendElementToCurrentPage(stroke);
        }

        function findTextElementAt(xPt, yPt) {
            if (!currentDoc || !currentDoc.pages) return null;
            const page = currentDoc.pages[currentPageIndex];
            if (!page || !page.layers) return null;
            for (let i = page.layers.length - 1; i >= 0; i--) {
                const layer = page.layers[i];
                if (!layer.elements) continue;
                for (let j = layer.elements.length - 1; j >= 0; j--) {
                    const el = layer.elements[j];
                    if (el.type === 'text') {
                        const lines = String(el.content).split('\n');
                        let maxLineLen = 0;
                        for (let k = 0; k < lines.length; k++) {
                            if (lines[k].length > maxLineLen) maxLineLen = lines[k].length;
                        }
                        const size = el.size || 14.0;
                        const w = Math.max(50, maxLineLen * size * 0.65);
                        const h = Math.max(size * 1.5, lines.length * size * 1.3);
                        if (xPt >= el.x && xPt <= el.x + w && yPt >= el.y - 4 && yPt <= el.y + h) {
                            return el;
                        }
                    }
                }
            }
            return null;
        }

        function openTextEditorAt(ptX, ptY, existingElem) {
            activeTextPt = {
                x: existingElem ? existingElem.x : ptX,
                y: existingElem ? existingElem.y : ptY,
                existing: existingElem || null
            };
            const scale = 1.4;
            const overlay = document.getElementById('textEditorOverlay');
            const input = document.getElementById('textEditorInput');

            overlay.style.left = (activeTextPt.x * scale) + 'px';
            overlay.style.top = (activeTextPt.y * scale) + 'px';

            const fontSizePx = Math.round(14 * scale);
            input.style.fontSize = fontSizePx + 'px';
            const color = currentColor;
            const r = (color >> 16) & 0xFF;
            const g = (color >> 8) & 0xFF;
            const b = color & 0xFF;
            input.style.color = 'rgb(' + r + ',' + g + ',' + b + ')';

            input.value = existingElem ? existingElem.content : '';
            overlay.classList.remove('hidden');

            input.style.height = 'auto';
            input.style.height = Math.max(32, input.scrollHeight) + 'px';

            setTimeout(() => {
                input.focus();
                if (input.value) {
                    input.setSelectionRange(input.value.length, input.value.length);
                }
            }, 50);
        }

        function commitTextEditor() {
            if (!activeTextPt) return;
            const input = document.getElementById('textEditorInput');
            const content = input.value.trim();
            const overlay = document.getElementById('textEditorOverlay');
            overlay.classList.add('hidden');

            const info = activeTextPt;
            activeTextPt = null;

            if (!content) {
                renderCurrentPage();
                return;
            }

            if (info.existing) {
                info.existing.content = content;
                info.existing.color = currentColor;
                if (ws && ws.readyState === WebSocket.OPEN && currentDoc) {
                    showSavedStatus('Sincronizando texto...');
                    ws.send(JSON.stringify({
                        type: 'ADD_TEXT',
                        notebookId: currentDoc.notebookId,
                        pageIndex: currentPageIndex,
                        text: info.existing
                    }));
                }
            } else {
                const textElem = {
                    type: 'text',
                    font: 'Liberation Sans',
                    size: 14.0,
                    x: info.x,
                    y: info.y,
                    color: currentColor,
                    content: content
                };
                appendElementToCurrentPage(textElem);

                if (ws && ws.readyState === WebSocket.OPEN && currentDoc) {
                    showSavedStatus('Sincronizando texto...');
                    ws.send(JSON.stringify({
                        type: 'ADD_TEXT',
                        notebookId: currentDoc.notebookId,
                        pageIndex: currentPageIndex,
                        text: textElem
                    }));
                }
            }

            renderCurrentPage();
        }

        function cancelTextEditor() {
            activeTextPt = null;
            const overlay = document.getElementById('textEditorOverlay');
            overlay.classList.add('hidden');
            renderCurrentPage();
        }

        // --- Eraser Geometry & Hit Testing ---
        let isErasing = false;
        let erasedPoints = [];
        let didEraseAny = false;

        function pointSegmentDist(px, py, ax, ay, bx, by) {
            const dx = bx - ax;
            const dy = by - ay;
            const lenSq = dx * dx + dy * dy;
            if (lenSq === 0) {
                const dxx = px - ax;
                const dyy = py - ay;
                return Math.sqrt(dxx * dxx + dyy * dyy);
            }
            let t = ((px - ax) * dx + (py - ay) * dy) / lenSq;
            if (t < 0) t = 0;
            if (t > 1) t = 1;
            const nx = ax + t * dx;
            const ny = ay + t * dy;
            const dxx = px - nx;
            const dyy = py - ny;
            return Math.sqrt(dxx * dxx + dyy * dyy);
        }

        function strokeHitsEraser(stroke, px, py, radius) {
            const pts = stroke.points || [];
            if (pts.length === 0) return false;
            if (pts.length === 1) {
                const p = pts[0];
                const w = p.w !== undefined ? p.w : (p.width !== undefined ? p.width : 1.5);
                const reach = radius + w / 2.0;
                const dx = px - p.x;
                const dy = py - p.y;
                return Math.sqrt(dx * dx + dy * dy) <= reach;
            }
            for (let i = 1; i < pts.length; i++) {
                const a = pts[i - 1];
                const b = pts[i];
                const wa = a.w !== undefined ? a.w : (a.width !== undefined ? a.width : 1.5);
                const wb = b.w !== undefined ? b.w : (b.width !== undefined ? b.width : 1.5);
                const reach = radius + Math.max(wa, wb) / 2.0;
                if (pointSegmentDist(px, py, a.x, a.y, b.x, b.y) <= reach) return true;
            }
            return false;
        }

        function textHitsEraser(textElem, px, py, radius) {
            if (!textElem) return false;
            const x = textElem.x || 0;
            const y = textElem.y || 0;
            const lines = String(textElem.content || '').split('\n');
            let maxLineLen = 0;
            for (let k = 0; k < lines.length; k++) {
                if (lines[k].length > maxLineLen) maxLineLen = lines[k].length;
            }
            const size = textElem.size || 14.0;
            const w = Math.max(50, maxLineLen * size * 0.65);
            const h = Math.max(size * 1.5, lines.length * size * 1.3);

            const minX = x;
            const maxX = x + w;
            const minY = y - 4;
            const maxY = y + h;

            const closestX = Math.max(minX, Math.min(px, maxX));
            const closestY = Math.max(minY, Math.min(py, maxY));
            const dx = px - closestX;
            const dy = py - closestY;
            return (dx * dx + dy * dy) <= (radius * radius);
        }

        function performEraseAt(px, py, radius) {
            if (!currentDoc || !currentDoc.pages) return false;
            const page = currentDoc.pages[currentPageIndex];
            if (!page || !page.layers) return false;

            let pageChanged = false;
            page.layers.forEach(layer => {
                if (!layer.elements || layer.elements.length === 0) return;
                const remaining = [];
                for (let i = 0; i < layer.elements.length; i++) {
                    const el = layer.elements[i];
                    let hit = false;
                    if (el.type === 'stroke') {
                        hit = strokeHitsEraser(el, px, py, radius);
                    } else if (el.type === 'text') {
                        hit = textHitsEraser(el, px, py, radius);
                    }
                    if (hit) {
                        pageChanged = true;
                    } else {
                        remaining.push(el);
                    }
                }
                layer.elements = remaining;
            });

            if (pageChanged) {
                didEraseAny = true;
                renderCurrentPage();
            }
            return pageChanged;
        }

        // Pointer / Mouse events on Canvas
        function getCanvasPoint(event) {
            const rect = canvas.getBoundingClientRect();
            const scale = 1.4;
            const x = (event.clientX - rect.left) / scale;
            const y = (event.clientY - rect.top) / scale;
            return { x: x, y: y };
        }

        canvas.addEventListener('pointerdown', (e) => {
            const pt = getCanvasPoint(e);

            if (currentTool === 'text') {
                if (activeTextPt) {
                    commitTextEditor();
                }
                const hit = findTextElementAt(pt.x, pt.y);
                openTextEditorAt(pt.x, pt.y, hit);
                return;
            }

            if (activeTextPt) {
                commitTextEditor();
            }

            if (currentTool === 'eraser') {
                isErasing = true;
                didEraseAny = false;
                const radius = Math.max(1.0, currentWidthPt);
                erasedPoints = [{ x: pt.x, y: pt.y, radius: radius }];
                canvas.setPointerCapture(e.pointerId);
                performEraseAt(pt.x, pt.y, radius);
                return;
            }

            isDrawing = true;
            canvas.setPointerCapture(e.pointerId);
            currentStrokePoints = [{ x: pt.x, y: pt.y, w: currentWidthPt }];

            const scale = 1.4;
            const pixelRatio = window.devicePixelRatio || 1;
            ctx.save();
            ctx.scale(scale * pixelRatio, scale * pixelRatio);

            const isHighlighter = currentTool === 'highlighter';
            const alpha = isHighlighter ? 0.35 : 1.0;
            const r = (currentColor >> 16) & 0xFF;
            const g = (currentColor >> 8) & 0xFF;
            const b = currentColor & 0xFF;
            ctx.strokeStyle = 'rgba(' + r + ',' + g + ',' + b + ',' + alpha + ')';
            ctx.lineWidth = currentWidthPt;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';
            ctx.beginPath();
            ctx.moveTo(pt.x, pt.y);
        });

        canvas.addEventListener('pointermove', (e) => {
            if (isErasing) {
                const pt = getCanvasPoint(e);
                const radius = Math.max(1.0, currentWidthPt);
                erasedPoints.push({ x: pt.x, y: pt.y, radius: radius });
                performEraseAt(pt.x, pt.y, radius);
                return;
            }

            if (!isDrawing) return;
            const pt = getCanvasPoint(e);
            currentStrokePoints.push({ x: pt.x, y: pt.y, w: currentWidthPt });

            ctx.lineTo(pt.x, pt.y);
            ctx.stroke();
        });

        canvas.addEventListener('pointerup', (e) => {
            if (isErasing) {
                isErasing = false;
                if (erasedPoints.length > 0 && currentDoc) {
                    if (ws && ws.readyState === WebSocket.OPEN) {
                        showSavedStatus('Sincronizando borrado...');
                        ws.send(JSON.stringify({
                            type: 'ERASE_STROKES',
                            notebookId: currentDoc.notebookId,
                            pageIndex: currentPageIndex,
                            points: erasedPoints
                        }));
                    }
                }
                erasedPoints = [];
                return;
            }

            if (!isDrawing) return;
            isDrawing = false;
            ctx.restore();

            if (currentStrokePoints.length < 2) return;

            const strokeData = {
                type: 'stroke',
                tool: currentTool,
                color: currentColor,
                capStyle: 'round',
                uniformWidth: true,
                lineStyle: 'plain',
                points: currentStrokePoints
            };

            appendStrokeToCurrentPage(strokeData);

            // Send to tablet via WebSocket
            if (ws && ws.readyState === WebSocket.OPEN && currentDoc) {
                showSavedStatus('Sincronizando...');
                ws.send(JSON.stringify({
                    type: 'ADD_STROKE',
                    notebookId: currentDoc.notebookId,
                    pageIndex: currentPageIndex,
                    stroke: strokeData
                }));
            }
        });

        function setTool(tool) {
            if (activeTextPt) {
                commitTextEditor();
            }
            currentTool = tool;
            document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
            if (tool === 'text') {
                document.getElementById('toolText').classList.add('active');
                canvas.style.cursor = 'text';
            } else if (tool === 'pen') {
                document.getElementById('toolPen').classList.add('active');
                currentWidthPt = 1.5;
                canvas.style.cursor = 'crosshair';
            } else if (tool === 'highlighter') {
                document.getElementById('toolHighlighter').classList.add('active');
                currentWidthPt = 8.0;
                canvas.style.cursor = 'crosshair';
            } else if (tool === 'eraser') {
                document.getElementById('toolEraser').classList.add('active');
                currentWidthPt = 12.0;
                canvas.style.cursor = 'crosshair';
            }
            document.getElementById('widthSlider').value = currentWidthPt;
            document.getElementById('widthValue').textContent = currentWidthPt;
        }

        function setColor(argb, elem) {
            currentColor = argb;
            document.querySelectorAll('.swatch').forEach(s => s.classList.remove('selected'));
            if (elem) elem.classList.add('selected');
        }

        function updateWidth(val) {
            currentWidthPt = parseFloat(val);
            document.getElementById('widthValue').textContent = currentWidthPt;
        }

        function prevPage() {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                renderCurrentPage();
            }
        }

        function nextPage() {
            if (currentDoc && currentPageIndex < currentDoc.pages.length - 1) {
                currentPageIndex++;
                renderCurrentPage();
            }
        }

        function addPagePrompt() {
            if (!ws || !currentDoc) return;
            showSavedStatus('Añadiendo página...');
            ws.send(JSON.stringify({
                type: 'ADD_PAGE',
                notebookId: currentDoc.notebookId,
                width: 595.276,
                height: 841.890,
                template: 'ruled'
            }));
        }

        function saveDocumentExplicit() {
            if (!ws || !currentDoc) return;
            showSavedStatus('Guardando...');
            ws.send(JSON.stringify({
                type: 'SAVE_DOCUMENT',
                notebookId: currentDoc.notebookId
            }));
        }

        function showSavedStatus(text) {
            saveStatus.textContent = text;
            saveStatus.style.color = 'var(--success)';
        }

        function escapeHtml(str) {
            return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }

        // Auto-connect if initialToken is provided
        window.addEventListener('DOMContentLoaded', () => {
            const textInput = document.getElementById('textEditorInput');
            if (textInput) {
                textInput.addEventListener('input', () => {
                    textInput.style.height = 'auto';
                    textInput.style.height = Math.max(32, textInput.scrollHeight) + 'px';
                });
                textInput.addEventListener('keydown', (e) => {
                    if (e.key === 'Escape') {
                        e.preventDefault();
                        cancelTextEditor();
                    } else if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
                        e.preventDefault();
                        commitTextEditor();
                    }
                });
            }

            if (currentToken && currentToken.length > 0) {
                connectWebSocket(currentToken);
            } else {
                libraryView.classList.add('hidden');
                pairingView.classList.remove('hidden');
                setStatus(false, 'Esperando código');
            }
        });
    </script>
</body>
</html>
        """.trimIndent()
    }
}
