<div align="center">
  <img src="app/src/main/res/drawable/ic_hisabi_brand.png" alt="Hisabi Logo" width="88" height="88" />
  <h1>Hisabi</h1>
  <p>
    <strong>Pure OLED Black Personal Expense Tracker, Multi-Wallet Ledger &amp; Loans Vault for Android</strong>
  </p>
  <p>
    Built with Kotlin, Jetpack Compose, Room, and Hilt — 100% offline with zero telemetry, zero cloud lock-in, and strict PIN/biometric vault protection.
  </p>

  <p>
    <a href="https://github.com/0xrobiul/Hisabi">
      <img src="https://img.shields.io/badge/GitHub-0xrobiul%2FHisabi-D6B46A?style=for-the-badge&logo=github&logoColor=000000" alt="GitHub Repository" />
    </a>
    <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%E2%80%9336)-24B47E?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0+" />
    <img src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
    <img src="https://img.shields.io/badge/Room%20DB-v4%20Offline-0D1017?style=for-the-badge&logo=sqlite&logoColor=D6B46A" alt="Room Database v4" />
  </p>

  <p>
    <a href="#-interface-preview">Interface Preview</a> •
    <a href="#-core-features">Core Features</a> •
    <a href="#-architecture--flowcharts">Flowcharts</a> •
    <a href="#-oled-design-tokens">Design Tokens</a> •
    <a href="#-tech-stack">Tech Stack</a> •
    <a href="#-getting-started">Getting Started</a>
  </p>
</div>

<p align="center">
  <img src="docs/assets/hero-banner.svg" alt="Hisabi Hero Banner" width="100%" />
</p>

---

## 📱 Interface Preview

<p align="center">
  <img src="docs/assets/screenshots-showcase.svg" alt="Hisabi UI Preview — Home Wallet, Account Transfer, Loans & Borrowings, and Vault Lock" width="100%" />
</p>

<p align="center">
  <sub>
    <strong>1. Home:</strong> Swipeable <code>BankCardVisual</code>, Portfolio &amp; Loans Summary &nbsp;|&nbsp;
    <strong>2. Add Transfer:</strong> Frequent Charges (<code>1.85%</code>, <code>1.20%</code>, <code>1.50%</code>) &amp; Live Breakdown &nbsp;|&nbsp;
    <strong>3. Loans:</strong> 6 Summary Tiles &amp; Account Sync &nbsp;|&nbsp;
    <strong>4. Vault Lock:</strong> Pure Black Keypad + Biometric
  </sub>
</p>

---

## ✨ Core Features

