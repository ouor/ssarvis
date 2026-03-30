# Frontend Design Plan

## 1. Overview

This document defines the visual and structural design plan for the `frontend` app that consumes the Spring Boot backend in `../backend`.

The product is not a generic social feed. It combines:

- social identity
- profile discovery and follow relationships
- human-to-human direct messages
- AI clone and voice workspace

The design direction is therefore:

`Soft Futurism`

This means:

- bright and breathable instead of dark cyberpunk
- futuristic, but not cold
- human-centered first, AI-enhanced second
- clean layered surfaces, gentle translucency, and subtle motion

The product feeling should be:

- trustworthy
- calm
- expressive
- slightly experimental

The app should feel like a place where a user manages both their social presence and their AI persona.

## 2. Product Framing

### Core Product Identity

The frontend should present the service as:

`A social network where people, conversations, and AI personas coexist.`

The main user-facing value should be:

- share updates
- discover people
- message directly
- configure an AI clone and voice
- interact with clone-related features inside a dedicated workspace

### UX Priority

The backend API spec indicates the main product flow is centered around:

- auth
- profiles
- follows
- posts
- DMs

The clone, voice, chat, and debate features are important differentiators, but should not dominate the initial navigation.

So the frontend must position features in this order:

1. social app first
2. communication app second
3. persona studio third

That balance keeps the app understandable while still highlighting its unique capabilities.

## 3. Design Principles

### 1. Human Before Machine

Profiles, names, conversations, and relationships should feel primary.
AI-related functionality should feel like a powerful extension of a person, not a replacement for them.

### 2. Calm Precision

Interfaces should be clean and crisp, but not sterile.
Use clear spacing, restrained color accents, and deliberate information grouping.

### 3. Layered Lightness

Surfaces should feel airy and slightly elevated.
Favor subtle translucent panels, soft gradients, and thin borders over heavy cards and thick outlines.

### 4. Focused Futurism

Future-facing details should appear in moments where they matter:

- Studio
- voice playback
- clone status
- AI conversation utilities

Avoid making every screen look like a sci-fi control panel.

### 5. Mobile Practicality

Even with a polished visual direction, the app must remain easy to use on mobile.
DM, feed scrolling, and profile access must stay simple and direct.

## 4. Information Architecture

## Primary Navigation

Desktop:

- Home
- Messages
- Studio
- People
- Profile

Mobile:

- Home
- Messages
- Studio
- Profile

`People` can be accessed from search within Home or through a secondary navigation pattern if tab count must stay low.

## Entry Flow

Recommended app entry:

- unauthenticated users land on Auth
- authenticated users land on Home

This keeps onboarding simple and reflects the backend priority around social and messaging workflows.

## MVP Scope

The first meaningful frontend milestone should include:

- login
- signup
- home feed
- people discovery
- DM inbox and thread view
- profile view and profile settings
- studio overview for clone and voice management

Not required for the first visual milestone:

- exhaustive debate workflows
- edge-case legacy workspace screens
- advanced admin tooling

## 5. Visual Direction

## Mood

The visual language should combine:

- clear daylight
- soft glass-like surfaces
- muted editorial calm
- subtle technical sophistication

Think:

- frosted social dashboard
- premium messaging app
- lightweight AI workspace

Avoid:

- heavy neon cyberpunk
- monochrome enterprise dashboard
- purple-on-white startup template
- generic social app clone

## Background System

The app background should not be a flat color.

Use a layered combination of:

- light fog gray base
- soft blue-gray radial gradients
- very subtle noise texture or grain
- large blurred orbital shapes in strategic screens

Recommended behavior:

- global app shell uses a very soft ambient background
- Studio can increase contrast slightly with more visible gradient and light effects
- Auth can use the most atmospheric version of the system

## Surface Language

Surfaces should feel elevated without becoming flashy.

Use:

- semi-opaque white surfaces
- 1px hairline borders
- gentle background blur
- long soft shadows with cool undertones

Avoid:

- thick borders
- high-contrast drop shadows
- overdone glassmorphism
- overly rounded toy-like UI

## 6. Color System

## Design Intent

The palette should support:

- warmth for people and content
- cool precision for AI and system controls
- readability for long usage sessions

