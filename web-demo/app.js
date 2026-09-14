/**
 * Stickr - Application Web Interactive (Demo Localhost:8080)
 * Reproduit fidèlement l'application Android native Stickr :
 * Clean Architecture, Room persistence, MediaPipe Vision, Die-Cut border, WebP WhatsApp 512x512
 */

// --- DONNÉES PAR DÉFAUT & PERSISTANCE ---
const DEFAULT_PACKS = [
  {
    id: "pack_manga_mèmes",
    name: "Mèmes & Réactions 🔥",
    publisher: "Stickr Studio",
    trayImagePath: null,
    stickers: [
      { id: "s1", emojis: "😎,🔥", url: createSampleSticker("😎", "#FFE600", "COOL DUDE") },
      { id: "s2", emojis: "👑,✨", url: createSampleSticker("👑", "#FFD700", "QUEEN") },
      { id: "s3", emojis: "🚀,💎", url: createSampleSticker("🚀", "#00F2FE", "TO THE MOON") }
    ]
  },
  {
    id: "pack_animals",
    name: "Animaux Mignons 🐱",
    publisher: "CatLover",
    trayImagePath: null,
    stickers: [
      { id: "s4", emojis: "🐱,❤️", url: createSampleSticker("🐱", "#FF6B8B", "MEOW") },
      { id: "s5", emojis: "🐶,✨", url: createSampleSticker("🐶", "#FFA07A", "WOOF") }
    ]
  }
];

let packs = JSON.parse(localStorage.getItem("stickr_packs") || "null") || DEFAULT_PACKS;
function savePacks() {
  localStorage.setItem("stickr_packs", JSON.stringify(packs));
}

// État global de l'application
let currentPackId = null;
let activeScreen = "dashboard";

// État de l'éditeur tactile multi-calques
let editorState = {
  originalImage: null,
  cutoutImage: null,
  isCutout: false,
  borderSize: 0,
  borderColor: "#FFFFFF",
  layers: [], // [ { id, type: 'subject'|'text'|'deco', offset:{x,y}, scale, rotation, ... } ]
  selectedLayerId: "subject",
  undoStack: [],
  redoStack: []
};

// Catégories d'accessoires
const DECORATION_CATEGORIES = [
  ["😎", "🕶️", "👑", "🎩", "🧢", "🎀", "🥸", "🎧", "💍", "💄"],
  ["🔥", "💯", "💥", "⭐", "✨", "🚀", "💣", "💎", "🎉", "🥳"],
  ["💬", "💭", "❤️", "💔", "💀", "🤡", "👻", "🤖", "👽", "😈"],
  ["🍕", "🍔", "🌮", "🍩", "🍦", "☕", "🍺", "🥑", "🏆", "🎮"]
];

// --- INITIALISATION ---
window.addEventListener("DOMContentLoaded", () => {
  updateClock();
  setInterval(updateClock, 1000);
  initNavigation();
  initDashboard();
  initPackDetail();
  initEditor();
  renderDashboard();
});

function updateClock() {
  const now = new Date();
  const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  const el = document.getElementById("status-time");
  if (el) el.textContent = timeStr;
}

// --- NAVIGATION ---
function initNavigation() {
  window.navigateTo = (screenId) => {
    document.querySelectorAll(".screen").forEach(s => s.classList.remove("active"));
    const target = document.getElementById(`screen-${screenId}`);
    if (target) target.classList.add("active");
    activeScreen = screenId;

    if (screenId === "dashboard") renderDashboard();
    if (screenId === "pack-detail") renderPackDetail();
    if (screenId === "editor") renderEditorCanvas();
  };

  document.getElementById("btn-back-to-dashboard").addEventListener("click", () => navigateTo("dashboard"));
  document.getElementById("btn-back-to-pack").addEventListener("click", () => navigateTo("pack-detail"));
}

