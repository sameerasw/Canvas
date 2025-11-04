# DoodList MVVM Refactoring Documentation

## Project Structure

The project has been refactored into a clean MVVM (Model-View-ViewModel) architecture with clear separation of concerns.

### Directory Structure

```
app/src/main/java/com/sameerasw/canvas/
├── MainActivity.kt                          # Main Activity & CanvasApp Composable
├── CanvasViewModel.kt                       # ViewModel managing canvas state and data
│
├── model/                                   # Data Models
│   ├── DrawStroke.kt                       # Stroke data model
│   └── ToolType.kt                         # Tool enum
│
├── ui/                                      # UI Layer
│   ├── components/                          # Reusable Composable Components
│   │   ├── PenWidthOptionsPanel.kt         # Pen width slider UI
│   │   ├── TextSizeOptionsPanel.kt         # Text size slider UI
│   │   ├── TextInputDialog.kt              # Text input dialog
│   │   ├── TextOptionsDialog.kt            # Text edit/move/delete options
│   │   ├── TopMenuButtons.kt               # Top menu action buttons
│   │   ├── TopOverlayToolbar.kt            # Top toolbar container
│   │   └── ToolbarFloating.kt              # Main floating toolbar
│   │
│   ├── drawing/                             # Drawing & Rendering Utilities
│   │   ├── BitmapExportHelper.kt           # Bitmap creation from strokes/texts
│   │   ├── BitmapStorageHelper.kt          # Save/share bitmap operations
│   │   ├── StrokeDrawer.kt                 # Stroke rendering logic
│   │   └── TextDrawer.kt                   # Text rendering logic
│   │
│   ├── screens/                             # Full Screen Composables
│   │   └── DrawingCanvasScreen.kt          # Main canvas with gesture handling
│   │
│   ├── state/                               # UI State Data Classes
│   │   ├── CanvasUiState.kt                # Main canvas UI state
│   │   ├── DrawingCanvasState.kt           # Canvas transform state
│   │   ├── PenSliderState.kt               # Pen width slider state
│   │   └── TextSliderState.kt              # Text size slider state
│   │
│   ├── theme/                               # Theme Configuration
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   │
│   └── TaskViewModel.kt
│
├── data/                                    # Data Layer
│   ├── AppDatabase.kt                      # Room Database
│   ├── CanvasDao.kt                        # Canvas DAO
│   ├── CanvasEntity.kt                     # Canvas Entity
│   ├── CanvasRepository.kt                 # Canvas Repository
│   ├── TextItem.kt                         # Text item model
│   ├── StrokeDao.kt
│   ├── StrokeEntity.kt
│   ├── TaskDao.kt
│   ├── TaskEntity.kt
│   └── Repository.kt
│
└── utils/                                   # Utility Classes
    └── HapticUtil.kt                       # Haptic feedback utility
```

## Architecture Overview

### Model Layer (model/)
- `DrawStroke.kt`: Immutable data class representing a drawn stroke
- `ToolType.kt`: Enum for drawing tools (Hand, Pen, Eraser, Text)

### ViewModel Layer (CanvasViewModel.kt)
- Manages canvas drawing state (strokes and texts)
- Handles save/load operations via repository
- Exposes data as StateFlow for reactive UI updates
- Methods:
  - `addStroke()`, `removeStroke()`, `setStrokes()`, `clearStrokes()`
  - `addText()`, `updateText()`, `removeText()`, `clearTexts()`
  - `clearAll()`, `save()`, `load()`

### View Layer

#### Main Entry (MainActivity.kt)
- `MainActivity`: Activity that initializes Compose and the ViewModel
- `CanvasApp()`: Root composable managing all UI state

#### Screens (ui/screens/)
- `DrawingCanvasScreen.kt`: Canvas component handling:
  - Gesture detection (pan, zoom, draw, erase, text)
  - Stroke and text rendering
  - Event callbacks to ViewModel

#### Components (ui/components/)
Reusable, focused UI components:
- **Toolbars**:
  - `ToolbarFloating.kt`: Main expandable floating toolbar with tool buttons
  - `TopOverlayToolbar.kt`: Top menu bar that appears when expanded
  - `TopMenuButtons.kt`: Individual menu action buttons

- **Options Panels**:
  - `PenWidthOptionsPanel.kt`: Slider for pen width with live preview
  - `TextSizeOptionsPanel.kt`: Slider for text size with preview

- **Dialogs**:
  - `TextInputDialog.kt`: Add/edit text dialog
  - `TextOptionsDialog.kt`: Edit/move/delete options for existing text

#### Drawing Utilities (ui/drawing/)
- `StrokeDrawer.kt`: Renders smooth strokes with layering
- `TextDrawer.kt`: Renders text with custom fonts
- `BitmapExportHelper.kt`: Converts canvas data to Bitmap
- `BitmapStorageHelper.kt`: Save/share bitmap operations

#### UI State (ui/state/)
Organized state data classes:
- `CanvasUiState.kt`: Main UI state container
- `DrawingCanvasState.kt`: Canvas transform and drawing state
- `PenSliderState.kt`: Pen options state
- `TextSliderState.kt`: Text options state

### Data Layer (data/)
- Room Database for persistent storage
- Repository pattern for data access
- Entities for database schema
- TextItem: Data model for text annotations

### Utilities (utils/)
- `HapticUtil.kt`: Haptic feedback effects for user interactions

## Key Improvements

1. **Separation of Concerns**: Each component has a single responsibility
2. **Reusability**: UI components are composable and reusable
3. **Testability**: Business logic separated from UI
4. **Maintainability**: Clear file organization makes code easier to navigate
5. **Scalability**: Easy to add new features without affecting existing code
6. **State Management**: Organized state classes for clarity
7. **Drawing Logic**: Extracted into dedicated helper classes
8. **Storage Operations**: Separate utilities for bitmap export and storage

## State Flow

1. User interaction in `DrawingCanvasScreen` triggers event callback
2. Event calls `CanvasViewModel` method
3. ViewModel updates StateFlow
4. UI observes StateFlow change and recomposes
5. New state rendered on screen

## Data Persistence

- Canvas data (strokes + texts) serialized to JSON via Gson
- Saved to database via Room ORM
- Loaded on app start automatically
- Auto-saved on pause/stop lifecycle events

## No Functionality Changes

All original features are preserved:
- Drawing with pen, eraser, hand, text tools
- Pinch to zoom and pan canvas
- Variable haptics based on pen speed
- Export/share as PNG
- Save to Downloads
- Text annotation with editing
- Persistent storage

