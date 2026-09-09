# Google Play Store Listing Metadata & Compliance Guide

> [!IMPORTANT]
> **Source of Truth:** This document is the single source of truth for Google Play Console store listing copy, categorization, Data Safety declarations, and graphic asset specifications for **SplitTrip**.

---

## 1. Application Identity & Categorization

| Field | Production Value | Guidelines / Notes |
| :--- | :--- | :--- |
| **Package Name** | `es.pedrazamiguez.splittrip` | Defined in `:app/build.gradle.kts` |
| **Primary Category** | `Finance` | Financial management, group expense splitting |
| **Secondary Category** | `Travel & Local` | Vacation budget planning, multi-currency trips |
| **Default Language** | English (`en-US`) | Global baseline locale |
| **Additional Languages** | Spanish (`es-ES`) | European Castilian Spanish (Tone of Voice compliant) |
| **Content Rating** | PEGI 3 / ESRB Everyone | No offensive content, violence, gambling, or ads |
| **Target Audience** | 18+ (General Audience) | Group travelers, roommates, couples, families |
| **Privacy Policy URL** | `https://pedrazamiguez.github.io/split-trip/privacy-policy.html` | Public web-accessible URL |
| **Developer Contact** | `support@splittrip.com` | Official support inbox |
| **Website** | `https://github.com/pedrazamiguez/split-trip` | Public repository & issue tracker |

---

## 2. English Store Listing (`en-US`)

### App Title (25 / 30 characters)
```text
SplitTrip: Group Expenses
```

### Short Description (68 / 80 characters)
```text
Split travel expenses offline & multi-currency. Fair debts, zero stress.
```

### Full Description (2,336 / 4,000 characters)
```text
Traveling with friends, family, or your partner should be about making memories—not doing accounting gymnastics in messy spreadsheets. SplitTrip is the modern, offline-first group expense manager crafted specifically for travelers who cross borders, share expenses, and value fairness.

Whether you're backpacking through Southeast Asia, organizing a European road trip, or sharing an apartment with roommates, SplitTrip handles complex currencies, cash pockets, and debt settlements effortlessly.

⚡ TRUE OFFLINE-FIRST ARCHITECTURE
Log expenses anywhere—on a mountain hike, on a flight, or in remote areas with zero cell reception. SplitTrip writes all transactions instantly to your local device database. When internet connection is restored, background synchronization reconciles changes across your entire group automatically.

🌍 MULTI-CURRENCY & REAL-TIME EXCHANGE RATES
Spend in Japanese Yen, pay back in Euros, or split in US Dollars. SplitTrip supports 150+ global currencies with live exchange rates and captures immutable rate snapshots at the moment of each transaction so past calculations never shift unexpectedly.

🤝 SETTLEMENT CONSENSUS ("YOUR POSITION")
Eliminate end-of-trip awkwardness. SplitTrip simplifies tangled web-of-debt balances into the fewest possible transactions. Propose debt settlements directly within the app, review pending payments, and track mutual consensus state in real time.

💰 GROUP POCKETS & FIFO CASH TRANCHES
Keep shared virtual group funds separate from physical cash in hand. Track contributions into the group pocket and log cash withdrawals with First-In, First-Out (FIFO) currency tracking to ensure exact multi-currency precision down to the cent.

👥 SUBUNITS FOR COUPLES & FAMILIES
Traveling as a couple or family within a larger group? Create subunits to aggregate balances or split bills proportionally between units without tedious manual recalculations.

📸 SMART RECEIPT SCANNING [PRO]
Snap a photo of your dining or taxi receipt. SplitTrip extracts the total, tax, currency, and line items automatically, letting you assign specific dishes or add-on items to companions in seconds.

🔒 ZERO ADS & PRIVACY-FIRST
Your financial data belongs to you. SplitTrip contains zero third-party ads, zero tracking scripts, and never sells your transaction history to data brokers.
```

---

## 3. Spanish Store Listing (`es-ES`)

> [!NOTE]
> All Spanish copy follows SplitTrip's **Tone of Voice & Verbal Identity Guide** (`docs/design-system/tone-of-voice.md`) and **Ubiquitous Language** standards: adult elegance, conversational warmth, and precise financial travel terminology (*Pocket del grupo*, *Acuerdo de pago*, *Aportación*, *Retirada de efectivo*, *Reparto*).