<table>
  <tr>
    <td width="56" align="center" valign="top">
      <img src="docs/assets/icons/wallet.svg" width="38" height="38" alt="Multi-Wallet Cards Icon" />
    </td>
    <td valign="top">
      <strong>Multi-Wallet Virtual Cards</strong><br/>
      Manage <strong>Cash</strong> (default), <strong>Bank</strong>, <strong>Credit Cards</strong>, and Bangladeshi MFS wallets (<strong>bKash</strong>, <strong>Nagad</strong>, <strong>Rocket</strong>, <strong>Upay</strong>, or custom accounts) as swipeable matte-black virtual cards with customizable gradients, card pinning, credit limit tracking, and instant balance updates.
    </td>
  </tr>
  <tr>
    <td width="56" align="center" valign="top">
      <img src="docs/assets/icons/transfer.svg" width="38" height="38" alt="Transfers & Charges Icon" />
    </td>
    <td valign="top">
      <strong>Expenses, Income &amp; Account Transfers with Charge Presets</strong><br/>
      Log expenses, income, fees, and account-to-account transfers with tags, notes, and local receipt attachments. Transfers include a dedicated <strong>Transfer Charge</strong> panel with one-tap frequent fee presets (<code>1.85%</code>, <code>1.20%</code>, <code>1.50%</code>, plus flat fees), a live source/destination breakdown, and separate fee expense accounting.
    </td>
  </tr>
  <tr>
    <td width="56" align="center" valign="top">
      <img src="docs/assets/icons/loans.svg" width="38" height="38" alt="Loans & Borrowings Icon" />
    </td>
    <td valign="top">
      <strong>Loans &amp; Borrowings with Source/Destination Account Sync</strong><br/>
      Track money lent (<em>Receivables</em>) and borrowed (<em>Payables</em>) with partial/full repayments, payment methods, automatic <code>Active / Partial / Repaid / Overdue</code> statuses, due-date reminders, and optional source/destination account linking (automatically updating and reverting card balances on create, edit, or delete without distorting expense/income reports).
    </td>
  </tr>
  <tr>
    <td width="56" align="center" valign="top">
      <img src="docs/assets/icons/analytics.svg" width="38" height="38" alt="Analytics, Budgets & Goals Icon" />
    </td>
    <td valign="top">
      <strong>Insights, Budgets, Savings Goals &amp; Recurring Rules</strong><br/>
      Explore daily, weekly, monthly, and yearly Vico cashflow charts and category donuts; configure threshold-alerted spending budgets; fund or withdraw account-linked savings goals; and automate recurring transactions via <code>WorkManager</code>.
    </td>
  </tr>
  <tr>
    <td width="56" align="center" valign="top">
      <img src="docs/assets/icons/shield.svg" width="38" height="38" alt="Strict Vault Lock Icon" />
    </td>
    <td valign="top">
      <strong>Strict Cold-Start &amp; Background Vault Lock</strong><br/>
      Protected with a 4-digit SHA-256 hashed PIN, brute-force keypad cooldown, keypad-embedded biometric trigger (no bypass/skip actions), <code>FLAG_SECURE</code> recent-tasks snapshot masking, a cold-start loading gate that prevents balance leaks before unlock, and instant relock on app exit (<code>onUserLeaveHint</code> / <code>onStop</code>).
    </td>
  </tr>
  <tr>
    <td width="56" align="center" valign="top">
      <img src="docs/assets/icons/offline.svg" width="38" height="38" alt="100% Offline & Exports Icon" />
    </td>
    <td valign="top">
      <strong>100% Offline Storage &amp; Portable Exports</strong><br/>
      Uses integer minor-unit (paisa) precision inside Room SQLite with zero network permissions required. Export formatted <strong>PDF statements</strong>, <strong>CSV spreadsheets</strong>, or complete <strong>JSON v4</strong> offline backups (including accounts, transactions, budgets, goals, recurring rules, and loan/repayment links).
    </td>
  </tr>
</table>

---

## 🔀 Architecture & Flowcharts

### 1. App Launch & Strict Vault Security Flow

```mermaid
flowchart TD
    A([App Launch / Resume]) --> B{Vault Lock Enabled?}
    B -- No --> F[Show Main Ledger UI]
    B -- Yes --> C{Settings Loaded?}
    C -- Loading --> D[Pure Black Splash / Shield Gate<br/>Balances Hidden + FLAG_SECURE]
    C -- Ready --> E[Lock Screen<br/>4-Digit PIN + Keypad Biometric Icon]
    E -->|Invalid PIN| G{Attempts >= 5?}
    G -- Yes --> H[15s Cooldown Lockout]
    H --> E
    G -- No --> E
    E -->|Valid PIN or Biometric Success| F
    F -->|Home / App Switch / Screen Off| I[onUserLeaveHint / onStop<br/>Immediate Session Relock]
    I --> E
```

### 2. Accounting, Transfer Charge & Loan Isolation Flow

```mermaid
flowchart LR
    subgraph Wallets["Wallet & Card Balances (Room DB)"]
        ACC[(Accounts<br/>Cash / Bank / MFS / Card)]
    end

    EXP[Expense / Income] -->|"- / + Amount"| ACC
    EXP -->|Included in| RPT[Cashflow & Category Insights]

    TRF["Transfer (From → To)"] -->|"- (Principal + Charge)"| ACC
    TRF -->|"+ Principal to Target"| ACC
    CHG["Frequent Charge<br/>1.85% • 1.20% • 1.50%"] -->|Recorded as Transfer Fee| RPT

    LEND["Loan Given / Borrowed<br/>(Optional Account Link)"] -->|"Lent: -Amount / Borrowed: +Amount"| ACC
    REPAY["Loan Repayment<br/>(Partial or Full)"] -->|"Received: +Amount / Paid: -Amount"| ACC
    LEND -.->|Strictly Isolated| LOAN_LEDGER[Loans & Borrowings Ledger<br/>Receivables & Payables]
    REPAY -.->|Strictly Isolated| LOAN_LEDGER
```

