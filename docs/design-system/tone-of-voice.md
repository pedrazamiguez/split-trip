# Tone of Voice & Verbal Identity Guide

This document establishes the verbal identity, tone of voice principles, and copywriting standards for **SplitTrip**. It defines how the app communicates across all user touchpoints—including UI copy, form validation errors, push notifications, wizards, empty states, and system dialogs.

---

## 1. Brand & Audience Persona

### Brand Persona: *The Expert Travel Companion*
SplitTrip is the reliable, worldly travel companion who has everything under control. Calm under pressure, transparent with the numbers, and always focused on the trip rather than the bureaucracy of accounting.

- **Warm & Encouraging:** Welcomes travellers without sounding forced, childish, or overly enthusiastic.
- **Competent & Direct:** Demystifies complex multi-currency splits and FIFO cash tranches with concise, clear prose.
- **Unobtrusive:** Steps back so travellers can focus on their adventure.

### Audience Persona: *Modern Group Travellers*
Our users are young to mid-age friends, couples, families, and colleagues travelling together across borders. They value speed, transparency, and fairness. They appreciate clean design and clear explanations without slang or corporate jargon.

---

## 2. Core Tone Pillars

```
┌───────────────────────────┬───────────────────────────┐
│ 1. Approachable & Warm    │ 2. Clear & Travel-Oriented│
│ Friendly, respectful,     │ Grounded in trip context, │
│ never patronizing.        │ not corporate banking.    │
├───────────────────────────┼───────────────────────────┤
│ 3. Accurate & Empowering  │ 4. Purpose-Driven & Direct│
│ Technically exact math,   │ Concise microcopy, clear  │
│ plain-language clarity.   │ calls to action.          │
└───────────────────────────┴───────────────────────────┘
```

### Pillar 1: Approachable & Warm (Not Patronizing)
- **Do:** Use conversational phrasing and natural warmth (e.g., *"¡Listo! \"%1$s\" está preparado para el viaje"*, *"Compañeros de viaje"*).
- **Don't:** Use forced juvenile slang or overly casual language that patronizes adults (e.g., avoid *"Tus compis de aventura"*, *"ready para la aventura"*, *"la pasta"*).

### Pillar 2: Clear & Travel-Oriented (Not Corporate Accounting)
- **Do:** Frame interactions around the journey and shared experiences (e.g., *"Resumen del viaje"*, *"Registro de gastos"*).
- **Don't:** Use dry, corporate financial jargon (e.g., avoid *"Posición neta de balance general"*, *"Balance del grupo"* where *"Resumen del viaje"* or *"Estado de cuentas"* is clearer).

### Pillar 3: Accurate & Empowering (Not Overly Simplified)
- **Do:** Accurately reflect multi-currency, multi-subunit, and offline capabilities (e.g., *"Registro de gastos"*, *"Conversión de moneda"*).
- **Don't:** Oversimplify the app to a generic restaurant bill splitter (e.g., avoid *"¿Quién pagó qué?"* or *"¿En qué te gastaste la pasta?"*).

### Pillar 4: Purpose-Driven & Direct (Not Generic Clichés)
- **Do:** Use explicit, functional verbs and labels (e.g., *"Tus preferencias"*, *"Aportar al pocket"*, *"Retirar efectivo"*).
- **Don't:** Rely on vague marketing slogans (e.g., avoid *"Hazla tuya"*, *"Top up"*).

---

## 3. Ubiquitous Language & Terminology Mapping

All copy must align strictly with [`docs/domain/ubiquitous-language.md`](../domain/ubiquitous-language.md).

| Concept | English Preferred | Spanish Preferred | Forbidden / Deprecated |
| :--- | :--- | :--- | :--- |
| **Group Pool** | Group pocket / Virtual pocket | Pocket del grupo / Pocket virtual | *Bote*, *Bolsa*, *Fondo común*, *Cuenta* |
| **Debt Resolution** | Settlement | Acuerdo de pago | *Liquidación* (implies clearance sale), *Finiquito* |
| **Funding Inflow** | Contribution | Aportación | *Ingreso*, *Top-up*, *Recarga* |
| **Cash Outflow** | Cash withdrawal | Retirada de efectivo | *Retiro* (Latin American / retirement), *Saca* |
| **Expense Division** | Split / Entity split | Reparto / Divisiones | *Partición*, *Prorrateo* |
| **Extra Charges** | Add-on / Extra | Extra | *Recargo genérico*, *Plus* |
| **Group Companions** | Travel companions / Members | Compañeros de viaje / Miembros | *Compis*, *Amigotes*, *Peña* |
| **Settings Subtitle** | Your preferences | Tus preferencias | *Hazla tuya*, *A tu gusto* |

