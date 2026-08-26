document.documentElement.classList.add("js");

const revealElements = document.querySelectorAll(".reveal");
const reducedMotion = window.matchMedia(
    "(prefers-reduced-motion: reduce)"
).matches;

if (reducedMotion || !("IntersectionObserver" in window)) {
    revealElements.forEach((element) => {
        element.classList.add("is-visible");
    });
} else {
    const revealObserver = new IntersectionObserver(
        (entries, observer) => {
            entries.forEach((entry) => {
                if (!entry.isIntersecting) {
                    return;
                }

                entry.target.classList.add("is-visible");
                observer.unobserve(entry.target);
            });
        },
        {
            rootMargin: "0px 0px -8% 0px",
            threshold: 0.12
        }
    );

    revealElements.forEach((element) => {
        revealObserver.observe(element);
    });
}

document.querySelectorAll("[data-current-year]").forEach((element) => {
    element.textContent = new Date().getFullYear().toString();
});