## Core Tokens

Recommended base tokens:

```css
:root {
  --bg: #f3f6f8;
  --bg-muted: #eef3f6;
  --bg-orbit: rgba(73, 198, 217, 0.12);

  --surface: rgba(255, 255, 255, 0.72);
  --surface-strong: rgba(255, 255, 255, 0.9);
  --surface-solid: #fbfdff;

  --ink: #14212b;
  --ink-soft: #304250;
  --muted: #5f6f7c;
  --muted-strong: #465763;

  --line: rgba(20, 33, 43, 0.1);
  --line-strong: rgba(20, 33, 43, 0.16);

  --accent: #49c6d9;
  --accent-strong: #1ea7bd;
  --accent-tint: rgba(73, 198, 217, 0.14);

  --warm: #ff8a5b;
  --warm-strong: #e46d3c;
  --warm-tint: rgba(255, 138, 91, 0.14);

  --success: #31c48d;
  --warning: #f2b24f;
  --danger: #e76f6f;

  --shadow-soft: 0 16px 40px rgba(32, 61, 84, 0.08);
  --shadow-float: 0 28px 80px rgba(32, 61, 84, 0.12);
}
```

## Semantic Use

- `accent`: primary interactive color, links, selected nav, active states
- `warm`: human touchpoints, special callouts, writing actions, welcome areas
- `success`: active status, configured voice/clone state
- `warning`: cautionary settings
- `danger`: destructive account actions

## Screen Usage Guidance

- Home: more neutral, slightly warm
- Messages: neutral with accent highlights
- Studio: more accent-driven, slightly more technical
- Profile: warm and personal
- Auth: atmospheric and balanced

## 7. Typography

## Font Recommendations

Use one of:

- `Manrope`
- `Plus Jakarta Sans`
- `Sora`

Recommended default:

`Manrope`

Reason:

- modern and clean
- soft enough for social UI
- structured enough for AI and system areas

## Type Roles

```text
Display: 40/48, 700
H1: 32/40, 700
H2: 24/32, 700
H3: 20/28, 650
Body L: 17/28, 500
Body M: 15/24, 500
Body S: 13/20, 500
Label: 12/16, 700
```

## Typography Rules

- avoid large blocks of centered copy except on Auth empty space
- use strong headings sparingly
- timestamps, counters, and metadata should be quieter than primary content
- enable tabular numerals for times, counts, and durations where possible

## 8. Spacing, Radius, and Depth

## Spacing Scale

Recommended spacing scale:

- 4
- 8
- 12
- 16
- 20
- 24
- 32
- 40
- 48
- 64

## Radius Scale

Recommended radius tokens:

- `--radius-sm: 12px`
- `--radius-md: 16px`
- `--radius-lg: 20px`
- `--radius-xl: 24px`
- `--radius-pill: 999px`

Suggested use:

- inputs: 16px
- cards: 20px
- main panels: 24px
- chips and badges: pill

## Depth Model

Use depth to indicate interaction hierarchy:

- base background
- app shell panel
- card surface
- floating input or composer
- modal or drawer

Depth should come from:

- translucency
- soft shadows
- blur
- border contrast

Not from:

- harsh shadow stacking
- dark overlays everywhere

## 9. Motion System

Motion should be soft, controlled, and low-stress.

## Principles

- no bouncy consumer-app motion
- no aggressive sci-fi transitions
- movement should reinforce hierarchy and focus

## Recommended Motion Patterns

- page entry: fade + 8px upward movement
- card reveal: small opacity and translate transition
- active nav change: sliding highlight or soft background fill
- drawers and sheets: blurred backdrop + gentle rise
- audio interactions: subtle pulse or waveform shimmer

## Timing

- fast micro-interactions: `120ms - 180ms`
- panel transitions: `220ms - 280ms`
- page changes: `240ms - 320ms`

## Easing

Prefer:

- `cubic-bezier(0.22, 1, 0.36, 1)`

This gives soft but decisive movement.

## 10. App Shell Layout

## Desktop

Recommended shell:

- left sidebar
- center content column
- right contextual panel

### Left Sidebar

Contains:

