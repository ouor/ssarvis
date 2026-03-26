# SSARVIS v0.1 Release Notes

## Overview

SSARVIS v0.1 is the first MVP release of a full-stack clone conversation platform.
Users can create personality-based clones from questionnaire answers, chat with those clones, and let two clones discuss a chosen topic with optional voice output.

## Highlights

- Questionnaire-based clone creation
  - Generates a user-simulation `systemPrompt` from structured answers
  - Also generates a short clone alias and description
- 1:1 clone chat
  - Continues conversation with recent history context
  - Supports optional TTS using registered custom voices
  - Streams text and PCM audio in real time
- Clone debate
  - Starts a live discussion between two clones on a user-provided topic
  - Includes the topic in every debate generation context
  - Does not force fixed pro/con roles; each clone responds according to its personality and conversation flow
- Voice registration
  - Upload a sample voice and register it for TTS
  - Separates user-facing display name from internal provider name
- Audio pipeline
  - Real-time PCM playback on the frontend
  - Server-side WAV handling, optional ffmpeg encoding, and optional S3-compatible storage
- Speech input
  - Browser-based voice input with silence detection support

## Included in v0.1

### Frontend

- React 19 + TypeScript + Vite application
- Clone creation flow
- Clone list and action modals
- Live chat UI
- Live debate UI
- Real-time PCM playback support
- Web Speech API based input support

### Backend

- Spring Boot REST API
- OpenAI chat completion integration
- DashScope voice registration and realtime TTS integration
- MySQL persistence with JPA
- Optional S3-compatible audio asset storage
- NDJSON streaming endpoints for chat and debate

## Technical Notes

- Debate context now always includes the selected topic.
- Debate generation no longer assigns fixed `찬성/반대` roles to clones.
- OpenAI request handling is shared through a common backend client layer.
- Debate stop API was removed; debate ends when the client stops requesting the next turn.
- Frontend audio playback is stopped when the page is actually left.

## Known Limitations

- Web Speech API behavior varies by browser and is most reliable on Chromium-based browsers.
- Debate quality depends heavily on the generated clone prompt quality and the selected topic.
- Old voices registered for an incompatible DashScope TTS model may need to be re-registered.
- Integration tests depend on real external services and local environment configuration.
- No separate license file is included in this repository yet.

## Runtime Requirements

- Java 17
- Node.js 20+
- MySQL
- ffmpeg
- OpenAI API key
- DashScope API key
- Optional S3-compatible object storage

## Recommended Usage

- Run backend at `http://localhost:8080`
- Run frontend at `http://localhost:5173`
- Prepare `backend/.env` before running the backend locally

## Next Focus Areas

- Improve debate prompt quality and consistency
- Reduce noisy streaming disconnect logs on client abort
- Expand automated test coverage beyond controller-focused tests
- Improve production deployment and release packaging readiness
