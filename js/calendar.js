const Calendar = (() => {
    let overlay = null;
    let popup = null;
    let viewYear = 0;
    let viewMonth = 0;
    let selectedKey = '';
    let onSelectCallback = null;

    const MONTHS = [
        'Январь','Февраль','Март','Апрель','Май','Июнь',
        'Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'
    ];
    const DAYS = ['пн','вт','ср','чт','пт','сб','вс'];

    function keyToDate(key) {
        const [y, m, d] = key.split('-').map(Number);
        return new Date(y, m - 1, d);
    }

    function dateToKey(date) {
        return [
            date.getFullYear(),
            String(date.getMonth() + 1).padStart(2, '0'),
            String(date.getDate()).padStart(2, '0'),
        ].join('-');
    }

    function todayKey() {
        return dateToKey(new Date());
    }

    function build() {
        overlay = document.createElement('div');
        overlay.className = 'cal-overlay';

        popup = document.createElement('div');
        popup.className = 'cal-popup';

        overlay.appendChild(popup);
        document.body.appendChild(overlay);

        overlay.addEventListener('click', e => {
            if (e.target === overlay) close();
        });

        document.addEventListener('keydown', onKey);
    }

    function onKey(e) {
        if (e.key === 'Escape') close();
    }

    function render() {
        const today = todayKey();
        const firstDay = new Date(viewYear, viewMonth, 1);
        const lastDay  = new Date(viewYear, viewMonth + 1, 0);

        let startDow = firstDay.getDay();
        startDow = startDow === 0 ? 6 : startDow - 1;

        let html = `
            <div class="cal-header">
                <button class="cal-nav" id="calPrevMonth">‹</button>
                <span class="cal-month-label">${MONTHS[viewMonth]} ${viewYear}</span>
                <button class="cal-nav" id="calNextMonth">›</button>
            </div>
            <div class="cal-grid">
        `;

        DAYS.forEach(d => {
            html += `<div class="cal-dow">${d}</div>`;
        });

        for (let i = 0; i < startDow; i++) {
            html += `<div class="cal-cell empty"></div>`;
        }

        for (let d = 1; d <= lastDay.getDate(); d++) {
            const key = dateToKey(new Date(viewYear, viewMonth, d));
            const isToday    = key === today;
            const isSelected = key === selectedKey;
            let cls = 'cal-cell';
            if (isToday)    cls += ' today';
            if (isSelected) cls += ' selected';
            html += `<div class="${cls}" data-key="${key}">${d}</div>`;
        }

        html += `</div>`;

        const todayBtn = `<button class="cal-today-btn" id="calTodayBtn">Сегодня</button>`;
        html += todayBtn;

        popup.innerHTML = html;

        popup.querySelector('#calPrevMonth').addEventListener('click', e => {
            e.stopPropagation();
            viewMonth--;
            if (viewMonth < 0) { viewMonth = 11; viewYear--; }
            render();
        });

        popup.querySelector('#calNextMonth').addEventListener('click', e => {
            e.stopPropagation();
            viewMonth++;
            if (viewMonth > 11) { viewMonth = 0; viewYear++; }
            render();
        });

        popup.querySelector('#calTodayBtn').addEventListener('click', e => {
            e.stopPropagation();
            select(todayKey());
        });

        popup.querySelectorAll('.cal-cell[data-key]').forEach(cell => {
            cell.addEventListener('click', e => {
                e.stopPropagation();
                select(cell.dataset.key);
            });
        });
    }

    function select(key) {
        selectedKey = key;
        close();
        if (onSelectCallback) onSelectCallback(key);
    }

    function open(currentKey, onSelect) {
        selectedKey = currentKey;
        onSelectCallback = onSelect;

        const date = keyToDate(currentKey);
        viewYear  = date.getFullYear();
        viewMonth = date.getMonth();

        if (!overlay) build();

        render();
        overlay.classList.add('visible');
        popup.classList.add('visible');
    }

    function close() {
        if (!overlay) return;
        overlay.classList.remove('visible');
        popup.classList.remove('visible');
        document.removeEventListener('keydown', onKey);
        document.addEventListener('keydown', onKey);
    }

    return { open };
})();