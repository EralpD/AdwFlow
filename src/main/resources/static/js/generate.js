(() => {
    const securityHeaders = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        if (!token || !header) {
            throw new Error("Your security token is missing. Refresh the page or log in again.");
        }
        return { "Content-Type": "application/json", [header]: token };
    };
    const WORD_LIMIT = 5000;
    const FORMAT_CONFIG = {
        portrait: { label: "Portrait 4:5", width: 1080, height: 1350 },
        square: { label: "Square 1:1", width: 1080, height: 1080 },
        story: { label: "Story 9:16", width: 1080, height: 1920 }
    };
    const textarea = document.getElementById("ad-prompt");
    const counter = document.getElementById("word-counter");
    const form = document.getElementById("generate-form");
    const generateButton = document.getElementById("generate-button");
    const generateButtonText = document.getElementById("generate-button-text");
    const creativeToolbar = document.querySelector(".creative-toolbar");
    const aiStatusText = document.getElementById("ai-status-text");
    const formMessage = document.getElementById("form-message");
    const generationResult = document.getElementById("generation-result");
    const resultStatus = document.getElementById("result-status");
    const resultSummary = document.getElementById("result-summary");
    const candidateList = document.getElementById("candidate-list");
    const formatButtons = Array.from(document.querySelectorAll(".format-button"));
    let activeFormat = "portrait";
    let activeGenerationContext = null;
    const candidateViews = new Map();

    const setFormMessage = (message, type = "info") => {
        formMessage.textContent = message;
        formMessage.className = `form-message is-${type}`;
        formMessage.hidden = !message;
    };

    const setSubmitting = (submitting) => {
        generateButton.disabled = submitting;
        generateButton.setAttribute("aria-busy", String(submitting));
        generateButtonText.textContent = submitting ? "Generating…" : "Generate Ads";
        aiStatusText.textContent = submitting ? "Agents are working" : "AI is ready";
    };

    const appendTextElement = (parent, tagName, className, text) => {
        const element = document.createElement(tagName);
        element.className = className;
        element.textContent = text;
        parent.append(element);
        return element;
    };

    const formatCaption = (candidate) => {
        const parts = [candidate.primaryText, candidate.callToAction]
            .filter((part) => typeof part === "string" && part.trim());

        if (Array.isArray(candidate.hashtags) && candidate.hashtags.length > 0) {
            parts.push(candidate.hashtags.join(" "));
        }

        return parts.join("\n\n");
    };

    const copyText = async (text) => {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
            return;
        }

        const helper = document.createElement("textarea");
        helper.value = text;
        helper.setAttribute("readonly", "");
        helper.style.position = "fixed";
        helper.style.opacity = "0";
        document.body.append(helper);
        helper.select();
        document.execCommand("copy");
        helper.remove();
    };

    const showButtonFeedback = (button, message) => {
        const originalLabel = button.dataset.label || button.textContent;
        button.dataset.label = originalLabel;
        button.textContent = message;
        window.setTimeout(() => {
            button.textContent = originalLabel;
        }, 1600);
    };

    const getBrandLabel = (result) => {
        const product = result.strategy?.briefAnalysis?.productOrOffer;
        if (typeof product !== "string" || !product.trim()) {
            return result.trustedContext?.product?.name || "Your brand";
        }

        return product
            .split(/[,:;|—–]/)[0]
            .trim()
            .slice(0, 28) || "Your brand";
    };

    const getVisualUrl = (visual) => {
        if (!visual || typeof visual !== "object") {
            return "";
        }

        const directUrl = visual.imageUrl || visual.url || visual.visual?.imageUrl;
        if (typeof directUrl === "string" && directUrl.trim()) {
            return directUrl;
        }

        const base64 = visual.imageBase64 || visual.b64Json || visual.b64_json;
        return typeof base64 === "string" && base64.trim()
            ? `data:image/png;base64,${base64}`
            : "";
    };

    const setVisualState = (view, state, message) => {
        view.card.dataset.visualState = state;
        view.visualStatus.textContent = message;
        view.retryButton.hidden = state !== "error";
        view.retryButton.disabled = state === "generating";
        view.downloadButton.disabled = state !== "ready";
        view.placeholderLabel.textContent = state === "error"
            ? "Visual unavailable"
            : state === "ready"
                ? "Visual ready"
                : "Creating campaign visual";
    };

    const applyVisual = (view, imageUrl) => {
        view.imageUrl = imageUrl;
        setVisualState(view, "generating", "Finishing visual…");
        view.image.alt = `Generated visual for ${view.candidate.headline}`;

        try {
            const imageOrigin = new URL(imageUrl, window.location.href).origin;
            if (imageOrigin !== window.location.origin) {
                view.image.crossOrigin = "anonymous";
            }
        } catch {
            // Data URLs and same-document assets need no CORS configuration.
        }

        view.image.onload = () => {
            setVisualState(view, "ready", "Visual ready");
        };
        view.image.onerror = () => {
            setVisualState(view, "error", "The generated visual could not be loaded.");
        };
        view.image.src = imageUrl;
    };

    const findEmbeddedVisual = (result, candidateId) => {
        const visuals = result.visuals || result.generatedVisuals || result.adVisuals;
        if (!Array.isArray(visuals)) {
            return null;
        }

        return visuals.find((visual) => visual.candidateId === candidateId) || null;
    };


    const retrySavedVisual = (view) => {
        if (!view.imageUrl) {
            setVisualState(view, "error", "No saved visual is available. Open My Works to check this campaign.");
            return;
        }
        // Retry loading the saved file; never pay to regenerate an image on a load failure.
        applyVisual(view, view.imageUrl);
    };
    const drawCoverImage = (context, image, width, height) => {
        const scale = Math.max(width / image.naturalWidth, height / image.naturalHeight);
        const renderedWidth = image.naturalWidth * scale;
        const renderedHeight = image.naturalHeight * scale;
        context.drawImage(
            image,
            (width - renderedWidth) / 2,
            (height - renderedHeight) / 2,
            renderedWidth,
            renderedHeight
        );
    };

    const wrapCanvasText = (context, text, maximumWidth, maximumLines) => {
        const words = String(text || "").split(/\s+/).filter(Boolean);
        const lines = [];
        let line = "";

        words.forEach((word) => {
            const testLine = line ? `${line} ${word}` : word;
            if (context.measureText(testLine).width <= maximumWidth || !line) {
                line = testLine;
            } else if (lines.length < maximumLines - 1) {
                lines.push(line);
                line = word;
            } else if (lines.length < maximumLines) {
                line = `${line}…`;
            }
        });

        if (line && lines.length < maximumLines) {
            lines.push(line);
        }

        return lines;
    };

    const drawRoundedRectangle = (context, x, y, width, height, radius) => {
        const safeRadius = Math.min(radius, width / 2, height / 2);
        context.beginPath();
        context.moveTo(x + safeRadius, y);
        context.arcTo(x + width, y, x + width, y + height, safeRadius);
        context.arcTo(x + width, y + height, x, y + height, safeRadius);
        context.arcTo(x, y + height, x, y, safeRadius);
        context.arcTo(x, y, x + width, y, safeRadius);
        context.closePath();
    };

    const downloadCreative = async (view) => {
        const format = FORMAT_CONFIG[activeFormat];
        const canvas = document.createElement("canvas");
        canvas.width = format.width;
        canvas.height = format.height;
        const context = canvas.getContext("2d");

        const background = context.createLinearGradient(0, 0, format.width, format.height);
        background.addColorStop(0, "#0b1f4d");
        background.addColorStop(0.52, "#315fd6");
        background.addColorStop(1, "#9e7bff");
        context.fillStyle = background;
        context.fillRect(0, 0, format.width, format.height);

        if (view.image.complete && view.image.naturalWidth > 0) {
            try {
                drawCoverImage(context, view.image, format.width, format.height);
            } catch {
                showButtonFeedback(view.downloadButton, "Image blocks export");
                return;
            }
        } else {
            context.fillStyle = "rgba(155, 229, 255, 0.18)";
            context.beginPath();
            context.arc(format.width * 0.78, format.height * 0.18, format.width * 0.42, 0, Math.PI * 2);
            context.fill();
            context.fillStyle = "rgba(255, 255, 255, 0.1)";
            context.beginPath();
            context.arc(format.width * 0.2, format.height * 0.58, format.width * 0.28, 0, Math.PI * 2);
            context.fill();
        }

        const scrim = context.createLinearGradient(0, 0, 0, format.height);
        scrim.addColorStop(0, "rgba(5, 17, 46, 0.12)");
        scrim.addColorStop(0.48, "rgba(5, 17, 46, 0.18)");
        scrim.addColorStop(1, "rgba(5, 17, 46, 0.92)");
        context.fillStyle = scrim;
        context.fillRect(0, 0, format.width, format.height);

        const edge = format.width * 0.075;
        context.fillStyle = "rgba(255, 255, 255, 0.16)";
        drawRoundedRectangle(context, edge, edge, 260, 62, 31);
        context.fill();
        context.fillStyle = "#ffffff";
        context.font = "700 25px Arial, sans-serif";
        context.textBaseline = "middle";
        context.fillText(activeGenerationContext.brandLabel.toUpperCase(), edge + 28, edge + 31, 210);

        const headlineSize = activeFormat === "story" ? 82 : 72;
        context.font = `800 ${headlineSize}px Arial, sans-serif`;
        context.textBaseline = "alphabetic";
        const lines = wrapCanvasText(context, view.candidate.headline, format.width - edge * 2, 4);
        const lineHeight = headlineSize * 1.08;
        const ctaHeight = 82;
        const ctaY = format.height - edge - ctaHeight;
        const headlineBottom = ctaY - 88;
        context.fillStyle = "#ffffff";
        lines.forEach((line, index) => {
            const y = headlineBottom - ((lines.length - 1 - index) * lineHeight);
            context.fillText(line, edge, y, format.width - edge * 2);
        });

        context.font = "800 28px Arial, sans-serif";
        const ctaText = view.candidate.callToAction || "Learn more";
        const ctaWidth = Math.min(context.measureText(ctaText).width + 70, format.width - edge * 2);
        context.fillStyle = "#ffffff";
        drawRoundedRectangle(context, edge, ctaY, ctaWidth, ctaHeight, 41);
        context.fill();
        context.fillStyle = "#173b8f";
        context.textBaseline = "middle";
        context.fillText(ctaText, edge + 35, ctaY + ctaHeight / 2, ctaWidth - 70);

        try {
            const blob = await new Promise((resolve, reject) => {
                canvas.toBlob((value) => value ? resolve(value) : reject(new Error("Export failed")), "image/png");
            });
            const downloadUrl = URL.createObjectURL(blob);
            const link = document.createElement("a");
            const safeId = String(view.candidate.candidateId || "ad").replace(/[^a-z0-9-_]/gi, "-");
            link.download = `${safeId}-${activeFormat}.png`;
            link.href = downloadUrl;
            link.click();
            window.setTimeout(() => URL.revokeObjectURL(downloadUrl), 1000);
            showButtonFeedback(view.downloadButton, "Downloaded");
        } catch {
            showButtonFeedback(view.downloadButton, "Export failed");
        }
    };

    const renderCandidate = (candidate, reviewByCandidate, index, brandLabel) => {
        const card = document.createElement("article");
        card.className = "candidate-card";
        card.dataset.visualState = "generating";

        const previewColumn = document.createElement("div");
        previewColumn.className = "creative-preview-column";

        const postFrame = document.createElement("div");
        postFrame.className = "post-frame";
        postFrame.dataset.format = activeFormat;

        const image = document.createElement("img");
        image.className = "post-image";

        const placeholder = document.createElement("div");
        placeholder.className = "visual-placeholder";
        placeholder.setAttribute("aria-hidden", "true");
        const visualOrb = document.createElement("span");
        visualOrb.className = "visual-orb";
        const placeholderLabel = appendTextElement(
            placeholder,
            "span",
            "placeholder-label",
            "Creating campaign visual"
        );
        placeholder.prepend(visualOrb);

        const visualScrim = document.createElement("div");
        visualScrim.className = "post-scrim";
        const postTop = document.createElement("div");
        postTop.className = "post-top";
        appendTextElement(postTop, "span", "post-brand", brandLabel);
        appendTextElement(postTop, "span", "post-number", String(index + 1).padStart(2, "0"));
        const postCopy = document.createElement("div");
        postCopy.className = "post-copy";
        appendTextElement(postCopy, "h3", "post-headline", candidate.headline);
        if (candidate.supportingText) {
            appendTextElement(postCopy, "p", "post-supporting", candidate.supportingText);
        }
        appendTextElement(postCopy, "span", "post-cta", candidate.callToAction || "Learn more");
        visualScrim.append(postTop, postCopy);
        postFrame.append(image, placeholder, visualScrim);

        const visualMeta = document.createElement("div");
        visualMeta.className = "visual-meta";
        const visualStatusWrap = document.createElement("p");
        visualStatusWrap.className = "visual-status-wrap";
        const visualStatusDot = document.createElement("span");
        visualStatusDot.className = "visual-status-dot";
        const visualStatus = appendTextElement(
            visualStatusWrap,
            "span",
            "visual-status",
            "GPT Image 2 is creating this visual…"
        );
        visualStatusWrap.prepend(visualStatusDot);
        const visualFormat = appendTextElement(
            visualMeta,
            "span",
            "visual-format",
            FORMAT_CONFIG[activeFormat].label
        );
        visualMeta.prepend(visualStatusWrap);
        previewColumn.append(postFrame, visualMeta);

        const content = document.createElement("div");
        content.className = "candidate-content";
        const cardTop = document.createElement("div");
        cardTop.className = "candidate-card-top";
        appendTextElement(cardTop, "span", "candidate-id", candidate.candidateId);
        appendTextElement(cardTop, "span", "angle-id", candidate.sourceAngleId);

        appendTextElement(content, "p", "candidate-kicker", `Ad variation ${index + 1}`);
        appendTextElement(content, "h3", "candidate-headline", candidate.headline);

        const captionPanel = document.createElement("div");
        captionPanel.className = "caption-panel";
        appendTextElement(captionPanel, "p", "caption-label", "Instagram caption");
        appendTextElement(captionPanel, "p", "candidate-copy", candidate.primaryText);
        if (candidate.offerBadge && candidate.offerBadge !== "NONE") {
            appendTextElement(captionPanel, "p", "candidate-offer", `Teklif: ${candidate.offerBadge}`);
        }
        if (candidate.disclosureText && candidate.disclosureText !== "NONE") {
            appendTextElement(captionPanel, "p", "candidate-disclosure", candidate.disclosureText);
        }
        if (Array.isArray(candidate.hashtags) && candidate.hashtags.length > 0) {
            appendTextElement(captionPanel, "p", "candidate-hashtags", candidate.hashtags.join(" "));
        }

        const review = reviewByCandidate.get(candidate.candidateId);
        const findingCount = Array.isArray(review?.findings) ? review.findings.length : 0;
        const reviewLine = appendTextElement(
            content,
            "p",
            findingCount === 0 ? "candidate-review is-clear" : "candidate-review is-flagged",
            findingCount === 0
                ? "Compliance review passed"
                : `${findingCount} compliance finding(s) need attention`
        );

        const actions = document.createElement("div");
        actions.className = "candidate-actions";
        const downloadButton = document.createElement("button");
        downloadButton.type = "button";
        downloadButton.className = "candidate-action is-primary";
        downloadButton.textContent = "Download post";
        const copyButton = document.createElement("button");
        copyButton.type = "button";
        copyButton.className = "candidate-action";
        copyButton.textContent = "Copy caption";
        const retryButton = document.createElement("button");
        retryButton.type = "button";
        retryButton.className = "candidate-action is-retry";
        retryButton.textContent = "Reload saved visual";
        retryButton.hidden = true;
        actions.append(downloadButton, copyButton, retryButton);

        content.append(cardTop, captionPanel, reviewLine, actions);
        card.append(previewColumn, content);
        candidateList.append(card);

        const view = {
            candidate,
            card,
            postFrame,
            image,
            placeholderLabel,
            visualStatus,
            visualFormat,
            retryButton,
            downloadButton
        };
        candidateViews.set(candidate.candidateId, view);

        copyButton.addEventListener("click", async () => {
            try {
                await copyText(formatCaption(candidate));
                showButtonFeedback(copyButton, "Copied");
            } catch {
                showButtonFeedback(copyButton, "Copy failed");
            }
        });
        downloadButton.addEventListener("click", () => downloadCreative(view));
        retryButton.addEventListener("click", () => retrySavedVisual(view));

        return view;
    };

    const renderGenerationResult = (result) => {
        candidateList.replaceChildren();
        candidateViews.clear();

        resultStatus.textContent = result.status;
        resultStatus.className = result.status === "PASS"
            ? "result-status is-pass"
            : "result-status is-warning";

        const needsInput = result.status === "NEEDS_USER_INPUT";
        creativeToolbar.hidden = needsInput;

        const candidateReviews = result.review?.candidateReviews ?? [];
        const reviewByCandidate = new Map(
            candidateReviews.map((review) => [review.candidateId, review])
        );

        const candidates = Array.isArray(result.candidates) ? result.candidates : [];
        const brandLabel = getBrandLabel(result);
        activeGenerationContext = {
            workflowId: result.workflowId,
            generationId: result.generationId,
            brandLabel
        };

        const views = candidates.map((candidate, index) => (
            renderCandidate(candidate, reviewByCandidate, index, brandLabel)
        ));

        views.forEach((view) => {
            const embeddedVisual = findEmbeddedVisual(result, view.candidate.candidateId);
            const embeddedUrl = getVisualUrl(embeddedVisual);
            if (embeddedUrl) {
                applyVisual(view, embeddedUrl);
            } else {
                setVisualState(view, "error", "No saved visual is available for this variation.");
            }
        });

        if (needsInput) {
            const missing = Array.isArray(result.missingInputs) ? result.missingInputs : [];
            resultSummary.textContent = `Generation stopped. Missing or unverified fields: ${missing.join(", ") || "unknown"}. Workflow ${result.workflowId}.`;
        } else {
            const deltas = Array.isArray(result.revisionFindingDeltas) ? result.revisionFindingDeltas : [];
            const resolved = deltas.flatMap((delta) => delta.resolvedFindingCodes || []);
            const remaining = deltas.at(-1)?.remainingFindingCodes || [];
            resultSummary.textContent = [
                `${candidates.length} ad variations saved.`,
                `${result.revisionRounds ?? 0} revision rounds.`,
                `Resolved finding codes: ${resolved.join(", ") || "none"}.`,
                `Remaining finding codes: ${remaining.join(", ") || "none"}.`,
                `Workflow ${result.workflowId}.`
            ].join(" ");
        }

        generationResult.hidden = false;
        generationResult.scrollIntoView({ behavior: "smooth", block: "start" });
    };

    const readErrorMessage = async (response) => {
        if (response.status === 401 || response.status === 403) {
            document.getElementById("session-actions").hidden = false;
        }
        try {
            const error = await response.json();
            return error.message || `Request failed with status ${response.status}.`;
        } catch {
            return `Request failed with status ${response.status}.`;
        }
    };

    const countAndLimitWords = () => {
        const wordPattern = /\S+/g;
        let match;
        let wordCount = 0;
        let overflowIndex = -1;

        while ((match = wordPattern.exec(textarea.value)) !== null) {
            wordCount += 1;
            if (wordCount === WORD_LIMIT + 1) {
                overflowIndex = match.index;
                break;
            }
        }

        if (overflowIndex !== -1) {
            textarea.value = textarea.value.slice(0, overflowIndex).trimEnd();
            wordCount = WORD_LIMIT;
        }

        counter.textContent = `${wordCount}/${WORD_LIMIT}`;
        counter.classList.toggle("limit-reached", wordCount >= WORD_LIMIT);
    };

    const resizeTextarea = () => {
        textarea.style.height = "auto";
        const maximumHeight = window.innerHeight * 0.48;
        textarea.style.height = `${Math.min(textarea.scrollHeight, maximumHeight)}px`;
        textarea.style.overflowY = textarea.scrollHeight > maximumHeight ? "auto" : "hidden";
    };

    const validateForm = () => {
        const controls = Array.from(form.querySelectorAll?.("input, select, textarea") || []);
        const invalid = controls.find((control) => !control.checkValidity());
        if (invalid) {
            invalid.reportValidity();
            invalid.focus();
            return false;
        }
        return true;
    };

    textarea.addEventListener("input", () => {
        countAndLimitWords();
        resizeTextarea();
    });

    textarea.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && event.shiftKey) {
            window.requestAnimationFrame(resizeTextarea);
        }
    });

    formatButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const requestedFormat = button.dataset.format;
            if (!FORMAT_CONFIG[requestedFormat]) {
                return;
            }

            activeFormat = requestedFormat;
            formatButtons.forEach((formatButton) => {
                const isActive = formatButton === button;
                formatButton.classList.toggle("is-active", isActive);
                formatButton.setAttribute("aria-pressed", String(isActive));
            });

            candidateViews.forEach((view) => {
                view.postFrame.dataset.format = activeFormat;
                view.visualFormat.textContent = FORMAT_CONFIG[activeFormat].label;
            });
        });
    });

    window.addEventListener("resize", resizeTextarea);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (generateButton.disabled) return;

        const brief = textarea.value.trim();

        if (!validateForm()) return;

        setFormMessage("Copy is being reviewed for compliance, three visuals are being generated, and the work is being saved. This may take a few minutes.", "info");
        document.getElementById("saved-work-link").hidden = true;

        generationResult.hidden = true;
        setSubmitting(true);

        try {
            const response = await fetch("/api/advertisements/generate", {
                method: "POST",
                credentials: "same-origin",
                headers: securityHeaders(),
                body: JSON.stringify({
                    brief,
                    platform: document.getElementById("platform").value,
                    brandName: document.getElementById("brand-name").value.trim(),
                    language: "English",
                    reviewLanguage: "English",
                    requestedAngleCount: 3
                })
            });

            if (!response.ok) {
                throw new Error(await readErrorMessage(response));
            }

            const result = await response.json();
            renderGenerationResult(result);
            const savedLink = document.getElementById("saved-work-link");
            if (Number.isSafeInteger(result.workId) && result.workId > 0) {
                savedLink.href = `/dashboard/works/${result.workId}`;
                savedLink.hidden = false;
            }
            const messageByStatus = {
                PASS: "Three ads and visuals were saved to My Works.",
                NEEDS_USER_INPUT: "Complete the missing or unverified fields and try again.",
                REVISION_LIMIT_REACHED: "The work was saved; review the remaining finding codes after the revision limit."
            };
            setFormMessage(messageByStatus[result.status] || "Generation complete.",
                result.status === "PASS" ? "success" : "error");
        } catch (error) {
            generationResult.hidden = true;
            setFormMessage(
                error instanceof Error ? error.message : "Generation failed.",
                "error"
            );
        } finally {
            setSubmitting(false);
        }
    });

    countAndLimitWords();
    resizeTextarea();
})();
