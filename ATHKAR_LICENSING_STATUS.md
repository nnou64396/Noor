# Athkar Content Licensing Status

## Current Situation

The NOOR app includes an Athkar (أذكار) dataset sourced from the `rn0x/Adhkar-json` GitHub repository, which is based on "Hisn Al-Muslim" (حصن المسلم) by Sa'id bin Wahf Al-Qahtani.

## Repository License Status

**GitHub Repository**: rn0x/Adhkar-json  
**Repository Status**: Archived (July 10, 2023)  
**Repository License Statement**: "هذه الملفات بدون رخصة استخدام، يحق لك فعل ما تريد بها مجانا وبدون أي مسائلة قانونية" (These files are without a usage license; you are free to do whatever you want with them without any legal questions)

## Legal Analysis

### Issue 1: Repository Claim vs. Copyright Law

The GitHub repository's claim of "no license, free use" is not legally binding. Copyright law generally grants exclusive rights to the author unless:
- The work is in the public domain
- The author has explicitly released it under a license
- The copyright has expired

### Issue 2: Underlying Content (Hisn Al-Muslim)

The content is based on "Hisn Al-Muslim" (حصن المسلم), a modern Islamic supplication compilation by Sa'id bin Wahf Al-Qahtani. This is:
- **NOT** classical Islamic text (which would be in the public domain)
- A modern compilation with specific organization and presentation
- Potentially protected by copyright in many jurisdictions

### Issue 3: App's Current Attribution

The app currently includes this attribution in strings.xml:
```
المصدر: rn0x/Adhkar-json (حصن المسلم). لا توجد رخصة رسمية؛ يذكر المستودع أن الملفات متاحة للاستخدام الحر.
```

This acknowledges the license uncertainty but does not resolve it.

## Risk Assessment

**Risk Level**: MEDIUM

**Potential Issues**:
1. The GitHub repository's "free use" claim may not be legally enforceable
2. The underlying work (Hisn Al-Muslim) may be copyrighted
3. The repository is archived, making it difficult to seek clarification
4. Distributing the app with this content could infringe copyright

**Impact**: 
- Could face takedown requests
- Could face legal action from the original author
- Could undermine the project's credibility

## Recommended Actions

### Option 1: Seek Explicit Permission (Recommended)

**Action**: Contact Sa'id bin Wahf Al-Qahtani (author of Hisn Al-Muslim) to request explicit permission to redistribute the supplications in an app.

**Pros**:
- Provides legal certainty
- Respects the original author
- Maintains current feature

**Cons**:
- May be difficult to contact
- Permission may be denied or come with conditions

### Option 2: Replace with Clearly Licensed Alternative

**Action**: Find an Athkar/Adhkar dataset with a clear, permissive license (e.g., CC BY, CC0, MIT).

**Sources to investigate**:
- Open-source Islamic app projects
- Public domain supplication collections
- Other GitHub repositories with explicit licenses

**Pros**:
- Legal certainty
- No dependency on individual permission

**Cons**:
- Requires implementation work
- May not have the same content quality

### Option 3: Remove Athkar Feature Temporarily

**Action**: Remove the Athkar feature from the app until licensing is resolved.

**Pros**:
- Eliminates legal risk immediately
- Can be re-added later when resolved

**Cons**:
- Reduces app functionality
- May disappoint users

### Option 4: Accept Current Risk (Not Recommended)

**Action**: Publish with current attribution and "no license" claim.

**Pros**:
- No changes needed
- Maintains feature

**Cons**:
- Legal uncertainty remains
- Could face consequences later
- Irresponsible for a public release

## Recommendation

**Do NOT publish with the current Athkar dataset until licensing is resolved.**

The GitHub repository's claim of "no license, free use" is not sufficient legal protection for public distribution. The underlying work (Hisn Al-Muslim) is a modern compilation that may be copyrighted.

**Recommended priority**:
1. First attempt: Seek explicit permission from Sa'id bin Wahf Al-Qahtani
2. If permission cannot be obtained: Replace with a clearly licensed alternative
3. If no alternative is available: Remove the feature temporarily

## Resources

- Hisn Al-Muslim author: Sa'id bin Wahf Al-Qahtani
- GitHub repository: https://github.com/rn0x/Adhkar-json (archived)
- Current location in app: `app/src/main/assets/athkar/adhkar.json`
- Current attribution: `app/src/main/res/values/strings.xml` line 77
