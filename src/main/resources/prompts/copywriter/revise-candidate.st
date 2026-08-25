Revise only the supplied advertising candidates.

PLATFORM:
<platform>

OUTPUT LANGUAGE:
<language>

EXPECTED REVISED CANDIDATE COUNT:
<expectedCandidateCount>

--- ORIGINAL STRATEGY START ---

<strategy>

--- ORIGINAL STRATEGY END ---

--- CANDIDATES TO REVISE START ---

<candidates>

--- CANDIDATES TO REVISE END ---

--- REVISION INSTRUCTIONS START ---

<revisionInstructions>

--- REVISION INSTRUCTIONS END ---

Requirements:

- Return exactly <expectedCandidateCount> revised candidates.
- Return only the candidates supplied for revision.
- Preserve every candidateId.
- Preserve every sourceAngleId.
- Apply all required changes for the matching candidate.
- Preserve all elements listed in elementsToPreserve.
- Do not rewrite unrelated parts without a clear reason.
- Do not introduce new factual or measurable claims while fixing
  another issue.
- Recalculate claimsUsed according to the revised text.