- logo or wordmark
- primary navigation
- profile mini card
- quick compose or quick action button

Should feel fixed and stable.
Width recommendation:

- `248px` to `280px`

### Main Content

Contains:

- page title and subheader
- scrollable primary content
- page-specific composition tools

Width recommendation:

- flexible
- ideal content width for feed: `680px - 760px`
- inbox message canvas can expand wider

### Right Panel

Contains contextual support information:

- selected profile summary
- relationship status
- clone and voice status
- shortcuts and secondary actions

Width recommendation:

- `300px` to `360px`

Use this panel only when it adds value. It should not feel mandatory on every mobile-sized desktop window.

## Tablet

Collapse right panel first.
Keep left sidebar, but reduce padding and label density.

## Mobile

Use:

- top app bar
- content area
- bottom tab bar

Messages should use a master/detail flow:

- thread list
- thread detail

Studio should use stacked cards rather than two-column layouts.

## 11. Screen Plans

## 11.1 Auth

### Goal

Make login and signup feel polished, easy, and product-aware without building a marketing-heavy landing page.

### Layout

- full-height screen
- atmospheric background
- centered auth card or split layout

Recommended split:

- left area: product statement, subtle shapes, supporting text
- right area: login/signup panel

On mobile:

- collapse into one centered stack

### Content

Login:

- username
- password
- primary CTA
- link to signup

Signup:

- username
- password
- display name
- primary CTA
- link to login

### Visual Notes

- use a larger heading than other screens
- add one concise product sentence
- keep forms extremely readable
- do not clutter with unnecessary onboarding steps in v1

## 11.2 Home

### Goal

Home should feel like the social center of the product.

### Structure

- top header
- quick post composer
- feed list
- right panel summary

### Header

Include:

- page title
- one-line mood or contextual message
- optional search shortcut

### Composer

The composer should be visually prominent but light.

Include:

- avatar
- text prompt
- create post CTA

Optional future additions:

- voice attachment entry
- prompt to continue in Studio

### Feed Cards

Each card should show:

- avatar
- display name
- username
- timestamp
- content
- optional media or voice badge

Card tone:

- personal
- legible
- spacious

Avoid overpacking actions beneath every post in MVP unless supported by backend APIs.

### Right Panel on Home

Useful content:

- my profile snapshot
- visibility status
- auto-reply status
- clone configured or not
- voice configured or not
- suggested people

## 11.3 Messages

### Goal

This should be one of the strongest screens in the app.

### Structure

Desktop:

- thread list
- active conversation
- context panel

### Thread List

Each row should include:

- avatar
- display name
- latest message preview
- timestamp
- unread indicator if available later

### Conversation View

Must support:

- text messages
- voice messages

### Message Styles

Text message:

- standard bubble with restrained contrast

Voice message:

- compact audio card
- play control
- waveform strip
- duration label

The voice message UI is a brand opportunity.
It should feel elegant and slightly futuristic, not like a generic audio player.

### Composer

Should include:

- text input
- send action
- voice upload or record action when implemented

### Empty and Loading States

- no conversation selected
- no messages yet
- private account or permission issue

These states should still feel designed, not default text dumps.

## 11.4 People

### Goal

Help users discover and follow others without making the screen feel transactional.

### Structure

- search bar
- result list
- optional featured people section

### Result Card

Show:

- avatar
- display name
- username
- visibility
- follow status
- CTA

Private accounts should show a clear lock indicator.

### Interaction Tone

Search should feel soft and immediate, not enterprise-table-like.

## 11.5 Profile

### Goal

Make profile screens feel personal and calm.

### Header Area

Include:

- avatar
- display name
- username
- visibility badge
- follow state or edit state

### Sections

Recommended tabs:

- Posts
- Info
- Settings

For my profile:

- edit display name
- change visibility
- manage auto-reply
- account actions

For other users:

- follow or unfollow
- start DM when permitted

### Tone

Profile should feel less technical than Studio.
This is where the app should feel most human.

## 11.6 Studio

### Goal

Studio is where the product's distinctive identity becomes explicit.

### Role

Studio is not a separate product.
It is a focused workspace inside the social app for managing clone and voice assets.

### Layout

Recommended:

