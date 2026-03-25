class PcmStreamPlayer {
  private readonly audioContext: AudioContext
  private readonly sampleRate: number
  private readonly chunks: Uint8Array[] = []
  private nextStartTime = 0
  private activeSources = 0
  private streamFinished = false
  private finishResolver: (() => void) | null = null
  private finishPromise: Promise<void>

  constructor(sampleRate = 24000) {
    this.sampleRate = sampleRate
    this.audioContext = new AudioContext()
    this.finishPromise = new Promise<void>((resolve) => {
      this.finishResolver = resolve
    })
  }

  async appendBase64Chunk(base64Chunk: string) {
    if (this.audioContext.state === 'suspended') {
      await this.audioContext.resume()
    }

    const bytes = this.decodeBase64(base64Chunk)
    this.chunks.push(bytes)

    const pcm = new Int16Array(bytes.buffer, bytes.byteOffset, Math.floor(bytes.byteLength / 2))
    const audioBuffer = this.audioContext.createBuffer(1, pcm.length, this.sampleRate)
    const channelData = audioBuffer.getChannelData(0)

    for (let index = 0; index < pcm.length; index += 1) {
      channelData[index] = pcm[index] / 32768
    }

    const source = this.audioContext.createBufferSource()
    source.buffer = audioBuffer
    source.connect(this.audioContext.destination)

    this.activeSources += 1
    source.onended = () => {
      this.activeSources -= 1
      if (this.streamFinished && this.activeSources === 0) {
        this.finishResolver?.()
      }
    }

    const startAt = Math.max(this.audioContext.currentTime, this.nextStartTime)
    source.start(startAt)
    this.nextStartTime = startAt + audioBuffer.duration
  }

  finish() {
    this.streamFinished = true
    if (this.activeSources === 0) {
      this.finishResolver?.()
    }
    return this.finishPromise
  }

  async dispose() {
    this.streamFinished = true
    this.finishResolver?.()
    await this.audioContext.close()
  }

  buildWavUrl() {
    if (this.chunks.length === 0) {
      return undefined
    }

    const pcmBytes = this.concatChunks()
    const wavBytes = this.wrapPcmAsWav(pcmBytes)
    const blob = new Blob([wavBytes], { type: 'audio/wav' })
    return URL.createObjectURL(blob)
  }

  private concatChunks() {
    const totalLength = this.chunks.reduce((sum, chunk) => sum + chunk.byteLength, 0)
    const merged = new Uint8Array(totalLength)
    let offset = 0

    for (const chunk of this.chunks) {
      merged.set(chunk, offset)
      offset += chunk.byteLength
    }

    return merged
  }

  private wrapPcmAsWav(pcmBytes: Uint8Array) {
    const headerSize = 44
    const wav = new Uint8Array(headerSize + pcmBytes.byteLength)
    const view = new DataView(wav.buffer)

    this.writeAscii(wav, 0, 'RIFF')
    view.setUint32(4, 36 + pcmBytes.byteLength, true)
    this.writeAscii(wav, 8, 'WAVE')
    this.writeAscii(wav, 12, 'fmt ')
    view.setUint32(16, 16, true)
    view.setUint16(20, 1, true)
    view.setUint16(22, 1, true)
    view.setUint32(24, this.sampleRate, true)
    view.setUint32(28, this.sampleRate * 2, true)
    view.setUint16(32, 2, true)
    view.setUint16(34, 16, true)
    this.writeAscii(wav, 36, 'data')
    view.setUint32(40, pcmBytes.byteLength, true)
    wav.set(pcmBytes, headerSize)

    return wav
  }

  private writeAscii(target: Uint8Array, offset: number, value: string) {
    for (let index = 0; index < value.length; index += 1) {
      target[offset + index] = value.charCodeAt(index)
    }
  }

  private decodeBase64(base64: string) {
    const binary = window.atob(base64)
    const bytes = new Uint8Array(binary.length)
    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index)
    }
    return bytes
  }
}

export default PcmStreamPlayer
