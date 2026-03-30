# Frontend Implementation Plan

## 1. Objective

This document defines the implementation plan for building the `frontend` app that consumes the backend in `../backend`.

The plan is based on:

- the product scope in the backend API
- the visual direction in `DESIGN.md`
- the code organization in `STRUCTURE.md`

The goal is to move from a default Vite React template to a usable product frontend in a controlled sequence.

## 2. Guiding Strategy

The frontend should not be built screen by screen in a random order.

The safest and fastest path is:

1. establish structure
2. establish design tokens
3. establish app shell
4. build screen skeletons
5. connect APIs in priority order
6. refine responsiveness and interaction quality

This order reduces rework and keeps the visual system coherent.

## 3. Product Priority

The frontend should reflect backend priorities.

Primary product flows:

- authentication
- profiles
- follows
- posts
- DMs

Differentiating product flows:

- clone management
- voice management
- clone-related interaction

Therefore, implementation priority should be:

1. Auth
2. Home
3. Messages
4. Profile
5. People
6. Studio

Studio is a major brand differentiator, but the social and messaging foundation must be stable first.

## 4. Phase Plan

## Phase 0. Baseline Assessment

### Goal

Understand the current frontend template and prepare for replacement.

### Tasks

- inspect current `src` files
- confirm build and dev server work
- identify what from the starter template should be removed

### Deliverable

- clear understanding of the starting point

### Exit Criteria

- current frontend builds successfully
- no uncertainty remains about the template code

## Phase 1. Project Restructure

### Goal

Prepare the codebase for scalable development.

### Tasks

- create the directory structure described in `STRUCTURE.md`
- move away from the starter-file layout
- create empty or minimal route entry files
- create app-level folders for layout and router setup
- create style foundation files

### Suggested Output

```text
src/
  app/
  pages/
  features/
  components/
  lib/
  hooks/
  styles/
```

### Deliverable

- clean project skeleton aligned with the planned architecture

### Exit Criteria

- all key folders exist
- the app still builds
- the default Vite starter UI is removed

## Phase 2. Design Foundation

### Goal

Translate the visual plan from `DESIGN.md` into implementation primitives.

### Tasks

- define CSS variables in `styles/tokens.css`
- create `globals.css`
- define color, spacing, radius, shadow, blur, motion tokens
- load the chosen font
- define body background and typography defaults

### Deliverable

- reusable design token layer

### Exit Criteria

- app has the Soft Futurism visual base
- tokens can be reused consistently by components and pages

## Phase 3. UI Primitive Components

### Goal

Build the smallest reusable UI set needed for all major screens.

### Tasks

- create `Button`
- create `Card`
- create `Panel`
- create `Input`
- create `Textarea`
- create `Chip` or `StatusBadge`
- create `Avatar`
- create `EmptyState`

### Notes

These should be presentational and generic.
Avoid feature logic in these components.

### Deliverable

- reusable UI primitives

### Exit Criteria

- multiple pages could be composed using only these primitives
- visual consistency is visible across components

## Phase 4. App Shell and Routing

### Goal

Build the structural skeleton of the app.

### Tasks

- create router setup
- create authenticated and unauthenticated layouts
- build desktop app shell
- build responsive mobile shell
- create sidebar navigation
- create top bar and contextual panel regions

### Key Layout Targets

Desktop:

- left sidebar
- main content
- optional right context panel

Mobile:

- top bar
- content area
- bottom tab bar

### Deliverable

- navigable shell with placeholder pages

### Exit Criteria

- app navigation works
- route changes render correct page containers
- shell is responsive at a basic level

## Phase 5. Page Skeletons

### Goal

Build all key screens as visual skeletons before API integration.

### Tasks

- `AuthPage`
- `HomePage`
- `MessagesPage`
- `PeoplePage`
- `ProfilePage`
- `StudioPage`

### Implementation Rules

- use realistic placeholder content
- respect final layout and spacing decisions
- do not wait for backend integration to shape the screen

### Deliverable

- complete front-end visual walkthrough using mock data

### Exit Criteria

- all MVP pages exist
- a user can click through the product and understand its structure
- screens already feel close to final in layout and tone

## Phase 6. Authentication Integration

### Goal

Make the app usable with real sessions.

### Backend APIs

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `DELETE /api/auth/me`

### Tasks

- build shared API client
- implement auth feature API functions
- store access token
- restore auth state on app load
- create route protection strategy
- implement logout and account removal entry points as appropriate

### Deliverable

- real login and signup flow

### Exit Criteria

- user can sign up
- user can log in
- protected pages require auth
- session restore works on refresh if supported by chosen token strategy

## Phase 7. Home and Feed Integration

### Goal

Connect the social center of the app.

### Backend APIs

- `POST /api/posts`
- `GET /api/posts/feed`
- `GET /api/profiles/me/posts`
- `GET /api/profiles/{profileUserId}/posts`

### Tasks

- build feed query functions
- render real feed items
- connect post composer
- support my profile posts
- support other profile post lists where needed

### Deliverable

- live feed experience

### Exit Criteria

- feed loads real data
- post creation works
- profile posts can be displayed

## Phase 8. Messaging Integration

### Goal

Implement the messaging experience, including support for voice-oriented UI.

### Backend APIs

- `POST /api/dms/threads`
- `GET /api/dms/threads`
- `GET /api/dms/threads/{threadId}`
- `POST /api/dms/threads/{threadId}/messages`
- `POST /api/dms/threads/{threadId}/voice-messages`

### Tasks

- render thread list
- render active conversation
- send text messages
- design and connect voice message entry points
- handle empty conversations and missing thread selection

### Deliverable

- working DM inbox and message thread view

### Exit Criteria

- thread list loads
- conversation opens
- text message send works
- voice message UI has a defined integration path

