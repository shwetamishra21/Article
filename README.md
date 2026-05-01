# 🏘️ Article — Neighbourhood Community App

**Article** is a comprehensive Android application that brings neighbourhood communities together on a single, structured platform. The app facilitates **community interaction**, **local announcements**, **service coordination**, and **real-time messaging** with a focus on **real-time updates**, **role-based access**, and **professional UI/UX**.

> 🎯 **Status**: Complete — Full-stack production-ready implementation with Firebase backend, real-time data synchronization, push notifications, and premium Material 3 UI.
---
## ✨ Core Features

### 👥 Multi-Role System
- **Members**: Create posts, comment, request services, manage their profile, and chat
- **Service Providers**: Accept requests, manage availability, track completed work, and chat with members
- **Admins**: All member capabilities + manage members, approve providers, create announcements, moderate content, and handle join requests

### 📰 Community Feed
- View neighbourhood posts and announcements
- Announcements visually highlighted with premium styling
- Like and comment on posts
- Real-time updates via Firebase listeners
- Clean, card-based layout with proper hierarchy
- Post creation with image support
  

### 🛠️ Service Request System
**For Members:**
- Create service requests with 25+ service types (Plumber, Electrician, Cleaner, etc.)
- Optional preferred date selection
- Real-time status tracking: Pending → Accepted → In Progress → Completed
- View assigned provider information
- Cancel pending or accepted requests
- Rate completed requests (1–5 stars)

**For Service Providers:**
- Real-time request notifications
- Accept/decline requests
- Status management: Start Work → Mark Complete
- Request filtering by status
- Premium card UI with gradient accents
- Provider search screen for discovery

### 💬 Messaging & Inbox
- Member-to-Member and Member-to-Provider real-time chat (`EnhancedChatScreen`)
- Inbox screen for both members and providers
- Chat threads linked to service requests
- Full participant-based access control enforced in Firestore

### 🔔 Push Notifications
- Firebase Cloud Messaging (FCM) integration via `ArticleFirebaseMessagingService`
- Notification permission handling (Android 13+)
- FCM token saved on login and refreshed automatically
- In-app notification screen (`NotificationScreen`) for all roles
- Notification types: `announcement`, `message`, `service_request`

### 👤 Profile Management
- Edit profile (name, bio, neighborhood)
- Profile image upload with Cloudinary CDN
- View other users' profiles (`ViewProfileScreen`)
- Role-specific profiles:
  - **Members**: Post history with grid/list view toggle
  - **Providers**: Service type selection, availability toggle, stats dashboard
- Logout with confirmation (all roles)

### 🔐 Admin Panel
- **Dashboard**: Overview statistics with live data
- **Member Management**: Add/remove members, view member list
- **Provider Approval**: Approve/reject service providers
- **Announcements**: Create pinned community announcements
- **Content Moderation**: Review and remove posts
- **Join Requests**: Approve or reject neighbourhood join requests

---

## 🎨 Design Philosophy

- **Premium Material 3 Design**: Gradient top bars, elevated cards, smooth shadow
- **Blue-based Theme**: Calm, professional color palette (Blue Primary → Blue Secondary gradients)
- **Consistent UI Components**: Reusable premium cards, badges, and buttons across all screens
- **Accessibility-first**: High contrast, readable fonts, proper touch targets
- **No clutter**: Clean layouts with purposeful spacing

---

## 🏗️ Architecture

### Tech Stack

**Frontend:**
- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Coil (image loading)
- StateFlow (reactive state management)

**Backend:**
- Firebase Authentication (Email/Password with email verification)
- Firebase Firestore (real-time database)
- Firebase Storage (profile images)
- Firebase Cloud Messaging (push notifications)
- Cloudinary CDN (image optimization)

**Architecture Pattern:**
- MVVM (Model-View-ViewModel)
- Repository pattern
- Unidirectional data flow
- Real-time listeners with Kotlin Flow

### Navigation Structure

The app uses role-based navigation graphs defined in `MainActivity.kt`:

| Role | Entry Point | Nav Graph |
|------|-------------|-----------|
| Member | `home` | `MemberApp` — Home, Search, Inbox, Profile, Requests, New Post, Comments, Chat, Notifications |
| Service Provider | `provider_home` | `ProviderApp` — Requests, Search, Inbox, Profile, Chat, Notifications |
| Admin | `home` | `AdminApp` — All Member screens + Dashboard, Member Management, Provider Approval, Announcements, Moderation, Join Requests |

### Project Structure

```
app/src/main/java/com/example/article/
├── Repository/
│   ├── ServiceRequest.kt
│   ├── ServiceRequestRepository.kt
│   ├── ProviderRequestsViewModel.kt
│   ├── MemberRequestViewModel.kt
│   ├── ProfileViewModel.kt
│   ├── AdminViewModels.kt
│   └── ArticleFirebaseMessagingService.kt
├── provider/
│   ├── ProviderRequestsScreen.kt
│   ├── ProviderRequestCard.kt
│   ├── ProviderProfileScreen.kt
│   ├── ProviderInboxScreen.kt
│   ├── ProviderSearchScreen.kt
│   └── ProviderBottomBar.kt
├── admin/
│   ├── AdminDashboardScreen.kt
│   ├── AdminBottomBar.kt
│   ├── ProviderApprovalScreen.kt
│   ├── MemberManagementScreen.kt
│   ├── AnnouncementManagementScreen.kt
│   ├── ContentModerationScreen.kt
│   └── JoinRequestsScreen.kt
├── notifications/
│   └── NotificationScreen.kt
├── ui/
│   ├── screens/LoginScreen.kt
│   └── theme/Color.kt
├── MainActivity.kt             # Role-based navigation entry point
├── UserSessionManager.kt       # Auth state + profile loading
├── UserRole.kt                 # Role enum (MEMBER, SERVICE_PROVIDER, ADMIN)
├── HomeScreen.kt
├── SearchScreen.kt
├── InboxScreen.kt
├── EnhancedChatScreen.kt
├── CommentScreen.kt
├── NewPostScreen.kt
├── RequestFormScreen.kt
├── RequestsScreen.kt
├── ProfileScreen.kt
├── ViewProfileScreen.kt
└── BottomBar.kt
```

---

## 🔥 Firebase Integration

### Firestore Collections

| Collection | Description |
|---|---|
| `users` | User profiles with role, neighbourhood, and metadata |
| `posts` | Top-level community posts with comments subcollection |
| `service_requests` | Service requests with full lifecycle tracking |
| `providers` | Provider profiles and approval status |
| `announcements` | Legacy top-level announcements (backwards compatibility) |
| `chats` | Chat threads with messages subcollection |
| `neighbourhoods` | Neighbourhood documents with members, providers, announcements, posts subcollections |
| `join_requests` | Pending requests to join a neighbourhood |
| `reports` | Flagged content reports (admin-readable) |
| `notifications` | Per-user push notification records |

### Security Rules Highlights

- Email-verified users only — unverified accounts are signed out on launch
- Members can only create requests with their own `memberId`
- Providers can only update requests assigned to them, following valid status transitions
- `reportCount` on posts can only be incremented (never decremented) by any signed-in user
- Notifications are recipient-readable only; `recipientId` is immutable after creation
- Rating can only be submitted once, by the member, after completion (1–5 stars enforced server-side)
- Admin-only access to sensitive operations (member deletion, provider approval, moderation)

### Request Lifecycle

```
Member creates request       → status: pending
        ↓
Provider accepts             → status: accepted, providerId set
        ↓
Provider starts work         → status: in_progress
        ↓
Provider marks complete      → status: completed, completedAt set
        ↓
Member submits rating        → rating: 1–5 (one-time, server-enforced)
```

Members may cancel at `pending` or `accepted` stage. Providers may release an accepted request back to `pending`.

---

## 📱 Screen Highlights

### Member Flow
1. **Login** → Email/password authentication with email verification check
2. **Home (Feed)** → View neighbourhood posts and announcements
3. **Search** → Discover people and content
4. **Inbox** → Chats and service request messaging
5. **Requests** → Create and track service requests
6. **Profile** → Manage profile, view post history, create posts
7. **Notifications** → View all in-app notifications

