# Frontend Directory Structure

## 1. Goal

This document defines the recommended directory structure for the `frontend` app.

The goal is to keep the codebase:

- easy to navigate
- aligned with backend domains
- scalable as features grow
- simple enough for an MVP

The backend already has domain-oriented modules such as:

- auth
- profile
- follow
- post
- dm
- clone
- voice

The frontend should mirror that mental model as much as possible.

## 2. Recommended Structure

```text
frontend/
  src/
    app/
      layouts/
      providers/
      router/

    pages/
      auth/
      home/
      messages/
      people/
      profile/
      studio/

    features/
      auth/
      feed/
      post-compose/
      dm/
      people-search/
      profile-settings/
      clone-studio/
      voice-studio/

    components/
      ui/
      shared/

    lib/
      api/
      constants/
      types/
      utils/

    hooks/

    styles/

    assets/

    App.tsx
    main.tsx
```

## 3. Why This Structure

This structure balances three needs:

1. route-level clarity
2. feature-level ownership
3. reusable UI isolation

It avoids two common problems:

- putting everything into one giant `components` folder
- overengineering a design system before real features exist

It also matches the product well because the app naturally breaks into feature areas:

- authentication
- social feed
- direct messaging
- people discovery
- profile management
- AI studio

## 4. Folder Responsibilities

## `src/app`

Use this folder for app-level wiring.

Examples:

- app router setup
- top-level layouts
- providers for auth, theme, query state, or app context
- route guards

This folder should answer:

`How does the application run?`

It should not contain detailed feature UI.

Suggested contents:

```text
src/app/
  layouts/
    AppShell.tsx
    AuthLayout.tsx
  providers/
    AuthProvider.tsx
  router/
    router.tsx
```

## `src/pages`

Use this folder for route-level screen entry points.

Each file here should be thin.
A page should mostly:

- assemble feature components
- read route params
- connect layout sections
- define screen-level composition

Pages should not become giant business-logic files.

Suggested contents:

```text
src/pages/
  auth/
    AuthPage.tsx
  home/
    HomePage.tsx
  messages/
    MessagesPage.tsx
  people/
    PeoplePage.tsx
  profile/
    ProfilePage.tsx
  studio/
    StudioPage.tsx
```

## `src/features`

This is the most important folder.

Use `features` for domain-specific UI and logic.

Each feature should own:

- API calls related to that feature
- feature-specific types
- local hooks
- feature components

This folder should answer:

`How does this product capability work?`

Recommended feature folders:

```text
src/features/
  auth/
  feed/
  post-compose/
  dm/
  people-search/
  profile-settings/
  clone-studio/
  voice-studio/
```

### Example: `src/features/auth`

```text
src/features/auth/
  api.ts
  types.ts
  hooks.ts
  components/
    LoginForm.tsx
    SignupForm.tsx
```

### Example: `src/features/dm`

```text
src/features/dm/
  api.ts
  types.ts
  hooks.ts
  components/
    ThreadList.tsx
    ConversationView.tsx
    MessageComposer.tsx
    VoiceMessageCard.tsx
```

This lets the app scale without turning every cross-cutting change into a scavenger hunt.

## `src/components`

Use this for reusable components that are not tied to a single feature.

Split it into:

- `ui`: primitive building blocks
- `shared`: larger reusable pieces composed from primitives

### `src/components/ui`

Use for low-level presentational building blocks.

Examples:

- Button
- Card
- Input
- Textarea
- Chip
- Avatar
- Panel

These should be reusable across the whole app.
They should not know about auth, posts, or DMs.

### `src/components/shared`

Use for app-wide composed pieces that are still generic enough to reuse.

Examples:

- SidebarNav
- PageHeader
- ContextPanel
- EmptyState
- LoadingBlock

These can depend on `ui` primitives, but should still avoid feature-specific business logic when possible.

## `src/lib`

Use this folder for framework-agnostic support code.

This should contain utilities and shared infrastructure, not screen components.

Recommended subfolders:

```text
src/lib/
  api/
  constants/
  types/
  utils/
```

### `src/lib/api`

Put the shared API client and low-level request helpers here.

Examples:

- fetch wrapper
- auth header injection
- shared request/response handling
- base URL config

Example:

```text
src/lib/api/
  client.ts
  auth.ts
```

Feature-specific endpoint functions should generally live in each feature folder, but they can import the shared client from here.

### `src/lib/constants`

Put fixed values and app-wide config constants here.

Examples:

- route names
- tab IDs
- local storage keys

### `src/lib/types`

Put truly shared TypeScript types here.

Examples:

- common API response shape
- shared visibility enum
- common paging type

Avoid putting every domain type here.
If a type belongs mainly to one feature, keep it in that feature.

### `src/lib/utils`

Put small reusable helper functions here.

Examples:

- class name helpers
- formatting helpers
- date utilities
- string normalization

## `src/hooks`

Use for generic reusable hooks that are not owned by a single feature.

Examples:

- responsive breakpoint hooks
- keyboard shortcut hooks
- mounted-state hooks

If a hook is feature-specific, keep it inside that feature instead.

## `src/styles`

Use for global styling foundations.

Recommended contents:

```text
src/styles/
  tokens.css
  globals.css
```

### `tokens.css`

Put design tokens here:

- color variables
- spacing scale
- radius scale
- shadows
- z-index tokens
- transition timing

