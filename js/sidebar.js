const Sidebar = (() => {
    let overlay = null;
    let drawer = null;
    let onImportCallback = null;

    function build() {
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';

        drawer = document.createElement('div');
        drawer.className = 'sidebar-drawer';

        drawer.innerHTML = `
            <div class="sidebar-header">
                <span class="sidebar-title">Glucolog</span>
                <button class="sidebar-close" aria-label="Закрыть">✕</button>
            </div>

            <div class="sidebar-section-label">Данные</div>

            <button class="sidebar-item" id="sidebarExport">
                <span class="sidebar-item-icon">↑</span>
                <span>Экспорт JSON</span>
            </button>

            <button class="sidebar-item" id="sidebarImport">
                <span class="sidebar-item-icon">↓</span>
                <span>Импорт JSON</span>
            </button>

            <button class="sidebar-item" id="sidebarTutorial">
                <span class="sidebar-item-icon">?</span>
                <span>Туториал</span>
            </button>

            <input type="file" id="sidebarFileInput" accept=".json" style="display:none">

            <div class="sidebar-import-status" id="sidebarStatus"></div>
        `;

        overlay.appendChild(drawer);
        document.body.appendChild(overlay);

        overlay.addEventListener('click', e => {
            if (e.target === overlay) close();
        });

        drawer.querySelector('.sidebar-close')
            .addEventListener('click', close);

        drawer.querySelector('#sidebarExport')
            .addEventListener('click', doExport);

        drawer.querySelector('#sidebarImport')
            .addEventListener('click', () => {
                drawer.querySelector('#sidebarFileInput').click();
            });

        drawer.querySelector('#sidebarTutorial')
            .addEventListener('click', () => {
                close();

                setTimeout(() => {
                    if (Tutorial) {
                        Tutorial.start(true);
                    }
                }, 300);
            });

        drawer.querySelector('#sidebarFileInput')
            .addEventListener('change', function () {
                const file = this.files[0];
                if (!file) return;

                const reader = new FileReader();

                reader.onload = e => {
                    try {
                        const data = JSON.parse(e.target.result);

                        if (typeof data !== 'object' || Array.isArray(data)) {
                            throw new Error();
                        }

                        localStorage.setItem('glucolog', JSON.stringify(data));

                        showStatus('Импорт выполнен', true);

                        if (onImportCallback) {
                            onImportCallback();
                        }
                    } catch {
                        showStatus('Ошибка: неверный файл', false);
                    }

                    this.value = '';
                };

                reader.readAsText(file);
            });

        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') close();
        });
    }

    function doExport() {
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

        if (!overlay) {
            build();
        }

        overlay.classList.add('visible');
        drawer.classList.add('visible');
    }

    function close() {
        if (!overlay) return;

        overlay.classList.remove('visible');
        drawer.classList.remove('visible');
    }

    return {
        open,
        close,
    };
})();