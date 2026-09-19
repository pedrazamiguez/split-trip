# Ubiquitous Language

This article defines the ubiquitous language for the SplitTrip domain model, providing a single source of truth for terminology. It ensures consistency across the codebase, database schema, and user-facing translations.

## Core Entities & Domain Models

| English | Spanish | Definition |
| :--- | :--- | :--- |
| **Group** | Grupo | A collection of users sharing a trip or event where expenses are tracked together. |
| **User** | Usuario | A person interacting with the application. Can be registered or a guest. |
| **Subunit** | Subunidad | A distinct subset of group members (e.g., a couple or family) that can share expenses collectively. |
| **Expense** | Gasto | A financial cost incurred by one or more members on behalf of others in the group. |
| **Contribution** | Aportación | Funds added to a group's collective virtual pool to cover future shared expenses. |
| **Cash Withdrawal** | Retirada de efectivo | Physical cash withdrawn from an ATM or bank account to be used for group expenses. |
| **Your Position** | Tu posición / Tu balance | The personal financial standing view for the current user, summarizing net position, cash in hand, and settlement consensus actions. (Replaces "My Position" / "Mi posición"). |
| **Settlement / Settlement Consensus** | Acuerdo de pago / Consenso de pagos | A collaborative agreement and mutual consensus process to resolve a debt between two group members (propose, confirm receipt, or dispute). Replaces unilateral "Liquidación". |
| **Settlement Record** | Registro de pago | A historical record of a completed or disputed settlement. |
| **Balance** | Balance | The net financial position of a member or the group (how much is owed or owed to them). |
| **Member Balance** | Balance de miembro | The specific breakdown of a single member's financial position, including cash in hand and total spent. |
| **Group Pocket / Virtual Pocket** | Pocket del grupo / Pocket virtual | The collective pool of funds (virtual and physical cash) held by the group to cover shared expenses. |
| **AddOn** | Extra | An additional cost attached to an expense, such as a tip, fee, surcharge, or discount. |
| **Cash Tranche** | Tramo de efectivo | A specific portion of a cash withdrawal that is consumed to fund an expense. |
| **Entity Split** | Reparto | The calculation of how an expense is divided among entities (individual users or subunits). |
| **Exchange Rate** | Tipo de cambio | The conversion rate between a base currency and a target currency. |
| **Receipt Attachment** | Recibo adjunto | A scanned document or photo (e.g., PDF, image) attached to an expense as proof of purchase. |

## Enumerations (Enums)

| English | Spanish | Definition |
| :--- | :--- | :--- |
| **AddOnMode** | Modo de extra | Indicates if an add-on is already included in the base cost (`Included`) or applied on top of it (`OnTop`). |
| **AddOnType** | Tipo de extra | The specific type of the extra charge (e.g., `Tip`, `Fee`, `Surcharge`, `Discount`). |
| **AddOnValueType** | Tipo de valor | Whether the add-on is defined as a fixed amount (`Exact`) or a percentage (`Percentage`). |
| **AppLanguage** | Idioma de la app | Supported application languages for the user interface. |
| **AppTheme** | Tema de la app | Visual theme preference (`Light`, `Dark`, or `System`). |
| **AuthProviderType** | Proveedor de acceso | The authentication provider used by a user (e.g., `Google`, `Email/Password`, `Guest`). |
| **CashWithdrawalReason** | Motivo de retirada | The context or reason for a cash withdrawal. |
| **Currency** | Moneda | Supported currencies used in the application. |
| **ExpenseCategory** | Categoría de gasto | Categories for classifying expenses (e.g., `Food`, `Transport`, `Accommodation`). |
| **GroupRole** | Rol de grupo | The permission level of a user within a group (e.g., `Admin`, `Member`). |
| **GroupStatus** | Estado del grupo | The lifecycle state of a group (e.g., `Active`, `Archived`). |
| **NotificationCategory** | Categoría de aviso | Broad categories for push notifications (e.g., `Membership`, `Expenses`, `Financial`). |
| **NotificationType** | Tipo de aviso | The specific event triggering a notification (e.g., `ExpenseAdded`, `SettlementRequested`). |
| **PayerType** | Tipo de pagador | Indicates whether a payment/withdrawal was made by an individual or collectively from a group pool. |
| **PaymentMethod** | Método de pago | The medium used to pay for an expense (e.g., `Cash`, `Credit Card`, `Bizum`). |
| **PaymentStatus** | Estado de pago | The lifecycle of an expense's payment (e.g., `Pending`, `Paid`, `Cancelled`). |
| **SplitType** | Tipo de reparto | How an expense is divided among participants (e.g., `Equal`, `ExactAmount`, `Percentage`). |
| **SyncStatus** | Estado de sincronización | The state of local data synchronization with the backend (e.g., `Pending`, `Synced`, `Failed`). |

## Translation Guidelines
- **Always use "Acuerdo de pago" or "Consenso de pagos"**.
  - **Never use "Liquidación"**. In traditional financial tools, "liquidación" implies an aggressive unilateral clearance, debt foreclosure, or clearance sale. In SplitTrip, debt resolution is a collaborative consensus agreement between peers (proposing a payment, payer marking it as sent, payee confirming receipt, or disputing).
- **Always use "Your Position" / "Tu posición" (or "Tu balance")**.
  - **Never use "My Position" / "Mi posición"**. The application addresses the traveler in a natural, respectful second person ("Tu posición", "Tus preferencias"), avoiding first-person self-labeling.
- **Always use "Balance"**. Never use "Saldo" or "Posición neta" to keep the terminology approachable and less aggressively financial.
- **Always use "Pocket" / "Pocket del grupo" / "Pocket virtual"**. Never use "Bote", "Bolsa", or "Cuenta" to ensure a consistent, branded, and modern concept for the group's collective pool.
- Maintain consistency across all UI text, push notifications, error messages, and API responses.
