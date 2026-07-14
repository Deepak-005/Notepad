# Offline Notepad - User Manual & Feature Guide

Welcome to **Offline Notepad**! Offline Notepad is a beautiful, secure, distraction-free note-taking application designed to keep your thoughts, research, and personal logs 100% private, local, and protected.

This manual explains how to use all the main features, including folders, tags, color-coding, security locks, PDF/TXT exports, backups, and our newly added **Insights & Statistics** screen.

---

## 🚀 Key Features Overview

*   **100% Offline & Private**: Zero cloud sync or server connections. All data resides strictly in your device's secure sandbox.
*   **Insights & Statistics Screen**: Real-time metrics showing note counts, character/word summaries, category bar charts, tag clouds, and color distribution.
*   **Multi-Profile Workspace**: Create up to 4 isolated local profiles, each with custom emoji avatars, themes, and fully distinct note databases.
*   **Rich Markdown Editing & Preview**: Write notes in standard markdown and preview formatted lists, headers, code, and bold texts in real-time.
*   **PIN Security Lock**: Restrict access using a 4-digit PIN. Supports session tracking with automatic auto-lock if backgrounded for more than 1 minute.
*   **AES Password-Encrypted Backups**: Export notes to a JSON file. Optionally secure backups using industry-standard AES encryption with a custom password.
*   **Native PDF & TXT Exports**: Print or share any note immediately into a clean PDF booklet or plain text file.

---

## 📂 1. Multi-Profile Workspace

Offline Notepad lets you isolate your personal, work, and secret thoughts. 

### How to use:
1. On the main header, you will see circular icons representing your **Workspace Profiles** (or a selector on the Login screen).
2. Tap on any profile to switch database contexts.
3. **Long-press** any profile icon to customize its:
    *   **Profile Name** (e.g. "Work", "Journal")
    *   **Emoji Avatar** (e.g. 💼, 📓, 🔑)
    *   **Theme Color Accent**
4. If a profile is secure, you will be prompted to verify your identity.

---

## 📝 2. Writing & Organizing Notes

### Creating a Note
*   Tap the floating action button (FAB) `+` on the Notes tab.
*   Enter a **Title** and write your content.
*   You can toggle between **Write** and **Preview** modes using the view eye icon to read beautifully rendered markdown (headers, lists, blockquotes, code blocks).

### Advanced Organizing Features:
*   **Color-Coding**: Tap the color palette icon inside the note editor to choose an aesthetic color theme (Red, Orange, Yellow, Green, Teal, Blue, Purple, Pink). This helps group visual topics on your main grid dashboard.
*   **Categories**: Tap the category folder icon to assign the note to a folder (e.g., General, Work, Personal, Ideas, Study, or create a brand-new custom category).
*   **Tagging**: Write tag keywords separated by commas in the tags field (e.g., `invoice, expenses, 2026`). These tags form a global tag library for instant search and filtering.
*   **Pinning & Favoriting**: 
    *   **Pinning**: Keeps critical notes floating at the absolute top of your dashboard list.
    *   **Favoriting**: Groups your beloved notes together.

### Archive and Trash
*   **Archive**: Swipe or move notes to the Archive folder to hide them from your active daily dashboard.
*   **Trash**: Deleting a note moves it to the Trash. Notes remain safely in the Trash bin until you manually select **Empty Trash** or permanently erase them. You can restore them anytime.

---

## 📊 3. Insights & Statistics (New!)

To help you track your writing progress, cataloging habits, and note-taking patterns, we have introduced a dedicated **Insights & Statistics** center.

### How to access:
*   Tap the **Insights** icon (represented by a Bar Chart symbol 📊) in the bottom navigation bar.

### Metrics available in Insights:
1.  **Dashboard Counters**:
    *   **Total Notes**: Total active notes in your workspace database.
    *   **Pinned Notes**: Count of active notes flagged for quick access.
    *   **Archived Notes**: Notes preserved in the Archive.
    *   **Trashed Notes**: Deleted notes currently held in the Trash bin.
2.  **Word & Character Metrics**:
    *   **Total Words**: Total words written across all active notes.
    *   **Total Characters**: Cumulative on-device character count.
    *   **Avg. Words / Note**: Average word depth per note.
3.  **Category Distribution Chart**:
    *   A beautiful, proportional horizontal bar chart visualization detailing which folders contain the most notes, accompanied by percentage breakdowns.
4.  **Tag Cloud**:
    *   A responsive tag layout highlighting your top 10 most used tags and their relative frequency.
5.  **Color Themes Distribution**:
    *   An aesthetic row of colored indicators displaying how many notes have been styled under each colored background theme.

---

## 🔒 4. PIN Security & Autolock

Secure your notes from prying eyes!

### Setup Security PIN:
1. Navigate to the **Settings** tab.
2. Scroll to the **Security & Privacy** section.
3. Toggle the **App PIN Lock** switch.
4. Input a strong **4-digit PIN** and confirm it.

### PIN Lock Behavior:
*   Once enabled, launching the app or opening secure profiles prompts a numerical PIN screen.
*   **Autolock**: If you switch to another application or return to your device's home screen, a secure timer initiates. If the app is inactive for more than **1 minute**, it locks automatically.

---

## 🗄️ 5. Backup, Restore & Shared Exports

Because Offline Notepad stores all data locally, **you are the sole owner of your files**. We strongly recommend making periodic backups.

### How to Backup:
1. Open the **Settings** tab.
2. Under **Data Management**, select your backup mode:
    *   **Backup (JSON)**: Exports a standard JSON text backup to your device's Downloads directory.
    *   **Backup (Encrypted JSON)**: Prompts you for a password, derives a secure key, and exports an AES-encrypted JSON file. Highly recommended if your notes contain highly sensitive data or credentials.

### How to Restore:
1. Under **Data Management**, select **Restore (JSON)** or **Restore (Encrypted JSON)** depending on your backup type.
2. Use the system file picker to select your backup file.
3. If encrypted, enter the matching backup password.
4. Your notes will be parsed and safely merged back into your active Room database context.

### Individual Note Document Export:
While editing or reading any note:
*   **Export as TXT**: Generates a standard readable plain text document in your device Downloads.
*   **Export as PDF**: Renders your note title, headers, category details, and body text into an elegant A4 standard PDF booklet ready for local printing or sharing.