// --- DASHBOARD ---
function initDashboard() {
  const fab = document.getElementById("fab-create-pack");
  const modal = document.getElementById("modal-create-pack");
  const btnCancel = document.getElementById("btn-cancel-create-pack");
  const btnConfirm = document.getElementById("btn-confirm-create-pack");
  const inputName = document.getElementById("input-pack-name");
  const inputPublisher = document.getElementById("input-pack-publisher");

  fab.addEventListener("click", () => {
    inputName.value = "";
    inputPublisher.value = "";
    modal.classList.remove("hidden");
  });

  document.getElementById("btn-empty-create").addEventListener("click", () => {
    modal.classList.remove("hidden");
  });

  btnCancel.addEventListener("click", () => modal.classList.add("hidden"));

  btnConfirm.addEventListener("click", () => {
    const name = inputName.value.trim();
    const publisher = inputPublisher.value.trim() || "Stickr Maker";
    if (!name) return;

    const newPack = {
      id: "pack_" + Date.now(),
      name,
      publisher,
      trayImagePath: null,
      stickers: []
    };

    packs.unshift(newPack);
    savePacks();
    modal.classList.add("hidden");
    currentPackId = newPack.id;
    navigateTo("pack-detail");
    showToast(`Pack "${name}" créé avec succès !`);
  });
}

function renderDashboard() {
  const listEl = document.getElementById("packs-list");
  const emptyEl = document.getElementById("empty-dashboard");

  if (packs.length === 0) {
    listEl.innerHTML = "";
    emptyEl.classList.remove("hidden");
    return;
  }

  emptyEl.classList.add("hidden");
  listEl.innerHTML = packs.map(pack => {
    const count = pack.stickers.length;
    const isReady = count >= 3 && count <= 30;
    const thumbUrl = pack.stickers[0] ? pack.stickers[0].url : "";

    return `
      <div class="pack-card" onclick="openPackDetail('${pack.id}')">
        <div class="pack-card-header">
          <div class="pack-thumb">
            ${thumbUrl ? `<img src="${thumbUrl}" alt="${pack.name}">` : `<span>🎨</span>`}
          </div>
          <div class="pack-info">
            <h3>${escapeHtml(pack.name)}</h3>
            <p>Par ${escapeHtml(pack.publisher)}</p>
            <span class="badge badge-counter">${count}/30 stickers</span>
          </div>
        </div>
        <div class="pack-card-footer">
          <div class="badge-status ${isReady ? 'badge-ready' : 'badge-waiting'}">
            <span>${isReady ? '✓' : '⚠️'}</span>
            <span>${isReady ? 'Prêt pour WhatsApp' : `Ajoutez encore ${3 - count} sticker(s)`}</span>
          </div>
          ${isReady ? `
            <button class="btn btn-whatsapp" onclick="event.stopPropagation(); openWhatsAppExport('${pack.id}')" style="padding: 6px 12px; font-size:12px;">
              🚀 WhatsApp
            </button>
          ` : ''}
        </div>
      </div>
    `;
  }).join("");
}

window.openPackDetail = (packId) => {
  currentPackId = packId;
  navigateTo("pack-detail");
};

// --- PACK DETAIL ---
function initPackDetail() {
  document.getElementById("fab-add-sticker").addEventListener("click", () => {
    resetEditor();
    navigateTo("editor");
  });

  document.getElementById("btn-export-whatsapp").addEventListener("click", () => {
    if (currentPackId) openWhatsAppExport(currentPackId);
  });

  document.getElementById("btn-delete-current-pack").addEventListener("click", () => {
    if (!currentPackId) return;
    const pack = packs.find(p => p.id === currentPackId);
    if (!pack) return;

    if (confirm(`Supprimer définitivement le pack « ${pack.name} » et tous ses stickers ?`)) {
      packs = packs.filter(p => p.id !== currentPackId);
      savePacks();
      showToast("Pack supprimé.");
      navigateTo("dashboard");
    }
  });
}

