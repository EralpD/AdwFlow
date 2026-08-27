# My Works and persistent images

## Generation and storage

`POST /api/advertisements/generate` now runs the existing text workflow, generates one original PNG for each of its three candidates, writes all three images to private storage, and commits one `history` row. It returns the existing text response fields plus `workId`, `workUrl` and three `visuals` containing authenticated image URLs. No SQL transaction is held during the AI requests.

The number of variations must be three (the default is still three). The title is the first candidate's headline, limited to the existing 255-character column. The original brief, full structured generation output and ordered image metadata are saved. `result_json` contains file keys, MIME type, actual dimensions, model and format; it does not contain image Base64 or temporary external links. The original images are stored separately from their text overlays. All originals use the existing Portrait format; the generator's format controls still adjust the preview/export crop.

The browser no longer starts three separate visual-generation requests. It waits for the complete saved response and loads those three stored files. The saved-work link and success message appear only after that response. Reloading a failed image retries its authenticated file URL, not the paid AI API. Double form submissions are blocked while the request is in progress. The older standalone visual-generation API remains available for compatibility, but it does not create a saved work and is not used by this flow.

## Configuration

```properties
ADWFLOW_MEDIA_ROOT=./data/generated-images
ADWFLOW_WORK_RETENTION=P30D
```

The default media path resolves relative to the application working directory. For this checkout it is `C:\Users\eralp_duman\Desktop\Proje2\data\generated-images`. Use an absolute path when running as a service. Each work has an unpredictable UUID directory with `0.png`, `1.png` and `2.png`. The first file is the My Works cover.

The application currently runs on the host, while PostgreSQL runs in Docker. Media therefore persists in the host directory; it is **not** inside the PostgreSQL container or its volume. If the application is later containerized, mount a persistent volume at its configured media root (for example `/data/generated-images`). All application instances must see the same storage. Back up the media directory and PostgreSQL together. A PostgreSQL-only backup does not contain the image files.

The `ImageStorage` interface permits adding an object-storage provider later. This implementation is local filesystem storage; no S3/MinIO service, cloud bucket, Redis dependency, new SQL table or migration was added. Do not put the media directory under public static resources or expose it with an unauthenticated web-server alias.

## My Works UI and access control

- `/dashboard`: user-owned, unexpired works, newest first, 12 per page. Each card shows the first original image and its title. Empty, load-error and missing-image states are distinct.
- `/dashboard/works/{id}`: all three original images, captions, original brief, review status, creation and expiration timestamps.
- `/dashboard/works/{id}/images/{index}`: authenticated image response with `Cache-Control: no-store`; `?download=true` downloads the original PNG.

Every query uses the ID from the server-side authenticated principal. Existing principal ID/email database validation and CSRF enforcement remain enabled. Guessing another work ID, including as an administrator, returns 404. Expired works cannot be listed or opened. HTML values are escaped. Raw file paths/keys and account credentials are not sent to the browser.

History and Archive remain the existing tables. Like/archive UI and operations are outside this change. Expiration currently hides and denies access to records; a physical expired-record/file purge and archive-aware retention worker are not implemented here. Do not delete a shared file when adding archive operations later without checking surviving references.

## Failures and operational limits

If a visual or confirmed SQL rollback fails the work, no successful response is sent and files for that incomplete work are removed. Corrupt, non-PNG and oversized image responses are rejected. Storage only accepts server-generated keys, does not overwrite existing images, and refuses directory traversal/symlink directories.

SQL and files are not one distributed transaction. If commit acknowledgement is lost, files are retained rather than risking deletion of images for a committed record. Cleanup failures and uncertain commit outcomes are logged with the UUID directory. Abrupt process termination can also leave unreferenced files; an automated orphan-reconciliation worker is not included. Reconcile such directories against both History and Archive before removal.

Generation is synchronous, with three sequential image requests and no automatic paid retry. Configure proxy/client timeouts for this longer request. Closing the tab does not provide a cancellation/recovery UI; check My Works before explicitly generating again after a connection failure. There is no durable job queue or cross-request idempotency token in this change.

## Verification commands

```text
mvn -B test
node scripts/generate.test.mjs
node scripts/brand-motion.test.mjs
```

The integration tests mock only AI generation and use real temporary PNG files, migrations, H2, repositories, rendered templates, sessions and CSRF. They cover three-image persistence and reload, user/admin isolation, image downloads, pagination, expiration, HTML escaping, a failed second image, SQL save failure, and unavailable-list UI. Filesystem tests cover restart persistence, corrupt data, traversal rejection and non-overwriting storage. Frontend tests use DOM doubles and are not real-browser visual tests.

On 2026-08-27, `mvn -B test package` passed **56 tests** with no failures/errors and produced the executable jar (`target/my-works-tests.log`). The three generation frontend scenarios and nine shared logo checks also passed. No paid AI calls were made. The new My Works layout has not been visually checked in a real browser during this change.

Five integration scenarios were also rerun successfully against a separate **PostgreSQL 17.10** Docker instance: persistence/ownership/download, second-image failure, SQL save failure, expiration, and pagination. Actual `PostgreSQLDialect`/JSON round trips were exercised (`target/my-works-postgres-tests.log`). This temporary instance used an isolated in-memory Docker mount and was stopped after testing; the existing application PostgreSQL database and volume were not modified.
