# Lumen — Visual Mood Journal

> *Capture your mood. Curate your light.*

Lumen (formerly "Moments") is an offline-first visual journaling concept for Android.
It is built with Kotlin and Jetpack Compose.
The experience blends the visual minimalism of VSCO and the curatorial feel of Pinterest.
It also keeps the intimacy of a private mood diary.
The goal is to preserve how your day *felt*, not just how it looked.

---

## 🌄 Core Concept

Each entry in Lumen is a **visual vignette** captured as a personal mood moment:

- **Image** — Choose a photo from the gallery or capture directly with CameraX.
- **Tone** — Assign an emotion, color, or theme tag that reflects your feeling.
- **Caption** *(optional)* — Add a note that anchors the memory.
- **Timestamp** — Automatically saved so your mosaic forms a timeline.

Your home screen becomes a scrolling mosaic of memories, rendered as a minimalist VSCO-style grid.
There are no likes, follows, or algorithmic noise—just aesthetic memory curated for yourself.

---

## 🎯 Minimal V1 Feature Set

- **Feed Grid** — Offline-first mosaic of saved moments with pinch-to-zoom using Compose
  `LazyVerticalGrid`.
- **Add Moment Screen** — Capture or import imagery,
  assign a tone, and add a caption with Material 3 components.
- **Local Persistence** — Store entries locally with export and import support using Room and Flow.
- **Theme Engine** — Derive accent colors from each moment's palette via the Android Palette API.
- **Explore Tab (Prototype)** — Curated prompts such as “a place that calmed you today” sourced from
  local data.

---

## 🧠 Product Philosophy

> “If Pinterest is about what we want, and VSCO is about how we look, Lumen is about how we *felt.*”

Every grid square is a pixel of the soul’s timeline.
Over time, the collection becomes a modular memory system.
It mirrors your internal landscape through images and gentle annotations.

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
- **Persistence:** Room database with JSON export and import
- **Media:** CameraX for capture, Android Palette API for color extraction
- **Theming:** Dynamic accent hues generated from uploaded images
- **Offline-first:** All data stored locally, sync optional in later phases

---

## 🚀 Getting Started (Concept Draft)

1. Scaffold the Android project using Android Studio Flamingo or newer.
2. Set up Jetpack Compose with Material 3 and `LazyVerticalGrid` for the feed.
3. Implement the `Moment` data model (image URI, tone, caption, timestamp).
4. Configure Room entities and DAO with Flow-powered repositories.
5. Build `MainActivity` with a `NavHost` for **Feed**, **Add Moment**, and **Explore** destinations.
   This provides a single-activity shell for the prototype experience.
6. Wire CameraX plus storage permissions to capture and persist new moments.
7. Apply Palette-based theming to generate accent colors per entry.
8. Add export and import utilities using Kotlinx Serialization for JSON backups.

---

## 🧪 MVP Prototype

An initial Jetpack Compose prototype lives under `app/src/main/java/com/lumen/`. It ships with:

- A `LumenApp` navigation shell linking **Feed**, **Add**, and **Explore** screens.
- Sample `MomentRepository` data powering a mood mosaic grid using `LazyVerticalGrid`.
- Lightweight view models to simulate saving moments and rotating curated prompts.

This mockup is UI-focused and keeps state in memory so the experience can be previewed quickly in
Android Studio’s emulator or Compose previews.
Replacing the repository with a Room-backed data source is the next step toward feature-complete
functionality.

---

## 🤝 Contributing & Next Steps

This README serves as a creative and technical north star for building Lumen.
If you’re iterating on the concept:

- Open issues for design explorations or feature experiments.
- Share Figma frames, Compose prototypes, or palette studies.
- Explore additional themes (Neon Noir, Minimalist Forest Green) for future variants.

Let the mosaic glow. ✨
