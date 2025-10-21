# Lumen — Visual Mood Journal

> *Capture your mood. Curate your light.*

Lumen (formerly "Moments") is an offline-first visual journaling concept for Android built with Kotlin and Jetpack Compose. It blends the visual minimalism of VSCO, the curatorial feel of Pinterest, and the intimacy of a private mood diary to help you preserve how your day *felt*, not just how it looked.

---

## 🌄 Core Concept

Each entry in Lumen is a **visual vignette** captured as a personal mood moment:

- **Image** — Choose a photo from the gallery or capture directly with CameraX.
- **Tone** — Assign an emotion, color, or theme tag that reflects your feeling.
- **Caption** *(optional)* — Add a note that anchors the memory.
- **Timestamp** — Automatically saved so your mosaic forms a timeline.

Your home screen becomes a scrolling mosaic of memories, rendered as a minimalist VSCO-style grid with no likes, follows, or algorithmic noise—just aesthetic memory curated for yourself.

---

## 🎯 Minimal V1 Feature Set

| Feature                                  | Description                                                | Stack / Tools                    |
| ---------------------------------------- | ---------------------------------------------------------- | -------------------------------- |
| **Feed Grid**                            | Offline-first mosaic of saved moments with pinch-to-zoom   | Jetpack Compose `LazyVerticalGrid` |
| **Add Moment Screen**                    | Capture/import image, assign mood tone, write a caption    | CameraX, Compose, Material 3     |
| **Local Persistence**                    | Store entries locally with export/import support           | Room, Kotlinx Serialization, Flow|
| **Theme Engine**                         | Derive accent colors from each moment's palette            | Android Palette API              |
| **Explore Tab (Prototype)**              | Curated prompts like “a place that calmed you today”       | Local static JSON (Firebase later)|

---

## 🧠 Product Philosophy

> “If Pinterest is about what we want, and VSCO is about how we look, Lumen is about how we *felt.*”

Every grid square is a pixel of the soul’s timeline. Over time, the collection becomes a modular memory system that mirrors your internal landscape through images and gentle annotations.

---

## 🧱 Expansion Roadmap

### Phase 2 — Curation Layer
- Create Pinterest-like boards to group moods or palettes (e.g., *Autumn Energy*, *Solitude*).
- Support importing moments from the camera roll or Instagram.

### Phase 3 — AI Layer
- Auto-tag aesthetic tones ("warm", "melancholy", "luxury") using local or cloud ML models.
- Generate weekly visual journal summaries blending text and imagery.

### Phase 4 — Network Layer
- Optional federated sharing (ActivityPub or private share links).
- VSCO-inspired discover page showcasing anonymized aesthetic moods.

---

## 🎨 Design Language

- **Palette:** Neutral ivory base with muted gold and forest green accents.
- **Typography:** DM Sans or Inter Display for calm, modern legibility.
- **Shapes:** Rounded corners, soft edges, and layered cards.
- **Feedback:** Subtle haptics and micro-animations to keep interactions tangible.

Aesthetic goal: "Quiet luxury meets minimal Japanese UI."

---

## 🗂️ Repository Blueprint

```
lumen-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/lumen/
│   │   │   │   ├── ui/
│   │   │   │   ├── data/
│   │   │   │   ├── model/
│   │   │   │   └── viewmodel/
│   │   │   └── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── README.md
└── LICENSE
```

---

## 🛠️ Tech Stack Overview

- **Language:** Kotlin with Jetpack Compose (Material 3)
- **Architecture:** MVVM with Kotlin Coroutines & Flow
- **Persistence:** Room database with JSON export/import
- **Media:** CameraX for capture, Android Palette API for color extraction
- **Theming:** Dynamic accent hues generated from uploaded images
- **Offline-first:** All data stored locally, sync optional in later phases

---

## 🚀 Getting Started (Concept Draft)

1. Scaffold the Android project using Android Studio Flamingo+.
2. Set up Jetpack Compose with Material 3 and `LazyVerticalGrid` for the feed.
3. Implement the `Moment` data model (image URI, tone, caption, timestamp).
4. Configure Room entities/DAO with Flow-powered repositories.
5. Build `MainActivity` hosting a `NavHost` for **Feed**, **Add Moment**, and **Explore**.
6. Wire CameraX + Storage permissions to capture and persist new moments.
7. Apply Palette-based theming to generate accent colors per entry.
8. Add export/import utilities using Kotlinx Serialization for JSON backups.

---

## 🤝 Contributing & Next Steps

This README serves as a creative and technical north star for building Lumen. If you’re iterating on the concept:

- Open issues for design explorations or feature experiments.
- Share Figma frames, Compose prototypes, or palette studies.
- Explore additional themes (Neon Noir, Minimalist Forest Green) for future variants.

Let the mosaic glow. ✨
