// Web Audio API Synthesizer for School Bell & Alarm Chime

export class SchoolBellAudio {
  private audioCtx: AudioContext | null = null;

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

  // Ring a realistic brass school bell chime sequence
  playBellRing() {
    try {
      this.init();
      if (!this.audioCtx) return;

      const now = this.audioCtx.currentTime;
      
      // Play 3 successive brass bell strikes
      const strikes = [0, 0.6, 1.2, 1.8];
      
      strikes.forEach((delay) => {
        if (!this.audioCtx) return;
        
        // Primary oscillator (bell tone ~ 880Hz / A5)
        const osc1 = this.audioCtx.createOscillator();
        const gain1 = this.audioCtx.createGain();
        
        // Harmonics
        const osc2 = this.audioCtx.createOscillator();
        const gain2 = this.audioCtx.createGain();

        osc1.type = 'sine';
        osc1.frequency.setValueAtTime(880, now + delay);
        
        osc2.type = 'triangle';
        osc2.frequency.setValueAtTime(1760, now + delay);

        gain1.gain.setValueAtTime(0.4, now + delay);
        gain1.gain.exponentialRampToValueAtTime(0.001, now + delay + 1.2);

        gain2.gain.setValueAtTime(0.2, now + delay);
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
      console.warn('Audio bell playback error:', e);
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
      gain.gain.setValueAtTime(0.2, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.4);

      osc.connect(gain);
      gain.connect(this.audioCtx.destination);

      osc.start(now);
      osc.stop(now + 0.5);
    } catch (e) {
      console.warn('Audio warning chime error:', e);
    }
  }
}

export const bellAudio = new SchoolBellAudio();
