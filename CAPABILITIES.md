# Sofia App Capabilities & UI/UX Technical Documentation

This document provides a detailed breakdown of the Sofia Android app's capabilities, UI structure, and data architecture to facilitate its implementation in Swift.

---

## 1. UI Architecture & Navigation

The app uses a **Bottom Navigation Bar** as the primary navigation pattern, complemented by a **Horizontal Pager** in the main production view.

### A. Production Tab (The Primary Dashboard)
This tab features a "Composite Screen" that allows switching between two distinct data views using a **Floating Pager Slider**.

*   **Panel 1: Production (Planned Output)**
    *   **Gauge Card**: A circular or semi-circular gauge displaying `current_mw` vs `max_capacity_mw`.
    *   **Interactive Chart**: A line chart showing PN (Planned Production) levels. Supports time window selection (6h, 24h, 48h, 7d, All).
    *   **Records Card**: Displays top production values for different windows (7d, 30d, 90d, All Time).
    *   **Metadata**: Shows the timestamp of the latest data point and the last successful fetch time.
*   **Panel 2: Real Output (Metered Data)**
    *   Similar structure to the Production panel but focuses on B1610 (Real Output) data.
    *   Quantities are typically displayed in MW (calculated from MWh).

### B. Graph Tab (Multi-Series Analysis)
A dedicated screen for correlation analysis.
*   **Dataset Selector**: Toggle buttons (Filter Chips) to show/hide **Wind Speed**, **PN**, and **Real Output**.
*   **Dual-Axis Chart**:
    *   **Left Y-Axis**: Power output in **MW**.
    *   **Right Y-Axis**: Wind speed in **m/s**.
*   **Time Window Selection**: Synchronization of all series across the selected time frame.

### C. REMIT Tab (Market Transparency)
*   **Notice List**: A scrollable list of `RemitNoticeCard` elements showing active market notices.
*   **Filtering/Status**: Distinguishes between active and historical events.
*   **Detail View**: A dedicated drill-down screen (triggered on card click) showing:
    *   Event headers and status chips.
    *   **Capacity Section**: Normal, Available, and Unavailable MW.
    *   **Timeline Section**: Event start, end, and publication times.
    *   **Asset Info**: BMU ID, Participant ID, Asset ID.
    *   **Description**: Detailed "Cause" and "Related Information" text blocks.
    *   **Outage Profile**: Technical breakdown of the event's progression.

### D. Weather Tab
*   **Wind Speed View**: Dedicated focus on environmental data with a trend chart and current status.

### E. Settings Tab
*   **Test Mode Toggle**: Switches the entire app's data source from production BMUs to a test BMU (`T_HEYM11`).

---

## 2. Data Models & API Interface

The app interacts with a REST API. All timestamps are handled in **UTC**.

### Core Data Structures

| Model | Key Fields | Description |
| :--- | :--- | :--- |
| **PnEntry** | `bmu_id`, `time_from`, `time_to`, `level_mw` | Planned production levels. |
| **B1610Entry** | `bmu_id`, `time_from`, `time_to`, `quantity` | Actual metered output. |
| **WeatherEntry** | `time_from`, `time_to`, `wind_speed` | Environmental wind data. |
| **RemitNotice** | `mrid`, `event_status`, `event_type`, `capacity_mw` (Normal/Avail/Unavail), `cause` | Market transparency notices. |
| **TopProduction**| `max_mw`, `max_date` | Record-breaking production values. |

### Data Transformations
*   **Aggregation**: Data from multiple BMUs is summed at the repository level to provide a "Total Sofia" view.
*   **Filtering**: Clientside filtering based on `TimeWindow` (6h, 24h, etc.) using `Instant` comparisons.
*   **MW Conversion**: B1610 data (often received as quantity per settlement period) is scaled to MW for visual consistency with PN data.

---

## 3. Interactive Features

*   **Pull-to-Refresh**: Standardized across all data screens using Material 3 `PullToRefreshBox`.
*   **Floating Slider**: Custom UI component for high-frequency switching between PN and B1610 views.
*   **Back Handling**: Custom logic to navigate from REMIT details back to the list using the system back gesture.
*   **Error Banners**: Non-intrusive, dismissible banners for network or API errors.
*   **Empty States**: Context-aware cards displayed when no data is available for a selected time window.

Here is the model used :
package com.lemarc.sofia.data.model

import java.time.Instant

data class GraphPoint(
val id: String,
val timeFrom: Instant,
val timeTo: Instant,
val quantity: Double,
)

data class B1610Snapshot(
val points: List<GraphPoint>,
val latestDataTimestamp: Instant?,
val topB1610: TopWindows,
)

data class WeatherSnapshot(
val points: List<GraphPoint>,
val latestWindSpeed: Double?,
val latestDataTimestamp: Instant?,
)

data class ProductionSnapshot(
val points: List<GraphPoint>,
val currentMw: Double,
val latestDataTimestamp: Instant?,
val topProduction: TopWindows,
)

data class TopPoint(
val maxQuantity: Double,
val maxDate: Instant?,
)

data class TopWindows(
val allTime: TopPoint,
val last7Days: TopPoint,
val last30Days: TopPoint,
val last90Days: TopPoint,
) {
companion object {
val Empty = TopWindows(
allTime = TopPoint(0.0, null),
last7Days = TopPoint(0.0, null),
last30Days = TopPoint(0.0, null),
last90Days = TopPoint(0.0, null),
)
}
}

data class RemitNotice(
val id: Int,
val mrid: String,
val revisionNumber: Int,
val bmuId: String,
val participantId: String,
val assetId: String,
val unavailabilityType: String,
val eventType: String,
val messageHeading: String,
val fuelType: String,
val normalCapacityMw: Double?,
val availableCapacityMw: Double?,
val unavailableCapacityMw: Double?,
val eventStatus: String,
val eventStartTime: Instant?,
val eventEndTime: Instant?,
val cause: String,
val relatedInformation: String,
val publishTime: Instant?,
val outageProfile: String,
)