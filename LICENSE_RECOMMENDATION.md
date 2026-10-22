# License Recommendation for NOOR

## Current Status

This repository does not yet have a code license. By default, all rights are reserved, meaning no one can legally use, modify, or distribute the code without explicit permission from the copyright holder.

## Recommended Options

### Option 1: MIT License (Recommended for Open Source)

**Pros**:
- Simple and permissive
- Allows commercial use
- Requires attribution only
- Widely understood and compatible with most projects

**Cons**:
- Does not protect against patent trolls
- Allows closed-source derivatives

**Suitability**: HIGH - Best choice if you want maximum adoption and don't mind others using your code in proprietary projects.

### Option 2: Apache License 2.0

**Pros**:
- Permissive like MIT
- Includes explicit patent grant
- Requires attribution and state changes
- Well-suited for libraries and frameworks

**Cons**:
- Slightly more complex than MIT
- Patent provisions may be overkill for some projects

**Suitability**: HIGH - Good choice if you want patent protection and a more comprehensive license than MIT.

### Option 3: GNU GPL v3.0

**Pros**:
- Strong copyleft - requires derivative works to also be GPL
- Protects against proprietary appropriation
- Well-understood in the free software community

**Cons**:
- Restrictive for commercial use
- May discourage some contributors
- Incompatible with some other licenses

**Suitability**: MEDIUM - Appropriate if you want to ensure all derivatives remain open source.

## Important Note on Third-Party Content

**Choosing a code license does NOT automatically grant rights to third-party content in this repository:**

- **Quran text**: Licensed under CC BY 3.0 by Tanzil (attribution required)
- **Quran audio**: Streamed from EveryAyah (link-back required, license varies by reciter)
- **Athkar dataset**: Based on Hisn Al-Muslim (license status unclear - requires clarification)

The code license you choose applies only to the source code you wrote, not to the Quran text, audio recitations, or Athkar content. These have their own independent licensing requirements that must be respected regardless of the code license.

## Recommendation

**MIT License** is recommended for NOOR because:
1. The app is a consumer-facing Islamic utility, not a library
2. Maximum adoption aligns with the project's spiritual/educational purpose
3. Simplicity reduces legal complexity for contributors
4. Attribution requirements (MIT) are already satisfied for Quran text

## Action Required

To add a license:

1. Choose one of the above options
2. Visit https://choosealicense.com/ to generate the license text
3. Create a file named `LICENSE` in the repository root with the license text
4. Update the README.md to reference the chosen license

**Do not add a license until you have made this decision.**
