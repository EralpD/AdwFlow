(() => {
    const WORD_LIMIT = 5000;
    const textarea = document.getElementById("ad-prompt");
    const counter = document.getElementById("word-counter");
    const form = document.getElementById("generate-form");
    const attachmentToggle = document.getElementById("attachment-toggle");
    const attachmentMenu = document.getElementById("attachment-menu");
    const uploadImagesButton = document.getElementById("upload-images-button");
    const imageInput = document.getElementById("image-input");
    const attachments = document.getElementById("attachments");
    const generateButton = document.getElementById("generate-button");
    const generateButtonText = document.getElementById("generate-button-text");
    const aiStatusText = document.getElementById("ai-status-text");
    const formMessage = document.getElementById("form-message");
    const generationResult = document.getElementById("generation-result");
    const resultStatus = document.getElementById("result-status");
    const resultSummary = document.getElementById("result-summary");
    const candidateList = document.getElementById("candidate-list");
    let selectedImages = [];

    const closeAttachmentMenu = () => {
        attachmentToggle.classList.remove("is-open");
        attachmentToggle.setAttribute("aria-expanded", "false");
        attachmentMenu.classList.remove("is-open");
        attachmentMenu.setAttribute("aria-hidden", "true");
    };

    const openAttachmentMenu = () => {
        attachmentToggle.classList.add("is-open");
        attachmentToggle.setAttribute("aria-expanded", "true");
        attachmentMenu.classList.add("is-open");
        attachmentMenu.setAttribute("aria-hidden", "false");
    };

    const syncImageInput = () => {
        const transfer = new DataTransfer();
        selectedImages.forEach(({ file }) => transfer.items.add(file));
        imageInput.files = transfer.files;
    };

    const renderAttachments = () => {
        attachments.replaceChildren();
        attachments.hidden = selectedImages.length === 0;

        selectedImages.forEach((image) => {
            const chip = document.createElement("div");
            chip.className = "attachment-chip";

            const thumbnail = document.createElement("img");
            thumbnail.className = "attachment-thumbnail";
            thumbnail.src = image.previewUrl;
            thumbnail.alt = "";

            const name = document.createElement("span");
            name.className = "attachment-name";
            name.textContent = image.file.name;
            name.title = image.file.name;

            const removeButton = document.createElement("button");
            removeButton.className = "remove-attachment";
            removeButton.type = "button";
            removeButton.setAttribute("aria-label", `Remove ${image.file.name}`);
            removeButton.textContent = "×";
            removeButton.addEventListener("click", () => {
                URL.revokeObjectURL(image.previewUrl);
                selectedImages = selectedImages.filter(({ id }) => id !== image.id);
                syncImageInput();
                renderAttachments();
            });

            chip.append(thumbnail, name, removeButton);
            attachments.append(chip);
        });
    };

    const clearAttachments = () => {
        selectedImages.forEach(({ previewUrl }) => URL.revokeObjectURL(previewUrl));
        selectedImages = [];
        imageInput.value = "";
        renderAttachments();
    };

    const setFormMessage = (message, type = "info") => {
        formMessage.textContent = message;
        formMessage.className = `form-message is-${type}`;
        formMessage.hidden = !message;
    };

    const setSubmitting = (submitting) => {
        generateButton.disabled = submitting;
        generateButton.setAttribute("aria-busy", String(submitting));
        generateButtonText.textContent = submitting ? "Generating…" : "Generate Ad";
        aiStatusText.textContent = submitting ? "Agents are working" : "AI is ready";
    };

    const appendTextElement = (parent, tagName, className, text) => {
        const element = document.createElement(tagName);
        element.className = className;
        element.textContent = text;
        parent.append(element);
        return element;
    };

    const renderCandidate = (candidate, reviewByCandidate) => {
        const card = document.createElement("article");
        card.className = "candidate-card";

        const cardTop = document.createElement("div");
        cardTop.className = "candidate-card-top";
        appendTextElement(cardTop, "span", "candidate-id", candidate.candidateId);
        appendTextElement(cardTop, "span", "angle-id", candidate.sourceAngleId);

        appendTextElement(card, "h3", "candidate-headline", candidate.headline);
        appendTextElement(card, "p", "candidate-copy", candidate.primaryText);
        appendTextElement(card, "p", "candidate-cta", candidate.callToAction);

        if (Array.isArray(candidate.hashtags) && candidate.hashtags.length > 0) {
            appendTextElement(
                card,
                "p",
                "candidate-hashtags",
                candidate.hashtags.join(" ")
            );
        }

        const review = reviewByCandidate.get(candidate.candidateId);
        const findingCount = Array.isArray(review?.findings) ? review.findings.length : 0;
        appendTextElement(
            card,
            "p",
            findingCount === 0 ? "candidate-review is-clear" : "candidate-review is-flagged",
            findingCount === 0
                ? "Compliance review: clear"
                : `Compliance review: ${findingCount} finding(s)`
        );

        card.prepend(cardTop);
        candidateList.append(card);
    };

    const renderGenerationResult = (result) => {
        candidateList.replaceChildren();

        resultStatus.textContent = result.status;
        resultStatus.className = result.status === "PASS"
            ? "result-status is-pass"
            : "result-status is-warning";

        const candidateReviews = result.review?.candidateReviews ?? [];
        const reviewByCandidate = new Map(
            candidateReviews.map((review) => [review.candidateId, review])
        );

        const candidates = Array.isArray(result.candidates) ? result.candidates : [];
        candidates.forEach((candidate) => renderCandidate(candidate, reviewByCandidate));

        resultSummary.textContent = [
            `${candidates.length} candidate(s) generated.`,
            `${result.revisionRounds ?? 0} revision round(s).`,
            `Workflow ${result.workflowId}.`
        ].join(" ");

        generationResult.hidden = false;
        generationResult.scrollIntoView({ behavior: "smooth", block: "start" });
    };

    const readErrorMessage = async (response) => {
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

    textarea.addEventListener("input", () => {
        countAndLimitWords();
        resizeTextarea();
    });

    textarea.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && event.shiftKey) {
            window.requestAnimationFrame(resizeTextarea);
        }
    });

    attachmentToggle.addEventListener("click", () => {
        const isOpen = attachmentToggle.getAttribute("aria-expanded") === "true";
        if (isOpen) {
            closeAttachmentMenu();
        } else {
            openAttachmentMenu();
        }
    });

    uploadImagesButton.addEventListener("click", () => {
        closeAttachmentMenu();
        imageInput.click();
    });

    imageInput.addEventListener("change", () => {
        const existingFiles = new Set(
            selectedImages.map(({ file }) => `${file.name}-${file.size}-${file.lastModified}`)
        );

        Array.from(imageInput.files)
            .filter((file) => file.type.startsWith("image/"))
            .forEach((file) => {
                const fileKey = `${file.name}-${file.size}-${file.lastModified}`;
                if (!existingFiles.has(fileKey)) {
                    selectedImages.push({
                        id: `${fileKey}-${selectedImages.length}`,
                        file,
                        previewUrl: URL.createObjectURL(file)
                    });
                    existingFiles.add(fileKey);
                }
            });

        syncImageInput();
        renderAttachments();
    });

    document.addEventListener("click", (event) => {
        if (!event.target.closest(".attachment-control")) {
            closeAttachmentMenu();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && attachmentToggle.getAttribute("aria-expanded") === "true") {
            closeAttachmentMenu();
            attachmentToggle.focus();
        }
    });

    window.addEventListener("resize", resizeTextarea);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        closeAttachmentMenu();

        const brief = textarea.value.trim();

        if (!brief) {
            setFormMessage("Describe the advertisement you want to create.", "error");
            textarea.focus();
            return;
        }

        const hadImages = selectedImages.length > 0;
        setFormMessage(
            hadImages
                ? "This first test sends the text brief only; selected images are not sent to the model yet."
                : "",
            "info"
        );

        generationResult.hidden = true;
        setSubmitting(true);

        try {
            const response = await fetch("/api/advertisements/generate", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    brief,
                    platform: "Instagram",
                    brandName: "unspecified",
                    brandVoice: "Calm, clear and encouraging",
                    knownTargetAudience: "unspecified",
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
            setFormMessage(
                result.status === "PASS"
                    ? "Generation completed successfully."
                    : "The revision limit was reached; inspect the remaining findings.",
                result.status === "PASS" ? "success" : "error"
            );
        } catch (error) {
            generationResult.hidden = true;
            setFormMessage(
                error instanceof Error ? error.message : "Generation failed.",
                "error"
            );
        } finally {
            clearAttachments();
            setSubmitting(false);
        }
    });

    countAndLimitWords();
    resizeTextarea();
})();
