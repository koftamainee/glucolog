(function () {

    let currentDay = todayKey();

    function todayKey() {
        const d = new Date();
        return [
            d.getFullYear(),
            String(d.getMonth() + 1).padStart(2, '0'),
            String(d.getDate()).padStart(2, '0'),
        ].join('-');
    }

    function formatDateLabel(key) {
        const [y, m, d] = key.split('-');
        const dt = new Date(+y, +m - 1, +d);
        const days = ['вс', 'пн', 'вт', 'ср', 'чт', 'пт', 'сб'];
        return `${days[dt.getDay()]}, ${d}.${m}`;
    }

    function shiftDay(delta) {
        const dt = new Date(currentDay);
        dt.setDate(dt.getDate() + delta);
        currentDay = dt.toISOString().slice(0, 10);
        renderAll();
    }

    function renderAll() {
        document.getElementById('dateLabel').textContent = formatDateLabel(currentDay);
        Render.all(currentDay);
    }

    function nowTimeString() {
        const d = new Date();
        return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
    }

    function wireEvents() {
        document.getElementById('prevDay').addEventListener('click', () => shiftDay(-1));
        document.getElementById('nextDay').addEventListener('click', () => shiftDay(1));

        document.getElementById('dateLabel').addEventListener('click', () => {
            Calendar.open(currentDay, key => {
                currentDay = key;
                renderAll();
            });
        });

        // Fix 3: set current time on load for all time inputs
        document.getElementById('glucoseTime').value  = nowTimeString();
        document.getElementById('bolusTime').value    = nowTimeString();
        document.getElementById('basalTime').value    = nowTimeString();

        document.getElementById('addGlucoseBtn').addEventListener('click', () => {
            const val  = parseFloat(document.getElementById('glucoseVal').value);
            const time = document.getElementById('glucoseTime').value;
            const hourFloat = Render.timeToFloat(time);
            if (isNaN(val) || val < 1 || val > 30 || hourFloat === null) return;
            Storage.addGlucosePoint(currentDay, hourFloat, val);
            document.getElementById('glucoseVal').value = '';
            document.getElementById('glucoseTime').value = nowTimeString();
            Render.glucose(currentDay, Storage.getDay(currentDay));
        });

        document.getElementById('glucoseVal').addEventListener('keydown', e => {
            if (e.key === 'Enter') document.getElementById('addGlucoseBtn').click();
        });

        document.getElementById('addBolusBtn').addEventListener('click', () => {
            const bolusVal = parseFloat(document.getElementById('bolus').value);
            const time = document.getElementById('bolusTime').value;
            const hourFloat = Render.timeToFloat(time);
            if (isNaN(bolusVal) || bolusVal <= 0 || hourFloat === null) return;
            Storage.addInsulinPoint(currentDay, hourFloat, bolusVal, 0);
            document.getElementById('bolus').value = '';
            document.getElementById('bolusTime').value = nowTimeString();
            Render.glucose(currentDay, Storage.getDay(currentDay));
        });

        document.getElementById('addBasalBtn').addEventListener('click', () => {
            const basalVal = parseFloat(document.getElementById('basal').value);
            const time = document.getElementById('basalTime').value;
            const hourFloat = Render.timeToFloat(time);
            if (isNaN(basalVal) || basalVal <= 0 || hourFloat === null) return;
            Storage.addInsulinPoint(currentDay, hourFloat, 0, basalVal);
            document.getElementById('basal').value = '';
            document.getElementById('basalTime').value = nowTimeString();
            Render.glucose(currentDay, Storage.getDay(currentDay));
        });

        document.getElementById('sportRow').addEventListener('click', e => {
            const btn = e.target.closest('.toggle-btn');
            if (!btn) return;
            const val = btn.dataset.val === 'true';
            Storage.setField(currentDay, 'sport', val);
            document.querySelectorAll('#sportRow .toggle-btn').forEach(b => {
                b.classList.toggle('active', b.dataset.val === String(val));
            });
        });

        document.getElementById('steps').addEventListener('input', function () {
            Storage.setField(currentDay, 'steps', this.value);
        });

        ['sleepStart', 'sleepEnd'].forEach(field => {
            document.getElementById(field).addEventListener('change', function () {
                Storage.setField(currentDay, field, this.value);
                const start = document.getElementById('sleepStart').value;
                const end   = document.getElementById('sleepEnd').value;
                document.getElementById('sleepDuration').textContent =
                    Render.calcSleepDuration(start, end);
            });
        });

        ['notes', 'conclusions'].forEach(field => {
            document.getElementById(field).addEventListener('input', function () {
                Storage.setField(currentDay, field, this.value);
            });
        });

        window.addEventListener('resize', () => {
            Render.glucose(currentDay, Storage.getDay(currentDay));
        });

        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
            Render.glucose(currentDay, Storage.getDay(currentDay));
        });
    }

    function init() {
        wireEvents();
        renderAll();
    }

    document.addEventListener('DOMContentLoaded', init);

})();