### 3. Clean MVVM & Offline Data Architecture

```mermaid
flowchart TB
    UI["Jetpack Compose UI (Material 3)<br/>Home • Activity • Add/Edit • Loans • Insights • Budgets • Goals • Settings"]
    VM["LedgerViewModel<br/>StateFlow & Reactive Derived Metrics"]
    REPO["LedgerRepository<br/>Atomic Transactions • Validation • JSON/CSV/PDF Export"]
    ROOM[("Room Database v4<br/>8 Entities • Integer Paisa Math")]
    PREF[("DataStore Preferences<br/>Theme • Currency • Security")]
    WORK["WorkManager + Notifications<br/>Recurring Rules & Due Reminders"]

    UI --> VM
    VM --> REPO
    REPO --> ROOM
    REPO --> PREF
    WORK --> REPO
```

---

## 🎨 OLED Design Tokens

Hisabi defaults to a **True Pure Black (`#000000`)** OLED palette paired with **Champagne Gold** luxury accents (with optional Light and System theme support):

| Token | Hex | Preview | Usage |
| :--- | :--- | :---: | :--- |
| **Pure OLED Black** | `#000000` | ![#000000](https://img.shields.io/badge/%20-%23000000?style=flat-square&color=000000) | Primary app &amp; lock screen background |
| **Card Surface** | `#0A0A0A` | ![#0A0A0A](https://img.shields.io/badge/%20-%230A0A0A?style=flat-square&color=0A0A0A) | Summary cards, tiles &amp; keypad buttons |
| **Elevated Surface** | `#121212` | ![#121212](https://img.shields.io/badge/%20-%23121212?style=flat-square&color=121212) | Breakdown containers, sheets &amp; chips |
| **Hairline Border** | `#242424` | ![#242424](https://img.shields.io/badge/%20-%23242424?style=flat-square&color=242424) | Card outlines &amp; subtle dividers |
| **Champagne Gold** | `#D6B46A` | ![#D6B46A](https://img.shields.io/badge/%20-%23D6B46A?style=flat-square&color=D6B46A) | Primary CTA, active borders &amp; accents |
| **Emerald Positive** | `#24B47E` | ![#24B47E](https://img.shields.io/badge/%20-%2324B47E?style=flat-square&color=24B47E) | Income, inflows &amp; receivables |
| **Crimson Negative** | `#E45D68` | ![#E45D68](https://img.shields.io/badge/%20-%23E45D68?style=flat-square&color=E45D68) | Expenses, outflows, overdue &amp; payables |
| **Transfer Blue** | `#7CA7E6` | ![#7CA7E6](https://img.shields.io/badge/%20-%237CA7E6?style=flat-square&color=7CA7E6) | Transfer switcher &amp; active loan badges |
| **Primary Text** | `#F5F5F5` | ![#F5F5F5](https://img.shields.io/badge/%20-%23F5F5F5?style=flat-square&color=F5F5F5) | Headings, balances &amp; primary labels |
| **Secondary Text** | `#A1A7B2` | ![#A1A7B2](https://img.shields.io/badge/%20-%23A1A7B2?style=flat-square&color=A1A7B2) | Subtitles, metadata &amp; timestamps |

---

## 🛠 Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language & UI** | Kotlin `2.4.21`, Jetpack Compose (BOM `2024.09.00`), Material Design 3 |
| **Architecture** | MVVM + Single-Source-of-Truth Repository (`LedgerViewModel`, `LedgerRepository`) |
| **Dependency Injection** | Hilt `2.59.2` + Hilt Work (`1.2.0`) |
| **Local Database** | Room `2.7.0` (SQLite schema v4, KSP) with integer minor-unit (`Long` paisa) math |
| **Preferences** | Jetpack DataStore Preferences `1.1.7` |
| **Navigation** | Navigation Compose `2.8.9` |
| **Charts & Media** | Vico Compose M3 `2.1.2`, Coil `2.7.0` |
| **Background & Security** | WorkManager `2.10.0`, AndroidX Biometric `1.1.0`, SHA-256 PIN Hashing |

---

<details>
<summary><strong>📁 Project Structure</strong> (click to expand)</summary>

```text
Hisabi/
├── docs/assets/                  # GitHub README banner, UI showcase & SVG icons
├── app/src/main/java/com/example/
│   ├── MainActivity.kt           # Edge-to-edge host, Navigation Compose, strict vault relock & FLAG_SECURE
│   ├── CardVaultApplication.kt   # Hilt + WorkManager application setup
│   ├── data/
│   │   ├── local/                # Room database (v4), DAOs, entities, DataStore preferences
│   │   └── repository/           # Atomic ledger, transfers, loans, budgets, goals & backup repository
│   ├── di/                       # Hilt dependency modules
│   ├── ui/
│   │   ├── components/           # Virtual card visuals, transaction/loan sheets, Vico charts
│   │   ├── screens/              # Home, Activity, Add/Edit, Insights, Accounts, Loans, Budgets, Goals, Lock, Settings
│   │   ├── theme/                # Pure OLED black & light Material 3 ColorScheme, Typography & Shapes
│   │   └── viewmodel/            # LedgerViewModel & derived UI state
│   ├── util/                     # InputValidator, Formatters, PinSecurity, LoanCalculations, PdfExporter, Notifications
│   └── worker/                   # WorkManager recurring transaction worker
└── app/src/test/java/com/example/
    └── ExampleUnitTest.kt        # Unit tests for accounting math, loans, transfer charges & validators
```

</details>

<details>
<summary><strong>🗄️ Room Database Schema (Version 4)</strong> (click to expand)</summary>

| Entity | Purpose |
| :--- | :--- |
| `AccountEntity` | Cash, Bank, Credit Card, and MFS wallets with gradient themes, pin state, and minor-unit balances |
| `CategoryEntity` | Income & expense categories with icons and hex colors |
| `TransactionEntity` | Expenses, income, transfers (`toAccountId`), transfer charges, tags, notes, and receipt URI |
| `BudgetEntity` | Category spending limits across weekly, monthly, or custom windows |
| `SavingsGoalEntity` | Target amounts, saved progress, and linked wallet accounts |
| `RecurringRuleEntity` | Automated daily/weekly/monthly/yearly schedules executed by WorkManager |
| `LoanRecordEntity` | Money lent/borrowed records with optional `accountId`, due date, notes, and reminder flags |
| `LoanRepaymentEntity` | Partial/full repayment history linked to `loanId` (CASCADE) and optional `accountId` |

</details>

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Ladybug or newer) / Android Gradle Plugin `9.1.1`
- **JDK 17+**
- **Android SDK**: `minSdk 26` (Android 8.0) • `compileSdk 36` • `targetSdk 36`

### Clone & Build

```bash
git clone https://github.com/0xrobiul/Hisabi.git
cd Hisabi
```

Run the local unit test suite (covers input validation, transfer percentage math, and loan/account sync logic):

```bash
./gradlew :app:testDebugUnitTest --tests "me.robiul.hisabi.ExampleUnitTest"
```

Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

> **Note:** Hisabi declares only `POST_NOTIFICATIONS` (optional, for budget alerts and loan due-date reminders) and requests **no internet permission**, guaranteeing your financial records never leave your device.

---

## 👤 Developer

<div align="center">
  <strong>Robiul Awal Fagun</strong><br/>
  <a href="https://0xrobiul.me">0xrobiul.me</a> &nbsp;•&nbsp;
  <a href="https://github.com/0xrobiul/Hisabi">github.com/0xrobiul/Hisabi</a>
</div>
