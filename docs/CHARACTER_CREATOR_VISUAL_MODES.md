# Character creator visual modes

MetaHuman Legacy exposes two intentionally separate appearance paths.

- **Illustrated portrait**: one curated Library atlas cell is one complete visual identity. The game does not stack generated eyes, hair, beard or masks over that face.
- **Free mode**: the procedural renderer owns the facial structure, skin tone, hair, beard and eye controls.

Body build, stature, civilian clothing and accessories remain independent of the face mode. This separation prevents incompatible generated assets from drifting, clipping or producing atlas-sheet artifacts while preserving the hidden formative-power rules unchanged.