---

## 4. Grammar, Formality, and Localization Rules

### Spanish (Castilian — `values-es/`)
- **Grammatical Person:** Use informal second-person singular (*tú* / *tuteo*), but with adult elegance.
- **Punctuation:** Always include opening exclamation and question marks (`¡`, `¿`).
- **Currency & Numbers:** Format numbers using European spacing and comma decimals (e.g., `1.250,50 €`). Handled by `FormattingHelper`.
- **Gender Inclusivity:** Use neutral group terms (*"Compañeros de viaje"*, *"Miembros del grupo"*, *"Viajeros"*).

### English (`values/`)
- **Formality:** Modern, clean, conversational British English (en-GB) as the single baseline standard across default resource catalogs (`res/values/strings.xml`).
- **Spelling & Vocabulary:**
  - `-ise` / `-isation` / `-ising` / `-ised` suffixes instead of `-ize` / `-ization` / `-izing` / `-ized` (*organise*, *customise*, *synchronise*, *synchronisation*, *prioritise*, *recognise*, *monetise*, *authorise*, *normalise*, *finalise*, *neutralise*, *itemise*, *analyse*).
  - Double 'l' consonants (*traveller*, *travelling*, *cancelled*, *cancelling*, *labelled*).
  - `-our` endings (*colour*, *favourite*, *behaviour*).
  - `-re` endings (*centre*, *theatre*).
  - Noun / verb distinctions (*licence* [noun] vs. *license* [verb], *practise* [verb] vs. *practice* [noun]).
- **Sentence Case:** Use Sentence case for subtitles, helper text, and descriptions. Title Case is reserved for TopBar titles and formal modal headers.
- **Contractions:** Natural contractions (*"Couldn't"*, *"You'll"*, *"Don't"*) are encouraged in body and error copy to maintain warmth.

### Andalusian EPA (`values-es-rAN/`)
- Translations in `values-es-rAN/` are derived automatically via `./gradlew generateAndaluzStrings`.
- Base Spanish strings must adhere to standard spelling and phonetic conventions so the EPA transliteration engine can process them accurately without syntax errors.

---

## 5. Contextual Do's & Don'ts Matrix

| UI Context | Principle | ✅ Do | ❌ Don't |
| :--- | :--- | :--- | :--- |
| **Tab Subtitles** | Grounded in context | *"Travel companions"*, *"Expense log"*, *"Resumen del viaje"* | *"Your travel squad"*, *"Who paid for what?"*, *"Balance del grupo"* |
| **Action Buttons** | Explicit verb + target | *"Aportar al pocket"*, *"Retirar efectivo"* | *"Añadir dinero"*, *"Sacar pasta"* |
| **Form Validation** | Helpful, friendly guidance | *"¡Ey! Dale un nombre a tu grupo"*, *"¡Ey! ¿Para qué fue este gasto?"* | *"Error: nombre requerido"*, *"¿En qué te gastaste la pasta?"* |
| **Empty States** | Forward-looking invitation | *"Añade una aportación al pocket del grupo para empezar"* | *"Añade dinero a la cuenta del grupo para empezar"* |
| **Confirmation Dialogs**| Clear impact statement | *"¿Seguro que quieres eliminar \"%1$s\"? Se borrarán todos los gastos."* | *"¿Borrar esto para siempre?"* |
| **Success Feedback** | Crisp celebration | *"¡Listo! \"%1$s\" está preparado para el viaje"* | *"¡Listo! \"%1$s\" está ready para la aventura"* |

---

## 6. Review Checklist for New Features

When writing or reviewing UI strings for SplitTrip:
1. **Pillars Check:** Is the copy warm, travel-oriented, technically accurate, and direct?
2. **Ubiquitous Language:** Does it use *Pocket*, *Acuerdo de pago*, *Aportación*, *Retirada de efectivo*?
3. **No Slang:** Are forced colloquialisms (*compis*, *pasta*, *ready*) eliminated?
4. **Bilingual Parity:** Does every string in `values/strings.xml` have an equivalent in `values-es/strings.xml`?
5. **EPA Synchronization:** Did you run `./gradlew generateAndaluzStrings` after updating Spanish strings?
