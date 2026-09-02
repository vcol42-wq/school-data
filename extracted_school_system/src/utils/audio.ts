// Web Audio API Synthesizer for School Bell & Alarm Chime with Multiple Tones

export type BellTone = 'classic_brass' | 'electronic_melody' | 'digital_pulse' | 'gentle_chime' | 'siren_alert';

export class SchoolBellAudio {
  private audioCtx: AudioContext | null = null;
  private masterVolume: number = 0.8;

  private init() {
    if (!this.audioCtx) {
      const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (AudioContextClass) {
        this.audioCtx = new AudioContextClass();
      }
    }
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
  }

  setVolume(vol: number) {
    this.masterVolume = Math.max(0, Math.min(1, vol));
  }

  getVolume(): number {
    return this.masterVolume;
  }

  // Play based on chosen tone
  playTone(tone: BellTone = 'classic_brass') {
    switch (tone) {
      case 'electronic_melody':
        this.playWestminsterMelody();
        break;
      case 'digital_pulse':
        this.playDigitalPulse();
        break;
      case 'gentle_chime':
        this.playGentleChime();
        break;
      case 'siren_alert':
        this.playSirenAlert();
        break;
      case 'classic_brass':
      default:
        this.playClassicBrass();
        break;
    }
  }

  // 1. Classic Brass School Bell (3 rich strikes)
  playClassicBrass() {
    try {
      this.init();
      if (!this.audioCtx) return;

      const now = this.audioCtx.currentTime;
      const strikes = [0, 0.5, 1.0, 1.5];

      strikes.forEach((delay) => {
        if (!this.audioCtx) return;

        const osc1 = this.audioCtx.createOscillator();
        const osc2 = this.audioCtx.createOscillator();
        const gain1 = this.audioCtx.createGain();
        const gain2 = this.audioCtx.createGain();

        osc1.type = 'sine';
        osc1.frequency.setValueAtTime(880, now + delay); // A5

        osc2.type = 'triangle';
        osc2.frequency.setValueAtTime(1760, now + delay); // A6

        const vol = this.masterVolume;
        gain1.gain.setValueAtTime(vol * 0.4, now + delay);
        gain1.gain.exponentialRampToValueAtTime(0.001, now + delay + 1.2);

        gain2.gain.setValueAtTime(vol * 0.2, now + delay);
        gain2.gain.exponentialRampToValueAtTime(0.001, now + delay + 0.8);

        osc1.connect(gain1);
        osc2.connect(gain2);

        gain1.connect(this.audioCtx.destination);
        gain2.connect(this.audioCtx.destination);

        osc1.start(now + delay);
        osc2.start(now + delay);

        osc1.stop(now + delay + 1.3);
        osc2.stop(now + delay + 1.3);
      });
    } catch (e) {
      console.warn('Audio bell error:', e);
    }
  }

  // 2. Electronic Melody (Westminster School Chime 4 Notes: E4 - G#4 - F#4 - B3)
  playWestminsterMelody() {
    try {
      this.init();
      if (!this.audioCtx) return;

      const now = this.audioCtx.currentTime;
      const notes = [
        { freq: 659.25, time: 0.0, dur: 0.5 },   // E5
        { freq: 523.25, time: 0.5, dur: 0.5 },   // C5
        { freq: 587.33, time: 1.0, dur: 0.5 },   // D5
        { freq: 392.00, time: 1.5, dur: 0.8 },   // G4
        { freq: 523.25, time: 2.3, dur: 0.6 },   // C5
        { freq: 659.25, time: 2.9, dur: 0.8 }    // E5
      ];

      notes.forEach(({ freq, time, dur }) => {
        if (!this.audioCtx) return;
        const osc = this.audioCtx.createOscillator();
        const gain = this.audioCtx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, now + time);

        const vol = this.masterVolume;
        gain.gain.setValueAtTime(vol * 0.35, now + time);
        gain.gain.exponentialRampToValueAtTime(0.001, now + time + dur);

        osc.connect(gain);
        gain.connect(this.audioCtx.destination);

        osc.start(now + time);
        osc.stop(now + time + dur + 0.1);
      });
    } catch (e) {
      console.warn('Electronic chime error:', e);
    }
  }

  // 3. Digital Modern Pulse
  playDigitalPulse() {
    try {
      this.init();
      if (!this.audioCtx) return;

      const now = this.audioCtx.currentTime;
      const pulses = [0, 0.15, 0.3, 0.6, 0.75, 0.9];

      pulses.forEach((delay) => {
        if (!this.audioCtx) return;
        const osc = this.audioCtx.createOscillator();
        const gain = this.audioCtx.createGain();

        osc.type = 'square';
        osc.frequency.setValueAtTime(1046.5, now + delay); // C6

        const vol = this.masterVolume;
        gain.gain.setValueAtTime(vol * 0.15, now + delay);
        gain.gain.exponentialRampToValueAtTime(0.001, now + delay + 0.12);

        osc.connect(gain);
        gain.connect(this.audioCtx.destination);

        osc.start(now + delay);
        osc.stop(now + delay + 0.13);
      });
    } catch (e) {
      console.warn('Digital pulse error:', e);
    }
  }

  // 4. Gentle Soft Chime
  playGentleChime() {
    try {
      this.init();
      if (!this.audioCtx) return;

      const now = this.audioCtx.currentTime;
      const chords = [523.25, 659.25, 783.99, 1046.5]; // C Major chord

      chords.forEach((freq, idx) => {
        if (!this.audioCtx) return;
        const delay = idx * 0.15;
        const osc = this.audioCtx.createOscillator();
        const gain = this.audioCtx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, now + delay);

        const vol = this.masterVolume;
        gain.gain.setValueAtTime(vol * 0.25, now + delay);
        gain.gain.exponentialRampToValueAtTime(0.001, now + delay + 1.5);

        osc.connect(gain);
        gain.connect(this.audioCtx.destination);

        osc.start(now + delay);
        osc.stop(now + delay + 1.6);
      });
    } catch (e) {
      console.warn('Gentle chime error:', e);
    }
  }

  // 5. Siren Alert (For emergency / urgent dismissal)
  playSirenAlert() {
    try {
      this.init();
      if (!this.audioCtx) return;

      const now = this.audioCtx.currentTime;
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();

      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(600, now);
      osc.frequency.linearRampToValueAtTime(1200, now + 0.5);
      osc.frequency.linearRampToValueAtTime(600, now + 1.0);
      osc.frequency.linearRampToValueAtTime(1200, now + 1.5);

      const vol = this.masterVolume;
      gain.gain.setValueAtTime(vol * 0.2, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 1.8);

      osc.connect(gain);
      gain.connect(this.audioCtx.destination);

      osc.start(now);
      osc.stop(now + 1.8);
    } catch (e) {
      console.warn('Siren alert error:', e);
    }
  }

  // Play subtle warning tick/chime when 1 minute remains
  playWarningChime() {
    try {
      this.init();
      if (!this.audioCtx) return;

      const now = this.audioCtx.currentTime;
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(1046.5, now); // C6
      gain.gain.setValueAtTime(this.masterVolume * 0.2, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.4);

      osc.connect(gain);
      gain.connect(this.audioCtx.destination);

      osc.start(now);
      osc.stop(now + 0.5);
    } catch (e) {
      console.warn('Audio warning chime error:', e);
    }
  }

  // Legacy support
  playBellRing() {
    this.playClassicBrass();
  }
}

export const bellAudio = new SchoolBellAudio();
