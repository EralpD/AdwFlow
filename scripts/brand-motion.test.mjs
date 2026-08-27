import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

const source = readFileSync(new URL('../src/main/resources/static/js/brand-motion.js', import.meta.url), 'utf8');

function fixture({ reduced = false, count = 1 } = {}) {
    const changes = [];
    const media = { matches: reduced, addEventListener: (_, listener) => changes.push(listener) };
    const items = Array.from({ length: count }, () => {
        const listeners = {};
        const properties = {};
        let keyboardFocus = false;
        const trigger = {
            addEventListener: (type, listener) => { listeners[type] = listener; },
            matches: () => keyboardFocus
        };
        return {
            mark: { closest: () => trigger, style: { setProperty: (name, value) => { properties[name] = value; } } },
            fire: (type, event = { pointerType: 'mouse' }) => listeners[type](event),
            focus: value => { keyboardFocus = value; listeners.focus(); },
            angle: () => properties['--brand-rotation'] || '0deg'
        };
    });
    runInNewContext(source, {
        document: { querySelectorAll: () => items.map(item => item.mark) },
        window: { matchMedia: () => media }
    });
    return {
        items,
        reduce: value => { media.matches = value; changes.forEach(listener => listener()); }
    };
}

const tests = {
    'enter/leave keeps rotating forward over repeated cycles'() {
        const { items: [icon] } = fixture();
        for (let i = 1; i <= 10; i++) {
            icon.fire(i % 2 ? 'pointerenter' : 'pointerleave');
            assert.equal(icon.angle(), `${i * 180}deg`);
        }
    },
    'duplicate hover events do not add extra half turns'() {
        const { items: [icon] } = fixture();
        icon.fire('pointerenter');
        icon.fire('pointerenter');
        assert.equal(icon.angle(), '180deg');
        icon.fire('pointerleave');
        icon.fire('pointerleave');
        assert.equal(icon.angle(), '360deg');
    },
    'all logo instances have independent rotation'() {
        const { items: [first, second] } = fixture({ count: 2 });
        first.fire('pointerenter');
        assert.equal(second.angle(), '0deg');
        second.fire('pointerenter');
        second.fire('pointerleave');
        assert.equal(first.angle(), '180deg');
        assert.equal(second.angle(), '360deg');
    },
    'touch interactions do not create sticky hover'() {
        const { items: [icon] } = fixture();
        icon.fire('pointerenter', { pointerType: 'touch' });
        icon.fire('pointerleave', { pointerType: 'touch' });
        assert.equal(icon.angle(), '0deg');
    },
    'pointer cancellation restores the resting orientation'() {
        const { items: [icon] } = fixture();
        icon.fire('pointerenter');
        icon.fire('pointercancel');
        assert.equal(icon.angle(), '360deg');
    },
    'keyboard focus combines with hover without double rotation'() {
        const { items: [icon] } = fixture();
        icon.focus(true);
        icon.fire('pointerenter');
        icon.fire('pointerleave');
        assert.equal(icon.angle(), '180deg');
        icon.fire('blur');
        assert.equal(icon.angle(), '360deg');
    },
    'mouse focus does not prevent rotation on pointer leave'() {
        const { items: [icon] } = fixture();
        icon.fire('pointerenter');
        icon.focus(false);
        icon.fire('pointerleave');
        assert.equal(icon.angle(), '360deg');
    },
    'reduced motion disables rotation including live preference changes'() {
        const state = fixture({ reduced: true });
        const [icon] = state.items;
        icon.fire('pointerenter');
        assert.equal(icon.angle(), '0deg');
        state.reduce(false);
        assert.equal(icon.angle(), '180deg');
        state.reduce(true);
        assert.equal(icon.angle(), '0deg');
        icon.fire('pointerleave');
        state.reduce(false);
        assert.equal(icon.angle(), '0deg');
    },
    'every page containing a logo loads the shared assets once'() {
        for (const name of ['home', 'dashboard', 'generate', 'work', 'login', 'register', 'admin', 'error', 'error/403']) {
            const html = readFileSync(new URL(`../src/main/resources/templates/${name}.html`, import.meta.url), 'utf8');
            assert.equal((html.match(/\/js\/brand-motion\.js/g) || []).length, 1, name);
            assert.equal((html.match(/\/css\/brand-motion\.css/g) || []).length, 1, name);
            assert.match(html, /<script[^>]*brand-motion\.js[^>]*defer/);
        }
    }
};

for (const [name, test] of Object.entries(tests)) {
    test();
    console.log(`PASS: ${name}`);
}
console.log(`${Object.keys(tests).length} brand motion checks passed.`);