### App Title (26 / 30 characters)
```text
SplitTrip: Gastos de viaje
```

### Short Description (77 / 80 characters)
```text
Divide gastos de viaje sin conexión y multidivisa. Cuentas claras sin estrés.
```

### Full Description (2,434 / 4,000 characters)
```text
Viajar con amigos, en pareja o en familia consiste en coleccionar experiencias inolvidables, no en pelear con hojas de cálculo ni acumular recibos arrugados. SplitTrip es el gestor inteligente de gastos compartidos diseñado específicamente para viajeros que cruzan fronteras, comparten aventuras y buscan cuentas transparentes.

Tanto si estás de ruta en furgoneta, de vacaciones en la playa o compartiendo piso, SplitTrip resuelve los cambios de moneda, los botes comunes y los acuerdos de pago con total claridad y elegancia.

⚡ FUNCIONAMIENTO REAL SIN CONEXIÓN (OFFLINE-FIRST)
Añade gastos en cualquier lugar: en un vuelo, en alta mar o en pueblos de montaña sin cobertura. SplitTrip registra todas las transacciones de forma instantánea en la base de datos local de tu móvil. En cuanto recuperes la conexión, la sincronización en segundo plano actualiza los datos del grupo de forma automática y transparente.

🌍 MULTIDIVISA Y TIPOS DE CAMBIO OFICIALES
Paga en yenes, aporta en dólares o liquida en euros. Compatible con más de 150 divisas internacionales con tipos de cambio actualizados. SplitTrip almacena una instantánea inmutable del cambio oficial en cada gasto para que las cuentas nunca varíen con el tiempo.

🤝 ACUERDOS DE PAGO Y CONSENSO ("TU POSICIÓN")
Olvídate de las tensiones al final del viaje. SplitTrip simplifica las deudas cruzadas para saldarlas con el menor número de transferencias posible. Propón acuerdos de pago, revisa transferencias pendientes y confirma recepciones con un sistema de consenso claro y transparente.

💰 POCKET DEL GRUPO Y RETIRADAS DE EFECTIVO
Gestiona el fondo virtual compartido separado del dinero físico. Registra aportaciones al pocket del grupo y retiradas de efectivo con trazabilidad FIFO de divisas, garantizando precisión bancaria hasta el último céntimo.

👥 SUBUNIDADES PARA PAREJAS Y FAMILIAS
¿Viajas en pareja o con hijos dentro de un grupo grande? Agrupa miembros en subunidades para repartir costes de forma conjunta o proporcional sin necesidad de hacer cálculos manuales.

📸 ESCANEO INTELIGENTE DE TIQUES [PRO]
Fotografía el tique de una cena o transporte. SplitTrip detecta automáticamente importes, impuestos y conceptos para que puedas asignar extras o consumiciones individuales en segundos.

🔒 CERO PUBLICIDAD Y MÁXIMA PRIVACIDAD
Tus datos financieros son únicamente tuyos. SplitTrip no contiene publicidad, no incluye rastreadores de terceros y jamás comercializa tus registros de viaje.
```

---

## 4. Google Play Data Safety Declarations

Every declaration below directly maps to Google Play Console's Data Safety questionnaire requirements, verified against our codebase architecture (Room local database, Firebase Auth, Cloud Firestore, Firebase Storage, Firebase Crashlytics):

| Data Category | Specific Data Type | Collected? | Shared? | Stored Ephemerally? | Required or Optional? | Purpose | Security & Deletion |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Personal Info** | Name | **Yes** | **No** | No | Required | App functionality, Account management | Encrypted in transit (TLS 1.3). Deletion supported in-app & via email. |
| **Personal Info** | Email address | **Yes** | **No** | No | Required | App functionality, Account management | Encrypted in transit (TLS 1.3). Deletion supported in-app & via email. |
| **Personal Info** | User IDs | **Yes** | **No** | No | Required | App functionality (Firebase Auth UID) | Encrypted in transit (TLS 1.3). Deletion supported in-app & via email. |
| **Financial Info** | Other financial info | **Yes** | **No** | No | Required | App functionality (Shared expense tracking, balances, settlements) | Encrypted in transit (TLS 1.3). Deletion supported in-app & via email. |
| **Photos & Videos** | Photos | **Yes** | **No** | No | Optional | App functionality (Receipt images, group avatars) | Encrypted in transit (TLS 1.3). Deletion supported in-app & via email. |
| **App Info & Performance** | Crash logs | **Yes** | **No** | No | Required | Analytics & Performance (Firebase Crashlytics) | Encrypted in transit (TLS 1.3). Automatically purged after 90 days. |
| **App Info & Performance** | Diagnostics | **Yes** | **No** | No | Required | Analytics & Performance (Performance Monitoring) | Encrypted in transit (TLS 1.3). Automatically purged after 90 days. |

