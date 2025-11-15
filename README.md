# HandyHood — Smart Neighborhood Companion

> **Connecting residents, empowering communities, and enabling seamless local services through modern Android architecture**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Android](https://img.shields.io/badge/Android-8.0+-blue.svg)](https://www.android.com)
[![Supabase](https://img.shields.io/badge/Supabase-Backend-green.svg)](https://supabase.com)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

---

## What is HandyHood?

HandyHood is a **community-driven Android application** that bridges the gap between residents, local service providers, and neighborhood administrators. Built with **100% Jetpack Compose**, it delivers a modern, intuitive experience for managing community interactions, service requests, and local engagement.

**Problem Solved:** Traditional neighborhood management relies on fragmented communication channels. HandyHood consolidates everything into one beautiful, accessible platform.

---

## Key Features

### Resident Experience
- **Interactive Dashboard** — Real-time community feed with personalized content
- **Service Request Management** — Create, track, and manage service bookings
- **Smart Request Form** — Category selection, date picker, and rich descriptions
- **Dynamic Profile System** — Editable profiles with image upload and verification badges
- **Community Identity** — Neighborhood details, birthday tracking, and verification status

### Design & UX
- Material 3 Design System implementation
- Soft peach-to-white gradient theming
- Elevated cards with smooth animations
- Color-coded service categories
- Accessibility-first typography
- Intuitive bottom navigation

### Coming Soon
- Admin dashboard for community management
- Artisan/service provider portal
- Real-time notifications
- Advanced analytics
- Booking assignment system

---

## Technical Architecture

### **Frontend Stack**
```
Kotlin + Jetpack Compose
├── Material 3 Components
├── Compose Navigation
├── ViewModel Architecture
├── State Management (StateFlow)
├── Coroutines & Flow
└── Image Handling (Coil/Glide)
```

### **Backend Stack**
- **Supabase** — Authentication, PostgreSQL database, Storage, Realtime subscriptions
- **REST API** — Supabase auto-generated APIs
- **Row Level Security** — Secure data access policies
- **Storage Buckets** — Profile image and document storage

### **Architecture Pattern**
- **MVVM** (Model-View-ViewModel)
- Repository pattern for data abstraction
- Clean separation of concerns
- Unidirectional data flow

---

## Project Structure

```
HandyHood/
│
├── 📱 app/src/main/
│   ├── java/com/handyhood/
│   │   ├── data/
│   │   │   ├── repository/       # Data layer abstraction
│   │   │   ├── remote/           # Supabase client & API calls
│   │   │   └── models/           # Data classes
│   │   ├── auth/
│   │   │   └── SessionManager.kt # Auth state management
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   │   ├── DashboardScreen.kt
│   │   │   │   ├── RequestsScreen.kt
│   │   │   │   ├── AddRequestScreen.kt
│   │   │   │   └── ProfileScreen.kt
│   │   │   ├── components/       # Reusable UI components
│   │   │   └── theme/
│   │   │       ├── Color.kt
│   │   │       ├── Type.kt
│   │   │       └── Theme.kt
│   │   ├── navigation/
│   │   │   └── HandyHoodNavigation.kt
│   │   └── MainActivity.kt
│   └── res/                      # Resources
└── README.md
```

---

## Feature Showcase

### Dashboard
- Community post feed with card-based layout
- Quick action buttons for common tasks
- Service request summary widget
- Smooth scroll performance

### Request Management
- View all active service requests
- Status tracking with visual indicators
- Tap to view detailed request information
- Filter and search capabilities

### Add Request
- Category dropdown (Plumbing, Electrical, Cleaning, etc.)
- Material 3 date picker integration
- Multi-line description input
- Form validation

### Profile Screen
**Highlights:**
- Real-time profile editing
- Gallery-based profile picture upload
- Community verification badge display
- Neighborhood information
- Birthday management
- Save/cancel functionality

---

## Development Progress

| Module | Status | Description |
|--------|--------|-------------|
| UI/UX Design | ✅ Complete | All screens designed with Material 3 |
| Navigation | ✅ Complete | Bottom nav + deep linking ready |
| Dashboard | ✅ Complete | Functional feed and quick actions |
| Request System | ✅ Complete | Full CRUD operations (UI layer) |
| Profile Management | ✅ Enhanced | Editable with image picker |
| Supabase Auth | 🔧 In Progress | User authentication flow |
| Database Integration | 🔧 In Progress | PostgreSQL tables & queries |
| Storage Integration | 🔧 In Progress | Profile images & documents |
| Realtime Features | ⏳ Planned | Live updates & notifications |
| Admin Portal | ⏳ Planned | Management dashboard |
| Artisan Module | ⏳ Planned | Service provider interface |

---

## Why This Project Matters

### For Recruiters & Hiring Managers:

✅ **Modern Android Development** — Uses latest Compose patterns, not legacy XML  
✅ **Production-Ready Architecture** — MVVM, repository pattern, clean code  
✅ **Real-World Problem** — Addresses actual community management needs  
✅ **Scalable Design** — Built for growth from day one  
✅ **Best Practices** — Type safety, state management, proper navigation  
✅ **Industry-Standard Tools** — Kotlin, Compose, Supabase, Material 3  

### Technical Achievements:
- 100% Kotlin codebase with zero Java dependencies
- Fully declarative UI with Jetpack Compose
- Proper state hoisting and unidirectional data flow
- Material 3 theming system implementation
- Image picker integration with permission handling
- Form validation and user input management

---

## 🚦 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Minimum SDK: API 26 (Android 8.0)
- Target SDK: API 34 (Android 14)
- Kotlin 1.9+

### Installation

```bash
# Clone the repository
git clone https://github.com/yourusername/handyhood.git

# Open in Android Studio
# File → Open → Select HandyHood directory

# Sync Gradle
# Build → Make Project

# Run on emulator or device
# Run → Run 'app'
```

---

##  Learning & Growth Opportunities

This project demonstrates proficiency in:
- Jetpack Compose declarative UI
- State management patterns
- Navigation component architecture
- Material Design implementation
- Image handling in Android
- Form validation techniques
- MVVM architecture pattern
- Kotlin coroutines and Flow
- Repository pattern
- Clean code principles

---

## Future Roadmap

### Phase 1 (Current) — MVP Foundation
-  Core screens and navigation
-  Backend integration
-  Data persistence

### Phase 2 — Enhanced Features
- Push notifications
- Real-time chat
- Payment integration
- Rating system

### Phase 3 — Scaling
- Admin analytics dashboard
- Artisan booking management
- Multi-language support
- Dark mode

---

##  About the Developer

**Shweta Mishra**  
Android Developer | Kotlin Enthusiast | Community Tech Advocate

*Passionate about building apps that solve real-world problems and improve daily life for communities.*

 Contact for collaboration opportunities  
🔗 [LinkedIn]([#](https://www.linkedin.com/in/shweta-mishra-92810a313/)) | [GitHub](#https://github.com/shwetamishra21)

---

##  Contributing

Contributions are welcome! Areas for improvement:
- Enhanced artisan booking logic
- Advanced dashboard feed algorithms
- Cloud storage optimization
- Unit test coverage
- Accessibility improvements

**Fork** → **Create Branch** → **Commit** → **Push** → **Pull Request**

---

##  Acknowledgments

- Material 3 Design Guidelines
- Jetpack Compose Documentation
- Android Developer Community

---

<div align="center">

###  If you find this project interesting, please give it a star!

**HandyHood — Building connected communities, one neighborhood at a time.**

</div>