- page header
- two-column management area on desktop
- stacked cards on mobile

### Header

Include:

- title
- concise description
- high-level status chips

Example statuses:

- Clone ready
- Voice missing
- Private
- Public

### Main Cards

#### Clone Card

Include:

- current prompt summary
- edit action
- visibility badge
- last updated if available

#### Voice Card

Include:

- registration state
- upload or replace action
- preview action
- visibility badge

#### Test Panel

Optional first MVP if time allows:

- send a short test message
- preview clone output

#### Debate Entry

Can be presented as a secondary action card.

### Visual Tone

Studio may use:

- more visible accent lines
- more status chips
- slightly stronger gradients

But it must remain visually related to the rest of the app.

## 12. Component System

The UI should be built from a small, reusable component set.

## Core Components

- `AppShell`
- `SidebarNav`
- `TopBar`
- `ContextPanel`
- `Card`
- `GlassPanel`
- `Button`
- `IconButton`
- `TextInput`
- `Textarea`
- `SearchInput`
- `Chip`
- `StatusBadge`
- `Avatar`
- `EmptyState`
- `Composer`
- `MessageBubble`
- `VoiceMessageCard`
- `ProfileCard`
- `SectionHeader`

## Component Rules

### Card

- default surface for feed items, profile blocks, and studio sections
- should support muted, elevated, and highlighted variants

### Button

Recommended variants:

- primary
- secondary
- ghost
- danger

Primary should use `accent`, not a generic blue.

### Chip and StatusBadge

Use for:

- visibility
- configured states
- active modes
- warnings

Chips should be clean and compact, never noisy.

### Avatar

Should support:

- image
- initial fallback
- status dot

### VoiceMessageCard

One of the key branded components.
Should include:

- play state
- waveform
- duration
- sender alignment

## 13. Responsive Strategy

The frontend must be intentionally responsive, not merely collapsed.

## Desktop

- full 3-panel app shell when space allows
- richer context visible

## Tablet

- keep left nav
- hide or compress right panel
- avoid tiny three-column layouts

## Mobile

- bottom tab navigation
- one primary task visible at a time
- DM becomes stacked navigation
- Studio cards become full-width blocks

## Touch Considerations

- large tap areas
- pill buttons for key actions
- stable bottom safe-area spacing

## 14. Accessibility

Accessibility should be built into the system from the start.

## Requirements

- sufficient contrast for text and interactive controls
- visible focus states
- semantic heading structure
- labels for inputs and icon buttons
- keyboard navigation for desktop
- clear success and error states

## Design Notes

- translucency should not reduce readability
- placeholder text must not carry all meaning
- status should not rely on color alone

## 15. Suggested Frontend Architecture Support

The design system should be easy to implement in the current Vite + React app.

Recommended structure:

- `src/app`
- `src/pages`
- `src/components`
- `src/features`
- `src/lib`
- `src/styles`

Suggested style layering:

- `tokens.css`
- `globals.css`
- component-level CSS modules or scoped CSS strategy

If the app stays lightweight, a simple structure is enough.
If the app expands, feature-based folders will scale better.

## 16. Implementation Sequence

Recommended implementation order:

1. Define design tokens in CSS
2. Replace the default Vite starter screen with the app shell
3. Build base layout primitives
4. Implement Home visual skeleton
5. Implement Messages visual skeleton
6. Implement Studio visual skeleton
7. Add Auth and Profile screens
8. Add People search screen
9. Refine responsive behavior
10. Add motion, empty states, and polish

## 17. Non-Goals

The first iteration should not aim for:

- pixel-perfect cloning of another social app
- excessive visual effects
- full design-system abstraction before real screens exist
- overcomplicated animation libraries

The product should become distinctive through:

- composition
- tone
- reusable component quality
- careful contrast between social and studio layers

## 18. Final Direction Summary

The frontend should feel like:

- a premium social app in daylight
- with a built-in AI persona workspace
- designed for calm, trust, and expressive communication

The design should present:

- Home as the social center
- Messages as the most functional communication space
- Studio as the differentiated identity layer
- Profile as the personal anchor

If implementation follows this document well, the result should feel more original than a generic React dashboard while still being practical to build on top of the current backend.
