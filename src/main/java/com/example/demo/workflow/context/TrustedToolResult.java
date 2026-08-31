package com.example.demo.workflow.context;

import java.util.List;

public record TrustedToolResult<T>(
        T data,
        List<String> missingInputs,
        List<String> verifiedEvidenceIds
) {
    public TrustedToolResult {
        missingInputs = missingInputs == null ? List.of() : List.copyOf(missingInputs);
        verifiedEvidenceIds = verifiedEvidenceIds == null ? List.of() : List.copyOf(verifiedEvidenceIds);
    }

    public boolean complete() {
        return missingInputs.isEmpty();
    }
}
