const GlucoseChart = (() => {
    const MIN_G = 0.5;
    const MAX_G = 18;
    const RANGE_LO = 4;
    const RANGE_HI = 8;
    const MAX_INS = MAX_G;

    function getCSSVar(name) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    function draw(canvasEl, glucosePoints, insulinPoints) {
        const dpr = window.devicePixelRatio || 1;
        const W = canvasEl.offsetWidth;
        const H = canvasEl.offsetHeight;

        canvasEl.width  = W * dpr;
        canvasEl.height = H * dpr;

        const ctx = canvasEl.getContext('2d');
        ctx.scale(dpr, dpr);

        const toX = h => (h / 24) * W;
        const toY = g => H - ((g - MIN_G) / (MAX_G - MIN_G)) * H;
        const toYIns = v => H - (v / MAX_INS) * H;

        ctx.strokeStyle = getCSSVar('--chart-grid') || 'rgba(0,0,0,0.07)';
        ctx.lineWidth = 0.5;
        for (let g = MIN_G; g <= MAX_G; g++) {
            const y = toY(g);
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(W, y);
            ctx.stroke();
        }

        for (let h = 0; h <= 24; h += 6) {
            const x = toX(h);
            ctx.beginPath();
            ctx.moveTo(x, 0);
            ctx.lineTo(x, H);
            ctx.stroke();
        }

        ctx.fillStyle = getCSSVar('--chart-text') || 'rgba(0,0,0,0.45)';
        ctx.font = `bold ${10}px -apple-system, system-ui, sans-serif`;
        ctx.textBaseline = 'middle';
        for (let g = MIN_G + 1; g <= MAX_G; g += 2) {
            ctx.fillText(String(g), 4, toY(g));
        }

        ctx.fillStyle = getCSSVar('--chart-range') || 'rgba(29,158,117,0.10)';
        ctx.fillRect(0, toY(RANGE_HI), W, toY(RANGE_LO) - toY(RANGE_HI));

        function drawLine(points, color, useInsScale, dotColor) {
            if (!points || points.length === 0) return;
            const scale = useInsScale ? toYIns : toY;
            if (points.length > 1) {
                ctx.beginPath();
                ctx.strokeStyle = color;
                ctx.lineWidth = 1.5;
                ctx.lineJoin = 'round';
                points.forEach((p, i) => {
                    const val = useInsScale ? (p.b || p.ba || 0) : p.g;
                    if (i === 0) ctx.moveTo(toX(p.h), scale(val));
                    else ctx.lineTo(toX(p.h), scale(val));
                });
                ctx.stroke();
            }
            points.forEach(p => {
                const val = useInsScale ? (p.b || p.ba || 0) : p.g;
                ctx.beginPath();
                ctx.fillStyle = dotColor;
                ctx.arc(toX(p.h), scale(val), 3.5, 0, Math.PI * 2);
                ctx.fill();
                ctx.fillStyle = getCSSVar('--chart-text') || 'rgba(0,0,0,0.45)';
                ctx.textBaseline = 'bottom';
                ctx.font = `${9}px -apple-system, system-ui, sans-serif`;
                ctx.fillText(val.toFixed(1), toX(p.h) + 5, scale(val) - 2);
            });
        }

        const glucoseColor = getCSSVar('--chart-glucose') || '#1D9E75';
        const bolusColor   = getCSSVar('--chart-bolus')   || '#E05A33';
        const basalColor   = getCSSVar('--chart-basal')   || '#507FCC';

        if (insulinPoints && insulinPoints.length > 0) {
            const bolusPoints = insulinPoints.filter(p => p.b !== undefined && p.b !== null && p.b > 0);
            const basalPoints = insulinPoints.filter(p => p.ba !== undefined && p.ba !== null && p.ba > 0);
            drawLine(bolusPoints, bolusColor, true, bolusColor);
            drawLine(basalPoints, basalColor, true, basalColor);
        }

        drawLine(glucosePoints || [], glucoseColor, false, '#0F6E56');
    }

    return { draw };
})();