### Provider Flow
1. **Login** → Provider account
2. **Requests** → View and manage assigned and pending requests
3. **Search** → Discover members
4. **Inbox** → Chat with members
5. **Profile** → Set service type and availability, view stats
6. **Notifications** → View all in-app notifications

### Admin Flow
1. **Feed & Member Screens** → Full access to all member features
2. **Admin Dashboard** → Live overview statistics
3. **Members** → Add/remove members
4. **Providers** → Approve/reject provider applications
5. **Announcements** → Create pinned neighbourhood messages
6. **Moderation** → Review and remove flagged content
7. **Join Requests** → Approve or reject neighbourhood membership requests

---

## 🛡️ Quality Assurance

### Stability
- ✅ No duplicate LazyColumn keys
- ✅ Proper null safety throughout
- ✅ Error handling on all Firebase operations
- ✅ Loading states for all async operations
- ✅ Graceful error messages to users
- ✅ Email verification enforced on launch

### Code Quality
- **MVVM architecture** for separation of concerns
- **Repository pattern** for data layer abstraction
- **StateFlow** for reactive state management
- **Proper coroutine scoping** (`viewModelScope`, `lifecycleScope`)
- **Type-safe navigation** with sealed route strings and `NavType` arguments

### Performance
- Firestore queries optimized with indexes
- Image loading with Coil library caching
- Lazy loading for all list screens
- Proper composable recomposition boundaries
- Minimal re-renders with `remember` and `derivedStateOf`

---

## 🔧 Setup Instructions

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11 or higher
- Firebase project with Authentication, Firestore, Storage, and Cloud Messaging configured

### Run the App
```bash
git clone https://github.com/shwetamishra21/Article.git
cd Article
# Add google-services.json to app/
./gradlew assembleDebug
```

---

## 📊 Project Status

| Module | Status |
|--------|--------|
| Authentication (Email + Verification) | ✅ Complete |
| User Roles (Member / Provider / Admin) | ✅ Complete |
| Community Feed | ✅ Complete |
| Posts & Comments | ✅ Complete |
| Service Requests (Member) | ✅ Complete |
| Service Requests (Provider) | ✅ Complete |
| Service Request Rating | ✅ Complete |
| Real-time Updates | ✅ Complete |
| Profile Management | ✅ Complete |
| View Other Profiles | ✅ Complete |
| Admin Dashboard | ✅ Complete |
| Provider Approval | ✅ Complete |
| Member Management | ✅ Complete |
| Announcement Management | ✅ Complete |
| Content Moderation | ✅ Complete |
| Join Requests | ✅ Complete |
| Messaging / Chat | ✅ Complete |
| Push Notifications (FCM) | ✅ Complete |
| In-app Notification Screen | ✅ Complete |
| Firebase Security Rules | ✅ Complete |
| Search | ✅ Complete |
| Premium UI Components | ✅ Complete |
| Image Posts | ✅ Complete |

---

## 🎯 Service Types Supported

Plumber • Electrician • Cleaner • Carpenter • Painter • Gardener • AC Repair • Appliance Repair • Pest Control • Locksmith • Handyman • Mason • Welder • Tailor • Beautician • Tutor • Chef/Cook • Driver • Security Guard • Moving & Packing • Interior Designer • Solar Panel Installer • Water Tank Cleaner • Car Wash • Other

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 👤 Author

**Shweta Mishra**  
Android Developer — focused on clean architecture, real-time systems, and production-ready applications

[![GitHub](https://img.shields.io/badge/GitHub-shwetamishra21-181717?style=flat&logo=github)](https://github.com/shwetamishra21)

---

## 📄 License

This project is developed for educational and academic purposes.

---

## 🙏 Acknowledgments

- Firebase for backend infrastructure (Auth, Firestore, Storage, FCM)
- Material Design 3 for UI guidelines
- Jetpack Compose for modern Android UI toolkit
- Cloudinary for image optimization
