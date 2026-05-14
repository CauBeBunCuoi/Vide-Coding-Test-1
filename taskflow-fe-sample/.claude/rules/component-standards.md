# Component Standards

## Shared Component Inventory
These components live in `src/components/` and are reused across features:

| Component | Variants / Notes |
|-----------|-----------------|
| `Button` | Variants: primary, secondary, danger, ghost. Sizes: sm, md. Loading state with spinner. Disabled when `isPending`. |
| `Input` | Label, placeholder, error message slot, optional icon. |
| `TextArea` | Auto-growing, character counter. |
| `Select` | Single-select and multi-select variants; search within options. |
| `DatePicker` | Calendar popup; clear button. |
| `Modal` | Centered overlay; focus-trapped; closes on Escape and backdrop click. |
| `SlideOver` | Right-side panel 480px (full-screen on mobile); focus-trapped; closes on Escape and backdrop. |
| `Toast` | success (green, 5s auto-dismiss), error (red, 8s), info (blue, 5s). Stacks bottom-right (desktop) / bottom-center (mobile). Max 3 visible. |
| `Avatar` | Initial-based circular avatar. Sizes: sm 24px, md 32px, lg 48px. Background color derived from username hash (`src/utils/avatarColor.ts`). |
| `Badge` | Small pill with background + text. Used for priority and status. |
| `LabelPill` | Colored pill with label name. Removable variant with X button. |
| `EmptyState` | Icon/illustration + message + optional CTA button. |
| `Pagination` | Page number buttons + prev/next arrows. Shows current page and total. |
| `ConfirmDialog` | "Are you sure?" modal with cancel + confirm (danger) buttons. |
| `Skeleton` | `animate-pulse` with `bg-gray-200` on `bg-gray-50` background. Match content shape. |

## Layout
- App shell: fixed top navbar (56px height) + scrollable page content area.
- Max content width: 1280px centered horizontally. Padding: 24px (desktop), 16px (mobile).
- Responsive breakpoints: desktop ≥1024px | tablet 768–1023px | mobile <768px.

## Design Tokens
Use CSS custom properties — never hardcode hex values in components:
| Token | Hex | Usage |
|-------|-----|-------|
| `--color-primary` | #2563EB | Buttons, links, active states |
| `--color-primary-hover` | #1D4ED8 | Button hover |
| `--color-danger` | #DC2626 | Delete buttons, error text, overdue badges |
| `--color-success` | #16A34A | Success toasts, DONE column accent |
| `--color-warning` | #D97706 | Overdue deadline, IN_REVIEW column accent |
| `--color-gray-50` | #F9FAFB | Page background |
| `--color-gray-100` | #F3F4F6 | Card background, input background |
| `--color-gray-200` | #E5E7EB | Borders, dividers |
| `--color-gray-500` | #6B7280 | Secondary text, placeholders |
| `--color-gray-700` | #374151 | Primary body text |
| `--color-gray-900` | #111827 | Headings |

## Priority Badge Colors
| Priority | Background | Text |
|----------|-----------|------|
| URGENT | #FEE2E2 | #991B1B |
| HIGH | #FEF3C7 | #92400E |
| MEDIUM | #DBEAFE | #1E40AF |
| LOW | #F3F4F6 | #374151 |

## Kanban Column Accent Colors (top border)
TODO #6B7280 | IN_PROGRESS #2563EB | IN_REVIEW #D97706 | DONE #16A34A

## Typography
Font stack: `Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`
Page title: 24px/700 | Section heading: 18px/600 | Card title + body: 14px | Small/meta: 12px

## Label Color Palette (12 predefined options)
`#EF4444` `#F97316` `#F59E0B` `#84CC16` `#10B981` `#06B6D4` `#3B82F6` `#6366F1` `#8B5CF6` `#EC4899` `#78716C` `#1F2937`
Frontend color picker emits only these values. Backend validates hex format only.

## Optimistic Updates Pattern
Used for all drag-and-drop status changes and inline field edits:
1. `onMutate`: cancel outgoing queries, snapshot previous data, apply optimistic update
2. `onError`: restore snapshot from context
3. `onSettled`: invalidate query to refetch fresh data

If an API call fails after drag-and-drop: animate card back to original column, show error toast.

## Loading / Empty / Error States
Every data-fetching view must implement all three:
- **Loading:** `Skeleton` components matching content shape (pulse animation)
- **Empty:** `EmptyState` component with icon, message, and optional CTA button
- **Error:** inline message with "Try again" button that calls `refetch()`

## Accessibility
All interactive elements reachable via Tab.
Modals and SlideOver components trap focus.
Escape key closes overlays.
