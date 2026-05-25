const Drive = (() => {
    const CLIENT_ID = '364510220045-5cpiqhvphcevtln368pp1td6d94ohl08.apps.googleusercontent.com';
    const SCOPE = 'https://www.googleapis.com/auth/drive.file';
    const FILE_NAME = 'glucolog-backup.json';
    const DRIVE_API = 'https://www.googleapis.com/drive/v3';
    const UPLOAD_API = 'https://www.googleapis.com/upload/drive/v3';

    let tokenClient = null;

    function waitForGis() {
        return new Promise(resolve => {
            if (typeof google !== 'undefined' && google.accounts && google.accounts.oauth2) {
                resolve();
                return;
            }
            const check = setInterval(() => {
                if (typeof google !== 'undefined' && google.accounts && google.accounts.oauth2) {
                    clearInterval(check);
                    resolve();
                }
            }, 100);
            setTimeout(() => clearInterval(check), 10000);
        });
    }

    function isLinked() {
        return !!localStorage.getItem('glucolog_drive_file_id');
    }

    function getBackupTime() {
        return localStorage.getItem('glucolog_drive_backup_at') || '';
    }

    function getToken(forcePrompt) {
        return new Promise((resolve, reject) => {
            if (!tokenClient) {
                try {
                    tokenClient = google.accounts.oauth2.initTokenClient({
                        client_id: CLIENT_ID,
                        scope: SCOPE,
                        callback: response => {
                            if (response.error) {
                                reject(new Error(response.error_description || response.error));
                                return;
                            }
                            resolve(response.access_token);
                        },
                    });
                } catch (e) {
                    reject(new Error('Failed to init Google OAuth: ' + e.message));
                    return;
                }
            } else {
                const origCb = tokenClient.callback;
                tokenClient.callback = response => {
                    tokenClient.callback = origCb;
                    if (response.error) {
                        reject(new Error(response.error_description || response.error));
                        return;
                    }
                    resolve(response.access_token);
                };
            }
            try {
                tokenClient.requestAccessToken({ prompt: forcePrompt ? 'consent' : '' });
            } catch (e) {
                reject(new Error('Failed to get token: ' + e.message));
            }
        });
    }

    async function driveFetch(path, options) {
        const token = await getToken(false);
        const res = await fetch(DRIVE_API + path, {
            ...options,
            headers: {
                ...options?.headers,
                Authorization: 'Bearer ' + token,
            },
        });
        if (res.status === 401) {
            const token2 = await getToken(true);
            const retry = await fetch(DRIVE_API + path, {
                ...options,
                headers: {
                    ...options?.headers,
                    Authorization: 'Bearer ' + token2,
                },
            });
            if (!retry.ok) throw new Error('Drive API error: ' + retry.status);
            return retry.json();
        }
        if (!res.ok) throw new Error('Drive API error: ' + res.status);
        return res.json();
    }

    async function findFile() {
        const q = encodeURIComponent("name='" + FILE_NAME + "' and trashed=false");
        const data = await driveFetch('/files?q=' + q + '&fields=files(id,modifiedTime,description)');
        return data.files && data.files.length ? data.files[0] : null;
    }

    async function createFile(content) {
        const token = await getToken(false);
        const metadata = {
            name: FILE_NAME,
            description: 'Glucolog backup — ' + new Date().toISOString().slice(0, 10),
            mimeType: 'application/json',
        };
        const form = new FormData();
        form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
        form.append('file', new Blob([content], { type: 'application/json' }));
        const res = await fetch(UPLOAD_API + '/files?uploadType=multipart', {
            method: 'POST',
            headers: { Authorization: 'Bearer ' + token },
            body: form,
        });
        if (!res.ok) throw new Error('Failed to create file: ' + res.status);
        const data = await res.json();
        return data.id;
    }

    async function updateFile(fileId, content) {
        const token = await getToken(false);
        const res = await fetch(UPLOAD_API + '/files/' + fileId + '?uploadType=media', {
            method: 'PATCH',
            headers: {
                Authorization: 'Bearer ' + token,
                'Content-Type': 'application/json',
            },
            body: content,
        });
        if (!res.ok) throw new Error('Failed to update file: ' + res.status);
    }

    async function downloadFile(fileId) {
        const token = await getToken(false);
        const res = await fetch(DRIVE_API + '/files/' + fileId + '?alt=media', {
            headers: { Authorization: 'Bearer ' + token },
        });
        if (res.status === 401) {
            const token2 = await getToken(true);
            const retry = await fetch(DRIVE_API + '/files/' + fileId + '?alt=media', {
                headers: { Authorization: 'Bearer ' + token2 },
            });
            if (!retry.ok) throw new Error('Failed to download: ' + retry.status);
            return retry.text();
        }
        if (!res.ok) throw new Error('Failed to download: ' + res.status);
        return res.text();
    }

    function getLocalData() {
        return localStorage.getItem('glucolog') || '{}';
    }

    function setLocalData(json) {
        localStorage.setItem('glucolog', json);
    }

    function getLocalModified() {
        return localStorage.getItem('glucolog_modified_at') || '';
    }

    function touchLocalModified() {
        localStorage.setItem('glucolog_modified_at', new Date().toISOString());
    }

    async function link() {
        await waitForGis();

        try {
            await getToken(true);
        } catch (e) {
            showDialog('Ошибка', 'Не удалось авторизоваться: ' + e.message);
            return;
        }

        let file;
        try {
            file = await findFile();
        } catch (e) {
            showDialog('Ошибка', 'Не удалось проверить Google Drive: ' + e.message);
            return;
        }

        if (!file) {
            try {
                const id = await createFile(getLocalData());
                localStorage.setItem('glucolog_drive_file_id', id);
                const now = new Date().toISOString();
                localStorage.setItem('glucolog_drive_backup_at', now);
                touchLocalModified();
                showDialog('Готово', 'Резервная копия создана на Google Drive');
            } catch (e) {
                showDialog('Ошибка', 'Не удалось создать копию: ' + e.message);
            }
            return;
        }

        localStorage.setItem('glucolog_drive_file_id', file.id);

        const driveTime = file.modifiedTime || '';
        const localModified = getLocalModified();
        const backupTime = getBackupTime();
        const driveNewer = driveTime > backupTime;
        const localNewer = localModified > backupTime;

        if (!backupTime || (!driveNewer && !localNewer)) {
            try {
                await updateFile(file.id, getLocalData());
                const now = new Date().toISOString();
                localStorage.setItem('glucolog_drive_backup_at', now);
                touchLocalModified();
                showDialog('Готово', 'Резервная копия обновлена на Google Drive');
            } catch (e) {
                showDialog('Ошибка', 'Не удалось обновить копию: ' + e.message);
            }
            return;
        }

        if (driveNewer && localNewer) {
            showChoice(
                'Конфликт версий',
                'Данные изменились и локально, и на Google Drive. Что делать?',
                () => restoreFromDrive(file.id),
                () => backupToDrive(file.id)
            );
        } else if (driveNewer) {
            showChoice(
                'Найдена более новая копия',
                'На Google Drive более свежие данные. Загрузить их?',
                () => restoreFromDrive(file.id),
                null
            );
        } else {
            try {
                await backupToDrive(file.id);
            } catch (e) {
                showDialog('Ошибка', 'Не удалось обновить копию: ' + e.message);
            }
        }
    }

    async function backupToDrive(fileId) {
        const id = fileId || localStorage.getItem('glucolog_drive_file_id');
        if (!id) throw new Error('Not linked');
        await updateFile(id, getLocalData());
        const now = new Date().toISOString();
        localStorage.setItem('glucolog_drive_backup_at', now);
        touchLocalModified();
    }

    async function restoreFromDrive(fileId) {
        const id = fileId || localStorage.getItem('glucolog_drive_file_id');
        if (!id) throw new Error('Not linked');
        const content = await downloadFile(id);
        const data = JSON.parse(content);
        if (typeof data !== 'object' || Array.isArray(data)) throw new Error('Invalid backup');
        setLocalData(content);
        const now = new Date().toISOString();
        localStorage.setItem('glucolog_drive_backup_at', now);
        touchLocalModified();
    }

    async function backup() {
        if (!isLinked()) { showDialog('Не подключено', 'Сначала подключите Google Drive в боковом меню'); return; }
        await waitForGis();
        try {
            const id = localStorage.getItem('glucolog_drive_file_id');
            await backupToDrive(id);
            showDialog('Готово', 'Резервная копия сохранена на Google Drive');
        } catch (e) {
            showDialog('Ошибка', 'Не удалось сохранить копию: ' + e.message);
        }
    }

    async function restore() {
        if (!isLinked()) { showDialog('Не подключено', 'Сначала подключите Google Drive'); return; }
        await waitForGis();
        try {
            const id = localStorage.getItem('glucolog_drive_file_id');
            await restoreFromDrive(id);
            showDialog('Готово', 'Данные восстановлены с Google Drive');
            if (onImportCallback) onImportCallback();
        } catch (e) {
            showDialog('Ошибка', 'Не удалось восстановить: ' + e.message);
        }
    }

    function unlink() {
        localStorage.removeItem('glucolog_drive_file_id');
        localStorage.removeItem('glucolog_drive_backup_at');
        showDialog('Готово', 'Google Drive отключён');
    }

    function showDialog(title, message) {
        const existing = document.getElementById('driveDialog');
        if (existing) existing.remove();

        const overlay = document.createElement('div');
        overlay.id = 'driveDialog';
        overlay.className = 'delete-confirm-overlay';

        overlay.innerHTML =
            '<div class="delete-confirm-box">' +
            '<div class="delete-confirm-title">' + title + '</div>' +
            '<div class="delete-confirm-text">' + message + '</div>' +
            '<div class="delete-confirm-actions">' +
            '<button class="delete-confirm-cancel" id="driveDialogOk">OK</button>' +
            '</div>' +
            '</div>';

        document.body.appendChild(overlay);
        requestAnimationFrame(() => overlay.classList.add('visible'));

        function close() {
            overlay.classList.remove('visible');
            setTimeout(() => overlay.remove(), 180);
        }

        overlay.querySelector('#driveDialogOk').addEventListener('click', close);
        overlay.addEventListener('click', e => { if (e.target === overlay) close(); });

        document.addEventListener('keydown', function onKey(e) {
            if (e.key === 'Escape') { close(); document.removeEventListener('keydown', onKey); }
        });
    }

    function showChoice(title, message, onDrive, onLocal) {
        const existing = document.getElementById('driveChoice');
        if (existing) existing.remove();

        const overlay = document.createElement('div');
        overlay.id = 'driveChoice';
        overlay.className = 'delete-confirm-overlay';

        overlay.innerHTML =
            '<div class="delete-confirm-box">' +
            '<div class="delete-confirm-title">' + title + '</div>' +
            '<div class="delete-confirm-text">' + message + '</div>' +
            '<div class="delete-confirm-actions">' +
            (onDrive ? '<button class="delete-confirm-ok" id="driveChoiceDrive">С Drive</button>' : '') +
            (onLocal ? '<button class="delete-confirm-cancel" id="driveChoiceLocal">Локальные</button>' : '') +
            '<button class="delete-confirm-cancel" id="driveChoiceCancel">Отмена</button>' +
            '</div>' +
            '</div>';

        document.body.appendChild(overlay);
        requestAnimationFrame(() => overlay.classList.add('visible'));

        function close() {
            overlay.classList.remove('visible');
            setTimeout(() => overlay.remove(), 180);
        }

        if (onDrive) {
            overlay.querySelector('#driveChoiceDrive').addEventListener('click', () => {
                close();
                onDrive().catch(e => showDialog('Ошибка', e.message));
            });
        }
        if (onLocal) {
            overlay.querySelector('#driveChoiceLocal').addEventListener('click', () => {
                close();
                onLocal().catch(e => showDialog('Ошибка', e.message));
            });
        }
        overlay.querySelector('#driveChoiceCancel').addEventListener('click', close);
        overlay.addEventListener('click', e => { if (e.target === overlay) close(); });

        document.addEventListener('keydown', function onKey(e) {
            if (e.key === 'Escape') { close(); document.removeEventListener('keydown', onKey); }
        });
    }

    let onImportCallback = null;

    function setOnImport(cb) {
        onImportCallback = cb;
    }

    touchLocalModified();

    return { link, backup, restore, unlink, isLinked, getBackupTime, setOnImport, touchLocalModified };
})();
