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

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        clearAttachments();
        closeAttachmentMenu();
        textarea.focus();
    });

    countAndLimitWords();
    resizeTextarea();
})();
