# 🏘️ Article — Neighbourhood Community App

**Article** is a comprehensive Android application that brings neighbourhood communities together on a single, structured platform. The app facilitates **community interaction**, **local announcements**, and **service coordination** with a focus on **real-time updates**, **role-based access**, and **professional UI/UX**.

> 🎯 **Current Status**: Full-stack implementation with Firebase backend, real-time data synchronization, and premium Material 3 UI

---

## ✨ Core Features

### 👥 Multi-Role System
- **Members**: Create posts, request services, manage their profile
- **Service Providers**: Accept requests, manage availability, track completed work
- **Admins**: Manage members, approve providers, create announcements, moderate content

### 📰 Community Feed
- View neighbourhood **posts and announcements**
- Announcements visually highlighted with premium styling
- Like and comment on posts
- Real-time updates via Firebase listeners
- Clean, card-based layout with proper hierarchy

### 🛠️ Service Request System
**For Members:**
- Create service requests with 25+ service types (Plumber, Electrician, Cleaner, etc.)
- Optional preferred date selection
- Real-time status tracking (Pending → Accepted → In Progress → Completed)
- View assigned provider information
- Cancel pending requests
- Provider contact integration

**For Service Providers:**
- Real-time request notifications
- Accept/decline requests
- Status management (Start Work, Mark Complete)
- Request filtering by status
- Premium card UI with gradient accents

### 💬 Inbox & Messaging (Planned)
- Member-to-Member chats
- Member-to-Service Provider chats
- Real-time messaging support
- Role-based message routing

### 👤 Profile Management
- Edit profile (name, bio, neighborhood)
- Profile image upload with Cloudinary CDN
- Role-specific profiles:
  - **Members**: Post history with grid/list view toggle
  - **Providers**: Service type selection, availability toggle, stats dashboard
- Logout with confirmation

### 🔐 Admin Panel
- **Member Management**: Add/remove members, view member list
- **Provider Approval**: Approve/reject service providers, manage provider status
- **Announcements**: Create pinned community announcements
- **Content Moderation**: Review and remove posts
- Real-time statistics dashboard

---

## 🎨 Design Philosophy

- **Premium Material 3 Design**: Gradient top bars, elevated cards, smooth shadows
- **Blue-based Theme**: Calm, professional color palette (Blue Primary → Blue Secondary gradients)
- **Consistent UI Components**: Reusable premium cards, badges, and buttons across all screens
- **Accessibility-first**: High contrast, readable fonts, proper touch targets
- **No clutter**: Minimal animations, clean layouts, purposeful spacing

> ❝ Professional appearance with production-ready polish ❞

---

## 🏗️ Architecture

### Tech Stack

**Frontend:**
- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Coil for image loading
- StateFlow for reactive state management

**Backend:**
- Firebase Authentication (Email/Password)
- Firebase Firestore (Real-time database)
- Firebase Storage (Profile images)
- Cloudinary CDN (Image optimization)

**Architecture Pattern:**
- MVVM (Model-View-ViewModel)
- Repository pattern
- Unidirectional data flow
- Real-time listeners with Kotlin Flow

### Project Structure

```
app/src/main/java/com/example/article/
├── Repository/
│   ├── ServiceRequest.kt          # Data model
│   ├── ServiceRequestRepository.kt # Firestore operations
│   ├── ProviderRequestsViewModel.kt
│   ├── MemberRequestViewModel.kt
│   ├── ProfileViewModel.kt
│   └── AdminViewModels.kt
├── provider/
│   ├── ProviderRequestsScreen.kt
│   ├── ProviderRequestCard.kt
│   ├── ProviderProfileScreen.kt
│   └── ProviderBottomBar.kt
├── admin/
│   ├── AdminDashboardScreen.kt
│   ├── ProviderApprovalScreen.kt
│   ├── MemberManagementScreen.kt
│   └── ContentModerationScreen.kt
├── ui/theme/
│   └── Color.kt                   # Theme colors
├── RequestFormScreen.kt           # Member request creation
├── RequestsScreen.kt              # Member request list
├── ProfileScreen.kt               # Member profile
├── FeedScreen.kt                  # Community feed
└── UserSessionManager.kt          # Auth state management
```

---

## 🔥 Firebase Integration

### Firestore Collections



### Security Rules

Comprehensive Firestore security rules enforce:
- Members can only create requests with their own `memberId`
- Providers can only update requests assigned to them
- Proper status transition validation (pending → accepted → in_progress → completed)
- Admin-only access to sensitive operations
- Real-time read permissions based on user role

---

##  Key Features Implementation

