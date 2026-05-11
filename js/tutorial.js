const Tutorial = (() => {
    const KEY = 'glucolog_tutorial_done';

    let overlay = null;
    let box = null;

    let current = 0;

    let sidebarHooked = false;

    let activeTarget = null;
    let unbindReposition = null;

    const steps = [
        {
            target: '.header h1',
            title: 'Меню',
            text: 'Нажмите на "Glucolog" чтобы открыть меню. В нем можно заново просмотреть туториал, а так же импортировать и экспортировать данные',
            nextOnClick: true
        },
        {
            target: '#dateLabel',
            title: 'Даты',
            text: 'Используйте стрелочки чтобы перемещаться между датами или нажмите на текущую дату, чтобы открыть календарь',
            nextOnClick: true,
        },
        {
            target: '#glucoseVal',
            title: 'Глюкоза',
            text: 'Добавляйте измерения сахара здесь.',
            nextOnClick: true,
        },
        {
            target: '#mealList',
            title: 'Приемы пищи',
            text: 'Фиксируйте еду, эмоции и ощущения.',
            nextOnClick: true,
        },
        {
            target: '.log-card',
            title: 'Журнал',
            text: 'Здесь отображается история всех измерений и инсулина.',
            nextOnClick: true,
        },
    ];

    function shouldShow() {
        if (localStorage.getItem(KEY)) return false;

        const raw = localStorage.getItem('glucolog');
        if (!raw) return true;

        try {
            const data = JSON.parse(raw);
            return Object.keys(data).length === 0;
        } catch {
            return true;
        }
    }

    function build() {
        overlay = document.createElement('div');
        overlay.className = 'tutorial-overlay';

        box = document.createElement('div');
        box.className = 'tutorial-box';

        overlay.appendChild(box);
        document.body.appendChild(overlay);
    }

    function highlight(target) {
        document.querySelectorAll('.tutorial-highlight')
            .forEach(el => el.classList.remove('tutorial-highlight'));

        if (target) target.classList.add('tutorial-highlight');
    }

    function scrollToTarget(target) {
        return new Promise(resolve => {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'center',
                inline: 'nearest'
            });

            setTimeout(resolve, 350);
        });
    }

    function position(target) {
        const rect = target.getBoundingClientRect();

        const boxWidth = Math.min(320, window.innerWidth - 32);

        let top = rect.bottom + 12;
        let left = rect.left;

        if (left + boxWidth > window.innerWidth) {
            left = window.innerWidth - boxWidth - 12;
        }

        if (left < 12) left = 12;
        if (top < 12) top = 12;

        box.style.position = 'fixed';
        box.style.top = `${top}px`;
        box.style.left = `${left}px`;
        box.style.width = `${boxWidth}px`;
    }

    function bindReposition(target) {
        const handler = () => position(target);

        window.addEventListener('scroll', handler, true);
        window.addEventListener('resize', handler);

        return () => {
            window.removeEventListener('scroll', handler, true);
            window.removeEventListener('resize', handler);
        };
    }

    function isLastStep() {
        return current >= steps.length - 1;
    }

    async function showStep() {
        const step = steps[current];
        if (!step) return finish();

        const target = document.querySelector(step.target);

        if (!target) {
            setTimeout(showStep, 100);
            return;
        }

        activeTarget = target;

        await scrollToTarget(target);

        highlight(target);
        position(target);

        if (unbindReposition) unbindReposition();
        unbindReposition = bindReposition(target);

        const last = isLastStep();

        box.innerHTML = `
            <div class="tutorial-title">${step.title}</div>
            <div class="tutorial-text">${step.text}</div>

            ${
            step.nextOnClick
                ? (
                    last
                        ? '<button class="tutorial-finish-btn">Туториал завершён</button>'
                        : '<button class="tutorial-btn">Далее</button>'
                )
                : '<div class="tutorial-wait">Ожидание действия...</div>'
        }
        `;

        if (step.nextOnClick && !last) {
            box.querySelector('.tutorial-btn').addEventListener('click', next);
        }

        if (last) {
            box.querySelector('.tutorial-finish-btn')
                .addEventListener('click', finish);
        }

        if (step.wait === 'sidebar') hookSidebar();
    }

    function next() {
        const prevStep = steps[current];
        current++;

        if (prevStep?.wait === 'sidebar' && typeof Sidebar?.close === 'function') {
            Sidebar.close();
        }

        showStep();
    }

    function finish() {
        localStorage.setItem(KEY, '1');

        if (unbindReposition) unbindReposition();

        overlay?.remove();
        overlay = null;
        box = null;

        document.querySelectorAll('.tutorial-highlight')
            .forEach(el => el.classList.remove('tutorial-highlight'));
    }

    function hookSidebar() {
        if (sidebarHooked) return;
        sidebarHooked = true;

        const original = Sidebar.open;

        Sidebar.open = function (...args) {
            const res = original.apply(this, args);
            setTimeout(() => next(), 500);
            return res;
        };
    }

    function start(force = false) {
        if (!force && !shouldShow()) return;

        build();

        requestAnimationFrame(() => {
            overlay.classList.add('visible');
            showStep();
        });
    }

    return { start };
})();