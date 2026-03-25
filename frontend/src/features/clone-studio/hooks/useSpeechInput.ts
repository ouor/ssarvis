import { useEffect, useRef, useState } from 'react'

const SILENCE_THRESHOLD = 0.018
const SILENCE_DURATION_MS = 1400
const RESTART_DELAY_MS = 250

type UseSpeechInputOptions = {
  getInput: () => string
  onInputChange: (value: string) => void
}

export function useSpeechInput({ getInput, onInputChange }: UseSpeechInputOptions) {
  const [listening, setListening] = useState(false)
  const [error, setError] = useState('')

  const recognitionRef = useRef<SpeechRecognition | null>(null)
  const restartTimeoutRef = useRef<number | null>(null)
  const sessionIdRef = useRef(0)
  const shouldKeepListeningRef = useRef(false)
  const baseInputRef = useRef('')
  const finalTranscriptRef = useRef('')

  const audioContextRef = useRef<AudioContext | null>(null)
  const mediaStreamRef = useRef<MediaStream | null>(null)
  const analyserRef = useRef<AnalyserNode | null>(null)
  const analysisFrameRef = useRef<number | null>(null)
  const heardSpeechRef = useRef(false)
  const silenceStartedAtRef = useRef<number | null>(null)

  const supported = typeof window !== 'undefined' && Boolean(window.SpeechRecognition ?? window.webkitSpeechRecognition)

  useEffect(() => {
    return () => {
      void stop()
    }
  }, [])

  async function toggle() {
    if (shouldKeepListeningRef.current || listening) {
      await stop()
      return
    }

    await start()
  }

  async function start() {
    if (!supported) {
      setError('이 브라우저는 Web Speech API 음성 인식을 지원하지 않습니다.')
      return
    }

    await stop()

    shouldKeepListeningRef.current = true
    sessionIdRef.current += 1
    baseInputRef.current = getInput()
    finalTranscriptRef.current = ''
    heardSpeechRef.current = false
    silenceStartedAtRef.current = null
    setError('')

    const sessionId = sessionIdRef.current

    try {
      await startSilenceMonitoring(sessionId)
      startRecognitionCycle(sessionId)
    } catch (caughtError) {
      shouldKeepListeningRef.current = false
      await stopSilenceMonitoring()
      setListening(false)
      setError(caughtError instanceof Error ? caughtError.message : '음성 인식을 시작하지 못했습니다.')
    }
  }

  async function stop() {
    shouldKeepListeningRef.current = false
    clearRestartTimer()

    const recognition = recognitionRef.current
    recognitionRef.current = null

    if (recognition) {
      recognition.onstart = null
      recognition.onresult = null
      recognition.onerror = null
      recognition.onend = null
      try {
        recognition.stop()
      } catch {
        // Ignore stop race conditions from the browser implementation.
      }
    }

    await stopSilenceMonitoring()
    setListening(false)
  }

  function clearError() {
    setError('')
  }

  function startRecognitionCycle(sessionId: number) {
    if (!shouldKeepListeningRef.current || sessionId !== sessionIdRef.current) {
      return
    }

    const RecognitionConstructor = window.SpeechRecognition ?? window.webkitSpeechRecognition
    if (!RecognitionConstructor) {
      shouldKeepListeningRef.current = false
      setListening(false)
      setError('이 브라우저는 Web Speech API 음성 인식을 지원하지 않습니다.')
      return
    }

    const recognition = new RecognitionConstructor()
    recognition.continuous = true
    recognition.interimResults = true
    recognition.lang = navigator.language || 'ko-KR'
    recognitionRef.current = recognition

    recognition.onstart = () => {
      if (sessionId !== sessionIdRef.current) {
        return
      }

      setListening(true)
      setError('')
    }

    recognition.onresult = (event) => {
      if (sessionId !== sessionIdRef.current) {
        return
      }

      let interimTranscript = ''

      for (let index = event.resultIndex; index < event.results.length; index += 1) {
        const transcript = event.results[index][0]?.transcript ?? ''
        if (event.results[index].isFinal) {
          finalTranscriptRef.current = `${finalTranscriptRef.current} ${transcript}`.trim()
        } else {
          interimTranscript += transcript
        }
      }

      const nextValue = [baseInputRef.current.trim(), finalTranscriptRef.current.trim(), interimTranscript.trim()]
        .filter(Boolean)
        .join(' ')
        .trim()

      onInputChange(nextValue)
      setError('')
    }

    recognition.onerror = (event) => {
      if (sessionId !== sessionIdRef.current) {
        return
      }

      if (isFatalSpeechError(event.error)) {
        shouldKeepListeningRef.current = false
        setError(mapSpeechRecognitionError(event.error))
        return
      }

      const message = mapSpeechRecognitionError(event.error)
      if (message) {
        setError(message)
      }
    }

    recognition.onend = () => {
      if (recognitionRef.current === recognition) {
        recognitionRef.current = null
      }

      recognition.onstart = null
      recognition.onresult = null
      recognition.onerror = null
      recognition.onend = null

      if (sessionId !== sessionIdRef.current) {
        return
      }

      if (shouldKeepListeningRef.current) {
        scheduleRestart(sessionId)
        return
      }

      setListening(false)
      void stopSilenceMonitoring()
    }

    try {
      recognition.start()
    } catch {
      scheduleRestart(sessionId)
    }
  }

  function scheduleRestart(sessionId: number) {
    clearRestartTimer()

    restartTimeoutRef.current = window.setTimeout(() => {
      if (!shouldKeepListeningRef.current || sessionId !== sessionIdRef.current) {
        return
      }

      startRecognitionCycle(sessionId)
    }, RESTART_DELAY_MS)
  }

  function clearRestartTimer() {
    if (restartTimeoutRef.current != null) {
      window.clearTimeout(restartTimeoutRef.current)
      restartTimeoutRef.current = null
    }
  }

  async function startSilenceMonitoring(sessionId: number) {
    await stopSilenceMonitoring()

    const stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      },
    })

    const audioContext = new AudioContext()
    const analyser = audioContext.createAnalyser()
    analyser.fftSize = 2048
    analyser.smoothingTimeConstant = 0.85

    const source = audioContext.createMediaStreamSource(stream)
    source.connect(analyser)

    mediaStreamRef.current = stream
    audioContextRef.current = audioContext
    analyserRef.current = analyser
    heardSpeechRef.current = false
    silenceStartedAtRef.current = null

    const sampleBuffer = new Float32Array(analyser.fftSize)

    const tick = () => {
      const currentAnalyser = analyserRef.current
      if (!currentAnalyser || sessionId !== sessionIdRef.current || !shouldKeepListeningRef.current) {
        return
      }

      currentAnalyser.getFloatTimeDomainData(sampleBuffer)
      let sumSquares = 0
      for (let index = 0; index < sampleBuffer.length; index += 1) {
        const sample = sampleBuffer[index]
        sumSquares += sample * sample
      }

      const rms = Math.sqrt(sumSquares / sampleBuffer.length)
      const now = window.performance.now()

      if (rms >= SILENCE_THRESHOLD) {
        heardSpeechRef.current = true
        silenceStartedAtRef.current = null
      } else if (heardSpeechRef.current) {
        if (silenceStartedAtRef.current == null) {
          silenceStartedAtRef.current = now
        } else if (now - silenceStartedAtRef.current >= SILENCE_DURATION_MS) {
          shouldKeepListeningRef.current = false
          clearRestartTimer()

          const recognition = recognitionRef.current
          recognitionRef.current = null
          if (recognition) {
            recognition.onstart = null
            recognition.onresult = null
            recognition.onerror = null
            recognition.onend = null
            try {
              recognition.stop()
            } catch {
              // Ignore stop race conditions from the browser implementation.
            }
          }

          setListening(false)
          void stopSilenceMonitoring()
          return
        }
      }

      analysisFrameRef.current = window.requestAnimationFrame(tick)
    }

    analysisFrameRef.current = window.requestAnimationFrame(tick)
  }

  async function stopSilenceMonitoring() {
    if (analysisFrameRef.current != null) {
      window.cancelAnimationFrame(analysisFrameRef.current)
      analysisFrameRef.current = null
    }

    heardSpeechRef.current = false
    silenceStartedAtRef.current = null

    analyserRef.current?.disconnect()
    analyserRef.current = null

    const stream = mediaStreamRef.current
    if (stream) {
      for (const track of stream.getTracks()) {
        track.stop()
      }
      mediaStreamRef.current = null
    }

    const audioContext = audioContextRef.current
    audioContextRef.current = null
    if (audioContext && audioContext.state !== 'closed') {
      await audioContext.close()
    }
  }

  return {
    supported,
    listening,
    error,
    toggle,
    stop,
    clearError,
  }
}

function mapSpeechRecognitionError(errorCode: string) {
  switch (errorCode) {
    case 'not-allowed':
    case 'service-not-allowed':
      return '마이크 권한이 허용되지 않았습니다.'
    case 'audio-capture':
      return '사용할 수 있는 마이크를 찾지 못했습니다.'
    case 'network':
      return '음성 인식 중 네트워크 오류가 발생했습니다.'
    case 'aborted':
    case 'no-speech':
      return ''
    default:
      return '음성 인식 중 오류가 발생했습니다.'
  }
}

function isFatalSpeechError(errorCode: string) {
  switch (errorCode) {
    case 'not-allowed':
    case 'service-not-allowed':
    case 'audio-capture':
    case 'network':
      return true
    default:
      return false
  }
}