### Data Safety Form Summary Answers
- **Does your app collect or share any of the required user data types?** -> **Yes**
- **Is all of the user data collected by your app encrypted in transit?** -> **Yes** (HTTPS / TLS 1.3 enforced)
- **Do you provide a way for users to request that their data be deleted?** -> **Yes** (Direct self-service account deletion in-app at `Settings > Account Status` and via `support@splittrip.com`)
- **Is personal data sold to any third party?** -> **No** (Zero monetization or third-party sharing)
- **Is personal data used for advertising or marketing?** -> **No** (No advertising networks present)

---

## 5. Graphic Assets & Screenshots Specification

### Core Store Assets

| Asset Name | Dimensions | Format | Max File Size | Specification Details |
| :--- | :--- | :--- | :--- | :--- |
| **App Icon** | 512 x 512 px | 32-bit PNG | 1,024 KB | Square icon, full bleed, no pre-rounded corners (Play Store applies corner masks automatically). Transparent or solid Horizon slate/teal background. |
| **Feature Graphic** | 1024 x 500 px | 24-bit PNG or JPG | 15 MB | Landscape presentation banner with no transparency. Visualizes the Horizon Narrative palette (deep slate `#0F172A`, vibrant cyan `#0EA5E9`), app logo, and tagline: *"Split expenses without borders"*. |

---

### Phone Screenshots Checklist (1080 x 2400 or 1080 x 1920)

A minimum of 4 and maximum of 8 screenshots are required. The recommended 6-screenshot sequence captures the core value proposition:

1. **Screen 1 — Trips Hub:**
   - Visual: Groups screen showing upcoming and completed journeys (e.g., *"Ruta por Islandia"*, *"Fin de semana en Roma"*), offline status chips, and group member avatars.
   - Header Caption: **Plan trips & track group costs effortlessly** / **Gestiona tus viajes y gastos en grupo**
2. **Screen 2 — Multi-Currency Expense Log:**
   - Visual: Expense listing displaying multiple currencies (e.g., EUR, JPY, USD), category badges (Food, Transport, Lodging), and live rate conversions.
   - Header Caption: **Log expenses in 150+ currencies with live rates** / **Registra gastos en más de 150 divisas al instante**
3. **Screen 3 — Fair Expense Splitting & Subunits:**
   - Visual: Add expense workflow showing equal, exact, or percentage splits, along with subunit breakdowns for couples.
   - Header Caption: **Split fairly by person, share, or subunit** / **Repartos justos por persona, porcentaje o subunidad**
4. **Screen 4 — Group Pocket & Cash Tranches:**
   - Visual: Balances dashboard highlighting the virtual group fund, member contributions, and cash withdrawals.
   - Header Caption: **Virtual group pockets & physical cash tracking** / **Pocket virtual del grupo y control de efectivo**
5. **Screen 5 — Settlement Consensus ("Your Position"):**
   - Visual: Debt resolution screen highlighting simplified debt pathways, pending settlements, and consensus status.
   - Header Caption: **Settle debts with minimal transactions** / **Acuerdos de pago claros y sin fricciones**
6. **Screen 6 — Smart AI Receipt Auto-Fill [Pro]:**
   - Visual: Camera scan overlay with receipt itemization and automated currency extraction.
   - Header Caption: **Instant receipt itemization with AI** / **Desglose inteligente de tiques con IA**

---

### Tablet Screenshots Specification
- **7-inch Tablet:** Minimum 1 screenshot (min 1200 x 1920 or 1920 x 1200), max 8 screenshots. Recommended: Trips Hub & Balances Dashboard in 2-pane master-detail layout.
- **10-inch Tablet:** Minimum 1 screenshot (min 1600 x 2560 or 2560 x 1600), max 8 screenshots. Recommended: Expense Log with side-by-side expense preview.