## Phase 9. Profile and People Integration

### Goal

Support identity management and discovery flows.

### Backend APIs

- `GET /api/profiles/me`
- `PATCH /api/profiles/me`
- `PATCH /api/profiles/me/visibility`
- `GET /api/profiles/me/auto-reply`
- `PATCH /api/profiles/me/auto-reply`
- `GET /api/profiles/{profileUserId}`
- `GET /api/follows`
- `GET /api/follows/users/search`
- `POST /api/follows/{targetUserId}`
- `DELETE /api/follows/{targetUserId}`

### Tasks

- render my profile summary
- support display name editing
- support visibility change
- display auto-reply status
- implement people search
- implement follow and unfollow
- support viewing other users

### Deliverable

- profile and discovery workflows

### Exit Criteria

- my profile is editable
- people search returns usable results
- follow state updates correctly

## Phase 10. Studio Integration

### Goal

Connect the product’s AI persona workspace.

### Backend APIs

- `POST /api/system-prompt`
- `GET /api/clones`
- `PATCH /api/clones/{cloneId}/visibility`
- `POST /api/voices`
- `GET /api/voices`
- `PATCH /api/voices/{registeredVoiceId}/visibility`

### Optional Later APIs

- clone test chat
- debate entry points

### Tasks

- render clone state
- support clone prompt editing
- support clone visibility change
- render voice registration state
- support voice upload or replacement
- support voice visibility change
- provide a clean status overview in Studio

### Deliverable

- functional Studio workspace

### Exit Criteria

- user can inspect clone and voice status
- user can update clone prompt
- user can manage visibility settings

## Phase 11. State Completeness

### Goal

Add the UI states that make the app feel complete and reliable.

### Tasks

- loading skeletons
- empty states
- error states
- auth expiry handling
- private account states
- permission-denied states
- no messages yet states
- no search results states

### Deliverable

- complete state coverage across key flows

### Exit Criteria

- every primary screen handles success, loading, and failure states
- app does not fall back to unstyled error dumps

## Phase 12. Responsive Refinement

### Goal

Make the experience work smoothly across desktop and mobile.

### Tasks

- refine mobile app shell
- refine bottom tab navigation
- convert three-column screens to stacked flows where needed
- optimize Messages for mobile
- optimize Studio cards for smaller widths
- ensure paddings and touch areas are comfortable

### Deliverable

- polished responsive UI

### Exit Criteria

- major screens are fully usable on mobile
- no critical overflow or cramped layouts remain

## Phase 13. Motion and Polish

### Goal

Bring the interface to a near-product level of finish.

### Tasks

- add transitions for panels and cards
- refine hover, active, and focus states
- improve visual rhythm
- tune hierarchy and spacing
- ensure accent usage is consistent

### Deliverable

- cohesive, refined frontend experience

### Exit Criteria

- app feels intentional and responsive
- motion enhances clarity without distraction

## Phase 14. Verification and Hardening

### Goal

Make sure the frontend works against the backend in realistic flows.

### Required Verification Flows

- signup
- login
- auth restore
- home feed load
- create post
- search people
- follow and unfollow
- open DM thread
- send message
- view and edit profile
- inspect Studio status

### Additional Checks

- mobile layout smoke test
- keyboard navigation smoke test
- empty and error states review

### Deliverable

- validated MVP frontend

### Exit Criteria

- all core flows complete without blocking issues
- major layout and state regressions are resolved

## 5. Implementation Order Summary

Recommended working order:

1. restructure the project
2. add tokens and global styles
3. build core UI primitives
4. build app shell and routing
5. build page skeletons with mock data
6. integrate authentication
7. integrate Home feed
8. integrate Messages
9. integrate Profile and People
10. integrate Studio
11. add state coverage
12. refine responsiveness
13. add motion and polish
14. verify against the backend

## 6. Suggested Deliverables by Milestone

## Milestone A. Foundation

Includes:

- structure
- design tokens
- app shell
- reusable primitives

Result:

- frontend is no longer a template

## Milestone B. Visual MVP

Includes:

- all page skeletons
- realistic placeholder content
- responsive shell baseline

Result:

- frontend is fully explorable without backend wiring

## Milestone C. Functional MVP

Includes:

- auth
- feed
- messages
- profile
- people
- studio

Result:

- primary user journeys work with real backend data

## Milestone D. Product Polish

Includes:

- loading and empty states
- responsive refinement
- interaction polish
- verification

Result:

- frontend feels close to a usable product

## 7. Risks and Notes

## Risk 1. Building API-first Too Early

If implementation starts with backend wiring before the app shell and component system exist, the UI may become inconsistent and harder to refactor.

Mitigation:

- complete shell and visual skeletons before deep integration

## Risk 2. Overbuilding the Design System

If too much time is spent abstracting components before real screens exist, progress may slow.

Mitigation:

- build only the primitives needed for the current pages

## Risk 3. DM and Studio Complexity

Messages and Studio are likely to become the most interaction-heavy areas.

Mitigation:

- build them early as skeletons
- connect them in separate phases

## Risk 4. Mobile Degradation

A desktop-first implementation can lead to awkward mobile behavior if responsive planning is delayed too long.

Mitigation:

- validate mobile structure by the time shell and page skeletons are complete

## 8. Recommended Immediate Next Step

The best next implementation step is:

1. restructure `src`
2. create `tokens.css` and `globals.css`
3. replace the Vite starter screen with the base app shell

That sequence creates the right foundation for every later phase.

## 9. Final Summary

The frontend should be implemented in layers:

- structure
- visual system
- shell
- screens
- backend integration
- state completeness
- polish

This approach will produce a frontend that is visually coherent, technically maintainable, and well aligned with both the backend API and the Soft Futurism product direction.
