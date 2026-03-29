# Changelog

## v1.0.0 (2026-01-17)

### Features
- Complete Sudoku game with 3 difficulty levels (Easy, Medium, Hard)
- Smart hint system with 3 progressive levels:
  - Gentle Nudge - highlights related cells
  - Show Technique - explains the logic
  - Show Answer - reveals with full explanation
- Real-time conflict detection (highlights duplicate numbers)
- Row, column, and box completion notifications
- Notes/pencil marks mode for advanced solving
- Timer (starts on first input)
- Statistics tracking (games completed, best times, learning level)
- PWA support - installable on mobile devices
- Offline support via service worker
- Keyboard navigation with arrow keys
- Responsive design for mobile and desktop

### UI/UX
- DaisyUI component library with "business" theme
- Black/gray/white color scheme
- High contrast numbers for readability
- Toast notification system (queued, non-overlapping)
- Cells turn green on puzzle completion
- Mobile-optimized with numeric keypad

### Bug Fixes (QA Testing)
- Fixed localStorage crash in Private/Incognito mode
- Fixed arrow key page scrolling at grid edges
- Fixed Check Solution crash on empty board
- Fixed given cells being selectable
- Fixed toast notifications overlapping
- Fixed memory leaks (event listeners, state sets)
- Fixed notes mode font size persistence
- Fixed clear cell not updating highlights

### Technical
- Event delegation for better performance
- Backtracking algorithm for puzzle generation
- LocalStorage for persistent statistics
- Service worker for offline caching
