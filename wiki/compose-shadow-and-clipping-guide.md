# Jetpack Compose Shadow & Clipping Guide

This guide documents the root causes, architectural patterns, and coding practices to implement **rounded, ambient shadows** in Jetpack Compose without introducing rectangular shadow artifacts during animations, layout size transitions, or tab switches.

---

## 1. The Root Cause of "Squared Shadows"

In Jetpack Compose, shadows are cast by the native Android `RenderNode` based on an `Outline` shape. 

Under the hood, `Modifier.shadow(...)` is a convenience wrapper around `Modifier.graphicsLayer` that sets:
- `shadowElevation`
- `shape` (to generate the outline path)
- `clip = elevation > 0.dp`

When `elevation > 0.dp`, Compose sets `clip = true` on the graphics layer, forcing the native `RenderNode` to clip all its contents to the outline.

This approach fails and falls back to a rectangular shadow under two primary conditions:

### A. Bounded Clipping during Size Animations
When a component's layout size changes dynamically (e.g., via `animateContentSize()` on the bottom navigation pill, or size changes in shared element transitions):
1. The layout boundaries of the node are continuously updated.
2. If `clip = true` is set on the shadow-casting node, the native Android `RenderNode` clips everything to the current outline.
3. During layout interpolation, the outline clipping path can fail to update synchronously or exceed hardware capabilities, causing the system to fallback to a **rectangular bounding box clip**.
4. This chops off the diffused shadow bleed (the soft ambient glow) at the outer layout boundaries, leaving a hard, sharp-cornered rectangular shadow.

### B. Percentage-Based Shapes under Size Fluctuations
If you use percentage-based shapes like `CircleShape` (which resolves dynamically to a `RoundedCornerShape` with a corner size of `50%` of the measured width and height):
1. During animations where the width or height of the component starts at `0` or undergoes rapid interpolation, the percentage-based corner radius fluctuates.
2. If the width/height ratio changes (e.g., 80x64), a `CircleShape` resolves to an ellipse, while a pill/stadium shape requires static semi-circular corners.
3. If the outline path fails to resolve to a symmetric rounded rectangle during any frame of a transition, Android falls back to rendering a rectangular shadow.

---

## 2. The Golden Rules of Compose Shadows

To guarantee perfectly rounded shadows that never render rectangular borders during transitions, adhere to the following architectural patterns:

### Rule 1: Separate Shadow Casting and Content Clipping (Nested Container Pattern)
Never apply both the shadow and the content clipping/background to the same container if the component is subject to size changes or transitions. Instead, split them into nested containers:

* **Outer Container (Shadow Caster):**
  - Sets the layout dimensions (`height`, `width`, or `weight`).
  - Casts the shadow using `graphicsLayer` with **`clip = false`**.
  - *Why:* Setting `clip = false` prevents the outer container from clipping the shadow bleed or content. The native `RenderNode` casts the rounded shadow using the specified `shape`, but never falls back to a rectangular clip outline because clipping is disabled.
* **Inner Container (Content & Background):**
  - Fills the parent container.
  - Applies **`clip(shape)`** to crop the background, ripples, and inner composables.
  - Draws the `.background(...)` using the same shape.

```mermaid
graph TD
    A["Outer Box: graphicsLayer { clip = false }"] -->|Casts Rounded Shadow| B["Shadow Path"]
    A --> C["Inner Box: clipShape + background"]
    C -->|Clips Ripples & Background| D["NavigationBar / Content"]
```

### Rule 2: Order Modifiers from Left to Right (Shadows Outer, Animations Inner)
In Compose, modifier order is applied from outer to inner. If you need a size animation (like `animateContentSize()`), place it *after* the shadow-casting modifier:

```kotlin
// ✅ CORRECT: Shadow wraps the size animation
Box(
    modifier = Modifier
        .weight(1f)
        .height(NavBarDefaults.BarHeight)
        .graphicsLayer {
            shadowElevation = barElevation.toPx()
            shape = pillShape
            clip = false // Never clip the shadow layer
        }
        .animateContentSize() // Inner size animation
)
```

* **Why:** The `.graphicsLayer` is drawn first. When `.animateContentSize()` animates the layout bounds, the size change propagates upwards to the parent. The outer layer draws the shadow at the animated size, but because it has `clip = false`, the outer boundary never clips the shadow bleed.

