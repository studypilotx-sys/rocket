import React, { useState, useEffect, useRef } from 'react';
import {
  Shield,
  ShieldAlert,
  ShieldCheck,
  ChevronLeft,
  Pause,
  Play,
  Coffee,
  CheckCircle,
  AlertTriangle,
  Clock,
  Video,
  FileText,
  BookOpen,
  Volume2,
  Maximize2,
  Minimize2,
  Move,
  Camera,
  X,
  RotateCcw,
  Layers
} from 'lucide-react';
import { Topic, Subject, Chapter, GuardianSettings, StudySessionRecord, StudyMaterialItem } from '../types';

interface FocusGuardianSessionProps {
  topic: Topic | null;
  subject?: Subject;
  chapter?: Chapter;
  material: StudyMaterialItem | null;
  guardianSettings: GuardianSettings;
  onExitMaterial: () => void;
  onFinishSession: (record: StudySessionRecord) => void;
  onExitSession: () => void;
}

type PresenceState = 'UNKNOWN' | 'PRESENT' | 'ABSENT';
type CornerPosition = 'TOP_RIGHT' | 'TOP_LEFT' | 'BOTTOM_RIGHT' | 'BOTTOM_LEFT';

// AudioContext controller for Reveille Alarm
let activeReveilleCtx: AudioContext | null = null;

function playReveilleAlarm(volume: 'LOW' | 'MEDIUM' | 'HIGH' = 'MEDIUM') {
  stopReveilleAlarm();
  try {
    const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    activeReveilleCtx = ctx;

    const gainNode = ctx.createGain();
    const gainVal = volume === 'LOW' ? 0.2 : volume === 'MEDIUM' ? 0.45 : 0.75;
    gainNode.gain.setValueAtTime(gainVal, ctx.currentTime);
    gainNode.connect(ctx.destination);

    // Reveille military bugle motif: G3 (196), C4 (261.63), E4 (329.63), G4 (392), C5 (523.25)
    const notes = [
      { freq: 196.0, dur: 0.15 },
      { freq: 261.63, dur: 0.3 },
      { freq: 329.63, dur: 0.15 },
      { freq: 261.63, dur: 0.15 },
      { freq: 196.0, dur: 0.15 },
      { freq: 261.63, dur: 0.3 },
      { freq: 329.63, dur: 0.15 },
      { freq: 261.63, dur: 0.15 },
      { freq: 196.0, dur: 0.15 },
      { freq: 261.63, dur: 0.15 },
      { freq: 329.63, dur: 0.15 },
      { freq: 392.0, dur: 0.3 },
      { freq: 329.63, dur: 0.15 },
      { freq: 261.63, dur: 0.3 },
      { freq: 329.63, dur: 0.15 },
      { freq: 261.63, dur: 0.15 },
      { freq: 196.0, dur: 0.3 },
      { freq: 261.63, dur: 0.15 },
      { freq: 329.63, dur: 0.15 },
      { freq: 392.0, dur: 0.15 },
      { freq: 523.25, dur: 0.45 }
    ];

    let t = ctx.currentTime + 0.05;
    notes.forEach(note => {
      if (note.freq > 0) {
        const osc = ctx.createOscillator();
        const noteGain = ctx.createGain();
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(note.freq, t);

        noteGain.gain.setValueAtTime(0.001, t);
        noteGain.gain.exponentialRampToValueAtTime(1, t + 0.02);
        noteGain.gain.setValueAtTime(0.9, t + note.dur - 0.03);
        noteGain.gain.exponentialRampToValueAtTime(0.001, t + note.dur);

        osc.connect(noteGain);
        noteGain.connect(gainNode);

        osc.start(t);
        osc.stop(t + note.dur);
      }
      t += note.dur;
    });
  } catch (e) {
    console.error('AudioContext error:', e);
  }
}

function stopReveilleAlarm() {
  try {
    if (activeReveilleCtx && activeReveilleCtx.state !== 'closed') {
      activeReveilleCtx.close().catch(() => {});
      activeReveilleCtx = null;
    }
  } catch (e) {}
}

function speakVoiceWarning() {
  try {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance("Please return to your study session.");
      utterance.rate = 1.0;
      utterance.pitch = 1.0;
      window.speechSynthesis.speak(utterance);
    }
  } catch (e) {
    console.error('SpeechSynthesis error:', e);
  }
}

function speakVoiceWarningPromise(): Promise<void> {
  return new Promise(resolve => {
    try {
      if (!('speechSynthesis' in window)) {
        setTimeout(resolve, 2000);
        return;
      }
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance("Please return to your study session.");
      utterance.rate = 1.0;
      utterance.pitch = 1.0;
      let finished = false;
      const done = () => {
        if (!finished) {
          finished = true;
          resolve();
        }
      };
      utterance.onend = done;
      utterance.onerror = done;
      // Fallback timeout in case browser TTS does not emit onend
      setTimeout(done, 3500);
      window.speechSynthesis.speak(utterance);
    } catch (e) {
      resolve();
    }
  });
}

function stopAllAlerts() {
  stopReveilleAlarm();
  try {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
  } catch (e) {}
  try {
    if ('vibrate' in navigator) {
      navigator.vibrate(0);
    }
  } catch (e) {}
}

function triggerVibration() {
  try {
    if ('vibrate' in navigator) {
      navigator.vibrate([300, 200, 300, 200, 500]);
    }
  } catch (e) {}
}

