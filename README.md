# 🏘️ Article — Neighbourhood Community App

**Article** is an Android application designed to bring neighbourhood communities together on a single, structured platform.  
The app focuses on **community interaction**, **local announcements**, and **service coordination**, while maintaining a strong emphasis on **UI consistency, stability, and scalability**.

> 🚦 Current Phase: **UI-first development (backend intentionally deferred)**

---

## ✨ Core Features

### 📰 Community Feed
- View neighbourhood **posts and announcements**
- Announcements are visually highlighted
- Like and comment entry points on posts
- Clean, card-based layout with clear hierarchy

### 📝 Create Content
- Create **Posts** (image + caption support planned)
- Create **Announcements** (text-only)
- Clear separation between social posts and official notices

### 🛠️ Service Requests
- Dedicated screen for all service requests
- Create new service requests with:
  - Service provider selector
  - Preferred service date (calendar picker)
- Cancel requests anytime
- Newly created requests appear immediately

### 💬 Inbox (UI Ready)
- Separate sections planned for:
  - Member-to-Member chats
  - Member-to-Service Provider chats
- Structure prepared for role-based messaging

### 👤 Profile
- Edit profile name
- Edit bio
- Profile image support
- Section reserved for user’s own uploaded posts
- Logout support

---

## 🎨 Design Philosophy

- Calm **blue-based gradient theme**
- Soft glow on selected actions
- Premium yet minimal UI
- No clutter, no noisy animations
- Accessibility-friendly contrast

> ❝ The app should never crash — visuals come after stability ❞

---

## 🧭 Navigation Structure


Each screen follows **single responsibility** — no mixed concerns.

---

## 🏗️ Tech Stack

### Frontend
- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- Navigation Compose

### Backend (Planned)
- Firebase Authentication
- Firebase Firestore
- Firebase Storage

> Backend integration is postponed to ensure UI stability first.

---

## 🚦 Project Status

| Module | Status |
|------|------|
| Core UI Screens | ✅ Implemented |
| Navigation | ✅ Stable |
| Crash Safety | ✅ Enforced |
| Theme Consistency | 🔄 Improving |
| Firebase Integration | ⏳ Planned |
| Role-based Logic | ⏳ Planned |

---

## 🛡️ Stability Guarantees

- No duplicate LazyColumn keys
- No unsafe navigation routes
- No backend dependency during UI phase
- Experimental APIs used only with explicit opt-in
- Screen-by-screen incremental updates

---

## 🔄 Development Workflow

1. Stabilize one screen at a time
2. Commit after every stable milestone
3. Avoid large sweeping refactors
4. Maintain rollback-ready Git history
5. Integrate backend only after UI freeze

---

## 📌 Planned Enhancements

- Persistent comments system
- Real-time chat
- Push notifications
- Role-based access (Member / Service Provider / Admin)
- Multi-neighbourhood support
- Content moderation tools

---

## 👤 Author

**Adi**  
Android Developer  
Focused on clean architecture, crash-free UI, and scalable systems.

---

## 📄 License

This project is developed for **educational and academic purposes**.

