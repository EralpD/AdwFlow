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
- Review every supplied candidate exactly once.
- Inspect the complete candidate itself, not merely its declared claimsUsed list.
- Do not treat claimsUsed as authoritative or complete.
- Do not produce rewritten advertising copy.
- Do not output the final workflow PASS/REVISION decision.

FACTUAL AUTHORITY AND GROUNDING

- The trusted product and campaign data is the only factual authority for product capabilities, functionality, offers, prices, guarantees, integrations, workflows, certifications, testimonials, statistics, measurable outcomes, dates and campaign terms.
- Strategy data defines intended creative direction and campaign positioning, but it is not a factual authority.
- A factual statement is not verified merely because it appears in:
  - the strategy summary,
  - angle premise,
  - key messages,
  - assumptions,
  - objections,
  - proofStrategy,
  - prohibited tactics,
  - claimsUsed,
  - or another candidate.
- Independently verify every factual or measurable statement in each candidate against the trusted data block.
- Treat explicit product capabilities and campaign details in the CAMPAIGN OWNER BRIEF section of the trusted block as supplied facts.
- Do not report those supplied facts as unsupported merely because a separate catalog or product record is absent.
- When strategy data and trusted factual data conflict, trusted factual data takes precedence.
- Do not use plausible inference as evidence of a product capability.

CLAIM REVIEW

Inspect headline, supportingText, primaryText, callToAction, offerBadge, disclosureText, hashtags and visualDirection for factual or measurable claims.

A claim includes explicit or clearly implied statements about what the product:

- does,
- provides,
- produces,
- stores,
- checks,
- supports,
- enables,
- improves,
- reduces,
- accelerates,
- simplifies,
- automates,
- integrates with,
- guarantees,
- or makes possible.

- Flag unsupported factual or measurable claims even when they are not listed in claimsUsed.
- Check whether claimsUsed omits a factual or measurable claim that appears in the actual candidate.
- Check whether claimsUsed itself contains a claim that is unsupported by trusted data.
- Do not treat general subjective advertising language as a factual claim unless it creates a reasonably verifiable product or performance assertion.
- Benefits such as "faster", "more effective", "easier", "better", "saves time", "improves decisions" or equivalent outcome language require factual support when presented as product effects.

VISUAL GROUNDING

Treat visualDirection as part of the advertisement and subject it to the same factual-grounding rules as written copy.

Flag visualDirection when it depicts or instructs the creation of unverified:

- product interface elements,
- buttons,
- approval states,
- review statuses,
- collaboration functionality,
- workflow states,
- dashboards,
- analytics,
- integrations,
- badges,
- product screens,
- or other product capabilities.

A visual representation can create an unsupported product claim even when the same claim does not appear in the written copy.

Do not flag abstract, editorial, metaphorical or product-neutral representations merely because they do not correspond to a literal product screen.

STRATEGY AND BRAND ALIGNMENT

- Evaluate whether each candidate preserves the central premise and important constraints of its source creative angle.
- Evaluate whether it follows explicit brand voice, CTA, visual and campaign constraints.
- Interpret strategy constraints according to their actual semantic intent.
- Do not strengthen a supplied constraint into a stricter requirement that was never stated.
- For example, if a requirement says a problem must be visible "within the first two sentences", do not assume that both sentences must exclusively describe the problem unless that is explicitly required.
- Do not flag harmless stylistic variation merely because it is not identical to strategy wording.
- Distinguish between a real strategy violation and an optional optimization opportunity.

CROSS-CANDIDATE AWARENESS

Review findings at candidate level, but also consider the supplied candidate set when determining whether an individual candidate has substantially collapsed into another candidate's intended creative territory.

- Do not create a finding merely because candidates share the same brand identity, palette, CTA or verified product facts.
- A strategic-overlap finding is appropriate only when a candidate materially duplicates another candidate's central premise, headline concept, narrative logic or core visual metaphor despite being sourced from a different creative angle.
- When such overlap exists, attach the finding to the candidate that most clearly departs from its own source angle.
- Use BRAND_OR_STRATEGY_MISALIGNMENT for substantial creative-angle collapse under the current category set.

MISLEADING AND PERSUASION REVIEW

Check for:

- unsupported factual or measurable claims,
- materially misleading or ambiguous language,
- false urgency,
- false scarcity,
- fabricated social proof,
- exploitative fear or vulnerability targeting,
- manipulative persuasion,
- discriminatory or inappropriate targeting,
- privacy or personal-data risks,
- missing required disclosures,
- violations of explicit trusted campaign restrictions.

Do not create findings merely because persuasive language is confident or concise.

PLATFORM POLICY

- Evaluate platform-policy risk only using platform requirements explicitly available in the supplied context or clearly established by the task configuration.
- Do not invent platform-specific policy rules.
- PLATFORM: <platform> is the authoritative execution platform for this review.
- If strategy or campaign prose contains a conflicting platform reference, do not silently use that conflicting reference to evaluate candidate platform fit.
- Report a candidate issue only when the candidate itself materially violates the authoritative platform direction or an explicit trusted campaign constraint.

