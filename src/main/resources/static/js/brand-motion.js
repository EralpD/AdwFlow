(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

    document.querySelectorAll('.brand-mark').forEach(mark => {
        // Listen on the stationary link so rotating corners cannot retrigger hover.
        const trigger = mark.closest('a') || mark;
        let hovered = false;
        let focused = false;
        let active = false;
        let rotation = 0;

        const update = () => {
            if (reducedMotion.matches) {
                rotation = 0;
                active = false;
            } else {
                const nextActive = hovered || focused;
                if (nextActive === active) return;
                active = nextActive;
                // Keep increasing: enter 180°, leave 360°, next enter 540°.
                rotation += 180;
            }
            mark.style.setProperty('--brand-rotation', `${rotation}deg`);
        };

        trigger.addEventListener('pointerenter', event => {
            if (event.pointerType === 'touch') return;
            hovered = true;
            update();
        });
        trigger.addEventListener('pointerleave', () => {
            hovered = false;
            update();
        });
        trigger.addEventListener('pointercancel', () => {
            hovered = false;
            update();
        });
        trigger.addEventListener('focus', () => {
            focused = trigger.matches(':focus-visible');
            update();
        });
        trigger.addEventListener('blur', () => {
            focused = false;
            update();
        });
        reducedMotion.addEventListener('change', update);
    });
})();