function renderPackDetail() {
  const pack = packs.find(p => p.id === currentPackId);
  if (!pack) return navigateTo("dashboard");

  document.getElementById("pack-detail-title").textContent = pack.name;
  document.getElementById("pack-detail-name").textContent = pack.name;
  document.getElementById("pack-detail-author").textContent = `Par ${pack.publisher}`;
  document.getElementById("pack-detail-counter").textContent = `${pack.stickers.length}/30 stickers`;

  const trayThumb = document.getElementById("pack-detail-tray");
  if (pack.stickers.length > 0) {
    trayThumb.innerHTML = `<img src="${pack.stickers[0].url}" alt="Tray">`;
  } else {
    trayThumb.innerHTML = `<span>🎨</span>`;
  }

  const gridEl = document.getElementById("stickers-grid");
  const count = pack.stickers.length;
  const isReady = count >= 3 && count <= 30;

  const btnExport = document.getElementById("btn-export-whatsapp");
  const hintEl = document.getElementById("whatsapp-hint");

  btnExport.disabled = !isReady;
  hintEl.textContent = isReady
    ? "✓ Le pack respecte toutes les contraintes de WhatsApp"
    : `WhatsApp requiert au moins 3 stickers (actuel : ${count})`;

  if (pack.stickers.length === 0) {
    gridEl.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 40px 10px; color: #888;">
        <p>Aucun sticker dans ce pack.</p>
        <p style="font-size: 13px; margin-top: 6px;">Clique sur <strong>+</strong> pour créer ton premier sticker !</p>
      </div>
    `;
    return;
  }

  gridEl.innerHTML = pack.stickers.map(s => `
    <div class="sticker-item">
      <img src="${s.url}" alt="Sticker">
      <span class="sticker-emoji-tag">${s.emojis || '✨'}</span>
      <button class="sticker-delete-btn" onclick="deleteSticker('${s.id}')" title="Supprimer">✕</button>
    </div>
  `).join("");
}

window.deleteSticker = (stickerId) => {
  const pack = packs.find(p => p.id === currentPackId);
  if (!pack) return;
  pack.stickers = pack.stickers.filter(s => s.id !== stickerId);
  savePacks();
  renderPackDetail();
  showToast("Sticker supprimé");
};

// --- WHATSAPP EXPORT MODAL ---
window.openWhatsAppExport = (packId) => {
  const pack = packs.find(p => p.id === packId);
  if (!pack) return;

  const modal = document.getElementById("modal-whatsapp-export");
  document.getElementById("wa-pack-name").textContent = pack.name;
  document.getElementById("wa-pack-publisher").textContent = `Éditeur officiel : ${pack.publisher}`;
  const trayImg = document.getElementById("wa-tray-img");
  trayImg.src = pack.stickers[0] ? pack.stickers[0].url : "";

  modal.classList.remove("hidden");

  document.getElementById("btn-close-wa-modal").onclick = () => modal.classList.add("hidden");
  document.getElementById("btn-confirm-wa-export").onclick = () => {
    modal.classList.add("hidden");
    showToast(`Pack "${pack.name}" ajouté à WhatsApp avec succès ! 🚀`);
  };
};

// --- ÉDITEUR TACTILE MULTI-CALQUES (CANVAS ENGINE) ---
const canvas = document.getElementById("sticker-canvas");
const ctx = canvas.getContext("2d");

function initEditor() {
  // Input fichier image
  const inputImage = document.getElementById("input-image-file");
  inputImage.addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        loadImageToEditor(img);
      };
      img.src = event.target.result;
    };
    reader.readAsDataURL(file);
  });

  // Exemple photo
  document.getElementById("btn-loadSample").addEventListener("click", () => {
    loadSamplePhoto();
  });

  // Bouton Détourage IA
  const btnAi = document.getElementById("btn-ai-segment");
  btnAi.addEventListener("click", () => {
    if (!editorState.originalImage) return;
    performAiSegmentation();
  });

  // Slider Épaisseur contour
  const borderSlider = document.getElementById("border-size-slider");
  const borderLabel = document.getElementById("border-size-label");
  borderSlider.addEventListener("input", (e) => {
    const size = parseInt(e.target.value);
    borderLabel.textContent = `${size} px`;
    editorState.borderSize = size;
    updateSubjectLayerBorder(size, editorState.borderColor);
    renderEditorCanvas();
  });

  // Palette couleur contour
  document.querySelectorAll("#border-color-palette .color-dot").forEach(dot => {
    dot.addEventListener("click", () => {
      document.querySelectorAll("#border-color-palette .color-dot").forEach(d => d.classList.remove("selected"));
      dot.classList.add("selected");
      const color = dot.dataset.color;
      editorState.borderColor = color;
      updateSubjectLayerBorder(editorState.borderSize, color);
      renderEditorCanvas();
    });
  });

  // Bouton Réinitialiser cadrage
  document.getElementById("btn-reset-transform").addEventListener("click", () => {
    const active = getActiveLayer();
    if (active) {
      active.offset = { x: 0, y: 0 };
      active.scale = 1.0;
      active.rotation = 0;
      renderEditorCanvas();
    }
  });

  // Undo / Redo
  document.getElementById("btn-undo").addEventListener("click", () => {
    if (editorState.undoStack.length > 0) {
      editorState.redoStack.push(editorState.borderSize);
      const prev = editorState.undoStack.pop();
      editorState.borderSize = prev;
      borderSlider.value = prev;
      borderLabel.textContent = `${prev} px`;
      updateSubjectLayerBorder(prev, editorState.borderColor);
      renderEditorCanvas();
    }
  });

  document.getElementById("btn-redo").addEventListener("click", () => {
    if (editorState.redoStack.length > 0) {
      editorState.undoStack.push(editorState.borderSize);
      const next = editorState.redoStack.pop();
      editorState.borderSize = next;
      borderSlider.value = next;
      borderLabel.textContent = `${next} px`;
      updateSubjectLayerBorder(next, editorState.borderColor);
      renderEditorCanvas();
    }
  });

  // Dialogue de Texte
  initTextEditDialog();

  // BottomSheet Accessoires
  initDecorationSheet();

  // Interaction Drag & Gestures sur Canvas
  initCanvasGestures();

  // Bouton Enregistrer le sticker final (WebP 512x512)
  document.getElementById("btn-save-sticker").addEventListener("click", () => {
    saveStickerFinal();
  });
}

function resetEditor() {
  editorState = {
    originalImage: null,
    cutoutImage: null,
    isCutout: false,
    borderSize: 0,
    borderColor: "#FFFFFF",
    layers: [],
    selectedLayerId: "subject",
    undoStack: [],
    redoStack: []
  };
  document.getElementById("canvas-placeholder").classList.remove("hidden");
  document.getElementById("border-size-slider").value = 0;
  document.getElementById("border-size-label").textContent = "0 px";
  document.getElementById("ai-btn-text").textContent = "Détourer IA";
  updateLayerChips();
  ctx.clearRect(0, 0, 512, 512);
}

function loadImageToEditor(img) {
  editorState.originalImage = img;
  editorState.cutoutImage = null;
  editorState.isCutout = false;
  editorState.borderSize = 0;
  editorState.borderColor = "#FFFFFF";

  const subjectLayer = {
    id: "subject",
    type: "subject",
    image: img,
    offset: { x: 0, y: 0 },
    scale: 1.0,
    rotation: 0,
    borderSize: 0,
    borderColor: "#FFFFFF"
  };

  editorState.layers = [subjectLayer];
  editorState.selectedLayerId = "subject";

  document.getElementById("canvas-placeholder").classList.add("hidden");
  document.getElementById("border-size-slider").value = 0;
  document.getElementById("border-size-label").textContent = "0 px";
  document.getElementById("ai-btn-text").textContent = "Détourer IA";

  updateLayerChips();
  renderEditorCanvas();
}

function updateSubjectLayerBorder(size, color) {
  const subj = editorState.layers.find(l => l.id === "subject");
  if (subj) {
    subj.borderSize = size;
    subj.borderColor = color;
  }
}

// Simule le détourage IA MediaPipe localement
function performAiSegmentation() {
  const loader = document.getElementById("ai-loader");
  loader.classList.remove("hidden");

  setTimeout(() => {
    const srcImg = editorState.originalImage;
    if (!srcImg) return;

    // Créer un masque de découpe sémantique
    const tempCanvas = document.createElement("canvas");
    tempCanvas.width = srcImg.width;
    tempCanvas.height = srcImg.height;
    const tCtx = tempCanvas.getContext("2d");
    tCtx.drawImage(srcImg, 0, 0);

    const imgData = tCtx.getImageData(0, 0, tempCanvas.width, tempCanvas.height);
    const data = imgData.data;

    // Détection sémantique simplifiée pour la démo (détourage d'arrière-plan doux)
    const centerX = tempCanvas.width / 2;
    const centerY = tempCanvas.height / 2;
    const radius = Math.min(centerX, centerY) * 0.95;

    for (let y = 0; y < tempCanvas.height; y++) {
      for (let x = 0; x < tempCanvas.width; x++) {
        const idx = (y * tempCanvas.width + x) * 4;
        const dx = x - centerX;
        const dy = y - centerY;
        const dist = Math.sqrt(dx * dx + dy * dy);

        // Clé de segmentation douce basée sur les bords
        if (dist > radius) {
          data[idx + 3] = 0; // Transparent
        } else if (dist > radius - 15) {
          const alpha = (radius - dist) / 15;
          data[idx + 3] = Math.round(data[idx + 3] * alpha);
        }
      }
    }

    tCtx.putImageData(imgData, 0, 0);

    const cutoutImg = new Image();
    cutoutImg.onload = () => {
      editorState.cutoutImage = cutoutImg;
      editorState.isCutout = true;

      const subj = editorState.layers.find(l => l.id === "subject");
      if (subj) subj.image = cutoutImg;

      // Appliquer une bordure blanche die-cut par défaut de 12px
      editorState.borderSize = 12;
      document.getElementById("border-size-slider").value = 12;
      document.getElementById("border-size-label").textContent = "12 px";
      updateSubjectLayerBorder(12, editorState.borderColor);

      document.getElementById("ai-btn-text").textContent = "Re-détourer";
      loader.classList.add("hidden");
      renderEditorCanvas();
      showToast("Détourage IA MediaPipe réussi ! ✨");
    };
    cutoutImg.src = tempCanvas.toDataURL("image/png");
  }, 900);
}

// Rendu Canvas Multi-calques fidèle à StickerFlattener.kt
function renderEditorCanvas() {
  ctx.clearRect(0, 0, 512, 512);

  editorState.layers.forEach(layer => {
    ctx.save();
    ctx.translate(256 + layer.offset.x, 256 + layer.offset.y);
    ctx.rotate((layer.rotation * Math.PI) / 180);
    ctx.scale(layer.scale, layer.scale);

    if (layer.type === "subject" && layer.image) {
      renderSubjectLayerCanvas(ctx, layer);
    } else if (layer.type === "text") {
      renderTextLayerCanvas(ctx, layer);
    } else if (layer.type === "deco") {
      renderDecorationLayerCanvas(ctx, layer);
    }

    // Si calque actif sélectionné : dessine le cadre visuel cyan
    if (layer.id === editorState.selectedLayerId && editorState.layers.length > 1) {
      drawBoundingBox(ctx, layer);
    }

    ctx.restore();
  });
}

function renderSubjectLayerCanvas(targetCtx, layer) {
  const img = layer.image;
  const maxDim = 460;
  const scaleFactor = Math.min(maxDim / img.width, maxDim / img.height);
  const destW = img.width * scaleFactor;
  const destH = img.height * scaleFactor;

  // Si bordure sticker demandée : dilatation radiale
  if (layer.borderSize > 0) {
    const borderCanvas = document.createElement("canvas");
    borderCanvas.width = 512;
    borderCanvas.height = 512;
    const bCtx = borderCanvas.getContext("2d");

    // Dessine l'image en silhouette blanche
    bCtx.drawImage(img, (512 - destW) / 2, (512 - destH) / 2, destW, destH);
    bCtx.globalCompositeOperation = "source-in";
    bCtx.fillStyle = layer.borderColor || "#FFFFFF";
    bCtx.fillRect(0, 0, 512, 512);

    const bSize = layer.borderSize;
    const step = 3;
    for (let r = step; r <= bSize; r += step) {
      const num = Math.max(8, Math.round((2 * Math.PI * r) / step));
      for (let i = 0; i < num; i++) {
        const ang = (i * 2 * Math.PI) / num;
        targetCtx.drawImage(borderCanvas, -256 + Math.cos(ang) * r, -256 + Math.sin(ang) * r);
      }
    }
  }

  // Image principale
  targetCtx.drawImage(img, -destW / 2, -destH / 2, destW, destH);
}

function renderTextLayerCanvas(targetCtx, layer) {
  targetCtx.font = `bold ${layer.fontSize}px ${layer.fontFamily || 'Impact'}, sans-serif`;
  targetCtx.textAlign = "center";
  targetCtx.textBaseline = "middle";

  // 1. Contour de lettre
  if (layer.strokeWidth > 0) {
    targetCtx.strokeStyle = layer.strokeColor || "#000000";
    targetCtx.lineWidth = layer.strokeWidth * 2;
    targetCtx.lineJoin = "round";
    targetCtx.strokeText(layer.text, 0, 0);
  }

  // 2. Remplissage
  targetCtx.fillStyle = layer.textColor || "#FFFFFF";
  targetCtx.fillText(layer.text, 0, 0);
}

function renderDecorationLayerCanvas(targetCtx, layer) {
  targetCtx.font = `${layer.size || 72}px sans-serif`;
  targetCtx.textAlign = "center";
  targetCtx.textBaseline = "middle";
  targetCtx.fillText(layer.emoji, 0, 0);
}

function drawBoundingBox(targetCtx, layer) {
  targetCtx.strokeStyle = "#00F2FE";
  targetCtx.lineWidth = 2;
  targetCtx.setLineDash([6, 4]);

  let boxW = 80, boxH = 80;
  if (layer.type === "text") {
    const m = targetCtx.measureText(layer.text);
    boxW = m.width + 30;
    boxH = layer.fontSize * 1.5;
  }

  targetCtx.strokeRect(-boxW / 2, -boxH / 2, boxW, boxH);
  targetCtx.setLineDash([]);

  // Poignée 'X' suppression
  targetCtx.fillStyle = "#E53935";
  targetCtx.beginPath();
  targetCtx.arc(boxW / 2, -boxH / 2, 11, 0, Math.PI * 2);
  targetCtx.fill();

  targetCtx.fillStyle = "#FFFFFF";
  targetCtx.font = "bold 12px sans-serif";
  targetCtx.fillText("✕", boxW / 2, -boxH / 2);
}

// --- GESTION DES CALQUES & CHIPS ---
function updateLayerChips() {
  const chipsBar = document.getElementById("layer-chips-bar");
  chipsBar.innerHTML = editorState.layers.map(layer => {
    const isActive = layer.id === editorState.selectedLayerId;
    let label = "Sujet";
    if (layer.type === "text") label = `T: "${layer.text.substring(0, 7)}"`;
    if (layer.type === "deco") label = `Déco: ${layer.emoji}`;

    return `<div class="layer-chip ${isActive ? 'active' : ''}" onclick="selectLayer('${layer.id}')">${label}</div>`;
  }).join("");
}

window.selectLayer = (layerId) => {
  editorState.selectedLayerId = layerId;
  updateLayerChips();
  renderEditorCanvas();
};

function getActiveLayer() {
  return editorState.layers.find(l => l.id === editorState.selectedLayerId) || editorState.layers[0];
}

// --- DIALOGUE TEXTE ---
function initTextEditDialog() {
  const modal = document.getElementById("modal-text-edit");
  const input = document.getElementById("input-sticker-text");
  const preview = document.getElementById("text-preview-render");
  const fontSizeSlider = document.getElementById("slider-font-size");
  const strokeWidthSlider = document.getElementById("slider-stroke-width");

  let currentFont = "Impact";
  let currentFill = "#FFFFFF";
  let currentStroke = "#000000";

  document.getElementById("btn-open-text-dialog").addEventListener("click", () => {
    input.value = "STICKR MEME";
    updateLiveTextPreview();
    modal.classList.remove("hidden");
  });

  input.addEventListener("input", updateLiveTextPreview);

  document.querySelectorAll(".font-chips .chip").forEach(c => {
    c.addEventListener("click", () => {
      document.querySelectorAll(".font-chips .chip").forEach(ch => ch.classList.remove("active"));
      c.classList.add("active");
      currentFont = c.dataset.font;
      updateLiveTextPreview();
    });
  });

  document.querySelectorAll("#text-fill-palette .color-dot").forEach(d => {
    d.addEventListener("click", () => {
      document.querySelectorAll("#text-fill-palette .color-dot").forEach(dot => dot.classList.remove("selected"));
      d.classList.add("selected");
      currentFill = d.dataset.color;
      updateLiveTextPreview();
    });
  });

  document.querySelectorAll("#text-stroke-palette .color-dot").forEach(d => {
    d.addEventListener("click", () => {
      document.querySelectorAll("#text-stroke-palette .color-dot").forEach(dot => dot.classList.remove("selected"));
      d.classList.add("selected");
      currentStroke = d.dataset.color;
      updateLiveTextPreview();
    });
  });

  fontSizeSlider.addEventListener("input", (e) => {
    document.getElementById("label-font-size").textContent = `${e.target.value}px`;
    updateLiveTextPreview();
  });

  strokeWidthSlider.addEventListener("input", (e) => {
    document.getElementById("label-stroke-width").textContent = `${e.target.value}px`;
    updateLiveTextPreview();
  });

  function updateLiveTextPreview() {
    preview.textContent = input.value || "TEXTE";
    preview.style.fontFamily = currentFont;
    preview.style.color = currentFill;
    preview.style.webkitTextStroke = `${strokeWidthSlider.value}px ${currentStroke}`;
    preview.style.fontSize = `${Math.min(36, fontSizeSlider.value)}px`;
  }

  document.getElementById("btn-cancel-text-edit").addEventListener("click", () => modal.classList.add("hidden"));

  document.getElementById("btn-confirm-text-edit").addEventListener("click", () => {
    const text = input.value.trim();
    if (!text) return;

    const newLayer = {
      id: "text_" + Date.now(),
      type: "text",
      text,
      fontFamily: currentFont,
      textColor: currentFill,
      strokeColor: currentStroke,
      fontSize: parseInt(fontSizeSlider.value),
      strokeWidth: parseInt(strokeWidthSlider.value),
      offset: { x: 0, y: 80 },
      scale: 1.0,
      rotation: 0
    };

    editorState.layers.push(newLayer);
    editorState.selectedLayerId = newLayer.id;
    updateLayerChips();
    modal.classList.add("hidden");
    renderEditorCanvas();
    showToast("Texte ajouté au sticker !");
  });
}

// --- BOTTOM SHEET ACCESSOIRES ---
function initDecorationSheet() {
  const sheet = document.getElementById("sheet-decoration-picker");
  const grid = document.getElementById("emoji-grid");

  document.getElementById("btn-open-deco-sheet").addEventListener("click", () => {
    renderCategoryEmojis(0);
    sheet.classList.remove("hidden");
  });

  sheet.addEventListener("click", (e) => {
    if (e.target === sheet) sheet.classList.add("hidden");
  });

  document.querySelectorAll(".tab-btn").forEach(tab => {
    tab.addEventListener("click", () => {
      document.querySelectorAll(".tab-btn").forEach(t => t.classList.remove("active"));
      tab.classList.add("active");
      renderCategoryEmojis(parseInt(tab.dataset.cat));
    });
  });

  function renderCategoryEmojis(catIndex) {
    const emojis = DECORATION_CATEGORIES[catIndex] || DECORATION_CATEGORIES[0];
    grid.innerHTML = emojis.map(emoji => `
      <button class="emoji-btn" onclick="addDecorationLayer('${emoji}')">${emoji}</button>
    `).join("");
  }

  window.addDecorationLayer = (emoji) => {
    const newLayer = {
      id: "deco_" + Date.now(),
      type: "deco",
      emoji,
      size: 72,
      offset: { x: 0, y: -60 },
      scale: 1.0,
      rotation: 0
    };

    editorState.layers.push(newLayer);
    editorState.selectedLayerId = newLayer.id;
    updateLayerChips();
    sheet.classList.add("hidden");
    renderEditorCanvas();
    showToast(`Accessoire ${emoji} ajouté !`);
  };
}

// --- GESTES MULTITOUCH & SOURIS ---
function initCanvasGestures() {
  let isDragging = false;
  let startX, startY;

  const onStart = (x, y) => {
    isDragging = true;
    startX = x;
    startY = y;
  };

  const onMove = (x, y) => {
    if (!isDragging) return;
    const dx = x - startX;
    const dy = y - startY;
    startX = x;
    startY = y;

    const layer = getActiveLayer();
    if (layer) {
      layer.offset.x += dx;
      layer.offset.y += dy;
      renderEditorCanvas();
    }
  };

  const onEnd = () => {
    isDragging = false;
  };

  canvas.addEventListener("mousedown", (e) => onStart(e.clientX, e.clientY));
  window.addEventListener("mousemove", (e) => onMove(e.clientX, e.clientY));
  window.addEventListener("mouseup", onEnd);

  canvas.addEventListener("touchstart", (e) => {
    if (e.touches.length === 1) onStart(e.touches[0].clientX, e.touches[0].clientY);
  });
  window.addEventListener("touchmove", (e) => {
    if (e.touches.length === 1) onMove(e.touches[0].clientX, e.touches[0].clientY);
  });
  window.addEventListener("touchend", onEnd);

  // Zoom via molette
  canvas.addEventListener("wheel", (e) => {
    e.preventDefault();
    const layer = getActiveLayer();
    if (!layer) return;
    const factor = e.deltaY < 0 ? 1.05 : 0.95;
    layer.scale = Math.max(0.2, Math.min(4.0, layer.scale * factor));
    renderEditorCanvas();
  });
}

// --- ENREGISTREMENT & EXPORT WEBP 512x512 ---
function saveStickerFinal() {
  if (editorState.layers.length === 0) {
    showToast("Veuillez d'abord ajouter une image ou un sticker.");
    return;
  }

  // Rendu final sans cadre de sélection
  const oldSelected = editorState.selectedLayerId;
  editorState.selectedLayerId = null;
  renderEditorCanvas();

  // Exportation WebP stricte
  const webpDataUrl = canvas.toDataURL("image/webp", 0.85);

  // Vérifier la taille estimée
  const head = "data:image/webp;base64,";
  const sizeInBytes = Math.round((webpDataUrl.length - head.length) * 3 / 4);

  // Restaurer sélection
  editorState.selectedLayerId = oldSelected;
  renderEditorCanvas();

  // Enregistrer dans le pack courant
  const pack = packs.find(p => p.id === currentPackId);
  if (pack) {
    const newSticker = {
      id: "s_" + Date.now(),
      emojis: "✨",
      url: webpDataUrl
    };
    pack.stickers.push(newSticker);
    savePacks();
  }

  // Téléchargement automatique du WebP
  const a = document.createElement("a");
  a.href = webpDataUrl;
  a.download = `sticker_${Date.now()}.webp`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);

  showToast(`Sticker WebP 512x512 sauvegardé (${(sizeInBytes / 1024).toFixed(1)} Ko) !`);
  navigateTo("pack-detail");
}

// --- UTILITAIRES ---
function loadSamplePhoto() {
  const sampleCanvas = document.createElement("canvas");
  sampleCanvas.width = 400;
  sampleCanvas.height = 400;
  const sCtx = sampleCanvas.getContext("2d");

  // Dégradé portrait avec visage emoji
  sCtx.fillStyle = "#6750A4";
  sCtx.fillRect(0, 0, 400, 400);

  sCtx.font = "160px sans-serif";
  sCtx.textAlign = "center";
  sCtx.textBaseline = "middle";
  sCtx.fillText("🤩", 200, 200);

  const img = new Image();
  img.onload = () => loadImageToEditor(img);
  img.src = sampleCanvas.toDataURL("image/png");
}

function createSampleSticker(emoji, bg, text) {
  const c = document.createElement("canvas");
  c.width = 512;
  c.height = 512;
  const ctx = c.getContext("2d");

  // Étoile de fond
  ctx.fillStyle = bg;
  ctx.beginPath();
  ctx.arc(256, 230, 160, 0, Math.PI * 2);
  ctx.fill();

  // Contour blanc die-cut
  ctx.lineWidth = 16;
  ctx.strokeStyle = "#FFFFFF";
  ctx.stroke();

  ctx.font = "140px sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(emoji, 256, 230);

  if (text) {
    ctx.font = "bold 44px 'Impact', sans-serif";
    ctx.strokeStyle = "#000000";
    ctx.lineWidth = 8;
    ctx.strokeText(text, 256, 420);
    ctx.fillStyle = "#FFFFFF";
    ctx.fillText(text, 256, 420);
  }

  return c.toDataURL("image/webp");
}

function showToast(msg) {
  const toast = document.getElementById("toast");
  toast.textContent = msg;
  toast.classList.remove("hidden");
  setTimeout(() => toast.classList.add("hidden"), 3000);
}

function escapeHtml(text) {
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}
