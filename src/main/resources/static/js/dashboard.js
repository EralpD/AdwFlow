(() => {
    const page = document.querySelector('.workspace-page');
    if (!page) return;

    page.querySelectorAll('[data-work-image]').forEach(img => {
        const showUnavailable = () => {
            img.hidden = true;
            const fallback = img.parentElement.querySelector('.workspace-image-fallback');
            if (fallback) fallback.hidden = false;
        };
        img.addEventListener('error', showUnavailable, { once: true });
        if (img.complete && img.naturalWidth === 0) showUnavailable();
    });

    const menu = document.getElementById('workspace-menu');
    const trigger = document.getElementById('workspace-menu-trigger');
    const close = menu?.querySelector('[data-menu-close]');
    const compact = window.matchMedia('(max-width: 960px)');

    if (menu && trigger && close && typeof menu.showModal === 'function') {
        const closeMenu = () => {
            if (menu.open) menu.close();
        };
        const syncViewport = () => {
            trigger.hidden = !compact.matches;
            if (!compact.matches) closeMenu();
        };
        trigger.addEventListener('click', () => {
            if (menu.open || !compact.matches) return;
            menu.showModal(); // Native dialog contains focus and makes the background inert.
            trigger.setAttribute('aria-expanded', 'true');
            page.dataset.menuOpen = 'true';
        });
        close.addEventListener('click', closeMenu);
        menu.addEventListener('cancel', event => {
            event.preventDefault();
            closeMenu();
        });
        menu.addEventListener('close', () => {
            trigger.setAttribute('aria-expanded', 'false');
            delete page.dataset.menuOpen;
            const returnTarget = compact.matches ? trigger : document.querySelector('.workspace-sidebar [aria-current="page"]');
            returnTarget?.focus({ preventScroll: true });
        });
        menu.addEventListener('click', event => {
            if (event.target !== menu) return;
            const bounds = menu.getBoundingClientRect();
            if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) closeMenu();
        });
        compact.addEventListener('change', syncViewport);
        // Without working dialog support/JavaScript the regular navigation stays visible.
        page.dataset.menuReady = 'true';
        syncViewport();
    }

    const logoutForms = Array.from(page.querySelectorAll('[data-logout-form]'));
    logoutForms.forEach(form => {
        const button = form.querySelector('button[type="submit"]');
        const label = form.querySelector('[data-logout-label]');
        const status = form.querySelector('[data-logout-status]');
        form.addEventListener('submit', event => {
            if (event.defaultPrevented) return;
            if (form.dataset.submitting === 'true') {
                event.preventDefault();
                return;
            }
            form.dataset.submitting = 'true';
            form.setAttribute('aria-busy', 'true');
            button.disabled = true;
            label.textContent = 'Signing out…';
            status.textContent = 'Signing out. Please wait.';
            // Native POST retains the server-rendered CSRF token.
        });
    });
    window.addEventListener('pageshow', () => {
        logoutForms.forEach(form => {
            delete form.dataset.submitting;
            form.removeAttribute('aria-busy');
            form.querySelector('button[type="submit"]').disabled = false;
            form.querySelector('[data-logout-label]').textContent = 'Logout';
            form.querySelector('[data-logout-status]').textContent = '';
        });
    });
})();