### `globals.css`

Put global reset and base styles here:

- font family
- body background
- text color
- selection styles
- scrollbar baseline if desired

Keep feature styling outside this file where possible.

## `src/assets`

Use for static frontend assets.

Examples:

- icons
- illustrations
- local images
- background textures

If a feature gets many dedicated assets, consider a feature-local `assets` folder inside that feature.

## 5. Suggested Concrete Layout

Here is a practical starting structure for this project.

```text
src/
  app/
    layouts/
      AppShell.tsx
      AuthLayout.tsx
    providers/
      AuthProvider.tsx
    router/
      router.tsx

  pages/
    auth/
      AuthPage.tsx
    home/
      HomePage.tsx
    messages/
      MessagesPage.tsx
    people/
      PeoplePage.tsx
    profile/
      ProfilePage.tsx
    studio/
      StudioPage.tsx

  features/
    auth/
      api.ts
      types.ts
      components/
        LoginForm.tsx
        SignupForm.tsx

    feed/
      api.ts
      types.ts
      components/
        FeedList.tsx
        FeedCard.tsx

    post-compose/
      components/
        PostComposer.tsx

    dm/
      api.ts
      types.ts
      components/
        ThreadList.tsx
        ConversationView.tsx
        MessageComposer.tsx
        VoiceMessageCard.tsx

    people-search/
      api.ts
      types.ts
      components/
        PeopleSearchBar.tsx
        PeopleResultList.tsx
        PeopleResultCard.tsx

    profile-settings/
      api.ts
      types.ts
      components/
        ProfileHeader.tsx
        VisibilityToggle.tsx
        AutoReplyCard.tsx

    clone-studio/
      api.ts
      types.ts
      components/
        CloneCard.tsx
        ClonePromptEditor.tsx

    voice-studio/
      api.ts
      types.ts
      components/
        VoiceCard.tsx
        VoiceUploadPanel.tsx

  components/
    ui/
      Button.tsx
      Card.tsx
      Input.tsx
      Textarea.tsx
      Chip.tsx
      Avatar.tsx
      Panel.tsx
    shared/
      SidebarNav.tsx
      PageHeader.tsx
      ContextPanel.tsx
      EmptyState.tsx

  lib/
    api/
      client.ts
    constants/
      routes.ts
    types/
      common.ts
    utils/
      cn.ts
      format.ts

  hooks/
    useBreakpoint.ts

  styles/
    tokens.css
    globals.css

  assets/

  App.tsx
  main.tsx
```

## 6. Import Direction Rules

To keep the structure healthy, use these dependency rules.

### Allowed Direction

- `pages` can import from `features`, `components`, `lib`, `hooks`
- `features` can import from `components`, `lib`, `hooks`
- `components/shared` can import from `components/ui`, `lib`
- `components/ui` can import from `lib`
- `app` can import from everywhere it needs to wire the application

### Avoid

- one feature importing implementation details from another feature unless the dependency is truly intentional
- `components/ui` importing feature code
- `lib` importing React components

If two features need the same thing, move that shared piece into:

- `components/shared`
- `components/ui`
- `lib`

depending on whether it is UI, logic, or infrastructure.

## 7. Naming Conventions

Recommended naming:

- React components: `PascalCase.tsx`
- hooks: `useSomething.ts`
- utility files: `camelCase.ts`
- CSS files: match the component or use scope-based naming

Examples:

- `HomePage.tsx`
- `ConversationView.tsx`
- `useBreakpoint.ts`
- `formatTime.ts`

Feature folder names should use concise domain names.

Prefer:

- `dm`
- `feed`
- `auth`
- `voice-studio`

Avoid vague names like:

- `commonStuff`
- `helpers2`
- `misc`

## 8. Styling Strategy

The structure should support the design plan in `DESIGN.md`.

Recommended styling layers:

1. global tokens in `src/styles/tokens.css`
2. global base styles in `src/styles/globals.css`
3. component-level styles near components

Possible options for component styling:

- plain CSS files next to components
- CSS modules
- a consistent single approach chosen early

For this project, a simple and maintainable option is:

- global tokens and resets in `styles`
- component-local CSS modules for feature and UI components

That keeps styles close to the code without losing a coherent design system.

## 9. What Not To Do

Avoid these anti-patterns:

### One giant `components` folder

This quickly becomes hard to search and maintain.

### Route components with all logic inside

Pages should compose features, not contain every fetch, form, and rendering rule themselves.

### Over-centralizing all types

Shared types should be shared, but domain types should stay near the domain.

### Premature atomic design overuse

Folders like `atoms`, `molecules`, and `organisms` often create more naming debate than clarity in early-stage apps.

### Feature leakage

Do not let `dm` logic spread across unrelated folders unless it truly belongs there.

## 10. Recommended Next Step

After adopting this structure, implement in this order:

1. create `styles`, `app`, `pages`, `features`, `components`, and `lib`
2. add the app shell and routing
3. build reusable UI primitives
4. build `Home`, `Messages`, and `Studio` first
5. connect backend APIs feature by feature

## 11. Final Recommendation

For this frontend, the best approach is:

- route screens in `pages`
- product capabilities in `features`
- reusable UI in `components`
- infrastructure in `lib`
- app bootstrapping in `app`

This structure is a strong fit for the current backend, the planned Soft Futurism UI, and the likely growth of the product.
