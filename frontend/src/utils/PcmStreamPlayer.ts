class PcmStreamPlayer {
  private readonly audioContext: AudioContext
  private readonly chunks: Uint8Array[] = []
  private sampleRate: number
  private channels: number
  private nextStartTime = 0
  private activeSources = 0
  private streamFinished = false
  private trailingByte: number | null = null
  private finishResolver: (() => void) | null = null
  private finishPromise: Promise<void>
  private wavUrl: string | null = null

  constructor(sampleRate = 24000, channels = 1) {
    this.sampleRate = sampleRate
    this.channels = channels
    this.audioContext = new AudioContext()
    this.finishPromise = new Promise<void>((resolve) => {
      this.finishResolver = resolve
    })
  }

  configure(sampleRate: number, channels: number) {
    this.sampleRate = sampleRate > 0 ? sampleRate : this.sampleRate
    this.channels = channels > 0 ? channels : this.channels
  }

  async appendBase64Chunk(base64Chunk: string) {
    if (this.audioContext.state === 'suspended') {
      await this.audioContext.resume()
    }

    const bytes = this.normalizeChunk(this.decodeBase64(base64Chunk))
    if (bytes.byteLength === 0) {
      return
    }

    this.chunks.push(bytes)

    const totalSamples = Math.floor(bytes.byteLength / 2)
    const frameCount = Math.floor(totalSamples / this.channels)
    if (frameCount <= 0) {
      return
    }

    const audioBuffer = this.audioContext.createBuffer(this.channels, frameCount, this.sampleRate)
    const view = new DataView(bytes.buffer, bytes.byteOffset, frameCount * this.channels * 2)

    for (let channel = 0; channel < this.channels; channel += 1) {
      const channelData = audioBuffer.getChannelData(channel)
      for (let frame = 0; frame < frameCount; frame += 1) {
        const sampleIndex = (frame * this.channels + channel) * 2
        channelData[frame] = view.getInt16(sampleIndex, true) / 32768
      }
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
    if (this.wavUrl) {
      URL.revokeObjectURL(this.wavUrl)
    }
    this.wavUrl = URL.createObjectURL(blob)
    return this.wavUrl
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
    view.setUint16(22, this.channels, true)
    view.setUint32(24, this.sampleRate, true)
    view.setUint32(28, this.sampleRate * this.channels * 2, true)
    view.setUint16(32, this.channels * 2, true)
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

  private normalizeChunk(bytes: Uint8Array) {
    if (this.trailingByte === null && bytes.byteLength % 2 === 0) {
      return bytes
    }

    const extraLength = this.trailingByte !== null ? 1 : 0
    const merged = new Uint8Array(extraLength + bytes.byteLength)
    let offset = 0

    if (this.trailingByte !== null) {
      merged[0] = this.trailingByte
      offset = 1
      this.trailingByte = null
    }

    merged.set(bytes, offset)

    if (merged.byteLength % 2 === 1) {
      this.trailingByte = merged[merged.byteLength - 1]
      return merged.slice(0, merged.byteLength - 1)
    }

    return merged
  }
}

export default PcmStreamPlayer
