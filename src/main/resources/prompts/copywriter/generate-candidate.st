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

--- TRUSTED PRODUCT AND CAMPAIGN DATA START ---

<trustedContext>

--- TRUSTED PRODUCT AND CAMPAIGN DATA END ---

Requirements:

- Produce exactly <expectedCandidateCount> candidates.
- Produce exactly one candidate for every supplied creative angle.
- Keep sourceAngleId identical to the corresponding creative angle ID.
- Use stable candidate identifiers.
- For ANGLE_A use CANDIDATE_A.
- For ANGLE_B use CANDIDATE_B.
- For ANGLE_C use CANDIDATE_C.

STRATEGY USAGE

- Use the strategy as the source of creative direction, not as the source of factual truth.
- Use only facts independently supported by the trusted product and campaign data.
- If the strategy contains an unsupported capability, benefit, integration, workflow, performance statement or other factual claim, do not reproduce it.
- Preserve the strategic intention of an unsupported strategy statement by expressing it through verified facts when possible.
- Do not convert assumptions, objections, proof strategies, hypotheses or missing-information notes into product facts.

CANDIDATE DISTINCTIVENESS

- The candidates must differ in creative territory, not merely in wording.
- Preserve the central premise of each source angle.
- Preserve any supplied angle boundaries such as creativeTerritory, mustEmphasize, mustNotCenterOn or equivalent constraints.
- Do not allow a secondary feature shared across the product to become the central idea of every candidate.
- Candidates should differ meaningfully across multiple dimensions such as:
  - customer tension,
  - value framing,
  - persuasion logic,
  - narrative structure,
  - emotional destination,
  - visual metaphor.
- Do not reuse essentially the same headline concept across multiple candidates.
- Do not reuse essentially the same hero visual, composition logic or central metaphor across multiple candidates.
- If two candidates could use the same advertisement concept with only different copy, revise them until they are meaningfully distinct.
- Before returning the result, internally perform a pairwise distinctiveness check across all candidates and resolve substantial overlap.

COPY

- Keep headline concise and angle-specific.
- headline must express the source angle's central idea rather than a generic product slogan.
- Make primaryText persuasive, easy to understand and appropriate for <platform>.
- Make supportingText a short secondary line suitable for the visual overlay.
- Make callToAction specific and compatible with the strategy and verified campaign data.
- Do not fabricate proof, product information or measurable benefits.
- Do not claim that a result is faster, easier, better, more effective or more successful unless that claim is supported by trusted data.
- Avoid turning proofStrategy recommendations into advertisement claims.

VISUAL DIRECTION

- Populate visualDirection for every candidate.
- visualDirection must express the source angle through a distinct strategic visual concept.
- The scene must remain compatible with verified product and campaign information.
- Do not invent product UI, buttons, workflow states, approval indicators, integrations, dashboards or other unverified interface details.
- When exact interface details are unavailable, use abstract, editorial, metaphorical or product-neutral representations.
- For a verified digital product or SaaS concept, simplified large abstract workspace panels, modular creative cards, structured content blocks or non-readable product-system cues are allowed when they communicate only supported capabilities.
- Do not simply illustrate words from the copy using generic SaaS cards, arrows or flow diagrams.
- Do not default to a desk, domestic room, coffee cup, morning routine, beige interior, natural wood palette or warm lifestyle still life unless the trusted brief or the source angle specifically requires it.
- For every candidate, explicitly define in visualDirection:
  - environment or background system,
  - hero subject or product mechanism,
  - spatial and composition logic,
  - motion, sequence or transformation logic when relevant,
  - palette and lighting grounded in supplied brand constraints,
  - a visual boundary describing what must differ from the other candidates.
- Do not invent teams, clients, stakeholder review, presentation workflows, collaboration, export or approval concepts unless the trusted data explicitly supports them.
- Keep verified global brand identity consistent across candidates while varying each candidate's:
  - central metaphor,
  - hero subject,
  - composition logic,
  - visual narrative.
- A visual metaphor explicitly associated with one angle should not automatically be repeated for the others.
- Reuse the same central visual concept across candidates only when the trusted campaign data explicitly requires it.

CLAIMS

- claimsUsed must list every factual or measurable product or campaign claim present or clearly implied in headline, primaryText, supportingText, callToAction and visualDirection.
- Each claim in claimsUsed must be independently supported by the trusted data block.
- Do not include unsupported claims merely because they appear in strategy data.
- Return an empty claimsUsed list only when no verifiable factual or measurable claim is expressed.

OFFER AND DISCLOSURE

- Populate offerBadge and disclosureText for every candidate.
- offerBadge must contain a verified offer, promotion or campaign term when one exists; otherwise use NONE.
- disclosureText must contain a verified applicable disclosure or term when one exists; otherwise use NONE.
- Never invent offer terms or disclosures.

FINAL SELF-CHECK

Before returning the structured output, internally verify that:

1. every candidate maps to exactly one source angle,
2. every candidate preserves that angle's strategic territory,
3. every factual claim is grounded in trusted data,
4. claimsUsed captures all factual or measurable claims,
5. no visualDirection depicts an unverified capability,
6. the candidates are meaningfully distinct in both copy concept and visual concept,
7. the output is appropriate for <platform> and written in <language>.

Return only the structured output required by the output contract.
