# Comprehensive Improvement Roadmap

This document provides a detailed technical guide for evolving the Sofia app into a production-grade, highly scalable monitoring platform.

---

## 1. Logic & Architecture Enhancements

### A. Dependency Injection (Hilt)
*   **Action**: Replace manual instantiation in `MainActivity` with **Dagger Hilt**.
*   **Detail**: 
    *   Define `@Module` for `NetworkModule` (Retrofit, OkHttpClient) and `RepositoryModule`.
    *   Use `@Inject` in ViewModels to receive repositories.
    *   **Benefit**: Removes boilerplate, enables easy swapping of mock/real data for testing, and manages component lifecycles automatically.

### B. Clean Architecture Implementation
*   **Action**: Introduce a **Domain Layer**.
*   **Detail**:
    *   **Use Cases**: Create classes like `GetAggregatedProductionUseCase` and `GetActiveRemitsUseCase`.
    *   **Logic Separation**: Move the logic in `aggregateProduction()` and `calculateTopProduction()` from the Repository to Use Cases.
    *   **Benefit**: Repositories stay "dumb" (fetching only), and business logic becomes unit-testable in isolation from Android/Network frameworks.

### C. Type-Safe Navigation
*   **Action**: Migrate to **Jetpack Compose Navigation (Type-Safe)**.
*   **Detail**:
    *   Define a sealed class hierarchy for routes.
    *   Pass the `noticeId` for REMIT details via navigation arguments rather than a simple `mutableStateOf` in the screen.
    *   **Benefit**: Provides a robust back-stack, enables deep-linking to specific REMIT notices, and standardizes screen transitions.

---

## 2. Advanced Data Handling

### A. Offline-First with Room
*   **Action**: Implement **Room Persistence Library** as a local cache.
*   **Detail**:
    *   **Schema**: Create tables for `ProductionEntity`, `WeatherEntity`, and `RemitEntity`.
    *   **Single Source of Truth**: The UI should always observe the Database (via Flow). The Repository handles fetching from Network and updating the Database.
    *   **Benefit**: Instant app startup (show last known data), offline browsing, and reduced battery consumption by avoiding redundant network calls.

### B. Intelligent Synchronization (SyncManager)
*   **Action**: Use **WorkManager** for background synchronization.
*   **Detail**:
    *   Periodically fetch REMIT notices in the background.
    *   Trigger local notifications when a new "High Impact" REMIT notice is detected.
    *   **Benefit**: Keeps data fresh even when the app isn't active.

### C. Network Optimization
*   **Action**: Implement **Etag** support and **Gzip compression**.
*   **Detail**: Use `OkHttp` interceptors to handle caching headers properly.
*   **Benefit**: Saves bandwidth and speeds up data loading for large historical datasets.

---

## 3. UI/UX & Visual Polish

### A. High-Fidelity Charting
*   **Action**: Enhance `ProductionChartMulti` with custom drawing.
*   **Detail**:
    *   **Tooltips**: Implement a "Long Press" gesture that shows a vertical line and a popup with exact MW/m/s values at that specific timestamp.
    *   **Shimmer Loading**: Replace `CircularProgressIndicator` with skeleton screens (`Shimmer`) that mimic the layout of the cards while data loads.
    *   **Animations**: Use `AnimatedContent` for transitions between "Production" and "Real Output" to make the slider feel more fluid.

### B. Accessibility & Internationalization
*   **Action**: Full **TalkBack** and **RTL** support.
*   **Detail**:
    *   Add `contentDescription` to all icons and charts.
    *   Move all hardcoded strings (e.g., "Mis à jour", "Cause") to `strings.xml`.
    *   Support **Dynamic Type** (resizing text based on system settings).

### C. Theming & Dark Mode
*   **Action**: Dynamic Color support (Material You).
*   **Detail**: Integrate with the system's primary color palette on Android 12+. Ensure high contrast for chart series in both Light and Dark themes.

---

## 4. Quality Assurance & Monitoring

*   **Unit Testing**: Target 80% coverage for Use Cases and Repository logic.
*   **Snapshot Testing**: Use libraries like `Paparazzi` or `Showkase` to ensure UI components don't regress visually.
*   **Crash Reporting**: Integrate **Firebase Crashlytics** and **Performance Monitoring** to track real-world stability and API latency.