export const FocusGuardianSession: React.FC<FocusGuardianSessionProps> = ({
  topic,
  subject,
  chapter,
  material,
  guardianSettings,
  onExitMaterial,
  onFinishSession,
  onExitSession
}) => {
  // Session Timers
  const [activeStudySeconds, setActiveStudySeconds] = useState(0);
  const [pauseSeconds, setPauseSeconds] = useState(0);
  const [breakSeconds, setBreakSeconds] = useState(0);
  const [interruptionCount, setInterruptionCount] = useState(0);
  const [absenceEventsCount, setAbsenceEventsCount] = useState(0);

  // States
  const [isManualPaused, setIsManualPaused] = useState(false);
  const [isOnBreak, setIsOnBreak] = useState(false);
  const [presenceState, setPresenceState] = useState<PresenceState>('UNKNOWN');
  const [hasCameraPermission, setHasCameraPermission] = useState<boolean | null>(null);
  const [absenceCountdown, setAbsenceCountdown] = useState<number | null>(null);
  const [isAlarmActive, setIsAlarmActive] = useState(false);

  // Floating PiP Camera State (When material is active)
  const [pipCorner, setPipCorner] = useState<CornerPosition>('TOP_RIGHT');
  const [pipCustomPos, setPipCustomPos] = useState<{ x: number; y: number } | null>(null);
  const [isDraggingPip, setIsDraggingPip] = useState(false);
  const [isPipCollapsed, setIsPipCollapsed] = useState(false);
  const dragStartRef = useRef<{ startX: number; startY: number; initialX: number; initialY: number }>({
    startX: 0,
    startY: 0,
    initialX: 0,
    initialY: 0
  });

  // Camera video ref and stream ref
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const prevFrameBufferRef = useRef<Uint8Array | null>(null);
  const historyBufferRef = useRef<boolean[]>([]);
  const alarmIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const presenceStateRef = useRef<PresenceState>(presenceState);
  presenceStateRef.current = presenceState;
  const sequenceIdRef = useRef<number>(0);

  // -------------------------------------------------------------
  // 1. Camera Initialization & Automatic Presence Detection
  // -------------------------------------------------------------
  useEffect(() => {
    let isActive = true;
    let detectionInterval: NodeJS.Timeout | null = null;

    if (!guardianSettings.isGuardianEnabled) {
      setPresenceState('PRESENT');
      return;
    }

    // Initialize offscreen analysis canvas (160x120 is ideal for real-time low-overhead analysis)
    const canvas = document.createElement('canvas');
    canvas.width = 160;
    canvas.height = 120;
    canvasRef.current = canvas;
    const ctx = canvas.getContext('2d', { willReadFrequently: true });

    // Request Android CameraX / WebRTC front camera
    navigator.mediaDevices?.getUserMedia({
      video: {
        facingMode: 'user',
        width: { ideal: 640 },
        height: { ideal: 480 }
      },
      audio: false
    })
      .then(stream => {
        if (!isActive) {
          stream.getTracks().forEach(t => t.stop());
          return;
        }

        streamRef.current = stream;
        setHasCameraPermission(true);

        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          videoRef.current.play().catch(() => {});
        }

        // Automatic on-device real-time presence detection loop (~3.5 fps)
        detectionInterval = setInterval(() => {
          if (!videoRef.current || videoRef.current.readyState < 2 || !ctx) {
            return;
          }

          try {
            ctx.drawImage(videoRef.current, 0, 0, 160, 120);
            const imgData = ctx.getImageData(0, 0, 160, 120);
            const pixels = imgData.data;

            // Region of Interest (Central 65% width, 75% height)
            const xStart = 28;
            const xEnd = 132;
            const yStart = 16;
            const yEnd = 104;
            const step = 4;

            let sampleCount = 0;
            let sumLuminance = 0;
            let skinPixelCount = 0;
            let edgeCount = 0;
            let motionEnergy = 0;

            const currentLuminances: number[] = [];
            const prevBuffer = prevFrameBufferRef.current;

            for (let y = yStart; y < yEnd; y += step) {
              for (let x = xStart; x < xEnd; x += step) {
                const idx = (y * 160 + x) * 4;
                const r = pixels[idx];
                const g = pixels[idx + 1];
                const b = pixels[idx + 2];

                // Y (Luminance)
                const lum = 0.299 * r + 0.587 * g + 0.114 * b;
                sumLuminance += lum;
                currentLuminances.push(lum);
                sampleCount++;

                // YCbCr Human Chrominance Test (Universal human skin clustering across all tones)
                const cb = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b;
                const cr = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b;

                if (cr >= 132 && cr <= 175 && cb >= 77 && cb <= 128 && lum >= 30 && lum <= 245) {
                  skinPixelCount++;
                }

                // Horizontal gradient / Edge contrast
                if (x + step < xEnd) {
                  const nextIdx = (y * 160 + (x + step)) * 4;
                  const nextLum = 0.299 * pixels[nextIdx] + 0.587 * pixels[nextIdx + 1] + 0.114 * pixels[nextIdx + 2];
                  if (Math.abs(lum - nextLum) > 18) {
                    edgeCount++;
                  }
                }

                // Micro-movement / Frame difference
                if (prevBuffer && prevBuffer.length === currentLuminances.length) {
                  const pLum = prevBuffer[currentLuminances.length - 1];
                  if (Math.abs(lum - pLum) > 6) {
                    motionEnergy++;
                  }
                }
              }
            }

            if (sampleCount === 0) return;

            // Store current luminance buffer for next frame delta
            prevFrameBufferRef.current = new Uint8Array(currentLuminances);

            const meanLum = sumLuminance / sampleCount;
            const skinRatio = skinPixelCount / sampleCount;
            const edgeRatio = edgeCount / sampleCount;
            const motionRatio = motionEnergy / sampleCount;

            // Calculate luminance variance in ROI
            let varianceSum = 0;
            for (let i = 0; i < currentLuminances.length; i++) {
              const diff = currentLuminances[i] - meanLum;
              varianceSum += diff * diff;
            }
            const variance = varianceSum / sampleCount;
            const stdDev = Math.sqrt(variance);

            // REAL PRESENCE DETECTION DECISION:
            // A person creates skin chrominance cluster OR distinct head/torso edge density & variance,
            // with natural micro-movements, within normal illumination (not total darkness or washed out)
            const isIlluminated = meanLum >= 16 && meanLum <= 246;
            const hasSkin = skinRatio >= 0.035;
            const hasHumanContours = stdDev >= 13.5 && edgeRatio >= 0.045;
            const hasMicroMotion = motionRatio >= 0.015;

            const rawDetected = isIlluminated && (hasSkin || (hasHumanContours && (hasMicroMotion || stdDev >= 18)));

            // Debounce ring-buffer across 4 consecutive frames (avoids flickers/blinks)
            const history = historyBufferRef.current;
            history.push(rawDetected);
            if (history.length > 4) history.shift();

            const detectedVotes = history.filter(Boolean).length;
            if (detectedVotes >= 3) {
              setPresenceState('PRESENT');
            } else if (detectedVotes <= 1) {
              setPresenceState('ABSENT');
            }
          } catch (e) {
            // Layout transition frame drop safe
          }
        }, 300);
      })
      .catch(err => {
        console.warn('Focus Guardian CameraX front camera permission not granted:', err);
        setHasCameraPermission(false);
        setPresenceState('UNKNOWN');
      });

    // Cleanup resources on unmount
    return () => {
      isActive = false;
      if (detectionInterval) clearInterval(detectionInterval);
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(t => t.stop());
        streamRef.current = null;
      }
      stopAllAlerts();
    };
  }, [guardianSettings.isGuardianEnabled]);

  // Ensure video element keeps stream attached when switching between views
  useEffect(() => {
    if (videoRef.current && streamRef.current && videoRef.current.srcObject !== streamRef.current) {
      videoRef.current.srcObject = streamRef.current;
      videoRef.current.play().catch(() => {});
    }
  });

  // -------------------------------------------------------------
  // 2. Study Timer Ticker (1-second tick)
  // -------------------------------------------------------------
  useEffect(() => {
    const ticker = setInterval(() => {
      if (isOnBreak) {
        setBreakSeconds(s => s + 1);
      } else if (isManualPaused || (presenceState === 'ABSENT' && (absenceCountdown === null || absenceCountdown <= 0))) {
        setPauseSeconds(s => s + 1);
      } else if (presenceState === 'PRESENT' && !isManualPaused) {
        setActiveStudySeconds(s => s + 1);
      }
    }, 1000);

    return () => clearInterval(ticker);
  }, [isOnBreak, isManualPaused, presenceState, absenceCountdown]);

  // -------------------------------------------------------------
  // 3. Automatic Absence Warning Sequence & Alarms
  // -------------------------------------------------------------
  useEffect(() => {
    // If Guardian disabled or on break, stop all alerts immediately
    if (!guardianSettings.isGuardianEnabled || isOnBreak) {
      sequenceIdRef.current++;
      setAbsenceCountdown(null);
      setIsAlarmActive(false);
      stopAllAlerts();
      if (alarmIntervalRef.current) {
        clearInterval(alarmIntervalRef.current);
        alarmIntervalRef.current = null;
      }
      return;
    }

    let countdownTimer: NodeJS.Timeout | null = null;

    if (presenceState === 'ABSENT') {
      if (absenceCountdown === null) {
        // Start 5-second countdown debounce
        setAbsenceCountdown(5);
      } else if (absenceCountdown > 0) {
        countdownTimer = setTimeout(() => {
          setAbsenceCountdown(prev => (prev !== null ? prev - 1 : null));
        }, 1000);
      } else if (absenceCountdown === 0 && !isAlarmActive) {
        // Absence CONFIRMED!
        setIsAlarmActive(true);
        setIsManualPaused(true); // Keep focus session paused until student resumes
        setInterruptionCount(c => c + 1);
        setAbsenceEventsCount(c => c + 1);

        const currentSeqId = ++sequenceIdRef.current;

        // NEW ALERT SEQUENCE:
        // 1. Start the first voice warning.
        // 2. Wait for the warning to finish.
        // 3. If the user is still absent, repeat the SAME voice warning a second time.
        // 4. Wait briefly after the second warning.
        // 5. If the user is still absent, THEN trigger the reveille/alarm + vibration.
        (async () => {
          const alertType = guardianSettings.alertType;
          if (alertType === 'OFF') return;

          // 1. Start the first voice warning & 2. Wait for the warning to finish
          if (guardianSettings.isAudioReminderEnabled) {
            await speakVoiceWarningPromise();
          } else {
            await new Promise(r => setTimeout(r, 2000));
          }

          if (sequenceIdRef.current !== currentSeqId || presenceStateRef.current !== 'ABSENT') return;

          // Brief pause before second voice warning
          await new Promise(r => setTimeout(r, 500));
          if (sequenceIdRef.current !== currentSeqId || presenceStateRef.current !== 'ABSENT') return;

          // 3. If the user is still absent, repeat the SAME voice warning a second time & wait
          if (guardianSettings.isAudioReminderEnabled) {
            await speakVoiceWarningPromise();
          } else {
            await new Promise(r => setTimeout(r, 2000));
          }

          if (sequenceIdRef.current !== currentSeqId || presenceStateRef.current !== 'ABSENT') return;

          // 4. Wait briefly after the second warning
          await new Promise(r => setTimeout(r, 1200));
          if (sequenceIdRef.current !== currentSeqId || presenceStateRef.current !== 'ABSENT') return;

          // 5. If the user is still absent, THEN trigger the reveille/alarm + vibration
          if (alertType === 'VOICE_AND_REVEILLE') {
            playReveilleAlarm(guardianSettings.volumeLevel);
          }
          if (guardianSettings.isVibrationEnabled) {
            triggerVibration();
          }
        })();
      }
    } else if (presenceState === 'PRESENT') {
      // User returned to camera view at ANY point:
      // Immediately stop the current warning/alarm/vibration
      sequenceIdRef.current++;
      setAbsenceCountdown(null);
      setIsAlarmActive(false);
      stopAllAlerts();
      if (alarmIntervalRef.current) {
        clearInterval(alarmIntervalRef.current);
        alarmIntervalRef.current = null;
      }
    }

    return () => {
      if (countdownTimer) clearTimeout(countdownTimer);
    };
  }, [presenceState, absenceCountdown, isAlarmActive, isOnBreak, guardianSettings]);

  // -------------------------------------------------------------
  // 4. Session Finish Handler
  // -------------------------------------------------------------
  const handleFinish = () => {
    stopAllAlerts();
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop());
      streamRef.current = null;
    }

    const newRecord: StudySessionRecord = {
      id: `sess-${Date.now()}`,
      topicId: topic?.id || 'general',
      topicName: topic?.name || 'Academic Study Session',
      subjectName: subject?.name || 'Academic Study',
      durationSeconds: activeStudySeconds + pauseSeconds + breakSeconds,
      activeSeconds: activeStudySeconds,
      pauseSeconds,
      breakSeconds,
      interruptionCount,
      absenceEventsCount,
      isGuardianEnabled: guardianSettings.isGuardianEnabled,
      timestamp: Date.now(),
      dateString: new Date().toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      }),
      materialId: material?.id,
      materialName: material?.name
    };

    onFinishSession(newRecord);
  };

  const handleExit = () => {
    stopAllAlerts();
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop());
      streamRef.current = null;
    }
    onExitSession();
  };

  // -------------------------------------------------------------
  // 5. Draggable Floating PiP Logic
  // -------------------------------------------------------------
  const handlePointerDown = (e: React.PointerEvent) => {
    setIsDraggingPip(true);
    const rect = e.currentTarget.getBoundingClientRect();
    dragStartRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      initialX: pipCustomPos ? pipCustomPos.x : rect.left,
      initialY: pipCustomPos ? pipCustomPos.y : rect.top
    };
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!isDraggingPip) return;
    const deltaX = e.clientX - dragStartRef.current.startX;
    const deltaY = e.clientY - dragStartRef.current.startY;
    const newX = Math.max(8, Math.min(window.innerWidth - 160, dragStartRef.current.initialX + deltaX));
    const newY = Math.max(50, Math.min(window.innerHeight - 160, dragStartRef.current.initialY + deltaY));
    setPipCustomPos({ x: newX, y: newY });
  };

  const handlePointerUp = (e: React.PointerEvent) => {
    setIsDraggingPip(false);
    try {
      (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
    } catch {}
  };

  // Helper corner classes when not freely dragged
  const getCornerClass = () => {
    switch (pipCorner) {
      case 'TOP_LEFT':
        return 'top-14 left-3';
      case 'BOTTOM_LEFT':
        return 'bottom-20 left-3';
      case 'BOTTOM_RIGHT':
        return 'bottom-20 right-3';
      case 'TOP_RIGHT':
      default:
        return 'top-14 right-3';
    }
  };

  const formatTimer = (seconds: number) => {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${String(hrs).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  // =============================================================
  // RENDER: Mode A — Study Material with Floating Guardian PiP
  // =============================================================
  if (material) {
    return (
      <div id="guardian-material-study-container" className="flex-1 flex flex-col justify-between bg-[#0B0F19] text-white relative overflow-hidden select-none">
        {/* Top Floating Control Bar */}
        <div className="bg-[#0F172A]/90 backdrop-blur-md px-3 py-2 border-b border-[#334155] flex items-center justify-between z-20 shrink-0">
          <div className="flex items-center space-x-2 min-w-0 pr-2">
            <button
              onClick={onExitMaterial}
              className="p-1 rounded-lg hover:bg-[#1E293B] text-[#94A3B8] transition-colors"
              title="Return to standard Guardian Focus View"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <div className="min-w-0">
              <p className="text-[10px] text-[#B8860B] font-bold uppercase tracking-wider truncate">
                {subject?.name || 'Study Material'} • {material.type}
              </p>
              <h2 className="text-xs font-bold text-white truncate">{material.name}</h2>
            </div>
          </div>

          {/* Real-time Timer and Status Pill */}
          <div className="flex items-center space-x-2 shrink-0">
            <div className={`px-2 py-0.5 rounded-full font-mono text-[11px] font-bold border flex items-center space-x-1.5 ${
              isOnBreak
                ? 'bg-[#0284C7]/30 border-[#0284C7] text-[#7DD3FC]'
                : isManualPaused
                ? 'bg-[#EAB308]/20 border-[#EAB308] text-[#FEF08A]'
                : presenceState === 'ABSENT'
                ? 'bg-[#DC2626]/30 border-[#EF4444] text-[#FECACA] animate-pulse'
                : 'bg-[#16A34A]/30 border-[#22C55E] text-[#86EFAC]'
            }`}>
              <span className={`w-1.5 h-1.5 rounded-full ${
                presenceState === 'PRESENT' && !isManualPaused && !isOnBreak ? 'bg-[#22C55E] animate-ping' : 'bg-red-400'
              }`} />
              <span>{formatTimer(activeStudySeconds)}</span>
            </div>

            <button
              onClick={onExitMaterial}
              className="px-2 py-1 rounded-lg bg-[#1E293B] hover:bg-[#334155] text-[10px] font-bold text-[#E2E8F0] border border-[#475569] transition-colors"
            >
              Standard Mode
            </button>
          </div>
        </div>

        {/* MAIN FULL-SCREEN CONTENT: Video or Document */}
        <div className="flex-1 relative flex items-center justify-center bg-black overflow-hidden">
          {material.type === 'VIDEO' ? (
            <div className="w-full h-full flex items-center justify-center relative bg-black">
              <video
                src={material.uriOrPath}
                controls
                autoPlay
                playsInline
                className="w-full h-full object-contain max-h-[80vh]"
              />
            </div>
          ) : material.type === 'IMAGE' ? (
            <div className="w-full h-full flex items-center justify-center p-2 bg-[#0F172A]">
              <img
                src={material.uriOrPath}
                alt={material.name}
                className="max-w-full max-h-[78vh] object-contain rounded-lg border border-[#334155]"
              />
            </div>
          ) : (
            <div className="w-full h-full p-4 overflow-y-auto bg-[#0F172A] text-[#E2E8F0]">
              <div className="max-w-xl mx-auto space-y-3">
                <div className="p-3 bg-[#1E293B] rounded-xl border border-[#334155]">
                  <h3 className="text-xs font-bold text-[#B8860B] mb-1">Study Guide & Reference Notes</h3>
                  <p className="text-xs whitespace-pre-wrap leading-relaxed font-sans opacity-90">
                    {material.notes || material.uriOrPath || 'Academic study content loaded for Focus Guardian session.'}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Absence Warning Pop-over banner over video */}
          {presenceState === 'ABSENT' && !isOnBreak && !isManualPaused && (
            <div className="absolute top-4 left-4 right-4 z-40 bg-[#DC2626]/95 backdrop-blur-md border border-red-500 text-white p-3 rounded-xl shadow-2xl animate-bounce">
              <div className="flex items-center space-x-2">
                <ShieldAlert className="w-5 h-5 text-white shrink-0 animate-pulse" />
                <div className="text-xs">
                  <p className="font-bold">
                    {absenceCountdown === 0 || absenceCountdown === null
                      ? 'FOCUS GUARDIAN ALARM — STUDENT ABSENT'
                      : `STUDENT ABSENT (${absenceCountdown}s DEBOUNCE)`}
                  </p>
                  <p className="text-[11px] opacity-90">
                    {absenceCountdown === 0 || absenceCountdown === null
                      ? 'Timer is paused! Voice reminder & Reveille horn sounding. Please return to camera view.'
                      : 'Camera detected you left desk. Timer will pause and alarms sound in a moment.'}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* --------------------------------------------------------- */}
          {/* FLOATING GUARDIAN LIVE CAMERA WINDOW */}
          {/* --------------------------------------------------------- */}
          <div
            id="floating-guardian-camera"
            style={
              pipCustomPos
                ? { position: 'fixed', left: `${pipCustomPos.x}px`, top: `${pipCustomPos.y}px` }
                : undefined
            }
            className={`z-30 shadow-2xl transition-shadow ${
              pipCustomPos ? '' : `absolute ${getCornerClass()}`
            } ${isDraggingPip ? 'cursor-grabbing opacity-90 scale-102' : 'cursor-grab'}`}
          >
            <div className="bg-[#0F172A] border border-[#B8860B]/60 rounded-xl overflow-hidden shadow-2xl w-36 sm:w-44 flex flex-col">
              {/* Drag Handle & Status Header */}
              <div
                onPointerDown={handlePointerDown}
                onPointerMove={handlePointerMove}
                onPointerUp={handlePointerUp}
                className="bg-[#1E293B] px-2 py-1 flex items-center justify-between border-b border-[#334155] select-none touch-none"
              >
                <div className="flex items-center space-x-1">
                  <Move className="w-3 h-3 text-[#B8860B]" />
                  <span className="text-[9px] font-bold tracking-wider text-[#E2E8F0]">GUARDIAN</span>
                </div>

                <div className="flex items-center space-x-1">
                  {/* Corner cycle button */}
                  <button
                    onClick={() => {
                      setPipCustomPos(null);
                      setPipCorner(prev => {
                        if (prev === 'TOP_RIGHT') return 'BOTTOM_RIGHT';
                        if (prev === 'BOTTOM_RIGHT') return 'BOTTOM_LEFT';
                        if (prev === 'BOTTOM_LEFT') return 'TOP_LEFT';
                        return 'TOP_RIGHT';
                      });
                    }}
                    title="Move camera to next corner"
                    className="text-[9px] px-1 rounded bg-[#334155] text-[#B8860B] hover:bg-[#475569]"
                  >
                    Corner
                  </button>

                  <button
                    onClick={() => setIsPipCollapsed(c => !c)}
                    className="p-0.5 rounded text-[#94A3B8] hover:text-white"
                  >
                    {isPipCollapsed ? <Maximize2 className="w-2.5 h-2.5" /> : <Minimize2 className="w-2.5 h-2.5" />}
                  </button>
                </div>
              </div>

              {/* Live Front Camera Stream */}
              {!isPipCollapsed && (
                <div className="relative aspect-4/3 bg-black flex items-center justify-center overflow-hidden">
                  <video
                    ref={videoRef}
                    autoPlay
                    playsInline
                    muted
                    className="w-full h-full object-cover scale-x-[-1]"
                  />

                  {/* Face Framing Reticle */}
                  <div className="absolute inset-0 border border-dashed border-[#B8860B]/40 pointer-events-none flex items-center justify-center">
                    <div className="w-16 h-20 border border-[#B8860B]/60 rounded-md" />
                  </div>

                  {/* Status Overlay Badge */}
                  <div className="absolute bottom-1 left-1 right-1 bg-black/80 backdrop-blur-xs px-1.5 py-0.5 rounded flex items-center justify-between text-[9px]">
                    <span className="flex items-center space-x-1">
                      <span className={`w-1.5 h-1.5 rounded-full ${
                        presenceState === 'PRESENT'
                          ? 'bg-[#22C55E] animate-pulse'
                          : presenceState === 'ABSENT'
                          ? 'bg-red-500 animate-ping'
                          : 'bg-amber-400'
                      }`} />
                      <span className="font-mono font-bold">
                        {presenceState === 'PRESENT' ? 'PRESENT' : presenceState === 'ABSENT' ? 'ABSENT' : 'SCAN'}
                      </span>
                    </span>

                    {absenceCountdown !== null && (
                      <span className="text-red-400 font-bold font-mono">{absenceCountdown}s</span>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Bottom Persistent Session Controls Bar */}
        <div className="bg-[#0F172A] border-t border-[#334155] p-3 flex items-center justify-between gap-2 z-20 shrink-0">
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setIsManualPaused(prev => !prev)}
              className={`py-2 px-3 rounded-xl border text-xs font-bold flex items-center space-x-1.5 transition-colors ${
                isManualPaused
                  ? 'bg-[#B8860B] text-white border-[#B8860B]'
                  : 'bg-[#1E293B] text-[#E2E8F0] border-[#334155] hover:bg-[#334155]'
              }`}
            >
              {isManualPaused ? <Play className="w-3.5 h-3.5 fill-current" /> : <Pause className="w-3.5 h-3.5" />}
              <span>{isManualPaused ? 'Resume' : 'Pause'}</span>
            </button>

            <button
              onClick={() => setIsOnBreak(prev => !prev)}
              className={`py-2 px-3 rounded-xl border text-xs font-bold flex items-center space-x-1.5 transition-colors ${
                isOnBreak
                  ? 'bg-[#0284C7] text-white border-[#0284C7]'
                  : 'bg-[#1E293B] text-[#E2E8F0] border-[#334155] hover:bg-[#334155]'
              }`}
            >
              <Coffee className="w-3.5 h-3.5" />
              <span>{isOnBreak ? 'End Break' : 'Break'}</span>
            </button>
          </div>

          <button
            onClick={handleFinish}
            className="py-2 px-4 rounded-xl bg-[#B8860B] hover:bg-[#A17608] text-white text-xs font-bold flex items-center space-x-1.5 transition-colors shadow-xs"
          >
            <CheckCircle className="w-4 h-4" />
            <span>Finish Session</span>
          </button>
        </div>
      </div>
    );
  }

  // =============================================================
  // RENDER: Mode B — Standard Full-Screen Focus Guardian Session
  // =============================================================
  return (
    <div id="focus-guardian-session-view" className="p-4 space-y-3.5 flex-1 flex flex-col justify-between bg-white">
      <div>
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <button
              onClick={() => {
                if (window.confirm("Exit active focus session? Your session time will be discarded unless logged.")) {
                  handleExit();
                }
              }}
              className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#0F172A] transition-colors"
              aria-label="Exit session"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <div>
              <p className="text-[10px] text-[#B8860B] font-bold uppercase tracking-wider">
                {subject?.name || 'Study Session'} • {chapter?.name || 'Curriculum'}
              </p>
              <h1 className="text-base font-bold text-[#0F172A] line-clamp-1">
                {topic?.name || 'Focus Session'}
              </h1>
            </div>
          </div>

          {/* Automatic Presence Status Pill */}
          {guardianSettings.isGuardianEnabled ? (
            isOnBreak ? (
              <div className="flex items-center space-x-1.5 px-2.5 py-1 rounded-full bg-[#E0F2FE] border border-[#BAE6FD] text-[#0284C7] text-[10px] font-bold">
                <Coffee className="w-3 h-3" />
                <span>ON BREAK</span>
              </div>
            ) : isManualPaused ? (
              <div className="flex items-center space-x-1.5 px-2.5 py-1 rounded-full bg-[#FEFCE8] border border-[#FEF08A] text-[#CA8A04] text-[10px] font-bold">
                <Pause className="w-3 h-3" />
                <span>PAUSED</span>
              </div>
            ) : presenceState === 'ABSENT' && (absenceCountdown === 0 || absenceCountdown === null) ? (
              <div className="flex items-center space-x-1.5 px-2.5 py-1 rounded-full bg-[#FEF2F2] border border-[#FECACA] text-[#DC2626] text-[10px] font-bold animate-pulse">
                <ShieldAlert className="w-3 h-3" />
                <span>STUDENT ABSENT</span>
              </div>
            ) : presenceState === 'ABSENT' && absenceCountdown !== null ? (
              <div className="flex items-center space-x-1.5 px-2.5 py-1 rounded-full bg-[#FFF7ED] border border-[#FFEDD5] text-[#EA580C] text-[10px] font-bold animate-pulse">
                <Clock className="w-3 h-3" />
                <span>ABSENT ({absenceCountdown}s)</span>
              </div>
            ) : presenceState === 'PRESENT' ? (
              <div className="flex items-center space-x-1.5 px-2.5 py-1 rounded-full bg-[#F0FDF4] border border-[#BBF7D0] text-[#16A34A] text-[10px] font-bold">
                <span className="w-1.5 h-1.5 rounded-full bg-[#22C55E] animate-ping" />
                <ShieldCheck className="w-3 h-3" />
                <span>GUARDIAN: PRESENT</span>
              </div>
            ) : (
              <div className="flex items-center space-x-1.5 px-2.5 py-1 rounded-full bg-[#FEFCE8] border border-[#FEF08A] text-[#CA8A04] text-[10px] font-bold">
                <Shield className="w-3 h-3" />
                <span>SCANNING DESK</span>
              </div>
            )
          ) : (
            <div className="px-2.5 py-1 rounded-full bg-[#F8FAFC] border border-[#E2E8F0] text-[#64748B] text-[10px] font-bold">
              <span>GUARDIAN OFF</span>
            </div>
          )}
        </div>

        {/* Real Front Camera Viewfinder Card */}
        <div className="mt-3 bg-[#0F172A] rounded-2xl p-3 text-white overflow-hidden relative shadow-md">
          {hasCameraPermission === false ? (
            <div className="relative aspect-video rounded-xl bg-gradient-to-br from-[#1E293B] to-[#0F172A] flex flex-col items-center justify-center p-4 text-center border border-[#334155]">
              <Camera className="w-8 h-8 text-[#B8860B] mb-2" />
              <p className="text-xs font-bold text-white tracking-wide">CAMERA PERMISSION REQUIRED</p>
              <p className="text-[10px] text-[#94A3B8] mt-1 max-w-xs">
                Focus Guardian requires front camera access to monitor desk presence and keep your study time honest.
              </p>
            </div>
          ) : (
            <div className="relative aspect-video rounded-xl overflow-hidden bg-black flex items-center justify-center">
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                className="w-full h-full object-cover scale-x-[-1]"
              />

              {/* Central Human Framing Guide */}
              <div className="absolute inset-0 border-2 border-dashed border-[#B8860B]/40 rounded-xl pointer-events-none flex items-center justify-center">
                <div className="w-28 h-36 border border-[#B8860B]/60 rounded-lg" />
              </div>

              {/* Automatic On-Device Status Indicator */}
              <div className="absolute top-2 left-2 bg-black/75 backdrop-blur-xs px-2 py-0.5 rounded text-[10px] text-white font-mono flex items-center space-x-1.5">
                <span
                  className={`w-2 h-2 rounded-full ${
                    presenceState === 'PRESENT'
                      ? 'bg-[#22C55E] animate-pulse'
                      : presenceState === 'ABSENT'
                      ? 'bg-red-500 animate-ping'
                      : 'bg-amber-400 animate-pulse'
                  }`}
                />
                <span>
                  {presenceState === 'PRESENT'
                    ? 'GUARDIAN: PRESENT'
                    : presenceState === 'ABSENT'
                    ? 'GUARDIAN: ABSENT'
                    : 'GUARDIAN: INITIALIZING'}
                </span>
              </div>

              {/* Privacy badge */}
              <div className="absolute bottom-2 right-2 bg-black/70 px-2 py-0.5 rounded text-[9px] text-[#94A3B8]">
                100% On-Device • No Recording
              </div>
            </div>
          )}

          <div className="mt-2.5 flex items-center justify-between text-[10px] text-[#94A3B8] pt-2 border-t border-[#1E293B]">
            <span className="flex items-center space-x-1">
              <ShieldCheck className="w-3.5 h-3.5 text-[#B8860B]" />
              <span>Automatic On-Device Presence Detection</span>
            </span>
            <span
              className={`font-bold ${
                presenceState === 'PRESENT'
                  ? 'text-[#22C55E]'
                  : presenceState === 'ABSENT'
                  ? 'text-red-400'
                  : 'text-amber-400'
              }`}
            >
              {presenceState === 'PRESENT'
                ? 'At Study Desk'
                : presenceState === 'ABSENT'
                ? 'Student Away'
                : 'Detecting...'}
            </span>
          </div>
        </div>

        {/* Timer Display Card */}
        <div className="mt-3 bg-white rounded-2xl border border-[#E2E8F0] p-4 text-center shadow-xs">
          <p className="text-[11px] font-bold uppercase tracking-wider text-[#64748B]">
            Active Focused Study Time
          </p>
          <div className="text-4xl font-extrabold text-[#0F172A] font-mono tracking-tight my-1.5">
            {formatTimer(activeStudySeconds)}
          </div>
          <div className="flex items-center justify-center space-x-2 text-[11px] text-[#B8860B] font-medium">
            <span>Target: {topic?.estimatedMinutes || 25} min</span>
            <span>•</span>
            <span>
              {Math.min(100, Math.round((activeStudySeconds / ((topic?.estimatedMinutes || 25) * 60)) * 100))}% Completed
            </span>
          </div>

          {/* Progress Bar */}
          <div className="w-full bg-[#F1F5F9] h-2 rounded-full mt-2.5 overflow-hidden border border-[#E2E8F0]">
            <div
              className="bg-[#B8860B] h-full rounded-full transition-all duration-300"
              style={{
                width: `${Math.min(100, (activeStudySeconds / ((topic?.estimatedMinutes || 25) * 60)) * 100)}%`
              }}
            />
          </div>
        </div>

        {/* Metrics Grid */}
        <div className="grid grid-cols-4 gap-2 mt-3">
          <div className="bg-white rounded-xl border border-[#E2E8F0] p-2 text-center shadow-xs">
            <p className="text-[10px] text-[#64748B] font-medium">Study</p>
            <p className="text-xs font-bold text-[#0F172A] mt-0.5">
              {Math.floor(activeStudySeconds / 60)}m {activeStudySeconds % 60}s
            </p>
          </div>

          <div className="bg-white rounded-xl border border-[#E2E8F0] p-2 text-center shadow-xs">
            <p className="text-[10px] text-[#64748B] font-medium">Paused</p>
            <p className="text-xs font-bold text-[#D97706] mt-0.5">{pauseSeconds}s</p>
          </div>

          <div className="bg-white rounded-xl border border-[#E2E8F0] p-2 text-center shadow-xs">
            <p className="text-[10px] text-[#64748B] font-medium">Break</p>
            <p className="text-xs font-bold text-[#0284C7] mt-0.5">{breakSeconds}s</p>
          </div>

          <div className="bg-white rounded-xl border border-[#E2E8F0] p-2 text-center shadow-xs">
            <p className="text-[10px] text-[#64748B] font-medium">Leaves</p>
            <p className="text-xs font-bold text-[#DC2626] mt-0.5">{interruptionCount}</p>
          </div>
        </div>

        {/* Absence Warning Banner */}
        {presenceState === 'ABSENT' && !isOnBreak && !isManualPaused && (
          <div
            className={`mt-3 p-3 rounded-xl border transition-all ${
              absenceCountdown === 0 || absenceCountdown === null
                ? 'bg-[#FEF2F2] border-[#FECACA] text-[#DC2626]'
                : 'bg-[#FFF7ED] border-[#FFEDD5] text-[#EA580C]'
            }`}
          >
            <div className="flex items-start space-x-2">
              <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
              <div className="text-xs">
                <p className="font-bold">
                  {absenceCountdown === 0 || absenceCountdown === null
                    ? 'FOCUS GUARDIAN ALARM TRIGGERED'
                    : `Absence Detected (${absenceCountdown}s Debounce)`}
                </p>
                <p className="text-[11px] mt-0.5 opacity-90 leading-relaxed">
                  {absenceCountdown === 0 || absenceCountdown === null
                    ? 'Study timer is paused! Voice reminder & Reveille alarm active. Return to camera view to automatically resume.'
                    : 'Focus Guardian detected you stepped away from the study desk. Timer will pause shortly.'}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Bottom Action Controls */}
      <div className="space-y-2 pt-2 border-t border-[#E2E8F0]">
        <div className="grid grid-cols-2 gap-2">
          <button
            onClick={() => setIsManualPaused(prev => !prev)}
            className={`py-2.5 px-3 rounded-xl border text-xs font-bold flex items-center justify-center space-x-1.5 transition-colors ${
              isManualPaused
                ? 'bg-[#B8860B] text-white border-[#B8860B]'
                : 'bg-white text-[#0F172A] border-[#E2E8F0] hover:bg-[#F8FAFC]'
            }`}
          >
            {isManualPaused ? <Play className="w-3.5 h-3.5 fill-current" /> : <Pause className="w-3.5 h-3.5" />}
            <span>{isManualPaused ? 'Resume Session' : 'Pause Session'}</span>
          </button>

          <button
            onClick={() => setIsOnBreak(prev => !prev)}
            className={`py-2.5 px-3 rounded-xl border text-xs font-bold flex items-center justify-center space-x-1.5 transition-colors ${
              isOnBreak
                ? 'bg-[#0284C7] text-white border-[#0284C7]'
                : 'bg-white text-[#0F172A] border-[#E2E8F0] hover:bg-[#F8FAFC]'
            }`}
          >
            <Coffee className="w-3.5 h-3.5" />
            <span>{isOnBreak ? 'End Break' : 'Take 5m Break'}</span>
          </button>
        </div>

        <button
          onClick={handleFinish}
          className="w-full py-3 px-4 rounded-xl bg-[#0F172A] hover:bg-[#1E293B] text-white text-xs font-bold flex items-center justify-center space-x-2 shadow-xs transition-colors"
        >
          <CheckCircle className="w-4 h-4 text-[#22C55E]" />
          <span>Finish & Log Study Session</span>
        </button>
      </div>
    </div>
  );
};
