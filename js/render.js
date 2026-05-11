const Render = (() => {

  const MEALS = [
    { key: 'breakfast1', name: 'Первый завтрак' },
    { key: 'breakfast2', name: 'Второй завтрак' },
    { key: 'lunch',      name: 'Обед' },
    { key: 'snack',      name: 'Полдник' },
    { key: 'dinner',     name: 'Ужин' },
  ];

  const STOOL_OPTS = [
    'Натощак', 'Утром, после воды', 'После 1 завтрака', 'После 2 завтрака',
    'После обеда', 'После полдника', 'После ужина', 'Запор', 'Диарея',
  ];

  const STRESS_OPTS = ['Нет', 'Да', 'Хронический'];

  function glucose(dateKey, data) {
    const canvas = document.getElementById('glucoseChart');
    GlucoseChart.draw(canvas, data.glucose || []);
  }

  function insulin(data) {
    document.getElementById('bolus').value = data.bolus || '';
    document.getElementById('basal').value = data.basal || '';
  }

  function meals(dateKey, data) {
    const container = document.getElementById('mealList');
    container.innerHTML = '';

    MEALS.forEach(m => {
      const mData = data[m.key] || {};
      const hunger = mData.hunger || 0;

      const card = document.createElement('div');
      card.className = 'meal-card';

      const hungerHTML = [1, 2, 3, 4, 5]
        .map(i => `<button class="hunger-dot${hunger >= i ? ' filled' : ''}" data-meal="${m.key}" data-val="${i}" aria-label="Голод ${i}" type="button"></button>`)
        .join('');

      card.innerHTML = `
        <div class="meal-header">
          <span class="meal-name">${m.name}</span>
          <div class="meal-meta">
            <input type="time" value="${mData.time || ''}" data-meal="${m.key}" data-field="time" aria-label="Время ${m.name}">
            <div class="hunger-dots">${hungerHTML}</div>
          </div>
        </div>
        <div
          class="meal-food"
          contenteditable="true"
          data-ph="Что ели..."
          data-meal="${m.key}"
          data-field="food"
          role="textbox"
          aria-label="Еда — ${m.name}"
          aria-multiline="false"
        >${mData.food || ''}</div>
        <div class="meal-fields">
          <input type="text" placeholder="физ. ощущения" value="${mData.phys || ''}" data-meal="${m.key}" data-field="phys" aria-label="Физические ощущения — ${m.name}">
          <input type="text" placeholder="эмоции" value="${mData.emo || ''}" data-meal="${m.key}" data-field="emo" aria-label="Эмоции — ${m.name}">
        </div>
      `;

      container.appendChild(card);
    });

    container.querySelectorAll('.hunger-dot').forEach(btn => {
      btn.addEventListener('click', () => {
        const mealKey = btn.dataset.meal;
        const val = parseInt(btn.dataset.val);
        Storage.setMealField(dateKey, mealKey, 'hunger', val);
        container.querySelectorAll(`.hunger-dot[data-meal="${mealKey}"]`).forEach(d => {
          d.classList.toggle('filled', parseInt(d.dataset.val) <= val);
        });
      });
    });

    container.querySelectorAll('input[type="time"][data-meal]').forEach(el => {
      el.addEventListener('change', () => {
        Storage.setMealField(dateKey, el.dataset.meal, el.dataset.field, el.value);
      });
    });

    container.querySelectorAll('.meal-food[contenteditable]').forEach(el => {
      el.addEventListener('input', () => {
        Storage.setMealField(dateKey, el.dataset.meal, 'food', el.textContent);
      });
    });

    container.querySelectorAll('.meal-fields input').forEach(el => {
      el.addEventListener('input', () => {
        Storage.setMealField(dateKey, el.dataset.meal, el.dataset.field, el.value);
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
      g.setAttribute('role', 'button');
      g.setAttribute('aria-label', `${i} стакан`);
      g.addEventListener('click', () => {
        const next = i <= filled ? i - 1 : i;
        Storage.setField(dateKey, 'water', next);
        water(dateKey, Storage.getDay(dateKey));
      });
      container.appendChild(g);
    }
  }


  function sport(data) {
    const val = data.sport; // true | false | undefined
    document.querySelectorAll('#sportRow .toggle-btn').forEach(btn => {
      const isYes = btn.dataset.val === 'true';
      btn.classList.toggle('active',
        val === true  && isYes ||
        val !== true  && !isYes
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
      el.setAttribute('role', 'button');
      el.addEventListener('click', () => {
        Storage.toggleArrayItem(dateKey, 'stool', opt);
        stool(dateKey, Storage.getDay(dateKey));
      });
      container.appendChild(el);
    });
  }

  function sleep(data) {
    const start = data.sleepStart || '22:00';
    const end   = data.sleepEnd   || '06:00';
    document.getElementById('sleepStart').value = start;
    document.getElementById('sleepEnd').value   = end;
    document.getElementById('sleepDuration').textContent = calcSleepDuration(start, end);
  }

  function calcSleepDuration(start, end) {
    if (!start || !end) return '';
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    let mins = (eh * 60 + em) - (sh * 60 + sm);
    if (mins < 0) mins += 24 * 60;
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return m > 0 ? `${h}ч ${m}м` : `${h}ч`;
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

  function notes(data) {
    document.getElementById('notes').value       = data.notes       || '';
    document.getElementById('conclusions').value = data.conclusions || '';
  }

  function all(dateKey) {
    const data = Storage.getDay(dateKey);
    glucose(dateKey, data);
    insulin(data);
    meals(dateKey, data);
    water(dateKey, data);
    sport(data);
    stool(dateKey, data);
    sleep(data);
    stress(dateKey, data);
    notes(data);
  }

  return { all, glucose, calcSleepDuration };
})();