### Real-Time Updates
- **Flow-based listeners** for instant UI updates across all users
- Member sees status change when provider accepts request
- Provider sees new requests immediately when created
- No manual refresh required

### Request Lifecycle
```
Member creates request (status: pending)
    ↓
Provider accepts (status: accepted, providerId set)
    ↓
Provider starts work (status: in_progress)
    ↓
Provider completes (status: completed, completedAt set)
```

### Premium UI Components
- **Gradient buttons** with elevation
- **Status-based color coding** (Pending: Orange, Accepted: Blue, In Progress: Light Blue, Completed: Green)
- **Animated cards** with scale effects on press
- **Colored accent bars** on request cards
- **Provider avatars** with gradient backgrounds
- **Info chips** with icons for dates and times

### Data Validation
- Required fields validation before submission
- Date picker limited to future dates
- Role-based feature access
- Firestore rules enforce server-side validation

---

## 📱 Screen Highlights

### Member Flow
1. **Login** → Email/password authentication
2. **Feed** → View posts and announcements
3. **Requests** → Create and track service requests
4. **Profile** → Manage profile and view post history

### Provider Flow
1. **Login** → Provider account
2. **Requests** → View and manage assigned requests
3. **Profile** → Set service type and availability
4. **Stats** → Track completed jobs and ratings

### Admin Flow
1. **Dashboard** → Overview statistics
2. **Members** → Add/remove members
3. **Providers** → Approve/reject provider applications
4. **Announcements** → Create pinned messages
5. **Moderation** → Review flagged content

---

## 🛡️ Quality Assurance

### Stability
- ✅ No duplicate LazyColumn keys
- ✅ Proper null safety throughout
- ✅ Error handling on all Firebase operations
- ✅ Loading states for async operations
- ✅ Graceful error messages to users

### Code Quality
- **MVVM architecture** for separation of concerns
- **Repository pattern** for data layer abstraction
- **StateFlow** for reactive state management
- **Proper scoping** (viewModelScope for coroutines)
- **Type-safe navigation** with sealed classes

### Performance
- Firestore queries optimized with indexes
- Image loading with Coil library caching
- Lazy loading for lists
- Proper composable recomposition boundaries
- Minimal re-renders with remember and derivedStateOf

---

## 🔧 Setup Instructions

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11 or higher
- Firebase project configured

```

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
| Authentication | ✅ Complete |
| User Roles | ✅ Complete |
| Service Requests (Member) | ✅ Complete |
| Service Requests (Provider) | ✅ Complete |
| Real-time Updates | ✅ Complete |
| Profile Management | ✅ Complete |
| Admin Dashboard | ✅ Complete |
| Provider Approval | ✅ Complete |
| Premium UI Components | ✅ Complete |
| Firebase Security Rules | ✅ Complete |
| Community Feed | ✅ Complete |
| Messaging/Chat | ⏳ Planned |
| Push Notifications | ⏳ Planned |
| Image Posts | ⏳ Planned |

---

## 🎯 Service Types Supported

Plumber • Electrician • Cleaner • Carpenter • Painter • Gardener • AC Repair • Appliance Repair • Pest Control • Locksmith • Handyman • Mason • Welder • Tailor • Beautician • Tutor • Chef/Cook • Driver • Security Guard • Moving & Packing • Interior Designer • Solar Panel Installer • Water Tank Cleaner • Car Wash • Other

---

## 📸 Screenshots

*(Add screenshots of key screens here)*

---

## 🤝 Contributing

This project is developed for educational purposes. Contributions, issues, and feature requests are welcome!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 Git Commit History

This project follows **incremental development** with clean commit history:
- Each feature is committed separately
- UI improvements tracked independently
- Backend integration documented step-by-step
- Stable checkpoints maintained throughout

**Recent Milestones:**
- ✅ Complete provider backend with premium UI
- ✅ Firebase security rules implementation
- ✅ Real-time request synchronization
- ✅ Admin panel with live stats
- ✅ MVVM architecture with repositories

---

## 👤 Author

**Shweta Mishra**  
Android Developer  
Focused on clean architecture, real-time systems, and production-ready applications

[![GitHub](https://img.shields.io/badge/GitHub-shwetamishra21-181717?style=flat&logo=github)](https://github.com/shwetamishra21)

---

## 📄 License

This project is developed for **educational and academic purposes**.

---

## 🙏 Acknowledgments

- Firebase for backend infrastructure
- Material Design 3 for UI guidelines
- Jetpack Compose team for modern Android UI toolkit
- Cloudinary for image optimization

---
