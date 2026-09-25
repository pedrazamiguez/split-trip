# Google Play Console — SDD Execution Runbook

> **Target Package:** `es.pedrazamiguez.splittrip`  
> **Source of Truth:** `docs/release/store-listing-metadata.md` & GitHub Issue [#1685](https://github.com/pedrazamiguez/split-trip/issues/1685)  
> **Standard:** British English (`en-GB`) per [#1691](https://github.com/pedrazamiguez/split-trip/issues/1691) & Castilian Spanish (`es-ES`)  

This document is the **Spec-Driven Development (SDD) Runbook** for human developer execution in the Google Play Console web dashboard. Every form, dropdown, radio button, and checkbox is specified deterministically below.

---

## Overview: Codebase Deliverables vs. Console Actions

```
┌────────────────────────────────────────────────────────────────────────┐
│                   REPOSITORY ASSETS (Ready to Upload)                  │
│                                                                        │
│  - Text Copy: docs/release/assets/{en-GB, en-US, es-ES}/               │
│  - App Icon: docs/release/assets/icon.png (512x512 PNG)                │
│  - Feature Graphic: docs/release/assets/feature-graphic.png (1024x500) │
│  - Screenshots: docs/release/assets/screenshots/phone/                 │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Developer upload & entry)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     GOOGLE PLAY CONSOLE OPERATIONS                     │
│               (Manual Execution via Web UI per this Runbook)           │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Store Settings & Contact Details

**Navigation:** *Presencia en Google Play Store > Configuración de la tienda* (Store presence > Store settings)

| Field / Setting | Value to Enter / Select | Notes |
| :--- | :--- | :--- |
| **Tipo de aplicación (App / Game)** | **Aplicación** (App) | Financial & travel ledger |
| **Categoría principal (Category)** | **Finanzas** (Finance) | Required |
| **Etiquetas (Tags)** | `Viajes y guías locales` (Travel & Local), `Finanzas personales` (Personal Finance) | Max 5 tags |
| **Dirección de correo electrónico** | `support@splittrip.eu` | Public developer contact |
| **Número de teléfono** | *(Leave blank / optional)* | Not required |
| **Sitio web (Website)** | `https://splittrip.eu` | Production domain |

*Action:* Click **Guardar** (Save).

---

## Phase 2: App Content Declarations (Contenido de la aplicación)

**Navigation:** *Directivas y programas > Contenido de la aplicación* (Policy and programs > App content)

### 1. Política de privacidad (Privacy Policy)
- **URL de la política de privacidad:** `https://splittrip.eu/privacy-policy.html`
- *Action:* Click **Guardar** (Save).

### 2. Anuncios (Ads)
- **¿Tu aplicación contiene anuncios?**
  - Select: **Sí, contiene anuncios** (Yes, contains ads)
  - *Reasoning:* SplitTrip Free tier integrates Google Mobile Ads (AdMob banner & interstitial). Pro tier is ad-free.
- *Action:* Click **Guardar**.

### 3. Acceso a aplicaciones (App Access)
- **¿Alguna función de tu aplicación está restringida?**
  - Select: **Todas las funciones están disponibles sin restricciones** (All functionality is available without special access)
  - *Reasoning:* The app supports email login, Google sign-in, and guest browsing. No proprietary hardware or restricted geofencing is required for testing.
- *Action:* Click **Guardar**.

### 4. Público objetivo y contenido (Target Audience)
- **Grupos de edad a los que se dirige tu aplicación:**
  - Check: **18 años o más** (18 and older)
- **Presencia en la tienda de aplicaciones para niños:**
  - Select: **No** (The app is not designed specifically for children)
- *Action:* Click **Guardar**.

### 5. Aplicaciones de noticias (News Apps)
- **¿Tu aplicación es una aplicación de noticias?**
  - Select: **No**
- *Action:* Click **Guardar**.

### 6. Rastreo de contactos y estado de COVID-19
- Select: **Mi aplicación no es una aplicación de rastreo ni de confirmación del estado de contactos por COVID-19**
- *Action:* Click **Guardar**.

### 7. Seguridad de los datos (Data Safety Questionnaire)

Follow this exact mapping:

#### General Questions:
1. **¿Tu aplicación recoge o comparte alguno de los tipos de datos de usuario requeridos?** -> **Sí** (Yes)
2. **¿Todos los datos de usuario recogidos por tu aplicación están cifrados en tránsito?** -> **Sí** (Yes — TLS 1.3 enforced across all endpoints)
3. **¿Ofreces un método para que los usuarios soliciten la eliminación de sus datos?** -> **Sí** (Yes)
   - In-app self-service at `Settings > Account Status`
   - URL for request: `https://splittrip.eu/privacy-policy.html#deletion`

#### Specific Data Types to Declare:

1. **Información personal (Personal info):**
   - **Nombre (Name):**
     - Recogido: **Sí** | Compartido: **No** | Epímero: **No** | Requerido
     - Finalidad: **Funciones de la aplicación**, **Gestión de cuentas**
   - **Dirección de correo electrónico (Email address):**
     - Recogido: **Sí** | Compartido: **No** | Epímero: **No** | Requerido
     - Finalidad: **Funciones de la aplicación**, **Gestión de cuentas**
   - **IDs de usuario (User IDs):**
     - Recogido: **Sí** | Compartido: **No** | Epímero: **No** | Requerido
     - Finalidad: **Funciones de la aplicación** (Firebase Auth UID)

2. **Información financiera (Financial info):**
   - **Información de pago del usuario (tarjetas de crédito, cuentas bancarias):** **No se recoge** (No)
   - **Otra información financiera:**
     - Recogido: **Sí** | Compartido: **No** | Epímero: **No** | Requerido
     - Finalidad: **Funciones de la aplicación** (Gastos compartidos, saldos, acuerdos de pago entre amigos)

3. **Fotos y vídeos (Photos and videos):**
   - **Fotos (Photos):**
     - Recogido: **Sí** | Compartido: **No** | Epímero: **No** | **Opcional**
     - Finalidad: **Funciones de la aplicación** (Subida de fotos de tiques/recibos y avatares de grupo)

4. **Información y rendimiento de aplicaciones (App info and performance):**
   - **Registros de fallos (Crash logs):**
     - Recogido: **Sí** | Compartido: **No** | Requerido
     - Finalidad: **Análisis y diagnóstico** (Firebase Crashlytics, retención máxima 90 días)
   - **Diagnósticos (Diagnostics):**
     - Recogido: **Sí** | Compartido: **No** | Requerido
     - Finalidad: **Rendimiento** (Firebase Performance Monitoring)

5. **Dispositivo u otros identificadores (Device or other IDs):**
   - **ID de publicidad (Advertising ID / AAID):**
     - Recogido: **Sí** | Compartido: **Sí** (con Google AdMob) | Epímero: **No** | **Opcional**
     - Finalidad: **Publicidad o marketing** (Google AdMob con consentimiento UMP)

6. **Ubicación (Location):**
   - **Ubicación aproximada:**
     - Recogido: **Sí** | Compartido: **Sí** | **Opcional**
     - Finalidad: **Publicidad o marketing** (Derivada por IP por Google AdMob)

*Action:* Click **Guardar**.

### 8. ID de publicidad (Advertising ID)
- **¿Tu aplicación usa el ID de publicidad?** -> Select **Sí** (Yes)
- **¿Para qué se usa el ID de publicidad?** -> Check **Publicidad o marketing** (Advertising or marketing)
- *Action:* Click **Guardar**.

### 9. Funciones financieras (Financial Features)
- **¿Tu aplicación ofrece funciones financieras?** -> Select **Gestión de finanzas personales (registro de gastos y presupuestos)**
- *Confirmation:* Confirm SplitTrip is an expense ledger, NOT a licensed bank, lender, cryptocurrency exchange, or payment processor.
- *Action:* Click **Guardar**.

### 10. Clasificación del contenido (Content Ratings Questionnaire)
1. **Email:** `support@splittrip.eu`
2. **Categoría:** **Utilidad, productividad, comunicación u otro** (Utility, Productivity, Communication, or Other)
3. **Respuestas:**
   - ¿Violencia? -> **No**
   - ¿Sexualidad o desnudos? -> **No**
   - ¿Lenguaje ofensivo? -> **No**
   - ¿Sustancias controladas (drogas, tabaco, alcohol)? -> **No**
   - ¿Venta de artículos con restricción de edad? -> **No**
   - ¿Permite a los usuarios interactuar o intercambiar contenido? -> **Sí** (permite registrar y compartir gastos comunes en viajes)
   - ¿Comparte ubicación física exacta? -> **No**
   - ¿Permite comprar bienes digitales? -> **No** (las suscripciones Pro se gestionarán a través de Google Play Billing)
4. Submit questionnaire -> Rating awarded: **PEGI 3 / ESRB Everyone**.
5. *Action:* Click **Guardar y aplicar**.

---

## Phase 3: Ficha de Play Store predeterminada (Default Store Listing - English)

**Navigation:** *Presencia en Google Play Store > Ficha de Play Store principal* (Store presence > Main store listing)

### Text Resources
Copy directly from repository files:

- **Nombre de la aplicación (App Name) [max 30 chars]:**  
  File: `docs/release/assets/en-GB/title.txt`
  ```text
  SplitTrip: Group Expenses
  ```

- **Descripción breve (Short Description) [max 80 chars]:**  
  File: `docs/release/assets/en-GB/short_description.txt`
  ```text
  Split travel expenses offline & multi-currency. Fair debts, zero stress.
  ```

- **Descripción completa (Full Description) [max 4,000 chars]:**  
  File: `docs/release/assets/en-GB/full_description.txt`  
  *(Open file and paste entire content)*

### Graphic Assets
Upload directly from repository:

1. **Icono de la aplicación (App Icon - 512x512):**  
   Upload `docs/release/assets/icon.png`
2. **Gráfico de funciones (Feature Graphic - 1024x500):**  
   Upload `docs/release/assets/feature-graphic.png`
3. **Vídeo:**  
   Leave blank (optional field).
4. **Capturas de pantalla del teléfono (Phone Screenshots):**  
   Upload the 6 PNG captures from `docs/release/assets/screenshots/phone/`:
   - `01_trips_hub.png`
   - `02_expense_log.png`
   - `03_fair_splits.png`
   - `04_group_pocket.png`
   - `05_settlement_consensus.png`
   - `06_receipt_scanner.png`
5. **Capturas de pantalla de tablets (7" y 10"):**  
   Upload tablet captures from `docs/release/assets/screenshots/tablet-7/` and `tablet-10/`.

*Action:* Click **Guardar**.

---

## Phase 4: Traducción de la ficha de Play Store (Spanish - `es-ES`)

In the same Main store listing screen:
1. Click **Gestionar traducciones > Añadir tu propia traducción** (Manage translations > Add your own translation).
2. Select language: **Español (España) (es-ES)**.
3. Enter values from repository files:

- **Nombre de la aplicación:**  
  File: `docs/release/assets/es-ES/title.txt`
  ```text
  SplitTrip: Gastos de viaje
  ```

- **Descripción breve:**  
  File: `docs/release/assets/es-ES/short_description.txt`
  ```text
  Divide gastos de viaje sin conexión y multidivisa. Cuentas claras sin estrés.
  ```

- **Descripción completa:**  
  File: `docs/release/assets/es-ES/full_description.txt`  
  *(Open file and paste entire content)*

*Action:* Click **Guardar**.

---

## Phase 5: Verification Checklist Before Release Submission

- [ ] All mandatory compliance declarations in *Contenido de la aplicación* marked with green checkmarks.
- [ ] English (`en-GB`/`en-US`) store listing title, short description, and full description saved.
- [ ] Spanish (`es-ES`) store listing title, short description, and full description saved.
- [ ] App icon (512x512) and feature graphic (1024x500) uploaded and validated.
- [ ] Phone screenshots (minimum 4, recommended 6) uploaded.
- [ ] Privacy policy link `https://splittrip.eu/privacy-policy.html` active and accessible.
- [ ] Data deletion request URL verified in Data Safety form.
- [ ] Content rating approved (PEGI 3 / ESRB Everyone).
