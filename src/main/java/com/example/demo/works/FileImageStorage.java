package com.example.demo.works;

import com.example.demo.agent.visual.VisualGenerationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class FileImageStorage implements ImageStorage {
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final Pattern KEY = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/[0-2]\\.png");
    private final Path root;

    public FileImageStorage(@Value("${app.media.root:./data/generated-images}") String directory) {
        try {
            Path configured = Path.of(directory).toAbsolutePath().normalize();
            Files.createDirectories(configured);
            root = configured.toRealPath();
        } catch (IOException failure) {
            throw new WorkStorageException("Image storage is not available.", failure);
        }
    }

    @Override
    public StoredImage store(UUID group, int index, VisualGenerationResult image) {
        if (index < 0 || index > 2 || image == null || image.format() == null
                || !"image/png".equals(image.mimeType()) || image.imageBase64() == null
                || image.imageBase64().length() > ((MAX_BYTES + 2L) / 3) * 4) {
            throw new WorkStorageException("The generated image has an unsupported format or size.");
        }
        String key = group + "/" + index + ".png";
        try {
            byte[] bytes = Base64.getDecoder().decode(image.imageBase64());
            if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IOException("Invalid image size");
            int width;
            int height;
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IOException("Invalid image data");
                var reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    if (!"png".equalsIgnoreCase(reader.getFormatName())) throw new IOException("PNG required");
                    width = reader.getWidth(0);
                    height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 8192 || height > 8192
                            || (long) width * height > 20_000_000) throw new IOException("Invalid image dimensions");
                    // Decode once to reject truncated/corrupt images before any database record is created.
                    reader.read(0);
                } finally {
                    reader.dispose();
                }
            }
            Path target = resolve(key);
            if (!Files.exists(target.getParent(), LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(target.getParent());
            checkDirectory(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new StoredImage(image.candidateId(), key, "image/png", width, height,
                    image.model(), image.format().name().toLowerCase(Locale.ROOT));
        } catch (IOException | IllegalArgumentException failure) {
            throw new WorkStorageException("The generated image could not be saved.", failure);
        }
    }

    @Override
    public Resource load(String storageKey) {
        Path file = resolve(storageKey);
        try {
            checkDirectory(file.getParent());
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing image");
            return new FileSystemResource(file);
        } catch (IOException failure) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
    }

    @Override
    public void deleteGroup(UUID group) {
        Path directory = root.resolve(group.toString()).normalize();
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            checkDirectory(directory);
            // Only our three known files; never traverse arbitrary directories or symlinks.
            for (int i = 0; i < 3; i++) Files.deleteIfExists(resolve(group + "/" + i + ".png"));
            Files.deleteIfExists(directory);
        } catch (IOException failure) {
            throw new WorkStorageException("Incomplete work files could not be cleaned up.", failure);
        }
    }

    private Path resolve(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return file;
    }

    private void checkDirectory(Path directory) throws IOException {
        if (Files.isSymbolicLink(directory) || !directory.toRealPath().startsWith(root)) {
            throw new IOException("Invalid storage directory");
        }
    }
}
