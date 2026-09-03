import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

const source = readFileSync(new URL('../src/main/resources/static/js/generate.js', import.meta.url), 'utf8');

function fixture(fetchResponse) {
    const created = [];
    class Element {
        constructor() {
            this.listeners = {}; this.attributes = {}; this.dataset = {}; this.style = {}; this.children = [];
            this.hidden = true; this.disabled = false; this.value = ''; this.scrollHeight = 100;
            this.classList = { add() {}, remove() {}, toggle() {} };
        }
        addEventListener(type, handler) { this.listeners[type] = handler; }
        setAttribute(key, value) { this.attributes[key] = value; }
        getAttribute(key) { return this.attributes[key]; }
        append(...children) { this.children.push(...children); }
        prepend(...children) { this.children.unshift(...children); }
        replaceChildren(...children) { this.children = children; }
        scrollIntoView() {}
        focus() {}
    }
    const nodes = new Map();
    const get = id => {
        if (!nodes.has(id)) nodes.set(id, new Element());
        return nodes.get(id);
    };
    const calls = [];
    runInNewContext(source, {
        document: {
            getElementById: get,
            querySelector: selector => ({ content: selector.includes('_csrf_header') ? 'X-CSRF-TOKEN' : 'test-token' }),
            querySelectorAll: () => [],
            createElement: tag => { const element = new Element(); element.tag = tag; created.push(element); return element; },
            addEventListener() {}
        },
        window: { innerHeight: 900, location: { href: 'http://localhost/dashboard/generate', origin: 'http://localhost' }, addEventListener() {} },
        URL,
        fetch: async (url, options) => { calls.push({ url, options }); return await fetchResponse(); }
    });
    get('ad-prompt').value = 'A new campaign';
    return { get, created, calls, submit: () => get('generate-form').listeners.submit({ preventDefault() {} }) };
}

const result = {
    workId: 42, workflowId: 'workflow', generationId: 'generation', status: 'PASS', revisionRounds: 0,
    candidates: [0, 1, 2].map(i => ({ candidateId: `c${i}`, sourceAngleId: `a${i}`, headline: `Headline ${i}`,
        primaryText: 'Copy', callToAction: 'Explore', hashtags: [] })),
    visuals: [0, 1, 2].map(i => ({ candidateId: `c${i}`, imageUrl: `/dashboard/works/42/images/${i}` }))
};

let finish;
const pending = new Promise(resolve => { finish = resolve; });
const state = fixture(() => pending);
const submission = state.submit();
assert.equal(state.get('generate-button').disabled, true);
assert.equal(state.get('saved-work-link').hidden, true);
assert.match(state.get('form-message').textContent, /work is being saved/);
await state.submit();
assert.equal(state.calls.length, 1, 'Double submission must not generate another paid work');
assert.equal(state.calls[0].options.headers['X-CSRF-TOKEN'], 'test-token');
assert.equal(state.calls[0].options.credentials, 'same-origin');
const request = JSON.parse(state.calls[0].options.body);
assert.deepEqual(Object.keys(request).sort(), [
    'brandName', 'brief', 'language', 'platform', 'requestedAngleCount', 'reviewLanguage'
].sort(), 'Generation should request only the essential campaign inputs');
assert.equal(request.campaign, undefined);
assert.equal(request.product, undefined);
assert.equal(request.language, 'English');
assert.equal(request.reviewLanguage, 'English');
finish({ ok: true, json: async () => result });
await submission;
assert.equal(state.calls.length, 1, 'Embedded saved visuals must not trigger separate image generation');
assert.equal(state.get('saved-work-link').href, '/dashboard/works/42');
assert.equal(state.get('saved-work-link').hidden, false);
assert.match(state.get('form-message').textContent, /My Works/);
assert.equal(state.get('generate-button').disabled, false);
console.log('PASS: One CSRF-protected generation request, no duplicate submit, success only after server save');

const images = state.created.filter(element => element.className === 'post-image');
const retries = state.created.filter(element => element.className === 'candidate-action is-retry');
const downloads = state.created.filter(element => element.className === 'candidate-action is-primary');
assert.equal(images.length, 3);
assert.equal(downloads[0].disabled, true);
images[0].onerror();
assert.equal(retries[0].hidden, false);
retries[0].listeners.click();
assert.equal(state.calls.length, 1, 'Reloading an image must never call the paid generation API');
assert.equal(images[0].src, '/dashboard/works/42/images/0');
images[0].onload();
assert.equal(downloads[0].disabled, false);
console.log('PASS: Three stored images are shown; failed image loads retry the file, not generation');

const failed = fixture(() => ({ ok: false, status: 503, json: async () => ({ message: 'Work could not be saved' }) }));
await failed.submit();
assert.equal(failed.get('saved-work-link').hidden, true);
assert.equal(failed.get('generation-result').hidden, true);
assert.match(failed.get('form-message').textContent, /Work could not be saved/);
assert.equal(failed.get('generate-button').disabled, false);
console.log('PASS: Failed persistence is visible and never shown as success');
