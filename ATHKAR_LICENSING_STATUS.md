# Athkar Content Licensing & Attribution Status

## Dataset Summary

- **App Dataset Path**: `app/src/main/assets/athkar/adhkar.json`
- **Original Source Repository**: [rn0x/Adhkar-json](https://github.com/rn0x/Adhkar-json)
- **Underlying Content**: Based on *Hisn Al-Muslim* (حصن المسلم) by Sa'id bin Wahf Al-Qahtani
- **Repository Status**: Archived (July 10, 2023)

## Repository Usage Statement

The original repository owner publicly declared in the repository documentation:

* **Arabic Text**:
  > "هذه الملفات بدون رخصة استخدام، يحق لك فعل ما تريد بها مجانا وبدون أي مسائلة قانونية"

* **English Translation**:
  > "These files are without a usage license; you have the right to do whatever you want with them for free and without any legal liability."

## Licensing Assessment & Nuance

### 1. Free-Use Grant
The repository author explicitly published a grant permitting unrestricted, free use of the files and data. While the repository does not attach a standardized open-source SPDX license file (e.g., MIT or CC-BY), the lack of a formal license document should not be treated as an automatic prohibition on using the dataset when a public free-use authorization is explicitly stated by the maintainer.

### 2. Distinguishing Source Attribution from Application Software License
The source data attribution for Athkar (as well as Quran text from Tanzil and audio from EveryAyah) is separate and distinct from NOOR's own application source code license. Preserving clear in-app and documentation attribution fulfills source recognition without imposing software licensing constraints on NOOR's codebase.

### 3. Legal Nuance & Underlying Content Authority
Attribution alone does not constitute an absolute legal guarantee, as the underlying supplications in *Hisn Al-Muslim* represent a modern compilation of traditional Islamic supplications. However:
- There is no demonstrated, active, or explicit restriction, claim, or takedown demand prohibiting publication.
- The remaining consideration is theoretical uncertainty regarding the repository maintainer's formal copyright/compilation authority over the underlying supplications, rather than a specific blocking restriction.
- No legal guarantee is made or implied on behalf of third parties or the project owner.

## Reassessment of Publication Status

- **Concrete Blocker Status**: NO concrete blocking restriction has been identified that prevents publication.
- **In-App Attribution**: The application includes explicit user-facing source attribution (`athkar_source_attribution`) in `strings.xml` and displays it on the Athkar screen.
- **Action**: Retain the existing Athkar dataset (`app/src/main/assets/athkar/adhkar.json`) and Athkar feature without modification, maintaining full attribution.