### Rule 3: Use Static (DP-Based) Shapes for Non-Square Elements
Avoid using `CircleShape` on non-square components (like ovals, capsules, or pill shapes) to draw shadows. Instead, calculate a static corner radius based on the component's fixed dimension (typically `height / 2`):

```kotlin
// ✅ CORRECT: Corner radius is static and matches half the height
val pillShape = RoundedCornerShape(NavBarDefaults.BarHeight / 2)
```

* **Why:** Using `RoundedCornerShape(32.dp)` on a container with height `64.dp` guarantees a stadium shape with semi-circular ends. Because the corner radius is a fixed DP value rather than a percentage, the outline path is stable and fully calculated on the first layout pass, avoiding rectangular fallback render pathways on the Android GPU.

### Rule 4: Disable Alpha Animations on Elevated Items in Lazy Lists
When placing elevated components (e.g. `SelectedGroupCard`) inside a scrollable lazy list (`LazyColumn` or `LazyRow`) that uses `animateItem()`, always disable the default alpha fade transitions:

```kotlin
modifier = Modifier
    .animateItem(fadeInSpec = null, fadeOutSpec = null) // Disable alpha fade
```

* **Why:** The default fade-in/out animation in `animateItem()` creates a rectangular offscreen hardware compositing buffer. Even if your shadow container uses `clip = false`, the shadow bleed is silently clipped by this offscreen buffer's rectangular edge, resulting in a hard square border during placement animations.

---

## 3. Code Reference & Comparison

### A. Bottom Navigation Bar Pill (Capsule with Size Animation)

#### ❌ Anti-Pattern (Causes Squared Shadow)
```kotlin
Box(
    modifier = Modifier
        .weight(1f)
        .animateContentSize()
        .shadow(elevation = barElevation, shape = CircleShape) // Changed dynamically, clips to bounds
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(color)
    )
}
```

#### ✅ Correct Pattern (Stable, Unclipped Shadow)
```kotlin
val pillShape = RoundedCornerShape(NavBarDefaults.BarHeight / 2)

Box(
    modifier = Modifier
        .weight(1f)
        .height(NavBarDefaults.BarHeight)
        .graphicsLayer {
            shadowElevation = barElevation.toPx()
            shape = pillShape
            clip = false // Let shadow bleed outside bounds
        }
        .animateContentSize() // Animates size inside unclipped shadow layer
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(NavBarDefaults.BarHeight)
            .clip(pillShape) // Clips content to pill shape
            .background(color)
    )
}
```

---

## 4. Troubleshooting Checklist

If a component is displaying a squared shadow, verify the following:

1. **Is `graphicsLayer` used instead of `Modifier.shadow`?**
   If using `Modifier.shadow`, it will apply `clip = true` implicitly when elevation is active. Switch to `graphicsLayer` and set `clip = false`.
2. **Are there nested containers?**
   Make sure the shadow is applied to an outer container, and the background/clipping/click handlers are applied to the inner child container.
3. **Is the shape static?**
   If the container is non-square, replace `CircleShape` or percentage-based shapes with `RoundedCornerShape(height / 2)` or similar DP-based dimensions.
4. **Is the modifier order correct?**
   Ensure `.graphicsLayer { ... }` is declared *before* `.animateContentSize()` or other layout/clipping modifiers.
5. **Is the shadow suppressed in Dark Mode?**
   Per Horizon Narrative §4.4, shadows should be invisible in dark mode. Suppress the elevation to `0.dp` automatically:
   ```kotlin
   val isDarkMode = isSystemInDarkTheme()
   val elevation = if (isDarkMode) 0.dp else baseElevation
   ```
6. **Does the shadow container participate in `sharedBounds`?**
   If the element uses a shared element transition, the shadow **must** be on a separate outer wrapper that does NOT have `Modifier.sharedBounds`. See Section 5.

---

## 5. Shared Element Transitions with Floating Actions

This section documents the solution for shadow artifacts that appear specifically during `sharedBounds` container-transform transitions (e.g. a FAB in the bottom nav morphing into a full-screen form and back).

### Background

