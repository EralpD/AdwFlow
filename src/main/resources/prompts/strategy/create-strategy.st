Create an advertising strategy from the following input.

USER BRIEF:
<brief>

PLATFORM:
<platform>

BRAND NAME:
<brandName>

BRAND VOICE:
<brandVoice>

KNOWN TARGET AUDIENCE:
<knownTargetAudience>

OUTPUT LANGUAGE:
<language>

--- TRUSTED PRODUCT AND CAMPAIGN DATA START ---

<trustedContext>

--- TRUSTED PRODUCT AND CAMPAIGN DATA END ---

REVISION GUIDANCE:
- <revisionGuidance>

REQUESTED CREATIVE ANGLE COUNT:
<requestedAngleCount>

Requirements:

- Analyze the brief before creating the angles.
- Keep known facts separate from assumptions.
- Add missing information to the missingInformation field.
- Produce exactly <requestedAngleCount> creative angles.
- Use stable angle identifiers such as ANGLE_A, ANGLE_B and ANGLE_C.
- Each angle must have a meaningfully different strategic premise.
- Each angle must contain practical key messages.
- Each angle must contain one persuasion blueprint.
- awarenessStage must be exactly one of:
  UNAWARE,
  PROBLEM_AWARE,
  SOLUTION_AWARE,
  PRODUCT_AWARE,
  MOST_AWARE.
- Objections should represent realistic reasons why the audience
  may reject or postpone the offer.
- proofStrategy must describe the kind of evidence needed.
  It must not fabricate evidence.
- prohibitedTactics must list unsafe or misleading persuasion
  methods that should not be used.
- Do not produce final advertisement copy.
- Write all human-readable strategy fields in <language>.
- Treat the campaign-owner brief inside the trusted block, together with any verified product and campaign records, as the factual source for this generation.
- Apply every item in revision guidance while preserving valid strategic distinctions.

Additional creative-angle requirements:

- Treat each creative angle as a distinct creative territory, not merely a different wording, feature emphasis or benefit statement.
- Each angle should differ meaningfully from the others across at least four of these dimensions: customer tension, value framing, persuasion logic, narrative structure, emotional destination and creative/visual metaphor.
- Do not reuse the same central metaphor, narrative structure or visual concept across multiple angles.
- If two angles could use essentially the same hero visual or headline concept with only minor copy changes, revise them until they are meaningfully distinct.
- Give every angle one clearly defined creative territory.
- Give every angle explicit boundaries describing what it must emphasize and what it must not center on.
- Where visual direction is part of the output contract, define the strategic visual concept rather than writing a finished image-generation prompt.
- When the brief is sparse, still produce the requested number of useful angles using minimal, clearly labelled assumptions. Do not invent product facts in order to create variety.
- If the target audience is unspecified, build the angles around verified customer problems, jobs-to-be-done, motivations and plausible objections rather than invented demographic profiles.
- Key messages must be instructions about what the downstream copywriter should communicate; they must not be polished advertisement copy.
- Before returning the output, internally compare every angle pair and revise any pair that substantially overlaps in premise, persuasion logic or creative territory.
