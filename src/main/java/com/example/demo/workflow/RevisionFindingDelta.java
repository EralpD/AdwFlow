package com.example.demo.workflow;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record RevisionFindingDelta(
        int revisionRound,
        String routedTo,
        List<String> previousFindingCodes,
        List<String> resolvedFindingCodes,
        List<String> remainingFindingCodes,
        List<String> introducedFindingCodes
) {
    public RevisionFindingDelta {
        previousFindingCodes = copy(previousFindingCodes);
        resolvedFindingCodes = copy(resolvedFindingCodes);
        remainingFindingCodes = copy(remainingFindingCodes);
        introducedFindingCodes = copy(introducedFindingCodes);
    }

    public static RevisionFindingDelta compare(int round, String routedTo,
            List<String> previous, List<String> current) {
        Set<String> before = new LinkedHashSet<>(previous);
        Set<String> after = new LinkedHashSet<>(current);
        return new RevisionFindingDelta(
                round,
                routedTo,
                List.copyOf(before),
                before.stream().filter(code -> !after.contains(code)).toList(),
                after.stream().filter(before::contains).toList(),
                after.stream().filter(code -> !before.contains(code)).toList()
        );
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
