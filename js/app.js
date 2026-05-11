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

  function initGlucoseHourSelect() {
    const sel = document.getElementById('glucoseHour');
    for (let h = 0; h < 24; h++) {
      const opt = document.createElement('option');
      opt.value = h;
      opt.textContent = String(h).padStart(2, '0') + ':00';
      sel.appendChild(opt);
    }
    sel.value = new Date().getHours();
  }

  function wireEvents() {
    document.getElementById('prevDay').addEventListener('click', () => shiftDay(-1));
    document.getElementById('nextDay').addEventListener('click', () => shiftDay(1));
    document.getElementById('addGlucoseBtn').addEventListener('click', () => {
      const val  = parseFloat(document.getElementById('glucoseVal').value);
      const hour = parseInt(document.getElementById('glucoseHour').value);
      if (isNaN(val) || val < 1 || val > 30 || isNaN(hour)) return;
      Storage.addGlucosePoint(currentDay, hour, val);
      document.getElementById('glucoseVal').value = '';
      Render.glucose(currentDay, Storage.getDay(currentDay));
    });

    document.getElementById('glucoseVal').addEventListener('keydown', e => {
      if (e.key === 'Enter') document.getElementById('addGlucoseBtn').click();
    });

    ['bolus', 'basal'].forEach(field => {
      document.getElementById(field).addEventListener('input', function () {
        Storage.setField(currentDay, field, this.value);
      });
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
    initGlucoseHourSelect();
    wireEvents();
    renderAll();
  }

  document.addEventListener('DOMContentLoaded', init);

})();
