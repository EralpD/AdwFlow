# Dashboard implementation

**Current update:** My Works now lists persisted campaigns and opens their three saved visuals. See `MY_WORKS.md` for current storage, access-control, retention and test details. The original unavailable-library scope and browser verification below describe the earlier dashboard-only implementation.

## Scope and layout

`/dashboard` uses the existing Spring MVC, Thymeleaf and vanilla CSS/JavaScript stack. The existing blue, lavender and mint palette, typography and radius tokens are reused. A 240 px sidebar becomes a native modal navigation drawer below 961 px. White content surfaces, a compact introduction and one primary Generate action keep the first screen focused on starting a campaign.

The shared navigation fragment includes the home logo, current Dashboard link, ADMIN-only Admin link, display name and POST Logout. Account email, password hash and internal IDs are not rendered by the dashboard.

## Working behavior

- Home → `/dashboard` → Generate → `/dashboard/generate` remains the navigation flow.
- Generate opens the existing form; navigation does not submit an AI request.
- The legacy `/generate` redirect and the production screen's Dashboard return link remain unchanged.
- Mobile navigation supports keyboard interaction, native modal focus containment, Escape, backdrop/close button dismissal and focus restoration. Resizing to desktop closes the drawer and returns focus to the desktop navigation.
- Without JavaScript or native dialog support, the regular navigation remains available in document flow.
- Logout preserves the server-rendered CSRF token and native POST. Only its submitting button becomes busy; duplicate submission is blocked. Returning through browser history restores that button.
- Long display names wrap. Focus styles, a skip link, descriptive labels and reduced-motion styles are provided.

## Deliberately unavailable features

History and Archive currently have SQL tables but no application listing, search, like, archive or persistence services. The dashboard explicitly says the campaign library is **not available**, rather than implying that it is connected and empty. There are no fake records, counts, charts, disabled navigation links, filters or success messages. The sample campaign brief is labelled as an example.

No loading, retry, empty-search or data-operation states are simulated for endpoints that do not exist. Existing server authentication/error handling remains in place. No new SQL migrations, Redis/n8n work, security configuration changes or frontend framework dependencies are included.

## Files

### Shared logo motion

All eight page templates load `css/brand-motion.css` and `js/brand-motion.js`. Each `.brand-mark` rotates 180 degrees on hover and another 180 degrees in the same direction on leave, using a 420 ms transform transition. Only the icon rotates; its link stays stationary. Rotation is independent for header, footer, desktop and mobile logos. Keyboard focus is supported, touch does not create sticky hover, and `prefers-reduced-motion` disables the effect. There are no API or database changes.

Run the nine dependency-free animation logic and template coverage checks with `node scripts/brand-motion.test.mjs`. These exercise event/state logic using DOM doubles; they are not real-browser visual tests.

### Dashboard files

- `src/main/resources/templates/dashboard.html`: dashboard structure and content.
- `src/main/resources/templates/fragments/dashboard-navigation.html`: shared desktop/mobile navigation.
- `src/main/resources/static/css/dashboard.css`: scoped layout, responsive and accessibility styles.
- `src/main/resources/static/js/dashboard.js`: mobile menu and logout submission behavior.
- `src/test/java/com/example/demo/account/AccountSecurityTests.java`: extended route, role, escaping and real CSRF logout coverage.

## Automated verification

`mvn -B test package` completed successfully on 2026-08-27: **46 tests, 0 failures, 0 errors, 0 skipped**. Output: `target/dashboard-redesign-build.log`.

The existing security suite covers anonymous login redirects, saved requests, legacy routes, principal ID/email database validation, forged client IDs, denied admin access, missing/invalid/cross-session CSRF tokens and generation API mocks. New assertions cover the unavailable library, normal-user/Admin navigation, long HTML-like display names, avoiding account credential exposure and logout with the actual token rendered in the dashboard. Real generation services are mocked in these tests; no paid AI request is needed.

## Real browser verification

The newly packaged application ran in a temporary `eclipse-temurin:21-jre` Docker container with an externally loaded test H2 driver and an in-memory database. Only `127.0.0.1:8085` was published. The project `.env` was not mounted, a fake OpenAI key was supplied and no generation form was submitted. Existing PostgreSQL containers, volumes and records were not changed. The temporary `adwflow-dashboard-preview` container was stopped after testing; its in-memory accounts were discarded.

Observed in the real application:

- An anonymous `/dashboard` request redirected to `/login`.
- A synthetic account was registered and logged in; the saved Dashboard request was restored.
- Home → Dashboard → Generate opened the existing advertisement form with its Dashboard return link.
- Normal users had no Admin navigation link.
- At 1440, 1280, 768 and 360 px, the document scroll width did not exceed the viewport. Desktop navigation was 240 px wide; the Menu button was visible at 768 and 360 px.
- The mobile Menu button opened the dialog and focused Close menu. Clicking Close menu returned focus to Menu. Resizing an open drawer to desktop closed it, cleared `aria-expanded` and focused the desktop Dashboard link.

Screenshots are in `target/dashboard-preview/`: `dashboard-1280.png` (viewport), `dashboard-360.png` (viewport), `dashboard-menu-360.png` (viewport), plus full-page captures at 1440 and 768 px. The browser's full-page capture has stitching/scaling artifacts, so use the viewport captures for visual reference.

Verification limits: the browser automation's Enter/Escape key commands did not activate the browser's native default actions; keyboard activation, Escape dismissal and Tab containment therefore remain unconfirmed in a real keyboard session. The native dialog/cancel implementation is present, but a successful click test is not a keyboard test. Later browser calls repeatedly timed out, preventing live long-name, no-JavaScript, logout and additional legacy-route checks; relevant server behavior is covered by the automated suite. A requested viewport reset also timed out. These are not represented as passed browser checks. No production PostgreSQL integration test or paid AI end-to-end test was performed for this UI change.