Shared element transitions in Compose work by lifting the participating element into a **GPU overlay layer** for the duration of the morph. While in that overlay:

- All rendering (shadows, backgrounds, content) is clipped to the **current rectangular morphing bounds** of the container.
- This clipping happens at the **hardware/GPU layer level** — it is completely independent of any `shape` you declared in `graphicsLayer`.
- The result: any shadow on a `sharedBounds` participant will appear as a growing/shrinking **rectangle** during the transition, regardless of how you configure `clip`, `clipInOverlayDuringTransition`, or modifier order inside the shared element.

### The Definitive Fix: Isolate the Shadow Outside the Shared Element

The shadow **must** live on an outer wrapper Box that is **never** passed to `Modifier.sharedBounds`. Only the inner Box — which has no shadow of its own — participates in the transition.

```
OuterBox (graphicsLayer shadow — NOT in sharedBounds, stays in normal render tree)
  └── SharedBoundsBox (Modifier.sharedBounds — zero shadow, lifted into overlay)
        └── ContentBox (clip + background + click handlers)
```

#### ✅ Correct Implementation

```kotlin
val buttonShape = RoundedCornerShape(NavBarDefaults.BarHeight / 2)
val elevation = if (isSystemInDarkTheme()) 0.dp else NavBarDefaults.ShadowElevation
val sharedModifier = fabSharedTransitionModifier(key)

// Outer Box: owns the shadow. Never participates in sharedBounds.
Box(
    modifier = modifier
        .width(80.dp)
        .height(64.dp)
        .graphicsLayer {
            shadowElevation = elevation.toPx()
            shape = buttonShape
            clip = false
        }
) {
    // Middle Box: participates in sharedBounds. Has NO shadow.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(sharedModifier) // Modifier.sharedBounds lives here
    ) {
        // Inner Box: content, clipping, background, interaction.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(buttonShape)
                .background(color)
                .clickable(onClick = onClick)
        ) {
            Icon(...)
        }
    }
}
```

#### Why It Works

The outer `ShadowBox` is rendered in the Scaffold's normal layer. When `sharedBounds` lifts the inner element into the overlay, the outer box stays fixed in the normal tree. Its `graphicsLayer { shape = buttonShape }` shadow is always rendered correctly rounded. Since the content (the coloured inner box) was opaque and covered the shadow, and the full-screen destination immediately fills the screen, the shadow's momentary visibility during the transition is imperceptible.

### Anti-Patterns That Were Tried and Failed

| Approach | Why it fails |
|---|---|
| `Modifier.shadow(...)` on sharedBounds node | `Modifier.shadow` sets `clip = true` implicitly; shadow is clipped rectangularly in the GPU overlay |
| `graphicsLayer { shadow }` after `.then(sharedModifier)` | Same root cause — the shadow is still on the shared element's overlay layer |
| `graphicsLayer { shadow }` on inner child Box | The entire subtree including inner children is lifted into the overlay; same rectangular clip applies |
| `clipInOverlayDuringTransition = null` | Disables Compose-level overlay clipping but the GPU hardware layer still clips at rectangular bounds |
| `animatedVisibilityScope.transition.isRunning` to zero shadow | Suppresses the artifact but introduces a visible shadow pop-in animation when the transition ends |

### Companion Setup Requirements

For this pattern to work end-to-end, two additional things must be in place:

#### A. Single Root `SharedTransitionLayout` Wrapping the Entire Scaffold

The FAB (inside the `bottomBar` slot) and the destination screen (inside the `NavHost`) must share the same `SharedTransitionLayout` scope. Wrap the entire `Scaffold` at the `MainScreen` level:

```kotlin
SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
        Scaffold(
            bottomBar = { BottomNavigationBar(...) },   // FAB lives here
            content = { MainTabsContent(...) }           // NavHost lives here
        )
    }
}
```

#### B. Provide `LocalAnimatedVisibilityScope` from the `AnimatedVisibility` Block

The FAB's `AnimatedVisibility` scope must be explicitly provided so `fabSharedTransitionModifier` can read it:

```kotlin
AnimatedVisibility(visible = mainAction != null, ...) {
    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
        if (mainAction != null) {
            MainActionButton(mainAction = mainAction)
        }
    }
}
```

