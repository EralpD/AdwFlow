Review the supplied advertising candidates.

PLATFORM:
<platform>

REVIEW LANGUAGE:
<reviewLanguage>

EXPECTED REVIEW COUNT:
<expectedReviewCount>

--- STRATEGY DATA START ---

<strategy>

--- STRATEGY DATA END ---

--- TRUSTED PRODUCT AND CAMPAIGN DATA START ---

<trustedContext>

--- TRUSTED PRODUCT AND CAMPAIGN DATA END ---

--- ADVERTISING CANDIDATES START ---

<candidates>

--- ADVERTISING CANDIDATES END ---

Requirements:

- Return exactly <expectedReviewCount> candidate reviews.
- Preserve every candidateId and sourceAngleId.
- Review each candidate independently.
- Inspect the actual candidate text even when claimsUsed is empty.
- Do not treat claimsUsed as authoritative.
- Use only these severity values:
  WARNING,
  ERROR,
  CRITICAL.
- Use only these category values:
  UNSUPPORTED_CLAIM,
  MISLEADING_LANGUAGE,
  FALSE_URGENCY_OR_SCARCITY,
  HARMFUL_PERSUASION,
  DISCRIMINATION_OR_TARGETING,
  PRIVACY_OR_PERSONAL_DATA,
  PLATFORM_POLICY_RISK,
  BRAND_OR_STRATEGY_MISALIGNMENT,
  UNCLEAR_DISCLOSURE,
  OTHER.
- Use only these reviewed field values:
  HEADLINE,
  SUPPORTING_TEXT,
  PRIMARY_TEXT,
  CALL_TO_ACTION,
  OFFER_BADGE,
  DISCLOSURE_TEXT,
  VISUAL_DIRECTION,
  HASHTAGS,
  CLAIMS_USED,
  OVERALL,
  MISSING_DISCLOSURE.
- Create stable finding codes such as CANDIDATE_A-F01.
- evidence must be an exact candidate excerpt, or MISSING when
  the issue concerns omitted information.
- requiredChange must be specific enough for the Copywriter Agent.
- strengths must describe useful parts of the candidate.
- elementsToPreserve must identify strong parts that should remain
  unchanged during a revision.
- confidence must be between 0.0 and 1.0.
- Do not produce rewritten advertisement copy.
- Do not output the final workflow decision.
- Verify every factual claim, offer, price, date and disclosure against the trusted data block.
