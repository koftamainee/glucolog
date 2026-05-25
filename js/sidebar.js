const Sidebar = (() => {
    let overlay = null;
    let drawer = null;
    let onImportCallback = null;
    let _driveTimer = null;
    const MEAL_KEYS = ['breakfast1', 'breakfast2', 'lunch', 'snack', 'dinner'];

    function build() {
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';

        drawer = document.createElement('div');
        drawer.className = 'sidebar-drawer';

        const theme = document.documentElement.getAttribute('data-theme') || 'system';
        const themeMeta = {
            system: { label: 'Системная', icon: '\uD83C\uDF13' },
            dark:   { label: 'Тёмная',    icon: '\uD83C\uDF19' },
            light:  { label: 'Светлая',   icon: '\u2600\uFE0F' },
        };

        const driveLinked = Drive.isLinked();

        drawer.innerHTML = `
            <div class="sidebar-header">
                <span class="sidebar-title">Glucolog</span>
                <button class="sidebar-close" aria-label="Закрыть">✕</button>
            </div>

            <div class="sidebar-section-label">Данные</div>

            <button class="sidebar-item" id="sidebarExportJSON">
                <span class="sidebar-item-icon">↑</span>
                <span>Экспорт JSON</span>
            </button>

            <button class="sidebar-item" id="sidebarImportJSON">
                <span class="sidebar-item-icon">↓</span>
                <span>Импорт JSON</span>
            </button>

            <button class="sidebar-item" id="sidebarExportCSV">
                <span class="sidebar-item-icon">#</span>
                <span>Экспорт CSV</span>
            </button>

            <button class="sidebar-item" id="sidebarImportCSV">
                <span class="sidebar-item-icon">+</span>
                <span>Импорт CSV</span>
            </button>

            <div class="sidebar-section-label">Облако</div>

            ${driveLinked ? `
            <div class="sidebar-drive-status">
                <div class="sidebar-drive-header">
                    <span class="sidebar-drive-name">☁️ Google Drive</span>
                    <span class="sidebar-drive-badge">● Подключён</span>
                </div>
                <div class="sidebar-drive-time">
                    Резервная копия: <span id="driveRelativeTime">${Drive.getRelativeTime() || '-'}</span>
                </div>
                <div class="sidebar-drive-actions">
                    <button class="sidebar-drive-btn" id="sidebarDriveBackup">↑ Создать копию</button>
                    <button class="sidebar-drive-btn" id="sidebarDriveRestore">↓ Восстановить</button>
                    <button class="sidebar-drive-btn" id="sidebarDriveUnlink">✕ Отключить</button>
                    <button class="sidebar-drive-btn" id="sidebarDriveChange">🔄 Сменить</button>
                </div>
            </div>
            ` : `
            <button class="sidebar-item" id="sidebarDriveLink">
                <span class="sidebar-item-icon">☁️</span>
                <span>Google Drive</span>
            </button>
            `}

            <div class="sidebar-section-label">Вид</div>

            <button class="sidebar-item" id="sidebarThemeBtn">
                <span class="sidebar-item-icon" id="sidebarThemeIcon">${themeMeta[theme].icon}</span>
                <span>Тема: <span id="sidebarThemeLabel">${themeMeta[theme].label}</span></span>
            </button>

            <div class="sidebar-section-label">Помощь</div>

            <button class="sidebar-item" id="sidebarTutorial">
                <span class="sidebar-item-icon">?</span>
                <span>Туториал</span>
            </button>

            <input type="file" id="sidebarFileInputJSON" accept=".json" style="display:none">
            <input type="file" id="sidebarFileInputCSV" accept=".csv" style="display:none">

            <div class="sidebar-import-status" id="sidebarStatus"></div>
        `;

        overlay.appendChild(drawer);
        document.body.appendChild(overlay);

        overlay.addEventListener('click', e => {
            if (e.target === overlay) close();
        });

        drawer.querySelector('.sidebar-close')
            .addEventListener('click', close);

        drawer.querySelector('#sidebarExportJSON')
            .addEventListener('click', doExportJSON);

        drawer.querySelector('#sidebarImportJSON')
            .addEventListener('click', () => {
                drawer.querySelector('#sidebarFileInputJSON').click();
            });

        drawer.querySelector('#sidebarExportCSV')
            .addEventListener('click', doExportCSV);

        drawer.querySelector('#sidebarImportCSV')
            .addEventListener('click', () => {
                drawer.querySelector('#sidebarFileInputCSV').click();
            });

        if (driveLinked) {
            drawer.querySelector('#sidebarDriveBackup')
                .addEventListener('click', () => {
                    close();
                    setTimeout(() => {
                        Drive.backup().then(() => { rebuild(); if (onImportCallback) onImportCallback(); });
                    }, 300);
                });

            drawer.querySelector('#sidebarDriveRestore')
                .addEventListener('click', () => {
                    close();
                    setTimeout(() => {
                        Drive.restore().then(() => { rebuild(); if (onImportCallback) onImportCallback(); });
                    }, 300);
                });

            drawer.querySelector('#sidebarDriveUnlink')
                .addEventListener('click', () => {
                    stopDriveTimer();
                    Drive.unlink();
                    rebuild();
                });

            drawer.querySelector('#sidebarDriveChange')
                .addEventListener('click', () => {
                    close();
                    setTimeout(() => {
                        Drive.changeAccount().then(() => { rebuild(); if (onImportCallback) onImportCallback(); });
                    }, 300);
                });
        } else {
            drawer.querySelector('#sidebarDriveLink')
                .addEventListener('click', () => {
                    close();
                    setTimeout(() => {
                        Drive.link().then(() => { rebuild(); if (onImportCallback) onImportCallback(); });
                    }, 300);
                });
        }

        drawer.querySelector('#sidebarThemeBtn')
            .addEventListener('click', toggleTheme);

        drawer.querySelector('#sidebarTutorial')
            .addEventListener('click', () => {
                close();
                setTimeout(() => {
                    if (Tutorial) Tutorial.start(true);
                }, 300);
            });

        drawer.querySelector('#sidebarFileInputJSON')
            .addEventListener('change', function () {
                const file = this.files[0];
                if (!file) return;
                const reader = new FileReader();
                reader.onload = e => {
                    try {
                        const data = JSON.parse(e.target.result);
                        if (typeof data !== 'object' || Array.isArray(data)) throw new Error();
                        localStorage.setItem('glucolog', JSON.stringify(data));
                        showStatus('Импорт выполнен', true);
                        if (onImportCallback) onImportCallback();
                    } catch {
                        showStatus('Ошибка: неверный файл', false);
                    }
                    this.value = '';
                };
                reader.readAsText(file);
            });

        drawer.querySelector('#sidebarFileInputCSV')
            .addEventListener('change', function () {
                const file = this.files[0];
                if (!file) return;
                const reader = new FileReader();
                reader.onload = e => {
                    try {
                        const result = importCSV(e.target.result);
                        showStatus(`Импортировано ${result} дней`, true);
                        if (onImportCallback) onImportCallback();
                    } catch {
                        showStatus('Ошибка: неверный CSV', false);
                    }
                    this.value = '';
                };
                reader.readAsText(file);
            });

        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') close();
        });

        if (driveLinked) startDriveTimer();
    }

    function startDriveTimer() {
        stopDriveTimer();
        _driveTimer = setInterval(() => {
            const el = document.getElementById('driveRelativeTime');
            if (el) el.textContent = Drive.getRelativeTime() || '-';
        }, 10000);
    }

    function stopDriveTimer() {
        if (_driveTimer) {
            clearInterval(_driveTimer);
            _driveTimer = null;
        }
    }

    function rebuild() {
        if (!overlay) return;
        const wasVisible = overlay.classList.contains('visible');
        stopDriveTimer();
        overlay.remove();
        overlay = null;
        drawer = null;
        if (wasVisible) open(onImportCallback);
    }

    function doExportJSON() {
        const raw = localStorage.getItem('glucolog') || '{}';
        const blob = new Blob([raw], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        const date = new Date().toISOString().slice(0, 10);
        a.href = url;
        a.download = `glucolog-${date}.json`;
        a.click();
        URL.revokeObjectURL(url);
    }

    function doExportCSV() {
        const db = Storage.getAll();
        const keys = Object.keys(db).sort();
        const rows = [['Дата', 'Время', 'Тип', 'Значение', 'Детали']];

        keys.forEach(k => {
            const day = db[k];
            (day.glucose || []).forEach(p => {
                const h = Math.floor(p.h);
                const m = Math.round((p.h - h) * 60);
                const time = String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0');
                rows.push([k, time, 'Глюкоза', p.g.toFixed(1), 'ммоль/л']);
            });
            (day.insulin || []).forEach(p => {
                const h = Math.floor(p.h);
                const m = Math.round((p.h - h) * 60);
                const time = String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0');
                if (p.b) rows.push([k, time, 'Болюс', p.b, 'ед.']);
                if (p.ba) rows.push([k, time, 'Базальный', p.ba, 'ед.']);
            });
            if (day.water !== undefined) rows.push([k, '', 'Вода', day.water, 'стаканов']);
            if (day.sport !== undefined) rows.push([k, '', 'Спорт', day.sport ? 'Да' : 'Нет', '']);
            if (day.steps) rows.push([k, '', 'Шаги', day.steps, '']);
            if (day.sleepStart) rows.push([k, '', 'Сон (лёг)', day.sleepStart, '']);
            if (day.sleepEnd) rows.push([k, '', 'Сон (встал)', day.sleepEnd, '']);
            if (day.stress) rows.push([k, '', 'Стресс', day.stress, '']);
            if (day.stool && day.stool.length) rows.push([k, '', 'Стул', day.stool.join(', '), '']);
            if (day.notes) rows.push([k, '', 'Заметки', day.notes.replace(/\n/g, ' '), '']);
            if (day.conclusions) rows.push([k, '', 'Выводы', day.conclusions.replace(/\n/g, ' '), '']);
            MEAL_KEYS.forEach(mk => {
                const m = day[mk];
                if (!m) return;
                if (m.time) rows.push([k, m.time, mk + ' время', '', '']);
                if (m.hunger) rows.push([k, '', mk + ' голод', m.hunger, '']);
                if (m.food) rows.push([k, '', mk + ' еда', m.food.replace(/\n/g, ' '), '']);
                if (m.phys) rows.push([k, '', mk + ' физ', m.phys.replace(/\n/g, ' '), '']);
                if (m.emo) rows.push([k, '', mk + ' эмоции', m.emo.replace(/\n/g, ' '), '']);
            });
        });

        const csv = rows.map(r => r.map(c => {
            if (c == null) return '';
            const s = String(c);
            return s.includes(',') || s.includes('"') || s.includes('\n')
                ? '"' + s.replace(/"/g, '""') + '"' : s;
        }).join(',')).join('\n');

        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        const date = new Date().toISOString().slice(0, 10);
        a.href = url;
        a.download = `glucolog-${date}.csv`;
        a.click();
        URL.revokeObjectURL(url);
    }

    function importCSV(text) {
        const lines = text.split('\n').filter(l => l.trim());
        if (lines.length < 2) throw new Error();
        const db = Storage.getAll();
        let importedDays = 0;

        for (let i = 1; i < lines.length; i++) {
            const row = parseCSVLine(lines[i]);
            if (row.length < 4) continue;
            const [dateKey, time, type, val, detail] = row;
            if (!dateKey || !type) continue;
            if (!db[dateKey]) { db[dateKey] = {}; importedDays++; }

            if (type === 'Глюкоза' && time && val) {
                const [h, m] = time.split(':').map(Number);
                const hourFloat = h + m / 60;
                const g = parseFloat(val);
                if (!isNaN(g)) {
                    if (!db[dateKey].glucose) db[dateKey].glucose = [];
                    const idx = db[dateKey].glucose.findIndex(p => Math.abs(p.h - hourFloat) < 0.001);
                    if (idx >= 0) db[dateKey].glucose[idx].g = g;
                    else db[dateKey].glucose.push({ h: hourFloat, g });
                    db[dateKey].glucose.sort((a, b) => a.h - b.h);
                }
            } else if ((type === 'Болюс' || type === 'Базальный') && time && val) {
                const [h, m] = time.split(':').map(Number);
                const hourFloat = h + m / 60;
                const v = parseFloat(val);
                if (!isNaN(v)) {
                    if (!db[dateKey].insulin) db[dateKey].insulin = [];
                    const idx = db[dateKey].insulin.findIndex(p => Math.abs(p.h - hourFloat) < 0.001);
                    if (idx >= 0) {
                        if (type === 'Болюс') db[dateKey].insulin[idx].b = v;
                        else db[dateKey].insulin[idx].ba = v;
                    } else {
                        db[dateKey].insulin.push({ h: hourFloat, b: type === 'Болюс' ? v : 0, ba: type === 'Базальный' ? v : 0 });
                    }
                    db[dateKey].insulin.sort((a, b) => a.h - b.h);
                }
            } else if (type === 'Вода') db[dateKey].water = parseInt(val) || 0;
            else if (type === 'Спорт') db[dateKey].sport = val === 'Да';
            else if (type === 'Шаги') db[dateKey].steps = val;
            else if (type === 'Сон (лёг)') db[dateKey].sleepStart = val;
            else if (type === 'Сон (встал)') db[dateKey].sleepEnd = val;
            else if (type === 'Стресс') db[dateKey].stress = val;
            else if (type === 'Стул') db[dateKey].stool = val.split(', ').filter(Boolean);
            else if (type === 'Заметки') db[dateKey].notes = val;
            else if (type === 'Выводы') db[dateKey].conclusions = val;
            else if (type.endsWith(' время')) {
                const mk = type.replace(' время', '');
                if (!db[dateKey][mk]) db[dateKey][mk] = {};
                db[dateKey][mk].time = time;
            } else if (type.endsWith(' голод')) {
                const mk = type.replace(' голод', '');
                if (!db[dateKey][mk]) db[dateKey][mk] = {};
                db[dateKey][mk].hunger = parseInt(val) || 0;
            } else if (type.endsWith(' еда')) {
                const mk = type.replace(' еда', '');
                if (!db[dateKey][mk]) db[dateKey][mk] = {};
                db[dateKey][mk].food = val;
            } else if (type.endsWith(' физ')) {
                const mk = type.replace(' физ', '');
                if (!db[dateKey][mk]) db[dateKey][mk] = {};
                db[dateKey][mk].phys = val;
            } else if (type.endsWith(' эмоции')) {
                const mk = type.replace(' эмоции', '');
                if (!db[dateKey][mk]) db[dateKey][mk] = {};
                db[dateKey][mk].emo = val;
            }
        }

        localStorage.setItem('glucolog', JSON.stringify(db));
        return importedDays;
    }

    function parseCSVLine(line) {
        const result = [];
        let current = '';
        let inQuotes = false;
        for (let i = 0; i < line.length; i++) {
            const ch = line[i];
            if (inQuotes) {
                if (ch === '"') {
                    if (i + 1 < line.length && line[i + 1] === '"') {
                        current += '"';
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current += ch;
                }
            } else {
                if (ch === '"') {
                    inQuotes = true;
                } else if (ch === ',') {
                    result.push(current);
                    current = '';
                } else {
                    current += ch;
                }
            }
        }
        result.push(current);
        return result;
    }

    function toggleTheme() {
        const html = document.documentElement;
        const current = html.getAttribute('data-theme');
        const next = current === 'dark' ? 'light' : current === 'light' ? null : 'dark';
        if (next) html.setAttribute('data-theme', next);
        else html.removeAttribute('data-theme');
        localStorage.setItem('glucolog_dark', next || '');

        const label = drawer.querySelector('#sidebarThemeLabel');
        const icon = drawer.querySelector('#sidebarThemeIcon');
        const meta = {
            system: { label: '\u0421\u0438\u0441\u0442\u0435\u043C\u043D\u0430\u044F', icon: '\uD83C\uDF13' },
            dark:   { label: '\u0422\u0451\u043C\u043D\u0430\u044F',    icon: '\uD83C\uDF19' },
            light:  { label: '\u0421\u0432\u0435\u0442\u043B\u0430\u044F',   icon: '\u2600\uFE0F' },
        };
        const key = next || 'system';
        label.textContent = meta[key].label;
        icon.textContent = meta[key].icon;

        const day = Render.__day;
        if (day) Render.glucose(day, Storage.getDay(day));
    }

    function showStatus(msg, ok) {
        const el = drawer.querySelector('#sidebarStatus');
        el.textContent = msg;
        el.className = 'sidebar-import-status ' + (ok ? 'ok' : 'err');
        clearTimeout(el._t);
        el._t = setTimeout(() => {
            el.textContent = '';
            el.className = 'sidebar-import-status';
        }, 3000);
    }

    function open(onImport) {
        onImportCallback = onImport;
        Drive.setOnImport(onImport);
        if (!overlay) build();
        overlay.classList.add('visible');
        drawer.classList.add('visible');
    }

    function close() {
        stopDriveTimer();
        if (!overlay) return;
        overlay.classList.remove('visible');
        drawer.classList.remove('visible');
    }

    return { open, close, rebuild: rebuild };
})();