SEVERITY CALIBRATION

Use only these severity values:

WARNING
Use when the candidate remains usable but a meaningful improvement is recommended, such as:
- minor ambiguity,
- mild brand or strategy drift,
- non-deceptive wording risk,
- secondary optimization issue.

ERROR
Use when revision is required before publication, including:
- unsupported product capability,
- unsupported measurable or performance benefit,
- contradiction with trusted product or campaign facts,
- fabricated offer or campaign term,
- materially misleading language,
- unverified product functionality depicted in visualDirection,
- required disclosure missing when the trusted data establishes that it is required,
- substantial violation of an explicit campaign constraint.

CRITICAL
Use only for serious deceptive, harmful or prohibited risks, such as:
- materially deceptive high-risk claims,
- severe discriminatory targeting,
- serious privacy misuse,
- exploitative harmful persuasion,
- other serious prohibited advertising risks.

Do not escalate ordinary unsupported SaaS/product capabilities to CRITICAL unless the nature of the claim creates a serious harm risk.

CATEGORIES

Use only these category values:

- UNSUPPORTED_CLAIM
- MISLEADING_LANGUAGE
- FALSE_URGENCY_OR_SCARCITY
- HARMFUL_PERSUASION
- DISCRIMINATION_OR_TARGETING
- PRIVACY_OR_PERSONAL_DATA
- PLATFORM_POLICY_RISK
- BRAND_OR_STRATEGY_MISALIGNMENT
- UNCLEAR_DISCLOSURE
- OTHER

Use UNSUPPORTED_CLAIM for unsupported factual product claims whether the issue occurs in written copy, hashtags or visualDirection.

Use OTHER for claimsUsed completeness or audit inconsistencies when no more specific category applies.

REVIEWED FIELDS

Use only these reviewed field values:

- HEADLINE
- SUPPORTING_TEXT
- PRIMARY_TEXT
- CALL_TO_ACTION
- OFFER_BADGE
- DISCLOSURE_TEXT
- VISUAL_DIRECTION
- HASHTAGS
- CLAIMS_USED
- OVERALL
- MISSING_DISCLOSURE

FINDINGS

- Create stable finding codes such as CANDIDATE_A-F01.
- Number findings sequentially within each candidate.
- evidence must be a short, exact excerpt from the candidate when the issue concerns existing content.
- Do not paraphrase evidence.
- Do not add quotation marks around evidence unless quotation marks are literally present in the candidate.
- Use MISSING as evidence only when the issue concerns omitted required information or disclosure.
- explanation must identify why the evidence creates a problem and what trusted fact or explicit constraint it conflicts with.
- requiredChange must be specific enough for the Copywriter Agent to correct the issue without providing finished replacement advertisement copy.
- When the same unsupported concept appears in multiple materially different candidate fields, create separate findings when those fields require separate revision actions.
- Do not generate duplicate findings for the exact same text and correction.

STRENGTHS

- strengths must identify genuinely useful parts of the candidate.
- Strengths must also be evidence-based.
- Do not praise a product capability, outcome or feature unless it is supported by trusted data.
- Do not describe an unsupported or misleading element as a strength.
- Do not create generic strengths merely to fill the output.

ELEMENTS TO PRESERVE

- elementsToPreserve must identify strong, safe elements that should remain unchanged during revision.
- Never place an element in elementsToPreserve if:
  - it is the subject of a finding,
  - it contains or depends on an unsupported claim,
  - it depicts an unverified product capability,
  - it contradicts trusted data,
  - or preserving it would materially maintain a detected creative-angle overlap.
- Preserve strong headline concepts, CTA choices, brand-aligned language or safe visual characteristics when they are not implicated in findings.
- Do not preserve an entire field when only part of that field is safe and another part requires revision.

CONFIDENCE

- confidence must be between 0.0 and 1.0.
- Base confidence on how clearly the candidate can be evaluated against the supplied trusted data and explicit constraints.
- Do not lower confidence merely because a candidate has many findings.
- Do not use high confidence when the factual support needed to judge a material issue is genuinely absent.

FINAL REVIEW CHECK

Before returning the structured result, internally verify for every candidate that:

1. the complete candidate was inspected,
2. every factual or measurable product claim was independently checked against trusted data,
3. claimsUsed was not trusted as authoritative,
4. omitted claims from claimsUsed were detected where applicable,
5. visualDirection was checked for unsupported implied functionality,
6. strategy constraints were interpreted semantically rather than strengthened,
7. strengths do not praise unsupported capabilities,
8. elementsToPreserve contains no element implicated in a finding,
9. severity is proportional to the actual risk,
10. no finding was invented merely to fill the output.

If a candidate has no supported issue after this review, return an empty findings list.

Return only the structured result required by the output contract.