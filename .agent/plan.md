# Project Plan

Build a complete Android application called "FitTrack", an AI-powered fitness and health tracking application.

STRICT TECHNOLOGY REQUIREMENTS:
- Use ONLY Java for application logic.
- Use ONLY XML for UI layouts.
- DO NOT use Kotlin.
- DO NOT use Jetpack Compose.
- If these requirements are not met, the project will be considered a failure.

Core Features:
1. Splash Screen (Animated)
2. Login/Sign Up/Forgot Password (with validation and animations)
3. Health Permission Onboarding (Steps, Calories, Heart Rate - with mock fallbacks)
4. Main Dashboard (Body Analysis, Exercises, Diet Plan, Calories, Health/Activity, AI Assistant, Reminders, Profile, About Us)
5. Bottom Navigation (Home, Analysis, Workout, Diet, Profile)
6. Body Analysis (Camera preview, capture, demo composition analysis, posture observations)
7. Posture Detection (Pose landmarks overlay, corrective guidance for exercises)
8. Exercise Library (Categorized by body part, description, difficulty, duration, repetitions)
9. Diet Plan (Meal suggestions, water reminder, nutrition summary)
10. Calorie Tracker (Daily target vs consumed vs burned)
11. AI Fitness Assistant (Chatbot UI, separated service layer for future Gemini integration)
12. Exercise Reminders (Local notifications)
13. Profile & Settings (Dark/Light theme, Logout, Account info)
14. About Us (Team: Sagar B., Sagar P., Yash J.)

## Project Brief

# Project Brief: FitTrack

## Features
1.  **AI-Powered Body Analysis & Posture Detection**: Utilizes CameraX and pose landmark overlays to provide real-time composition analysis and corrective posture guidance during workouts.
2.  **Comprehensive Health Dashboard**: A centralized hub for tracking daily activity (steps, calories), body analysis results, diet summaries, and quick access to the AI assistant.
3.  **AI Fitness Assistant**: An integrated chatbot service that provides personalized fitness advice and exercise recommendations through a dedicated service layer.
4.  **Exercise Library & Tracking**: A categorized repository of workouts with detailed descriptions, difficulty levels, and daily calorie tracking (consumed vs. burned).
5.  **Secure Authentication & Onboarding**: A robust user management system with animated splash screens and a multi-step onboarding flow for health permission synchronization.

## High-Level Technical Stack
*   **Language**: Java (Core application logic)
*   **UI Framework**: XML Layouts (View-based UI system)
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Vision & AI**: CameraX (preview/capture) and ML Kit/MediaPipe (pose landmarks)
*   **Navigation**: Jetpack Navigation Component (Fragment-based implementation)
*   **Networking**: Retrofit & OkHttp (for AI Assistant integration)

> [!IMPORTANT]
> This stack strictly adheres to the project requirement of using **Java** and **XML** exclusively. Consequently, Jetpack Navigation 3 and Compose Material Adaptive libraries (which require Kotlin/Compose) have been replaced with their standard View-based counterparts.

## Implementation Steps
**Total Duration:** 10h 39m 50s

### 1: Project Setup: Configure Gradle, AndroidManifest, base styles (Light/Dark), and package structure in Java.
- **Status:** COMPLETED
- **Updates:** - Configured build.gradle with Material 3 and CameraX dependencies (Java-focused).
- **Acceptance Criteria:**
  - Project builds with Java and XML only
  - Basic theme setup complete
  - Manifest includes necessary permissions (Camera, Health)

### 2: Splash Screen: Implement animated splash screen with scale and fade animations using Java and XML.
- **Status:** COMPLETED
- **Updates:** - Created SplashActivity.java and activity_splash.xml with a gradient background and logo.
- **Acceptance Criteria:**
  - Splash screen displays logo with animations
  - Smooth transition to Login screen

### 3: Authentication: Implement Login, Sign Up, and Forgot Password screens with validation logic in Java.
- **Status:** COMPLETED
- **Updates:** - Implemented LoginActivity with validation, password toggle, and loading animation.
- **Acceptance Criteria:**
  - All auth screens functional
  - Input validation works
  - Navigation between auth screens is smooth

### 4: Onboarding & Permissions: Implement Health Permission Onboarding and mock health data fallback.
- **Status:** COMPLETED
- **Updates:** - Implemented OnboardingActivity with a professional UI for health permission explanation.
- **Acceptance Criteria:**
  - Permissions requested correctly
  - Mock data displayed if permissions denied/unavailable

### 5: Main Dashboard & Navigation: Implement Bottom Navigation and the core Dashboard UI with cards.
- **Status:** COMPLETED
- **Updates:** - Implemented BottomNavigationView in activity_main.xml with Home, Analysis, Workout, Diet, Profile.
- **Acceptance Criteria:**
  - Bottom navigation works correctly
  - Dashboard displays greeting and mock activity data

### 6: Body Analysis & Posture Detection: Integrate CameraX for live preview and capture with mock pose overlay.
- **Status:** COMPLETED
- **Updates:** - Implemented AnalysisFragment with entry points for Body Analysis and Posture Detection.
- **Acceptance Criteria:**
  - Live camera preview works
  - Pose/Skeleton overlay displayed
  - Analysis results screen functional

### 7: Exercise Library: Implement categorized exercise list and detailed workout screens.
- **Status:** COMPLETED
- **Updates:** - Implemented WorkoutFragment with search bar and 2-column category grid.
- **Acceptance Criteria:**
  - Categorization works (Chest, Back, etc.)
  - Detail screen shows duration/reps and 'Start Workout'
- **Duration:** 16h 4m 58s

### 8: Diet & Calorie Tracking: Implement Diet Plan and Calorie Tracker UIs.
- **Status:** COMPLETED
- **Updates:** - Implemented DietFragment with a comprehensive calorie summary and progress bar.
- **Acceptance Criteria:**
  - Meal suggestions displayed
  - Calorie progress indicator functional

### 9: AI Fitness Assistant: Implement chatbot UI and service layer.
- **Status:** COMPLETED
- **Updates:** - Implemented AiAssistantActivity and activity_ai_assistant.xml with a professional chat interface.
- **Acceptance Criteria:**
  - Chat interface works (User/AI messages)
  - Loading indicator shows for 'AI response'

### 10: Profile, Settings & About Us: Implement remaining utility screens and theme switching logic.
- **Status:** COMPLETED
- **Updates:** - Implemented ProfileFragment with user header and action list.
- **Acceptance Criteria:**
  - Theme selection works
  - Profile displays user info
  - About Us shows team details

### 11: Final Verification: Conduct a full app walkthrough using the critic_agent to ensure stability and UI quality.
- **Status:** COMPLETED
- **Updates:** - Conducted a full walkthrough of the FitTrack application.
- Verified all core modules: Splash, Auth, Onboarding, Dashboard, Camera Features (Analysis/Posture), Exercise, Diet, AI Chat, and Utilities.
- Confirmed stable performance, no crashes, and consistent Material 3 UI design.
- Verified 100% Java and XML implementation with no Kotlin/Compose.
- App is ready for deployment.
- **Acceptance Criteria:**
  - App is crash-free
  - UI matches professional requirements
  - All features functional
- **Duration:** N/A

