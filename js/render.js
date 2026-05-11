const Render = (() => {

    const MEALS = [
        { key: 'breakfast1', name: 'Первый завтрак' },
        { key: 'breakfast2', name: 'Второй завтрак' },
        { key: 'lunch', name: 'Обед' },
        { key: 'snack', name: 'Полдник' },
        { key: 'dinner', name: 'Ужин' },
    ];

    const STOOL_OPTS = [
        'Натощак', 'Утром, после воды', 'После 1 завтрака', 'После 2 завтрака',
        'После обеда', 'После полдника', 'После ужина', 'Запор', 'Диарея',
    ];

    const STRESS_OPTS = ['Нет', 'Да', 'Хронический'];

    function timeToFloat(timeStr) {
        if (!timeStr) return null;
        const [h, m] = timeStr.split(':').map(Number);
        return h + m / 60;
    }

    function floatToTime(hourFloat) {
        const h = Math.floor(hourFloat);
        const m = Math.round((hourFloat - h) * 60);
        return String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0');
    }

    function autoResize(el) {
        el.style.height = 'auto';
        el.style.height = el.scrollHeight + 'px';
    }

    function glucose(dateKey, data) {
        const canvas = document.getElementById('glucoseChart');
        GlucoseChart.draw(canvas, data.glucose || [], data.insulin || []);
    }

    function meals(dateKey, data) {
        const container = document.getElementById('mealList');
        container.innerHTML = '';

        MEALS.forEach(m => {
            const d = data[m.key] || {};
            const hunger = d.hunger || 0;

            const card = document.createElement('div');
            card.className = 'meal-card';

            const hungerHTML = [1, 2, 3, 4, 5]
                .map(i =>
                    `<button class="hunger-dot${hunger >= i ? ' filled' : ''}"
                        data-meal="${m.key}"
                        data-val="${i}"
                        type="button"></button>`
                ).join('');

            card.innerHTML = `
                <div class="meal-header">
                    <span class="meal-name">${m.name}</span>
                    <div class="meal-meta">
                        <input type="time"
                            value="${d.time || ''}"
                            data-meal="${m.key}">
                        <div class="hunger-dots">${hungerHTML}</div>
                    </div>
                </div>

                <textarea class="meal-food"
                    placeholder="Приём пищи"
                    data-meal="${m.key}"
                    data-field="food">${d.food || ''}</textarea>

                <div class="meal-fields">
                    <textarea placeholder="Физ. ощущения"
                        data-meal="${m.key}"
                        data-field="phys">${d.phys || ''}</textarea>

                    <textarea placeholder="Эмоции"
                        data-meal="${m.key}"
                        data-field="emo">${d.emo || ''}</textarea>
                </div>
            `;

            container.appendChild(card);
        });

        container.querySelectorAll('.hunger-dot').forEach(btn => {
            btn.addEventListener('click', () => {
                const meal = btn.dataset.meal;
                const val = parseInt(btn.dataset.val);

                Storage.setMealField(dateKey, meal, 'hunger', val);

                container.querySelectorAll(`.hunger-dot[data-meal="${meal}"]`)
                    .forEach(d => {
                        d.classList.toggle('filled', parseInt(d.dataset.val) <= val);
                    });
            });
        });

        container.querySelectorAll('input[type="time"]').forEach(el => {
            el.addEventListener('change', () => {
                Storage.setMealField(dateKey, el.dataset.meal, 'time', el.value);
            });
        });

        container.querySelectorAll('textarea').forEach(el => {
            autoResize(el);

            el.addEventListener('input', () => {
                autoResize(el);

                Storage.setMealField(
                    dateKey,
                    el.dataset.meal,
                    el.dataset.field,
                    el.value
                );
            });
        });
    }

    function water(dateKey, data) {
        const container = document.getElementById('waterTrack');
        container.innerHTML = '';

        const filled = data.water || 0;

        for (let i = 1; i <= 8; i++) {
            const g = document.createElement('div');
            g.className = 'water-glass' + (i <= filled ? ' filled' : '');
            g.textContent = '▾';

            g.addEventListener('click', () => {
                const next = i <= filled ? i - 1 : i;
                Storage.setField(dateKey, 'water', next);
                water(dateKey, Storage.getDay(dateKey));
            });

            container.appendChild(g);
        }
    }

    function sport(data) {
        const val = data.sport;

        document.querySelectorAll('#sportRow .toggle-btn').forEach(btn => {
            const isYes = btn.dataset.val === 'true';

            btn.classList.toggle(
                'active',
                (val === true && isYes) || (val !== true && !isYes)
            );
        });

        document.getElementById('steps').value = data.steps || '';
    }

    function stool(dateKey, data) {
        const container = document.getElementById('stoolGrid');
        container.innerHTML = '';

        const sel = data.stool || [];

        STOOL_OPTS.forEach(opt => {
            const el = document.createElement('div');
            el.className = 'stool-opt' + (sel.includes(opt) ? ' active' : '');
            el.textContent = opt;

            el.addEventListener('click', () => {
                Storage.toggleArrayItem(dateKey, 'stool', opt);
                stool(dateKey, Storage.getDay(dateKey));
            });

            container.appendChild(el);
        });
    }

    function sleep(data) {
        const start = data.sleepStart || '22:00';
        const end = data.sleepEnd || '06:00';

        document.getElementById('sleepStart').value = start;
        document.getElementById('sleepEnd').value = end;

        document.getElementById('sleepDuration').textContent =
            calcSleepDuration(start, end);
    }

    function calcSleepDuration(start, end) {
        const [sh, sm] = start.split(':').map(Number);
        const [eh, em] = end.split(':').map(Number);

        let mins = (eh * 60 + em) - (sh * 60 + sm);
        if (mins < 0) mins += 1440;

        const h = Math.floor(mins / 60);
        const m = mins % 60;

        return m ? `${h}ч ${m}м` : `${h}ч`;
    }

    function stress(dateKey, data) {
        const container = document.getElementById('stressRow');
        container.innerHTML = '';

        const cur = data.stress;

        STRESS_OPTS.forEach(opt => {
            const btn = document.createElement('button');
            btn.className = 'toggle-btn' + (cur === opt ? ' active' : '');
            btn.textContent = opt;

            btn.addEventListener('click', () => {
                Storage.setField(dateKey, 'stress', opt);
                stress(dateKey, Storage.getDay(dateKey));
            });

            container.appendChild(btn);
        });
    }

    function notes(dateKey, data) {
        const notesEl = document.querySelector('textarea.notes');
        const concEl = document.getElementById('conclusions');

        notesEl.value = data.notes || '';
        concEl.value = data.conclusions || '';

        autoResize(notesEl);
        autoResize(concEl);

        notesEl.addEventListener('input', () => {
            autoResize(notesEl);
            Storage.setField(dateKey, 'notes', notesEl.value);
        });

        concEl.addEventListener('input', () => {
            autoResize(concEl);
            Storage.setField(dateKey, 'conclusions', concEl.value);
        });
    }

    function log(dateKey, data) {
        const container = document.getElementById('logList');
        container.innerHTML = '';

        const entries = [];

        (data.glucose || []).forEach(p => {
            entries.push({ h: p.h, type: 'glucose', val: p.g });
        });

        (data.insulin || []).forEach(p => {
            if (p.b) entries.push({ h: p.h, type: 'bolus', val: p.b });
            if (p.ba) entries.push({ h: p.h, type: 'basal', val: p.ba });
        });

        entries.sort((a, b) => a.h - b.h);

        if (!entries.length) {
            container.innerHTML = `<div class="log-empty-card">Нет записей</div>`;
            return;
        }

        entries.forEach(e => {
            const time = floatToTime(e.h);

            let icon = '';
            let label = '';
            let value = '';
            let badge = '';

            if (e.type === 'glucose') {
                icon = '🩸';
                label = 'Глюкоза';
                value = `${e.val.toFixed(1)} ммоль/л`;
                badge = e.val < 4 ? 'low' : e.val > 10 ? 'high' : 'normal';
            }

            if (e.type === 'bolus') {
                icon = '💉';
                label = 'Болюс';
                value = `${e.val} ед.`;
                badge = 'insulin';
            }

            if (e.type === 'basal') {
                icon = '💊';
                label = 'Базальный';
                value = `${e.val} ед.`;
                badge = 'basal';
            }

            const row = document.createElement('div');
            row.className = `log-card ${badge}`;

            row.innerHTML = `
                <div class="log-time">${time}</div>
                <div>${icon} ${label}: ${value}</div>
            `;

            container.appendChild(row);
        });
    }

    function all(dateKey) {
        const data = Storage.getDay(dateKey);

        glucose(dateKey, data);
        meals(dateKey, data);
        water(dateKey, data);
        sport(data);
        stool(dateKey, data);
        sleep(data);
        stress(dateKey, data);
        notes(dateKey, data);
        log(dateKey, data);
    }

    return {
        all,
        glucose,
        log,
        calcSleepDuration,
        floatToTime,
        timeToFloat
    };
})();