Generate advertising candidates from the supplied strategy.

PLATFORM:
<platform>

OUTPUT LANGUAGE:
<language>

EXPECTED CANDIDATE COUNT:
<expectedCandidateCount>

--- STRATEGY DATA START ---

<strategy>

--- STRATEGY DATA END ---

Requirements:

- Produce exactly <expectedCandidateCount> candidates.
- Produce exactly one candidate for every creative angle.
- Keep sourceAngleId identical to the corresponding creative
  angle ID.
- Use stable candidate identifiers.
- For ANGLE_A use CANDIDATE_A.
- For ANGLE_B use CANDIDATE_B.
- For ANGLE_C use CANDIDATE_C.
- The candidates must differ strategically, not merely in wording.
- Keep the headline concise.
- Make primaryText persuasive but easy to understand.
- Make callToAction specific and compatible with the strategy.
- Do not fabricate proof or product information.
- List any factual or measurable claims in claimsUsed.
- Return an empty claimsUsed list when no claim is used.