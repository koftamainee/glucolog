const GlucoseChart = (() => {
  const MIN_G = 3;
  const MAX_G = 16;
  const RANGE_LO = 3.9;
  const RANGE_HI = 7.8;

  function getCSSVar(name) {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  }

  function draw(canvasEl, points) {
    const dpr = window.devicePixelRatio || 1;
    const W = canvasEl.offsetWidth;
    const H = canvasEl.offsetHeight;

    canvasEl.width  = W * dpr;
    canvasEl.height = H * dpr;

    const ctx = canvasEl.getContext('2d');
    ctx.scale(dpr, dpr);

    const toX = h => (h / 24) * W;
    const toY = g => H - ((g - MIN_G) / (MAX_G - MIN_G)) * H;

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

    ctx.fillStyle = getCSSVar('--chart-text') || 'rgba(0,0,0,0.35)';
    ctx.font = `${9 * dpr / dpr}px -apple-system, system-ui, sans-serif`;
    ctx.textBaseline = 'bottom';
    for (let g = MIN_G + 1; g <= MAX_G; g += 2) {
      ctx.fillText(String(g), 3, toY(g) - 2);
    }

    ctx.fillStyle = getCSSVar('--chart-range') || 'rgba(29,158,117,0.10)';
    ctx.fillRect(0, toY(RANGE_HI), W, toY(RANGE_LO) - toY(RANGE_HI));

    if (!points || points.length === 0) return;

    if (points.length > 1) {
      ctx.beginPath();
      ctx.strokeStyle = getCSSVar('--chart-line') || '#1D9E75';
      ctx.lineWidth = 1.5;
      ctx.lineJoin = 'round';
      points.forEach((p, i) => {
        if (i === 0) ctx.moveTo(toX(p.h), toY(p.g));
        else ctx.lineTo(toX(p.h), toY(p.g));
      });
      ctx.stroke();
    }

    const dotColor = getCSSVar('--chart-dot') || '#0F6E56';
    const labelColor = getCSSVar('--chart-text') || 'rgba(0,0,0,0.35)';
    points.forEach(p => {
      ctx.beginPath();
      ctx.fillStyle = dotColor;
      ctx.arc(toX(p.h), toY(p.g), 3.5, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = labelColor;
      ctx.textBaseline = 'bottom';
      ctx.fillText(p.g.toFixed(1), toX(p.h) + 5, toY(p.g) - 2);
    });
  }

  return { draw };
})();
