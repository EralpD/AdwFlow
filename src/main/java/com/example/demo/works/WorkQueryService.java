package com.example.demo.works;

import com.example.demo.agent.copywriter.AdCandidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class WorkQueryService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM uuuu, HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);
    private final HistoryWorkRepository history;
    public WorkQueryService(HistoryWorkRepository history) { this.history = history; }

    public record Summary(Long id, String title, String imageUrl, int visualCount, Instant createdAt, String createdLabel) {}
    public record Variation(AdCandidate candidate, String imageUrl) {}
    public record Detail(Long id, String title, String brief, String status, Instant createdAt, String createdLabel,
            Instant expiresAt, String expiresLabel, List<Variation> variations) {}

    public Page<Summary> list(Long userId, int page) {
        return history.findByUserIdAndExpiresAtAfterOrderByCreatedAtDescIdDesc(userId, Instant.now(),
                PageRequest.of(Math.max(0, Math.min(page, 10000)), 12)).map(work -> new Summary(
                work.getId(), work.getTitle(), work.getContent().images().isEmpty() ? null : imageUrl(work.getId(), 0),
                work.getContent().images().size(), work.getCreatedAt(), DATE.format(work.getCreatedAt())));
    }

    public Detail detail(Long userId, Long id) {
        HistoryWork work = owned(userId, id);
        var content = work.getContent();
        var generation = content.generation();
        List<Variation> variations = generation == null ? List.of() : IntStream.range(0, content.images().size())
                .mapToObj(i -> new Variation(generation.candidates().get(i), imageUrl(id, i))).toList();
        return new Detail(id, work.getTitle(), work.getBrief(), generation == null ? "Unavailable" : generation.status().name(),
                work.getCreatedAt(), DATE.format(work.getCreatedAt()), work.getExpiresAt(), DATE.format(work.getExpiresAt()), variations);
    }

    public StoredImage image(Long userId, Long id, int index) {
        HistoryWork work = owned(userId, id);
        var images = work.getContent().images();
        if (index < 0 || index >= images.size()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return images.get(index);
    }

    private HistoryWork owned(Long userId, Long id) {
        // Role ADMIN does not bypass ownership or expiration.
        return history.findByIdAndUserIdAndExpiresAtAfter(id, userId, Instant.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work not found"));
    }

    private static String imageUrl(Long id, int index) { return "/dashboard/works/" + id + "/images/" + index; }
}
