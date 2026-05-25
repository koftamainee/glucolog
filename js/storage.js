const Storage = (() => {
    const KEY = 'glucolog';

    function loadAll() {
        try {
            return JSON.parse(localStorage.getItem(KEY) || '{}');
        } catch {
            return {};
        }
    }

    function saveAll(db) {
        try {
            localStorage.setItem(KEY, JSON.stringify(db));
            Drive.touchLocalModified();
        } catch (e) {
            console.warn('glucolog: failed to save', e);
        }
    }

    function getDay(dateKey) {
        const db = loadAll();
        if (!db[dateKey]) db[dateKey] = {};
        return db[dateKey];
    }

    function setField(dateKey, field, value) {
        const db = loadAll();
        if (!db[dateKey]) db[dateKey] = {};
        db[dateKey][field] = value;
        saveAll(db);
    }

    function setMealField(dateKey, mealKey, field, value) {
        const db = loadAll();
        if (!db[dateKey]) db[dateKey] = {};
        if (!db[dateKey][mealKey]) db[dateKey][mealKey] = {};
        db[dateKey][mealKey][field] = value;
        saveAll(db);
    }

    function addGlucosePoint(dateKey, hourFloat, value) {
        const db = loadAll();
        if (!db[dateKey]) db[dateKey] = {};
        if (!db[dateKey].glucose) db[dateKey].glucose = [];
        const idx = db[dateKey].glucose.findIndex(p => Math.abs(p.h - hourFloat) < 0.001);
        if (idx >= 0) {
            db[dateKey].glucose[idx].g = value;
        } else {
            db[dateKey].glucose.push({ h: hourFloat, g: value });
            db[dateKey].glucose.sort((a, b) => a.h - b.h);
        }
        saveAll(db);
    }

    function addInsulinPoint(dateKey, hourFloat, bolus, basal) {
        const db = loadAll();
        if (!db[dateKey]) db[dateKey] = {};
        if (!db[dateKey].insulin) db[dateKey].insulin = [];
        const idx = db[dateKey].insulin.findIndex(p => Math.abs(p.h - hourFloat) < 0.001);
        if (idx >= 0) {
            db[dateKey].insulin[idx].b = bolus;
            db[dateKey].insulin[idx].ba = basal;
        } else {
            db[dateKey].insulin.push({ h: hourFloat, b: bolus, ba: basal });
            db[dateKey].insulin.sort((a, b) => a.h - b.h);
        }
        saveAll(db);
    }

    function toggleArrayItem(dateKey, field, item) {
        const db = loadAll();
        if (!db[dateKey]) db[dateKey] = {};
        const arr = db[dateKey][field] || [];
        const idx = arr.indexOf(item);
        if (idx >= 0) arr.splice(idx, 1);
        else arr.push(item);
        db[dateKey][field] = arr;
        saveAll(db);
        return db[dateKey][field];
    }

    function removeGlucosePoint(dateKey, hourFloat) {
        const db = loadAll();
        if (!db[dateKey]?.glucose) return;
        db[dateKey].glucose = db[dateKey].glucose.filter(p => Math.abs(p.h - hourFloat) >= 0.001);
        saveAll(db);
    }

    function removeInsulinPoint(dateKey, hourFloat, type) {
        const db = loadAll();
        if (!db[dateKey]?.insulin) return;
        db[dateKey].insulin = db[dateKey].insulin
            .map(p => {
                if (Math.abs(p.h - hourFloat) >= 0.001) return p;
                const updated = { ...p };
                if (type === 'bolus') updated.b = 0;
                if (type === 'basal') updated.ba = 0;
                return updated;
            })
            .filter(p => (p.b || 0) > 0 || (p.ba || 0) > 0);
        saveAll(db);
    }

    function getAll() {
        return loadAll();
    }

    return { getDay, setField, setMealField, addGlucosePoint, addInsulinPoint, toggleArrayItem, removeGlucosePoint, removeInsulinPoint, getAll };
})();