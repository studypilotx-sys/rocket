import React, { useState, useEffect, useRef } from 'react';
import {
  Book,
  Folder,
  FileText,
  ChevronRight,
  ChevronLeft,
  Plus,
  Trash2,
  Edit3,
  Settings as SettingsIcon,
  Calendar,
  TrendingUp,
  Bot,
  Send,
  Info,
  MoreVertical,
  School,
  X,
  Smartphone,
  Code2,
  Check,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Camera,
  Play,
  Pause,
  Coffee,
  Volume2,
  Vibrate,
  AlertTriangle,
  Eye,
  EyeOff,
  Clock,
  CheckCircle,
  CheckCircle2,
  Paperclip,
  RotateCcw
} from 'lucide-react';
import { StudyMaterialsView } from './components/StudyMaterialsView';
import { SettingsView } from './components/SettingsView';
import { AboutView } from './components/AboutView';
import { BottomNavBar } from './components/BottomNavBar';
import { FocusGuardianSession } from './components/FocusGuardianSession';
import { PinUnlockScreen } from './components/PinUnlockScreen';
import { AuthScreen } from './components/AuthScreen';
import { GoogleSignInScreen } from './components/GoogleSignInScreen';
import { AdaptivePlannerView } from './components/AdaptivePlannerView';
import {
  StudyMaterialItem,
  AppSecuritySettings,
  DEFAULT_SECURITY_SETTINGS,
  AutoLockTimeout,
  AuthUser,
  GoogleAuthUser,
  TopicTestRecord
} from './types';
import {
  safeLocalStorage,
  safeGetJSON,
  safeSetJSON,
  saveMaterialsToDB,
  loadMaterialsFromDB,
  deleteMaterialFromDB,
  createCompactMaterials
} from './utils/storage';

interface UserProfile {
  country: string;
  educationSystem: string;
  grade: string;
  isOnboarded?: boolean;
  createdAt?: number;
  name?: string;
  dailyTargetMinutes?: number;
  evidenceRequired?: boolean;
  googleEmail?: string;
  googleDisplayName?: string;
  googlePhotoUrl?: string;
}

interface Topic {
  id: string;
  chapterId: string;
  name: string;
  orderIndex: number;
  state: 'NOT_STARTED' | 'STUDYING' | 'EVIDENCE_REQUIRED' | 'TEST_AVAILABLE' | 'NEEDS_REVIEW' | 'PASSED' | 'COMPLETED';
  estimatedMinutes: number;
  lastScore?: number;
  testAttempts?: number;
  failedAttempts?: number;
  lastStudiedAt?: number;
  lastTestedAt?: number;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD';
  isImportant?: boolean;
}

interface Chapter {
  id: string;
  subjectId: string;
  name: string;
  orderIndex: number;
  estimatedMinutes: number;
}

interface Subject {
  id: string;
  name: string;
  orderIndex: number;
  colorHex: string;
}

interface GuardianSettings {
  isGuardianEnabled: boolean;
  isAudioReminderEnabled: boolean;
  alertType: 'VOICE_ONLY' | 'VOICE_AND_REVEILLE' | 'OFF';
  volumeLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  isVibrationEnabled: boolean;
}

interface StudySessionRecord {
  id: string;
  topicId: string;
  topicName: string;
  subjectName: string;
  durationSeconds: number;
  activeSeconds: number;
  pauseSeconds: number;
  breakSeconds: number;
  interruptionCount: number;
  absenceEventsCount: number;
  isGuardianEnabled: boolean;
  timestamp: number;
  dateString: string;
  materialId?: string;
  materialName?: string;
}

// Synthesized Reveille Horn via Web Audio API (Authentic Bugle Notes: G3, C4, E4, G4, C5)
function playReveilleSound(volume: 'LOW' | 'MEDIUM' | 'HIGH' = 'MEDIUM', durationMs = 3500) {
  try {
    const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const gainNode = ctx.createGain();
    const gainVal = volume === 'LOW' ? 0.15 : volume === 'MEDIUM' ? 0.35 : 0.6;
    gainNode.gain.setValueAtTime(gainVal, ctx.currentTime);
    gainNode.connect(ctx.destination);

    // Reveille bugle notes: G3 (196), C4 (261.63), E4 (329.63), G4 (392), C5 (523.25)
    const notes = [
      { freq: 196.0, dur: 0.14 },
      { freq: 261.63, dur: 0.28 },
      { freq: 329.63, dur: 0.14 },
      { freq: 261.63, dur: 0.14 },
      { freq: 196.0, dur: 0.14 },
      { freq: 261.63, dur: 0.28 },
      { freq: 329.63, dur: 0.14 },
      { freq: 261.63, dur: 0.14 },
      { freq: 196.0, dur: 0.14 },
      { freq: 261.63, dur: 0.14 },
      { freq: 329.63, dur: 0.14 },
      { freq: 392.0, dur: 0.28 },
      { freq: 329.63, dur: 0.14 },
      { freq: 261.63, dur: 0.28 },
      { freq: 329.63, dur: 0.14 },
      { freq: 261.63, dur: 0.14 },
      { freq: 196.0, dur: 0.28 },
      { freq: 0, dur: 0.1 },
      { freq: 261.63, dur: 0.14 },
      { freq: 329.63, dur: 0.14 },
      { freq: 392.0, dur: 0.14 },
      { freq: 523.25, dur: 0.4 }
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

    setTimeout(() => {
      ctx.close().catch(() => {});
    }, durationMs);
  } catch (e) {
    console.error('AudioContext error:', e);
  }
}

function speakGuardianWarning() {
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

function triggerGuardianVibration() {
  try {
    if ('vibrate' in navigator) {
      navigator.vibrate([300, 200, 300, 200, 500]);
    }
  } catch (e) {}
}

interface ChatMessage {
  id: string;
  sender: 'Student' | 'Pluto';
  content: string;
  time: string;
}

const DEFAULT_CURRICULUM = {
  subjects: [
    { id: 'sub-phys', name: 'Physics', colorHex: '#B8860B', orderIndex: 0 },
    { id: 'sub-chem', name: 'Chemistry', colorHex: '#0F172A', orderIndex: 1 },
    { id: 'sub-math', name: 'Mathematics', colorHex: '#64748B', orderIndex: 2 },
    { id: 'sub-bio', name: 'Biology', colorHex: '#22C55E', orderIndex: 3 }
  ],
  chapters: [
    { id: 'ch-kin', subjectId: 'sub-phys', name: 'Kinematics & Motion', orderIndex: 0, estimatedMinutes: 90 },
    { id: 'ch-laws', subjectId: 'sub-phys', name: 'Laws of Motion', orderIndex: 1, estimatedMinutes: 120 },
    { id: 'ch-work', subjectId: 'sub-phys', name: 'Work, Energy & Power', orderIndex: 2, estimatedMinutes: 75 },
    { id: 'ch-atom', subjectId: 'sub-chem', name: 'Atomic Structure', orderIndex: 0, estimatedMinutes: 80 },
    { id: 'ch-bond', subjectId: 'sub-chem', name: 'Chemical Bonding', orderIndex: 1, estimatedMinutes: 100 },
    { id: 'ch-diff', subjectId: 'sub-math', name: 'Differential Calculus', orderIndex: 0, estimatedMinutes: 120 },
    { id: 'ch-int', subjectId: 'sub-math', name: 'Integral Calculus', orderIndex: 1, estimatedMinutes: 140 },
    { id: 'ch-cell', subjectId: 'sub-bio', name: 'Cellular Biology', orderIndex: 0, estimatedMinutes: 70 }
  ],
  topics: [
    { id: 'top-1', chapterId: 'ch-kin', name: 'Rectilinear Motion & Vectors', orderIndex: 0, state: 'NOT_STARTED' as const, estimatedMinutes: 25 },
    { id: 'top-2', chapterId: 'ch-kin', name: 'Speed, Velocity & Acceleration', orderIndex: 1, state: 'NOT_STARTED' as const, estimatedMinutes: 30 },
    { id: 'top-3', chapterId: 'ch-kin', name: 'Projectile Motion', orderIndex: 2, state: 'NOT_STARTED' as const, estimatedMinutes: 35 },
    { id: 'top-4', chapterId: 'ch-laws', name: "Newton's First & Second Laws", orderIndex: 0, state: 'NOT_STARTED' as const, estimatedMinutes: 30 },
    { id: 'top-5', chapterId: 'ch-laws', name: 'Conservation of Linear Momentum', orderIndex: 1, state: 'NOT_STARTED' as const, estimatedMinutes: 25 },
    { id: 'top-6', chapterId: 'ch-laws', name: 'Friction and Contact Forces', orderIndex: 2, state: 'NOT_STARTED' as const, estimatedMinutes: 30 },
    { id: 'top-7', chapterId: 'ch-work', name: 'Work-Energy Theorem', orderIndex: 0, state: 'NOT_STARTED' as const, estimatedMinutes: 25 },
    { id: 'top-8', chapterId: 'ch-atom', name: 'Bohr Model and Spectra', orderIndex: 0, state: 'NOT_STARTED' as const, estimatedMinutes: 30 },
    { id: 'top-9', chapterId: 'ch-diff', name: 'Limits & Continuity', orderIndex: 0, state: 'NOT_STARTED' as const, estimatedMinutes: 35 },
    { id: 'top-10', chapterId: 'ch-diff', name: 'Standard Derivatives & Chain Rule', orderIndex: 1, state: 'NOT_STARTED' as const, estimatedMinutes: 40 }
  ]
};

const DEFAULT_MATERIALS: StudyMaterialItem[] = [
  {
    id: 'mat-1',
    name: 'Kinematics & Motion Lecture 01.mp4',
    type: 'VIDEO',
    uriOrPath: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
    fileSize: '32.4 MB',
    subjectId: 'sub-phys',
    chapterId: 'ch-kin',
    topicId: 'top-1',
    dateAdded: Date.now() - 86400000 * 2,
    lastOpened: Date.now() - 3600000 * 4,
    durationMinutes: 45,
    notes: 'Key derivations for horizontal range and time of flight.'
  },
  {
    id: 'mat-2',
    name: 'Laws of Motion & Friction Summary Guide.pdf',
    type: 'PDF',
    uriOrPath: 'data:application/pdf;base64,JVBERi0xLjQKJcOkw7zDtsOfCjIgMCBvYmoKPDwvTGVuZ3RoIDM2ND4+CnN0cmVhbQpCVAovRjEgMTIgVGYKNzIgNzEyIFRECltTdHVkeVBpbG90XSBGcmVlIEJvZHkgRGlhZ3JhbXMgJiBOZXd0b24ncyBMYXdzIFN1bW1hcnkgR3VpZGUuClRQCjcyIDY4MCBURENvbXBsZXRlIGNvbmNlcHR1YWwgZ3VpZGUgZm9yIGxlY3R1cmUgcmV2aXNpb24uClRQCjcyIDY1MCBURE5ld3RvbidzIEZpcnN0IExhdzogSW5lcnRpYSAmIE5ldCBCYWxhbmNlZCBGb3JjZXMuClRQCjcyIDYyMCBURE5ld3RvbidzIFNlY29uZExhdzogRiA9IG1hIChEaWZmZXJlbnRpYWwgZm9ybTogRiA9IGRwL2R0KS4KVFByZWZlcmVuY2UgcmVzb3VyY2UgYXR0YWNoZWQuCmVuZHN0cmVhbQplbmRvYmoK',
    fileSize: '2.8 MB',
    subjectId: 'sub-phys',
    chapterId: 'ch-laws',
    topicId: 'top-4',
    dateAdded: Date.now() - 86400000 * 4,
    lastOpened: Date.now() - 3600000 * 24,
    notes: 'Free body diagrams, normal contact forces, and coefficient of friction formulas.'
  },
  {
    id: 'mat-3',
    name: 'Limits & Chain Rule Quick Revision Notes.txt',
    type: 'NOTES',
    uriOrPath: 'Calculus Continuity & Differentiation Summary:\n1. Limit definition: f\'(x) = lim_{h->0} [f(x+h) - f(x)] / h\n2. Product rule: (uv)\' = u\'v + uv\'\n3. Chain rule: dy/dx = (dy/du) * (du/dx)\n4. L\'Hospital\'s rule conditions: 0/0 or inf/inf indeterminate forms.',
    fileSize: '12 KB',
    subjectId: 'sub-math',
    chapterId: 'ch-diff',
    topicId: 'top-9',
    dateAdded: Date.now() - 86400000 * 1,
    notes: 'Important algebraic shortcuts for calculus problem sets.'
  },
  {
    id: 'mat-4',
    name: 'Bohr Model & Quantum Spectra Diagram.png',
    type: 'IMAGE',
    uriOrPath: 'https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=600&q=80',
    fileSize: '1.4 MB',
    subjectId: 'sub-chem',
    chapterId: 'ch-atom',
    topicId: 'top-8',
    dateAdded: Date.now() - 86400000 * 3,
    notes: 'Lyman, Balmer, and Paschen energy transition level diagrams.'
  }
];

// Helper to parse markdown formatting for Pluto chat messages
function renderMarkdownContent(text: string) {
  const lines = text.split('\n');
  return (
    <div className="space-y-1.5 text-xs leading-relaxed">
      {lines.map((line, idx) => {
        const trimmed = line.trim();
        if (!trimmed) return <div key={idx} className="h-1" />;

        if (trimmed.startsWith('### ')) {
          return <h4 key={idx} className="font-bold text-[12px] text-[#0F172A] pt-1.5 pb-0.5">{renderInlineFormatting(trimmed.slice(4))}</h4>;
        }
        if (trimmed.startsWith('## ')) {
          return <h3 key={idx} className="font-bold text-[13px] text-[#0F172A] pt-2 pb-0.5">{renderInlineFormatting(trimmed.slice(3))}</h3>;
        }
        if (trimmed.startsWith('# ')) {
          return <h2 key={idx} className="font-extrabold text-[14px] text-[#0F172A] pt-2 pb-0.5">{renderInlineFormatting(trimmed.slice(2))}</h2>;
        }
        if (trimmed === '---' || trimmed === '***') {
          return <hr key={idx} className="my-1.5 border-[#E2E8F0]" />;
        }
        if (trimmed.startsWith('* ') || trimmed.startsWith('- ') || trimmed.startsWith('• ')) {
          return (
            <div key={idx} className="flex items-start space-x-1.5 ml-1">
              <span className="text-[#B8860B] font-bold mt-0.5 select-none">•</span>
              <span className="flex-1">{renderInlineFormatting(trimmed.replace(/^(\*|-|•)\s+/, ''))}</span>
            </div>
          );
        }
        // Numbered list
        const numMatch = trimmed.match(/^(\d+)\.\s+(.*)$/);
        if (numMatch) {
          return (
            <div key={idx} className="flex items-start space-x-1.5 ml-1">
              <span className="text-[#B8860B] font-bold text-[11px] mt-0.5 select-none">{numMatch[1]}.</span>
              <span className="flex-1">{renderInlineFormatting(numMatch[2])}</span>
            </div>
          );
        }
        if (trimmed.startsWith('> ')) {
          return (
            <blockquote key={idx} className="border-l-2 border-[#B8860B] pl-2 py-0.5 text-[#334155] italic bg-[#F8FAFC] rounded-r my-1">
              {renderInlineFormatting(trimmed.slice(2))}
            </blockquote>
          );
        }
        return <p key={idx}>{renderInlineFormatting(line)}</p>;
      })}
    </div>
  );
}

function renderInlineFormatting(str: string) {
  const parts = str.split(/(\*\*.*?\*\*|\*.*?\*|`.*?`)/g);
  return parts.map((part, i) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length >= 4) {
      return <strong key={i} className="font-bold text-[#0F172A]">{part.slice(2, -2)}</strong>;
    }
    if (part.startsWith('*') && part.endsWith('*') && part.length >= 2 && !part.startsWith('**')) {
      return <em key={i} className="italic">{part.slice(1, -1)}</em>;
    }
    if (part.startsWith('`') && part.endsWith('`') && part.length >= 2) {
      return <code key={i} className="bg-black/5 px-1 py-0.5 rounded font-mono text-[11px] text-[#B8860B]">{part.slice(1, -1)}</code>;
    }
    return part;
  });
}

export default function App() {
  // Persistence state
  const [profile, setProfile] = useState<UserProfile | null>(() => {
    return safeGetJSON('studypilot_profile_v1', null);
  });

  const [subjects, setSubjects] = useState<Subject[]>(() => {
    return safeGetJSON('studypilot_subjects_v1', DEFAULT_CURRICULUM.subjects);
  });

  const [chapters, setChapters] = useState<Chapter[]>(() => {
    return safeGetJSON('studypilot_chapters_v1', DEFAULT_CURRICULUM.chapters);
  });

  const [topics, setTopics] = useState<Topic[]>(() => {
    return safeGetJSON('studypilot_topics_v1', DEFAULT_CURRICULUM.topics);
  });

  const [materials, setMaterials] = useState<StudyMaterialItem[]>(() => {
    return safeGetJSON('studypilot_materials_v1', DEFAULT_MATERIALS);
  });

  // Asynchronously hydrate materials from high-capacity IndexedDB (with full files/data URLs)
  useEffect(() => {
    let isMounted = true;
    loadMaterialsFromDB()
      .then((dbMaterials) => {
        if (isMounted && dbMaterials && dbMaterials.length > 0) {
          setMaterials(dbMaterials);
        }
      })
      .catch((err) => {
        console.warn('Failed to load materials from DB:', err);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Dual-layer persistence for materials: IndexedDB (full size) + localStorage (compact fallback)
  useEffect(() => {
    // 1. High-capacity IndexedDB persistence
    saveMaterialsToDB(materials).catch((err) => {
      console.warn('IndexedDB save warning:', err);
    });

    // 2. Safe compact fallback to avoid browser 5MB quota
    const compact = createCompactMaterials(materials);
    safeSetJSON('studypilot_materials_v1', compact);
  }, [materials]);

  // Selected material for Focus Guardian session
  const [selectedMaterialForFocus, setSelectedMaterialForFocus] = useState<StudyMaterialItem | null>(null);
  const [isViewingMaterialInFocus, setIsViewingMaterialInFocus] = useState(false);

  // Authenticated Student User Session (Email & Password)
  const [authUser, setAuthUser] = useState<AuthUser | null>(() => {
    return safeGetJSON<AuthUser | null>('studypilot_auth_user_v1', null) || safeGetJSON<AuthUser | null>('studypilot_google_auth_v1', null);
  });

  // Validate active session token with backend on mount
  useEffect(() => {
    const stored = safeGetJSON<AuthUser | null>('studypilot_auth_user_v1', null);
    if (stored?.token) {
      fetch('/api/auth/me', {
        headers: { Authorization: `Bearer ${stored.token}` },
      })
        .then((res) => {
          if (res.status === 401) {
            // Session invalidated on server (e.g. logged out elsewhere or password reset)
            setAuthUser(null);
            safeLocalStorage.removeItem('studypilot_auth_user_v1');
            safeLocalStorage.removeItem('studypilot_google_auth_v1');
          } else if (res.ok) {
            return res.json();
          }
        })
        .then((data) => {
          if (data?.user) {
            setAuthUser((prev) => (prev ? { ...prev, ...data.user } : data.user));
          }
        })
        .catch(() => {
          // Preserve local storage session for offline study resilience
        });
    }
  }, []);

  useEffect(() => {
    if (authUser) {
      safeSetJSON('studypilot_auth_user_v1', authUser);
    } else {
      safeLocalStorage.removeItem('studypilot_auth_user_v1');
      safeLocalStorage.removeItem('studypilot_google_auth_v1');
    }
  }, [authUser]);

  const handleSignInSuccess = (user: AuthUser) => {
    setAuthUser(user);
    setProfile((prev) => {
      const updated: UserProfile = prev
        ? {
            ...prev,
            name: prev.name && prev.name !== 'Student' ? prev.name : user.name,
            googleEmail: user.email,
            googleDisplayName: user.name,
            googlePhotoUrl: user.picture,
          }
        : {
            country: 'India',
            educationSystem: 'CBSE',
            grade: 'Grade 10',
            name: user.name,
            googleEmail: user.email,
            googleDisplayName: user.name,
            googlePhotoUrl: user.picture,
            dailyTargetMinutes: 120,
            evidenceRequired: true,
          };
      safeSetJSON('studypilot_profile_v1', updated);
      return updated;
    });
  };

  const handleSignOut = () => {
    const currentToken = authUser?.token;
    setAuthUser(null);
    safeLocalStorage.removeItem('studypilot_auth_user_v1');
    safeLocalStorage.removeItem('studypilot_google_auth_v1');
    setCurrentScreen('HOME');
    setIsSessionActive(false);
    setSelectedTopicForFocus(null);
    setSelectedMaterialForFocus(null);
    if (currentToken) {
      fetch('/api/auth/logout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${currentToken}`,
        },
      }).catch(() => {});
    }
  };

  // Compatibility aliases
  const googleUser = authUser;
  const handleGoogleSignInSuccess = handleSignInSuccess;
  const handleGoogleSignOut = handleSignOut;
  const handleSwitchGoogleAccount = handleSignOut;

  // Navigation state: 'ONBOARDING' | 'HOME' | 'SUBJECTS' | 'CHAPTERS' | 'TOPICS' | 'PLANNER' | 'PROGRESS' | 'PLUTO' | 'SETTINGS' | 'ABOUT' | 'FOCUS_SESSION'
  const [currentScreen, setCurrentScreen] = useState<string>(() => {
    const saved = safeLocalStorage.getItem('studypilot_profile_v1');
    return saved ? 'HOME' : 'ONBOARDING';
  });

  // Focus Guardian settings
  const [guardianSettings, setGuardianSettings] = useState<GuardianSettings>(() => {
    return safeGetJSON('studypilot_guardian_settings_v1', {
      isGuardianEnabled: true,
      isAudioReminderEnabled: true,
      alertType: 'VOICE_AND_REVEILLE',
      volumeLevel: 'MEDIUM',
      isVibrationEnabled: true
    });
  });

  // App Security & PIN Settings
  const [securitySettings, setSecuritySettings] = useState<AppSecuritySettings>(() => {
    return safeGetJSON('studypilot_security_settings_v1', DEFAULT_SECURITY_SETTINGS);
  });

  // App Lock State (locked if PIN enabled on app launch)
  const [isAppLocked, setIsAppLocked] = useState<boolean>(() => {
    const saved = safeGetJSON<AppSecuritySettings>('studypilot_security_settings_v1', DEFAULT_SECURITY_SETTINGS);
    return Boolean(saved?.isPinEnabled && saved?.pinHash);
  });

  const lastUserActivityRef = useRef<number>(Date.now());

  // Recorded study sessions
  const [studySessions, setStudySessions] = useState<StudySessionRecord[]>(() => {
    return safeGetJSON('studypilot_sessions_v1', []);
  });

  // Recorded topic tests & assessments for Adaptive Planner
  const [testRecords, setTestRecords] = useState<TopicTestRecord[]>(() => {
    return safeGetJSON('studypilot_test_records_v1', []);
  });

  useEffect(() => {
    safeSetJSON('studypilot_test_records_v1', testRecords);
  }, [testRecords]);

  // Active Focus Session state
  const [selectedTopicForFocus, setSelectedTopicForFocus] = useState<Topic | null>(null);
  const [isSessionActive, setIsSessionActive] = useState(false);
  const [activeStudySeconds, setActiveStudySeconds] = useState(0);
  const [pauseSeconds, setPauseSeconds] = useState(0);
  const [breakSeconds, setBreakSeconds] = useState(0);
  const [interruptionCount, setInterruptionCount] = useState(0);
  const [absenceEventsCount, setAbsenceEventsCount] = useState(0);
  const [isStudentPresent, setIsStudentPresent] = useState(true);
  const [absenceDebounce, setAbsenceDebounce] = useState<number | null>(null);
  const [isOnBreak, setIsOnBreak] = useState(false);
  const [isManualPaused, setIsManualPaused] = useState(false);
  const [hasCameraPermission, setHasCameraPermission] = useState<boolean | null>(null);
  const [isAlarmSounding, setIsAlarmSounding] = useState(false);
  const [isTestingSettingsAlert, setIsTestingSettingsAlert] = useState(false);
  const videoRef = useRef<HTMLVideoElement | null>(null);

  const [selectedSubjectId, setSelectedSubjectId] = useState<string | null>(null);
  const [selectedChapterId, setSelectedChapterId] = useState<string | null>(null);

  // Modals for CRUD
  const [modalType, setModalType] = useState<'ADD_SUBJECT' | 'RENAME_SUBJECT' | 'DELETE_SUBJECT' | 'ADD_CHAPTER' | 'RENAME_CHAPTER' | 'DELETE_CHAPTER' | 'ADD_TOPIC' | 'RENAME_TOPIC' | 'DELETE_TOPIC' | 'EDIT_PROFILE' | null>(null);
  const [targetItem, setTargetItem] = useState<any>(null);
  const [modalInput, setModalInput] = useState('');

  // Pluto AI state
  const [plutoMessages, setPlutoMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'Pluto',
      content: 'Hello! I am Pluto, your StudyPilot academic assistant. You can ask me to explain concepts, ask for motivation or study recommendations, or attach notes and ask me to generate a practice quiz!',
      time: 'Just now'
    }
  ]);
  const [plutoInput, setPlutoInput] = useState('');
  const [isPlutoThinking, setIsPlutoThinking] = useState(false);
  const [plutoError, setPlutoError] = useState<string | null>(null);
  const [lastFailedPayload, setLastFailedPayload] = useState<{ message: string; isQuiz: boolean } | null>(null);
  const [plutoAttachment, setPlutoAttachment] = useState<{ name: string; isImage: boolean; base64Data?: string; mimeType?: string; contentSnippet?: string } | null>(null);
  const [activePlutoQuiz, setActivePlutoQuiz] = useState<{
    title: string;
    questions: Array<{
      id: number;
      questionText: string;
      options: string[];
      correctOptionIndex: number;
      explanation: string;
    }>;
  } | null>(null);
  const [quizIndex, setQuizIndex] = useState(0);
  const [selectedQuizAnswer, setSelectedQuizAnswer] = useState<number | null>(null);
  const [answeredMap, setAnsweredMap] = useState<Record<number, number>>({});
  const [isQuizCompleted, setIsQuizCompleted] = useState(false);
  const plutoFileInputRef = useRef<HTMLInputElement | null>(null);
  const plutoMessagesEndRef = useRef<HTMLDivElement | null>(null);

  // Auto-scroll Pluto messages
  useEffect(() => {
    if (currentScreen === 'PLUTO' && !activePlutoQuiz) {
      plutoMessagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [plutoMessages, isPlutoThinking, currentScreen, activePlutoQuiz]);

  // View mode: 'device' or 'code'
  const [viewMode, setViewMode] = useState<'device' | 'code'>('device');

  // Save changes to localStorage safely
  useEffect(() => {
    if (profile) {
      safeSetJSON('studypilot_profile_v1', profile);
    }
  }, [profile]);

  useEffect(() => {
    safeSetJSON('studypilot_subjects_v1', subjects);
  }, [subjects]);

  useEffect(() => {
    safeSetJSON('studypilot_chapters_v1', chapters);
  }, [chapters]);

  useEffect(() => {
    safeSetJSON('studypilot_topics_v1', topics);
  }, [topics]);

  useEffect(() => {
    safeSetJSON('studypilot_guardian_settings_v1', guardianSettings);
  }, [guardianSettings]);

  useEffect(() => {
    safeSetJSON('studypilot_sessions_v1', studySessions);
  }, [studySessions]);

  // Persist security settings
  useEffect(() => {
    safeSetJSON('studypilot_security_settings_v1', securitySettings);
  }, [securitySettings]);

  // User activity tracker for Auto-Lock
  useEffect(() => {
    const recordActivity = () => {
      lastUserActivityRef.current = Date.now();
    };

    window.addEventListener('mousedown', recordActivity);
    window.addEventListener('keydown', recordActivity);
    window.addEventListener('touchstart', recordActivity);
    window.addEventListener('scroll', recordActivity, { passive: true });

    return () => {
      window.removeEventListener('mousedown', recordActivity);
      window.removeEventListener('keydown', recordActivity);
      window.removeEventListener('touchstart', recordActivity);
      window.removeEventListener('scroll', recordActivity);
    };
  }, []);

  // Auto-Lock execution: when tab becomes hidden or user is inactive beyond timeout
  useEffect(() => {
    if (!securitySettings.isPinEnabled || !securitySettings.pinHash) {
      if (isAppLocked) setIsAppLocked(false);
      return;
    }

    const getTimeoutMs = (timeout: AutoLockTimeout) => {
      switch (timeout) {
        case 'IMMEDIATE':
          return 0;
        case '1_MIN':
          return 60 * 1000;
        case '5_MIN':
          return 5 * 60 * 1000;
        case '15_MIN':
          return 15 * 60 * 1000;
        default:
          return 60 * 1000;
      }
    };

    const handleVisibility = () => {
      if (document.visibilityState === 'hidden') {
        if (securitySettings.autoLockTimeout === 'IMMEDIATE') {
          setIsAppLocked(true);
        }
      } else if (document.visibilityState === 'visible') {
        const elapsed = Date.now() - lastUserActivityRef.current;
        const limit = getTimeoutMs(securitySettings.autoLockTimeout);
        if (limit === 0 || elapsed >= limit) {
          setIsAppLocked(true);
        }
      }
    };

    document.addEventListener('visibilitychange', handleVisibility);

    // Periodic inactivity check
    const interval = setInterval(() => {
      if (isAppLocked) return;
      const limit = getTimeoutMs(securitySettings.autoLockTimeout);
      if (limit > 0) {
        const elapsed = Date.now() - lastUserActivityRef.current;
        if (elapsed >= limit) {
          setIsAppLocked(true);
        }
      }
    }, 3000);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibility);
      clearInterval(interval);
    };
  }, [securitySettings.isPinEnabled, securitySettings.pinHash, securitySettings.autoLockTimeout, isAppLocked]);

  const handleStartFocusSession = (topic: Topic) => {
    setSelectedTopicForFocus(topic);
    setSelectedMaterialForFocus(null);
    setIsSessionActive(true);
    setCurrentScreen('FOCUS_SESSION');
  };

  const handleFinishFocusSession = (record: StudySessionRecord) => {
    setStudySessions(prev => [record, ...prev]);

    // Update topic state & lastStudiedAt timestamp for adaptive scheduling
    if (selectedTopicForFocus) {
      setTopics(prev => prev.map(t => {
        if (t.id === selectedTopicForFocus.id) {
          const newState = (t.state === 'NOT_STARTED' || t.state === 'STUDYING') ? ('STUDYING' as const) : t.state;
          return {
            ...t,
            state: newState,
            lastStudiedAt: Date.now(),
          };
        }
        return t;
      }));
    }

    if (selectedMaterialForFocus) {
      setMaterials(prev => prev.map(m => m.id === selectedMaterialForFocus.id ? { ...m, lastOpened: Date.now() } : m));
    }

    setIsSessionActive(false);
    if (selectedMaterialForFocus) {
      setCurrentScreen('MATERIALS');
    } else {
      setCurrentScreen('PLANNER');
    }
    setSelectedMaterialForFocus(null);
  };

  const handleRecordTopicTestResult = (topicId: string, score: number, totalQuestions = 5, correctAnswers?: number) => {
    const topic = topics.find(t => t.id === topicId);
    const chapter = chapters.find(c => c.id === topic?.chapterId);
    const subject = subjects.find(s => s.id === chapter?.subjectId);

    const isPassed = score >= 70;
    const newRecord: TopicTestRecord = {
      id: `test-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
      topicId,
      topicName: topic?.name || 'Topic',
      chapterName: chapter?.name,
      subjectName: subject?.name || 'General',
      score,
      passed: isPassed,
      timestamp: Date.now(),
      totalQuestions,
      correctAnswers: correctAnswers ?? Math.round((score / 100) * totalQuestions),
    };

    setTestRecords(prev => [newRecord, ...prev]);

    // Adapt topic state and test history
    setTopics(prev => prev.map(t => {
      if (t.id === topicId) {
        const newAttempts = (t.testAttempts || 0) + 1;
        const newFailed = isPassed ? (t.failedAttempts || 0) : ((t.failedAttempts || 0) + 1);
        const newState = isPassed ? ('PASSED' as const) : ('NEEDS_REVIEW' as const);
        return {
          ...t,
          state: newState,
          lastScore: score,
          testAttempts: newAttempts,
          failedAttempts: newFailed,
          lastTestedAt: Date.now(),
        };
      }
      return t;
    }));
  };

  const handlePlannerStartTopic = (topic: Topic) => {
    const ch = chapters.find(c => c.id === topic.chapterId);
    if (ch) {
      setSelectedSubjectId(ch.subjectId);
      setSelectedChapterId(ch.id);
    }
    handleStartFocusSession(topic);
  };

  const handlePlannerNavigateToChapter = (subjectId: string, chapterId: string) => {
    setSelectedSubjectId(subjectId);
    setSelectedChapterId(chapterId);
    setCurrentScreen('TOPICS');
  };

  const handleStudyWithGuardian = (material: StudyMaterialItem) => {
    setSelectedMaterialForFocus(material);
    let targetTopic = topics.find(t => t.id === material.topicId);
    if (!targetTopic) {
      targetTopic = topics.find(t => t.chapterId === material.chapterId);
    }
    if (!targetTopic) {
      targetTopic = topics[0] || null;
    }
    setSelectedTopicForFocus(targetTopic);
    setIsSessionActive(true);
    setMaterials(prev => prev.map(m => m.id === material.id ? { ...m, lastOpened: Date.now() } : m));
    setCurrentScreen('FOCUS_SESSION');
  };

  const handleAddMaterial = (newMat: StudyMaterialItem) => {
    setMaterials(prev => [newMat, ...prev]);
  };

  const handleUpdateMaterial = (updated: StudyMaterialItem) => {
    setMaterials(prev => prev.map(m => m.id === updated.id ? updated : m));
  };

  const handleDeleteMaterial = async (id: string) => {
    // 1. Immediately update UI state so material disappears instantly
    setMaterials(prev => {
      const remaining = prev.filter(m => m.id !== id);
      // Immediately write compact version to localStorage fallback
      safeSetJSON('studypilot_materials_v1', createCompactMaterials(remaining));
      return remaining;
    });

    // 2. Permanently delete from IndexedDB data store
    try {
      await deleteMaterialFromDB(id);
    } catch (err) {
      console.warn('Failed to delete material from IndexedDB:', err);
    }
  };

  // Selected subject/chapter lookup
  const currentSubject = subjects.find(s => s.id === selectedSubjectId);
  const currentChapter = chapters.find(c => c.id === selectedChapterId);
  const currentSubjectChapters = chapters.filter(c => c.subjectId === selectedSubjectId);
  const currentChapterTopics = topics.filter(t => t.chapterId === selectedChapterId);

  // Onboarding submit
  const handleOnboardingSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const newProfile: UserProfile = {
      country: formData.get('country') as string,
      educationSystem: formData.get('educationSystem') as string,
      grade: formData.get('grade') as string,
      isOnboarded: true,
      createdAt: Date.now()
    };
    setProfile(newProfile);
    setCurrentScreen('HOME');
  };

  // CRUD operations
  const handleAddSubject = (name: string) => {
    if (!name.trim()) return;
    const newSub: Subject = {
      id: 'sub-' + Date.now(),
      name: name.trim(),
      orderIndex: subjects.length,
      colorHex: '#B8860B'
    };
    setSubjects(prev => [...prev, newSub]);
    setModalType(null);
  };

  const handleRenameSubject = (id: string, newName: string) => {
    if (!newName.trim()) return;
    setSubjects(prev => prev.map(s => s.id === id ? { ...s, name: newName.trim() } : s));
    setModalType(null);
  };

  const handleDeleteSubject = (id: string) => {
    setSubjects(prev => prev.filter(s => s.id !== id));
    // Cascade delete chapters & topics
    const deletedChapterIds = chapters.filter(c => c.subjectId === id).map(c => c.id);
    setChapters(prev => prev.filter(c => c.subjectId !== id));
    setTopics(prev => prev.filter(t => !deletedChapterIds.includes(t.chapterId)));
    setModalType(null);
    if (selectedSubjectId === id) {
      setCurrentScreen('SUBJECTS');
    }
  };

  const handleAddChapter = (name: string) => {
    if (!name.trim() || !selectedSubjectId) return;
    const newCh: Chapter = {
      id: 'ch-' + Date.now(),
      subjectId: selectedSubjectId,
      name: name.trim(),
      orderIndex: currentSubjectChapters.length,
      estimatedMinutes: 60
    };
    setChapters(prev => [...prev, newCh]);
    setModalType(null);
  };

  const handleRenameChapter = (id: string, newName: string) => {
    if (!newName.trim()) return;
    setChapters(prev => prev.map(c => c.id === id ? { ...c, name: newName.trim() } : c));
    setModalType(null);
  };

  const handleDeleteChapter = (id: string) => {
    setChapters(prev => prev.filter(c => c.id !== id));
    setTopics(prev => prev.filter(t => t.chapterId !== id));
    setModalType(null);
    if (selectedChapterId === id) {
      setCurrentScreen('CHAPTERS');
    }
  };

  const handleAddTopic = (name: string) => {
    if (!name.trim() || !selectedChapterId) return;
    const newTop: Topic = {
      id: 'top-' + Date.now(),
      chapterId: selectedChapterId,
      name: name.trim(),
      orderIndex: currentChapterTopics.length,
      state: 'NOT_STARTED',
      estimatedMinutes: 25
    };
    setTopics(prev => [...prev, newTop]);
    setModalType(null);
  };

  const handleRenameTopic = (id: string, newName: string) => {
    if (!newName.trim()) return;
    setTopics(prev => prev.map(t => t.id === id ? { ...t, name: newName.trim() } : t));
    setModalType(null);
  };

  const handleDeleteTopic = (id: string) => {
    setTopics(prev => prev.filter(t => t.id !== id));
    setModalType(null);
  };

  const handleSendPluto = async (e?: React.FormEvent, isQuizRequest = false, retryQuery?: string) => {
    if (e) e.preventDefault();
    setPlutoError(null);

    const query = retryQuery !== undefined ? retryQuery : plutoInput.trim();
    if (!query && !isQuizRequest && !plutoAttachment) return;

    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      const offlineMsg = "You appear to be offline. Please verify your internet connection to chat with Pluto.";
      setPlutoError(offlineMsg);
      setPlutoMessages(prev => [
        ...prev,
        {
          id: 'err-' + Date.now(),
          sender: 'Pluto',
          content: `⚠️ **Offline Notice**: ${offlineMsg}`,
          time: 'Just now'
        }
      ]);
      return;
    }

    const userText = isQuizRequest
      ? (query || `Generate a 4-question practice quiz based on ${plutoAttachment?.name || 'my study material'}`)
      : query;

    if (!retryQuery) {
      const newMsg: ChatMessage = {
        id: 'msg-' + Date.now(),
        sender: 'Student',
        content: userText,
        time: 'Just now'
      };
      setPlutoMessages(prev => [...prev, newMsg]);
      setPlutoInput('');
    }

    setIsPlutoThinking(true);
    setLastFailedPayload({ message: userText, isQuiz: isQuizRequest });

    const abortController = new AbortController();
    const timeoutId = setTimeout(() => abortController.abort(), 35000);

    try {
      const res = await fetch('/api/pluto/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        signal: abortController.signal,
        body: JSON.stringify({
          message: userText,
          attachment: plutoAttachment,
          userProfile: profile,
          subjects: subjects.map(s => ({ id: s.id, name: s.name })),
          generateQuiz: isQuizRequest,
          conversationHistory: plutoMessages.slice(-4).map(m => ({ sender: m.sender, content: m.content }))
        })
      });

      clearTimeout(timeoutId);

      const data = await res.json();

      if (!res.ok || data.error) {
        setPlutoError(data.error || "Pluto AI service encountered an error.");
      } else {
        setPlutoError(null);
        setLastFailedPayload(null);
      }

      setPlutoMessages(prev => [
        ...prev,
        {
          id: 'msg-' + (Date.now() + 1),
          sender: 'Pluto',
          content: data.reply || "I'm here to help you study! What topic would you like to review?",
          time: 'Just now'
        }
      ]);

      if (data.quiz && data.quiz.questions?.length > 0) {
        setActivePlutoQuiz(data.quiz);
        setQuizIndex(0);
        setSelectedQuizAnswer(null);
        setAnsweredMap({});
        setIsQuizCompleted(false);
      }
    } catch (err: any) {
      clearTimeout(timeoutId);
      const isAbort = err?.name === 'AbortError';
      const errMsg = isAbort
        ? "Request timed out. Pluto took too long to respond. Please try again."
        : "Failed to communicate with Pluto AI service. Please check your connection and retry.";

      setPlutoError(errMsg);
      setPlutoMessages(prev => [
        ...prev,
        {
          id: 'msg-' + (Date.now() + 1),
          sender: 'Pluto',
          content: `⚠️ **Connection Error**: ${errMsg}`,
          time: 'Just now'
        }
      ]);
    } finally {
      setIsPlutoThinking(false);
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size === 0) {
      setPlutoError("The selected file is empty (0 bytes). Please select a valid study document or image.");
      return;
    }

    if (file.size > 15 * 1024 * 1024) {
      setPlutoError("File exceeds the 15MB limit. Please attach a smaller file.");
      return;
    }

    const isImg = file.type.startsWith('image/');
    const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
    const mime = file.type || (isPdf ? 'application/pdf' : isImg ? 'image/png' : 'text/plain');

    setPlutoError(null);

    const reader = new FileReader();

    reader.onerror = () => {
      setPlutoError("Failed to read the file. Please check file permissions and try again.");
    };

    reader.onload = (event) => {
      const result = event.target?.result as string;
      const base64 = result.includes('base64,') ? result.split('base64,')[1] : '';

      setPlutoAttachment({
        name: file.name,
        isImage: isImg,
        base64Data: base64,
        mimeType: mime,
        contentSnippet: `${file.name} (${Math.round(file.size / 1024)} KB)`
      });

      setPlutoMessages(prev => [
        ...prev,
        {
          id: 'attach-' + Date.now(),
          sender: 'Pluto',
          content: `📎 Attached: **${file.name}** (${isImg ? 'Image' : isPdf ? 'PDF Document' : 'Study Notes'}).\n\nYou can ask questions about this material, or tap **"Quiz me from this"** below to generate a 4-question practice quiz.`,
          time: 'Just now'
        }
      ]);
    };

    reader.readAsDataURL(file);

    if (plutoFileInputRef.current) {
      plutoFileInputRef.current.value = '';
    }
  };

  return (
    <div id="studypilot-root" className="min-h-screen bg-[#F8FAFC] text-[#0F172A] flex flex-col font-sans">
      {/* Top Banner with mode switch and APK info */}
      <header id="studypilot-header" className="bg-[#FFFFFF] border-b border-[#E2E8F0] px-4 py-3 flex flex-wrap items-center justify-between gap-3 shadow-xs">
        <div className="flex items-center space-x-3">
          <div className="w-9 h-9 rounded-lg bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B]">
            <School className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-bold text-base tracking-tight text-[#0F172A]">StudyPilot</span>
              <span className="text-[11px] px-2 py-0.5 rounded bg-[#F1F5F9] text-[#64748B] font-medium border border-[#E2E8F0]">Phase 1 Native Android</span>
            </div>
            <p className="text-xs text-[#64748B]">Created by Mohammad Fahad</p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <button
            id="view-device-btn"
            onClick={() => setViewMode('device')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center space-x-1.5 transition-colors ${
              viewMode === 'device'
                ? 'bg-[#B8860B] text-white shadow-xs'
                : 'bg-white text-[#0F172A] border border-[#E2E8F0] hover:bg-[#F8FAFC]'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>Interactive Android Preview</span>
          </button>
          <button
            id="view-code-btn"
            onClick={() => setViewMode('code')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center space-x-1.5 transition-colors ${
              viewMode === 'code'
                ? 'bg-[#B8860B] text-white shadow-xs'
                : 'bg-white text-[#0F172A] border border-[#E2E8F0] hover:bg-[#F8FAFC]'
            }`}
          >
            <Code2 className="w-3.5 h-3.5" />
            <span>Native Android Codebase</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 flex justify-center items-start p-3 sm:p-6 overflow-y-auto">
        {viewMode === 'code' ? (
          /* Native Android Codebase Inspection View */
          <div className="w-full max-w-4xl bg-white rounded-2xl border border-[#E2E8F0] p-6 shadow-sm">
            <div className="border-b border-[#E2E8F0] pb-4 mb-6">
              <h2 className="text-xl font-bold text-[#0F172A]">Native Android Architecture & Source Files</h2>
              <p className="text-sm text-[#64748B] mt-1">
                Complete Kotlin + Jetpack Compose + Android Gradle + Room Database project structure located in repository root.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-mono">
              <div className="p-4 bg-[#F8FAFC] rounded-xl border border-[#E2E8F0]">
                <h3 className="font-bold text-[#B8860B] text-sm mb-3">Gradle & Manifest Files</h3>
                <ul className="space-y-1.5 text-[#334155]">
                  <li>📁 StudyPilot/</li>
                  <li>├── 📄 settings.gradle.kts</li>
                  <li>├── 📄 build.gradle.kts</li>
                  <li>├── 📄 gradle.properties</li>
                  <li>├── 📄 gradlew & gradlew.bat</li>
                  <li>└── 📁 app/</li>
                  <li>&nbsp;&nbsp;&nbsp;&nbsp;├── 📄 build.gradle.kts (SDK 34, Compose, Room)</li>
                  <li>&nbsp;&nbsp;&nbsp;&nbsp;├── 📄 proguard-rules.pro</li>
                  <li>&nbsp;&nbsp;&nbsp;&nbsp;└── 📁 src/main/</li>
                  <li>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;├── 📄 AndroidManifest.xml</li>
                  <li>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;└── 📁 res/values/ (strings, colors, themes)</li>
                </ul>
              </div>

              <div className="p-4 bg-[#F8FAFC] rounded-xl border border-[#E2E8F0]">
                <h3 className="font-bold text-[#B8860B] text-sm mb-3">Kotlin Source (com.studypilot.app)</h3>
                <ul className="space-y-1.5 text-[#334155]">
                  <li>├── 📄 MainActivity.kt</li>
                  <li>├── 📄 StudyPilotApplication.kt</li>
                  <li>├── 📁 data/model/ (UserProfile, Subject, Chapter, Topic)</li>
                  <li>├── 📁 data/local/ (Room Database, DAOs, Converters)</li>
                  <li>├── 📁 data/repository/ (StudyPilotRepository)</li>
                  <li>├── 📁 ui/viewmodel/ (StudyPilotViewModel)</li>
                  <li>├── 📁 ui/theme/ (Ivory/Camel academic design)</li>
                  <li>├── 📁 ui/navigation/ (StudyPilotNavGraph)</li>
                  <li>└── 📁 ui/screens/ (Onboarding, Home, Subjects, Chapters, Topics, Planner, Progress, Pluto, Settings, About)</li>
                </ul>
              </div>
            </div>

            <div className="mt-6 p-4 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] text-xs text-[#334155] space-y-2">
              <div className="flex items-center space-x-2 font-semibold text-[#B8860B]">
                <Check className="w-4 h-4 text-[#22C55E]" />
                <span>Phase 1 Requirements Strictly Met</span>
              </div>
              <p>• Clean, modular Android project structure ready for Android Studio or <code className="bg-white px-1.5 py-0.5 rounded border border-[#E2E8F0]">./gradlew assembleDebug</code>.</p>
              <p>• Room Database entities and DAOs with full cascade relationships (Subject → Chapter → Topic).</p>
              <p>• Modern Academic palette (White primary, dark contrast, subtle silver borders, elegant gold accents, and parrot-green highlights).</p>
              <p>• About Screen displays exactly: <strong className="text-[#0F172A]">Created by Mohammad Fahad</strong>.</p>
            </div>
          </div>
        ) : (
          /* Smartphone Preview Frame */
          <div className="w-full max-w-[420px] bg-white rounded-[38px] border-[6px] border-[#0F172A] shadow-2xl overflow-hidden flex flex-col min-h-[760px] max-h-[860px] relative">
            {/* Phone Speaker & Camera Notch */}
            <div className="bg-[#0F172A] h-5 w-full flex items-center justify-center relative">
              <div className="w-16 h-2.5 bg-black rounded-full"></div>
              <div className="w-2.5 h-2.5 bg-black rounded-full absolute right-16"></div>
            </div>

            {/* Android Status Bar */}
            <div className="bg-white px-6 py-2 flex items-center justify-between text-[11px] font-medium text-[#64748B] border-b border-[#F1F5F9]">
              <span>9:41 AM</span>
              <div className="flex items-center space-x-1.5">
                <span>5G</span>
                <span>100%</span>
              </div>
            </div>

            {/* Android App Content Area */}
            <div className="flex-1 overflow-y-auto bg-white flex flex-col">
              {!authUser ? (
                <AuthScreen onSignInSuccess={handleSignInSuccess} />
              ) : (
                <>
                  {/* ------------------------------------------------------------- */}
                  {/* SCREEN: ONBOARDING */}
                  {/* ------------------------------------------------------------- */}
                  {currentScreen === 'ONBOARDING' && (
                <div className="p-6 flex-1 flex flex-col justify-between">
                  <div>
                    <div className="inline-block px-2.5 py-1 rounded bg-[#FEFCE8] text-[#B8860B] border border-[#FEF08A] text-xs font-bold tracking-wider mb-4">
                      STUDYPILOT
                    </div>
                    <h1 className="text-2xl font-bold text-[#0F172A] tracking-tight">Configure Your Academic Profile</h1>
                    <p className="text-xs text-[#64748B] mt-2 leading-relaxed">
                      Set up your educational curriculum hierarchy to structure your subjects, chapters, and topics.
                    </p>

                    <form id="onboarding-form" onSubmit={handleOnboardingSubmit} className="mt-6 space-y-4">
                      <div>
                        <label className="block text-xs font-semibold text-[#0F172A] mb-1.5">1. Country</label>
                        <select
                          name="country"
                          defaultValue="India"
                          className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3.5 py-2.5 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                        >
                          <option value="India">India</option>
                          <option value="United States">United States</option>
                          <option value="United Kingdom">United Kingdom</option>
                          <option value="Canada">Canada</option>
                          <option value="Australia">Australia</option>
                          <option value="Singapore">Singapore</option>
                          <option value="United Arab Emirates">United Arab Emirates</option>
                          <option value="International">International</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-xs font-semibold text-[#0F172A] mb-1.5">2. Education System / Board</label>
                        <select
                          name="educationSystem"
                          defaultValue="CBSE"
                          className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3.5 py-2.5 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                        >
                          <option value="CBSE">CBSE (Central Board)</option>
                          <option value="ICSE">ICSE / ISC</option>
                          <option value="State Board">State Board</option>
                          <option value="International">International (IB / Cambridge)</option>
                          <option value="Other">Other National Curriculum</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-xs font-semibold text-[#0F172A] mb-1.5">3. Grade / Year</label>
                        <select
                          name="grade"
                          defaultValue="Grade 10"
                          className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3.5 py-2.5 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                        >
                          <option value="Grade 8">Grade 8</option>
                          <option value="Grade 9">Grade 9</option>
                          <option value="Grade 10">Grade 10</option>
                          <option value="Grade 11">Grade 11</option>
                          <option value="Grade 12">Grade 12</option>
                          <option value="Undergraduate Year 1">Undergraduate Year 1</option>
                          <option value="Undergraduate Year 2">Undergraduate Year 2</option>
                          <option value="Competitive Exams">Competitive Exam Prep</option>
                        </select>
                      </div>

                      <div className="pt-4">
                        <button
                          type="submit"
                          className="w-full bg-[#B8860B] hover:bg-[#996F0A] text-white font-semibold py-3 px-4 rounded-xl text-xs transition-colors shadow-xs"
                        >
                          Initialize Curriculum & Continue
                        </button>
                      </div>
                    </form>
                  </div>

                  <div className="text-center pt-6 pb-2 text-[11px] text-[#64748B]">
                    Created by Mohammad Fahad
                  </div>
                </div>
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: HOME DASHBOARD */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'HOME' && (
                <div className="p-5 space-y-4">
                  <div className="flex items-center justify-between pb-1">
                    <div>
                      <span className="text-[10px] font-bold tracking-widest text-[#B8860B] uppercase">STUDYPILOT</span>
                      <h1 className="text-xl font-bold text-[#0F172A]">Dashboard</h1>
                    </div>
                    <button
                      id="home-settings-btn"
                      onClick={() => setCurrentScreen('SETTINGS')}
                      className="p-2 rounded-xl bg-[#F1F5F9] text-[#0F172A] hover:bg-[#E2E8F0] transition-colors"
                      title="Settings"
                    >
                      <SettingsIcon className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Student Welcome Card */}
                  <div className="bg-white rounded-2xl p-4 border border-[#E2E8F0] shadow-2xs">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center space-x-2 text-xs text-[#64748B]">
                        <School className="w-3.5 h-3.5 text-[#B8860B]" />
                        <span>{new Date().toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })}</span>
                      </div>
                      {googleUser && (
                        <div className="flex items-center space-x-1.5 px-2 py-0.5 rounded-full bg-[#F0FDF4] border border-[#BBF7D0] text-[10px] font-semibold text-[#16A34A]">
                          <span className="w-1.5 h-1.5 rounded-full bg-[#22C55E]"></span>
                          <span className="truncate max-w-[110px]">{googleUser.email.split('@')[0]}</span>
                        </div>
                      )}
                    </div>
                    <div className="flex items-center space-x-3">
                      {googleUser?.picture ? (
                        <img
                          src={googleUser.picture}
                          alt={googleUser.name}
                          className="w-10 h-10 rounded-full border border-[#CBD5E1] object-cover shrink-0"
                          referrerPolicy="no-referrer"
                        />
                      ) : (
                        <div className="w-10 h-10 rounded-full bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] font-bold text-sm shrink-0 shadow-2xs">
                          {(googleUser?.name || profile?.name || 'S').charAt(0).toUpperCase()}
                        </div>
                      )}
                      <div className="min-w-0">
                        <h2 className="text-base font-bold text-[#0F172A] truncate">
                          Welcome back, {googleUser?.givenName || googleUser?.name || profile?.name || 'Student'}
                        </h2>
                        <p className="text-xs text-[#64748B] mt-0.5">
                          {profile?.educationSystem || 'CBSE'} • {profile?.grade || 'Grade 10'} ({profile?.country || 'India'})
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Primary Call to Action: START STUDYING */}
                  <div
                    onClick={() => {
                      if (subjects.length > 0) {
                        setSelectedSubjectId(subjects[0].id);
                        const firstCh = chapters.find(c => c.subjectId === subjects[0].id);
                        if (firstCh) {
                          setSelectedChapterId(firstCh.id);
                          setCurrentScreen('TOPICS');
                          return;
                        }
                        setCurrentScreen('CHAPTERS');
                      } else {
                        setCurrentScreen('SUBJECTS');
                      }
                    }}
                    className="cursor-pointer bg-[#B8860B] hover:bg-[#996F0A] text-white p-4 rounded-2xl shadow-sm transition-all flex items-center justify-between"
                  >
                    <div>
                      <div className="flex items-center space-x-1.5 text-[10px] font-bold tracking-wider uppercase text-white/90">
                        <Shield className="w-3.5 h-3.5" />
                        <span>FOCUS GUARDIAN ACTIVE</span>
                      </div>
                      <p className="text-sm font-extrabold mt-0.5">START STUDYING</p>
                      <p className="text-[11px] text-white/80">Choose Topic → Monitored Focus Session</p>
                    </div>
                    <div className="w-10 h-10 rounded-xl bg-[#0F172A] flex items-center justify-center text-white">
                      <Play className="w-5 h-5 fill-current ml-0.5" />
                    </div>
                  </div>

                  {/* Quick Access List */}
                  <div className="space-y-2">
                    <h3 className="text-xs font-semibold text-[#0F172A] px-1">Curriculum & Navigation</h3>

                    <button
                      id="nav-subjects-btn"
                      onClick={() => setCurrentScreen('SUBJECTS')}
                      className="w-full bg-white hover:bg-[#F8FAFC] p-3.5 rounded-xl border border-[#E2E8F0] flex items-center justify-between text-left transition-colors"
                    >
                      <div className="flex items-center space-x-3">
                        <div className="w-9 h-9 rounded-lg bg-[#FEFCE8] flex items-center justify-center text-[#B8860B]">
                          <Book className="w-4 h-4" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-[#0F172A]">Subjects & Curriculum</p>
                          <p className="text-[11px] text-[#64748B]">{subjects.length} subjects configured</p>
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-[#B8860B]" />
                    </button>

                    <button
                      id="nav-materials-btn"
                      onClick={() => setCurrentScreen('MATERIALS')}
                      className="w-full bg-white hover:bg-[#F8FAFC] p-3.5 rounded-xl border border-[#E2E8F0] flex items-center justify-between text-left transition-colors"
                    >
                      <div className="flex items-center space-x-3">
                        <div className="w-9 h-9 rounded-lg bg-[#FEFCE8] flex items-center justify-center text-[#B8860B]">
                          <FileText className="w-4 h-4" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-[#0F172A]">Study Materials Library</p>
                          <p className="text-[11px] text-[#64748B]">{materials.length} resources • Videos, PDFs & Guardian focus</p>
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-[#B8860B]" />
                    </button>

                    <button
                      id="nav-planner-btn"
                      onClick={() => setCurrentScreen('PLANNER')}
                      className="w-full bg-white hover:bg-[#F8FAFC] p-3.5 rounded-xl border border-[#E2E8F0] flex items-center justify-between text-left transition-colors"
                    >
                      <div className="flex items-center space-x-3">
                        <div className="w-9 h-9 rounded-lg bg-[#FEFCE8] flex items-center justify-center text-[#B8860B]">
                          <Calendar className="w-4 h-4" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-[#0F172A]">Study Planner</p>
                          <p className="text-[11px] text-[#64748B]">Adaptive daily targets & recommendations</p>
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-[#B8860B]" />
                    </button>

                    <button
                      id="nav-progress-btn"
                      onClick={() => setCurrentScreen('PROGRESS')}
                      className="w-full bg-white hover:bg-[#F8FAFC] p-3.5 rounded-xl border border-[#E2E8F0] flex items-center justify-between text-left transition-colors"
                    >
                      <div className="flex items-center space-x-3">
                        <div className="w-9 h-9 rounded-lg bg-[#FEFCE8] flex items-center justify-center text-[#B8860B]">
                          <TrendingUp className="w-4 h-4" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-[#0F172A]">Academic Progress</p>
                          <p className="text-[11px] text-[#64748B]">Mastery tracking & tests</p>
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-[#B8860B]" />
                    </button>

                    <button
                      id="nav-pluto-btn"
                      onClick={() => setCurrentScreen('PLUTO')}
                      className="w-full bg-white hover:bg-[#F8FAFC] p-3.5 rounded-xl border border-[#E2E8F0] flex items-center justify-between text-left transition-colors"
                    >
                      <div className="flex items-center space-x-3">
                        <div className="w-9 h-9 rounded-lg bg-[#FEFCE8] flex items-center justify-center text-[#B8860B]">
                          <Bot className="w-4 h-4" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-[#0F172A]">Pluto Assistant</p>
                          <p className="text-[11px] text-[#64748B]">Academic queries & motivation</p>
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-[#B8860B]" />
                    </button>
                  </div>

                  {/* Study Activity Record Card */}
                  <div className="bg-[#F8FAFC] rounded-xl p-4 border border-[#E2E8F0] text-xs">
                    <div className="flex items-center justify-between mb-2">
                      <p className="font-bold text-[#0F172A]">Recent Focus Sessions</p>
                      <span className="text-[10px] text-[#B8860B] font-semibold">{studySessions.length} total</span>
                    </div>

                    {studySessions.length === 0 ? (
                      <p className="text-[#64748B] text-[11px] leading-relaxed">
                        No study sessions recorded yet today. Open Topics to launch a Focus Guardian session.
                      </p>
                    ) : (
                      <div className="space-y-2 mt-2">
                        {studySessions.slice(0, 3).map(sess => (
                          <div key={sess.id} className="bg-white p-2.5 rounded-lg border border-[#E2E8F0] shadow-2xs">
                            <div className="flex items-center justify-between">
                              <p className="font-bold text-[#0F172A] text-xs">{sess.topicName}</p>
                              {sess.isGuardianEnabled && (
                                <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold bg-[#F0FDF4] text-[#16A34A] border border-[#BBF7D0]">
                                  <ShieldCheck className="w-3 h-3 mr-0.5" /> GUARDIAN VERIFIED
                                </span>
                              )}
                            </div>
                            <div className="flex items-center space-x-3 text-[10px] text-[#64748B] mt-1">
                              <span>Active: {Math.floor(sess.activeSeconds / 60)}m {sess.activeSeconds % 60}s</span>
                              <span>•</span>
                              <span>Paused: {sess.pauseSeconds}s</span>
                              <span>•</span>
                              <span>Interruptions: {sess.interruptionCount}</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: SUBJECTS */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'SUBJECTS' && (
                <div className="p-5 space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => setCurrentScreen('HOME')}
                        className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#0F172A]"
                      >
                        <ChevronLeft className="w-5 h-5" />
                      </button>
                      <h1 className="text-lg font-bold text-[#0F172A]">Curriculum Subjects</h1>
                    </div>

                    <button
                      id="add-subject-btn"
                      onClick={() => {
                        setModalInput('');
                        setModalType('ADD_SUBJECT');
                      }}
                      className="p-2 rounded-xl bg-[#B8860B] text-white hover:bg-[#996F0A] transition-colors"
                      title="Add Subject"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>

                  <p className="text-xs text-[#64748B]">
                    {subjects.length} Subjects configured in your curriculum. Tap to view chapters.
                  </p>

                  <div className="space-y-2.5">
                    {subjects.map(subject => {
                      const chCount = chapters.filter(c => c.subjectId === subject.id).length;
                      const subChapterIds = chapters.filter(c => c.subjectId === subject.id).map(c => c.id);
                      const topCount = topics.filter(t => subChapterIds.includes(t.chapterId)).length;

                      return (
                        <div
                          key={subject.id}
                          className="bg-white rounded-xl border border-[#E2E8F0] p-3.5 flex items-center justify-between hover:border-[#B8860B] transition-colors shadow-2xs"
                        >
                          <div
                            className="flex items-center space-x-3 flex-1 cursor-pointer"
                            onClick={() => {
                              setSelectedSubjectId(subject.id);
                              setCurrentScreen('CHAPTERS');
                            }}
                          >
                            <div className="w-9 h-9 rounded-lg bg-[#FEFCE8] flex items-center justify-center text-[#B8860B]">
                              <Folder className="w-4 h-4" />
                            </div>
                            <div>
                              <p className="text-xs font-bold text-[#0F172A]">{subject.name}</p>
                              <p className="text-[11px] text-[#64748B]">{chCount} Chapters • {topCount} Topics</p>
                            </div>
                          </div>

                          <div className="flex items-center space-x-1">
                            <button
                              onClick={() => {
                                setTargetItem(subject);
                                setModalInput(subject.name);
                                setModalType('RENAME_SUBJECT');
                              }}
                              className="p-1.5 rounded-md hover:bg-[#F1F5F9] text-[#64748B]"
                              title="Rename"
                            >
                              <Edit3 className="w-3.5 h-3.5" />
                            </button>
                            <button
                              onClick={() => {
                                setTargetItem(subject);
                                setModalType('DELETE_SUBJECT');
                              }}
                              className="p-1.5 rounded-md hover:bg-[#F1F5F9] text-rose-600"
                              title="Delete"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </div>
                      );
                    })}

                    {subjects.length === 0 && (
                      <div className="text-center py-10 bg-white rounded-xl border border-[#E2E8F0] p-6 text-xs text-[#64748B]">
                        No subjects yet. Tap + to add a subject.
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: CHAPTERS */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'CHAPTERS' && (
                <div className="p-5 space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => setCurrentScreen('SUBJECTS')}
                        className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#0F172A]"
                      >
                        <ChevronLeft className="w-5 h-5" />
                      </button>
                      <div>
                        <h1 className="text-lg font-bold text-[#0F172A]">{currentSubject?.name || 'Subject'}</h1>
                        <p className="text-[10px] text-[#B8860B] font-medium">Chapters Overview</p>
                      </div>
                    </div>

                    <button
                      id="add-chapter-btn"
                      onClick={() => {
                        setModalInput('');
                        setModalType('ADD_CHAPTER');
                      }}
                      className="p-2 rounded-xl bg-[#B8860B] text-white hover:bg-[#996F0A] transition-colors"
                      title="Add Chapter"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>

                  <p className="text-xs text-[#64748B]">
                    {currentSubjectChapters.length} Chapters in this subject. Tap to view topics.
                  </p>

                  <div className="space-y-2.5">
                    {currentSubjectChapters.map(chapter => {
                      const chTopicCount = topics.filter(t => t.chapterId === chapter.id).length;

                      return (
                        <div
                          key={chapter.id}
                          className="bg-white rounded-xl border border-[#E2E8F0] p-3.5 flex items-center justify-between hover:border-[#B8860B] transition-colors shadow-2xs"
                        >
                          <div
                            className="flex items-center space-x-3 flex-1 cursor-pointer"
                            onClick={() => {
                              setSelectedChapterId(chapter.id);
                              setCurrentScreen('TOPICS');
                            }}
                          >
                            <div className="w-9 h-9 rounded-lg bg-[#FEFCE8] flex items-center justify-center text-[#B8860B]">
                              <Book className="w-4 h-4" />
                            </div>
                            <div>
                              <p className="text-xs font-bold text-[#0F172A]">{chapter.name}</p>
                              <p className="text-[11px] text-[#64748B]">{chTopicCount} Topics</p>
                            </div>
                          </div>

                          <div className="flex items-center space-x-1">
                            <button
                              onClick={() => {
                                setTargetItem(chapter);
                                setModalInput(chapter.name);
                                setModalType('RENAME_CHAPTER');
                              }}
                              className="p-1.5 rounded-md hover:bg-[#F1F5F9] text-[#64748B]"
                              title="Rename"
                            >
                              <Edit3 className="w-3.5 h-3.5" />
                            </button>
                            <button
                              onClick={() => {
                                setTargetItem(chapter);
                                setModalType('DELETE_CHAPTER');
                              }}
                              className="p-1.5 rounded-md hover:bg-[#F1F5F9] text-rose-600"
                              title="Delete"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </div>
                      );
                    })}

                    {currentSubjectChapters.length === 0 && (
                      <div className="text-center py-10 bg-white rounded-xl border border-[#E2E8F0] p-6 text-xs text-[#64748B]">
                        No chapters in this subject. Tap + to add a chapter.
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: TOPICS */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'TOPICS' && (
                <div className="p-5 space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => setCurrentScreen('CHAPTERS')}
                        className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#0F172A]"
                      >
                        <ChevronLeft className="w-5 h-5" />
                      </button>
                      <div>
                        <h1 className="text-lg font-bold text-[#0F172A]">{currentChapter?.name || 'Chapter'}</h1>
                        <p className="text-[10px] text-[#B8860B] font-medium">{currentSubject?.name} • Topics</p>
                      </div>
                    </div>

                    <button
                      id="add-topic-btn"
                      onClick={() => {
                        setModalInput('');
                        setModalType('ADD_TOPIC');
                      }}
                      className="p-2 rounded-xl bg-[#B8860B] text-white hover:bg-[#996F0A] transition-colors"
                      title="Add Topic"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>

                  <p className="text-xs text-[#64748B]">
                    {currentChapterTopics.length} Topics in this chapter. Status is stored persistently.
                  </p>

                  <div className="space-y-2.5">
                    {currentChapterTopics.map(topic => (
                      <div
                        key={topic.id}
                        className="bg-white rounded-xl border border-[#E2E8F0] p-3.5 flex items-center justify-between shadow-2xs"
                      >
                        <div className="flex items-center space-x-3 flex-1">
                          <div className="w-9 h-9 rounded-lg bg-[#FEFCE8] flex items-center justify-center text-[#B8860B]">
                            <FileText className="w-4 h-4" />
                          </div>
                          <div>
                            <p className="text-xs font-bold text-[#0F172A]">{topic.name}</p>
                            <div className="flex items-center space-x-2 mt-1">
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-[#F1F5F9] text-[#64748B] font-medium border border-[#E2E8F0]">
                                {topic.state}
                              </span>
                              <span className="text-[10px] text-[#64748B]">~{topic.estimatedMinutes}m</span>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center space-x-1">
                          <button
                            onClick={() => handleStartFocusSession(topic)}
                            className="px-2.5 py-1 rounded-lg bg-[#B8860B] text-white hover:bg-[#996F0A] text-[11px] font-bold flex items-center space-x-1 shadow-2xs"
                            title="Start Focus Session"
                          >
                            <Play className="w-3 h-3 fill-current" />
                            <span>Study</span>
                          </button>
                          <button
                            onClick={() => {
                              setTargetItem(topic);
                              setModalInput(topic.name);
                              setModalType('RENAME_TOPIC');
                            }}
                            className="p-1.5 rounded-md hover:bg-[#F1F5F9] text-[#64748B]"
                            title="Rename"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => {
                              setTargetItem(topic);
                              setModalType('DELETE_TOPIC');
                            }}
                            className="p-1.5 rounded-md hover:bg-[#F1F5F9] text-rose-600"
                            title="Delete"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    ))}

                    {currentChapterTopics.length === 0 && (
                      <div className="text-center py-10 bg-white rounded-xl border border-[#E2E8F0] p-6 text-xs text-[#64748B]">
                        No topics in this chapter. Tap + to add a study topic.
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: PLANNER (Real StudyPilot Adaptive Planner) */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'PLANNER' && (
                <AdaptivePlannerView
                  topics={topics}
                  chapters={chapters}
                  subjects={subjects}
                  studySessions={studySessions}
                  testRecords={testRecords}
                  profile={profile}
                  onBack={() => setCurrentScreen('HOME')}
                  onStartTopic={handlePlannerStartTopic}
                  onNavigateToChapter={handlePlannerNavigateToChapter}
                  onRecordTestResult={handleRecordTopicTestResult}
                  onUpdateTopicState={(topicId, newState) => {
                    setTopics(prev => prev.map(t => t.id === topicId ? { ...t, state: newState } : t));
                  }}
                />
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: PROGRESS (Foundation) */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'PROGRESS' && (
                <div className="p-5 flex-1 flex flex-col justify-between">
                  <div>
                    <div className="flex items-center space-x-2 mb-4">
                      <button
                        onClick={() => setCurrentScreen('HOME')}
                        className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#0F172A]"
                      >
                        <ChevronLeft className="w-5 h-5" />
                      </button>
                      <h1 className="text-lg font-bold text-[#0F172A]">Academic Progress</h1>
                    </div>

                    {studySessions.length === 0 ? (
                      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-6 text-center shadow-2xs mt-8">
                        <div className="w-12 h-12 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] mx-auto mb-3">
                          <TrendingUp className="w-6 h-6" />
                        </div>
                        <h2 className="text-base font-bold text-[#0F172A]">Academic Analytics & Mastery</h2>
                        <p className="text-xs text-[#64748B] mt-2 font-medium">
                          No study data recorded yet.
                        </p>
                        <p className="text-[11px] text-[#334155] mt-2 leading-relaxed">
                          Complete Focus Guardian sessions on topics to generate verified study streaks, active minutes, and focus metrics.
                        </p>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {/* Summary Metrics */}
                        <div className="grid grid-cols-2 gap-2.5">
                          <div className="bg-white rounded-xl border border-[#E2E8F0] p-3 shadow-2xs">
                            <p className="text-[10px] text-[#64748B] font-medium">Total Focus Time</p>
                            <p className="text-lg font-extrabold text-[#0F172A] mt-0.5">
                              {Math.floor(studySessions.reduce((acc, s) => acc + s.activeSeconds, 0) / 60)}m {studySessions.reduce((acc, s) => acc + s.activeSeconds, 0) % 60}s
                            </p>
                            <p className="text-[9px] text-[#22C55E] font-semibold mt-0.5">Verified focus tracking</p>
                          </div>

                          <div className="bg-white rounded-xl border border-[#E2E8F0] p-3 shadow-2xs">
                            <p className="text-[10px] text-[#64748B] font-medium">Focus Sessions</p>
                            <p className="text-lg font-extrabold text-[#0F172A] mt-0.5">
                              {studySessions.length}
                            </p>
                            <p className="text-[9px] text-[#B8860B] font-semibold mt-0.5">
                              {studySessions.filter(s => s.isGuardianEnabled).length} Guardian Monitored
                            </p>
                          </div>
                        </div>

                        {/* Completed Sessions Log */}
                        <div>
                          <h3 className="text-xs font-bold text-[#0F172A] mb-2 px-0.5">Session History & Verification</h3>
                          <div className="space-y-2">
                            {studySessions.map(sess => (
                              <div key={sess.id} className="bg-white p-3 rounded-xl border border-[#E2E8F0] shadow-2xs">
                                <div className="flex items-center justify-between">
                                  <p className="text-xs font-bold text-[#0F172A]">{sess.topicName}</p>
                                  {sess.isGuardianEnabled ? (
                                    <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold bg-[#F0FDF4] text-[#16A34A] border border-[#BBF7D0]">
                                      <ShieldCheck className="w-3 h-3 mr-0.5" /> GUARDIAN VERIFIED
                                    </span>
                                  ) : (
                                    <span className="text-[9px] text-[#64748B] font-medium">Standard Timer</span>
                                  )}
                                </div>
                                <div className="flex items-center space-x-3 text-[10px] text-[#64748B] mt-1.5">
                                  <span>Active: {Math.floor(sess.activeSeconds / 60)}m {sess.activeSeconds % 60}s</span>
                                  <span>•</span>
                                  <span>Paused: {sess.pauseSeconds}s</span>
                                  <span>•</span>
                                  <span>Leaves: {sess.interruptionCount}</span>
                                </div>
                                <p className="text-[9px] text-[#64748B] mt-1 opacity-75">
                                  {new Date(sess.completedAt).toLocaleString()}
                                </p>
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: PLUTO AI ACADEMIC COMPANION */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'PLUTO' && (
                <div className="flex-1 flex flex-col justify-between h-full bg-white">
                  {/* Pluto Header */}
                  <div className="p-3.5 border-b border-[#E2E8F0] flex items-center justify-between bg-white shadow-2xs">
                    <div className="flex items-center space-x-2.5">
                      <button
                        onClick={() => {
                          if (activePlutoQuiz) {
                            setActivePlutoQuiz(null);
                          } else {
                            setCurrentScreen('HOME');
                          }
                        }}
                        className="p-1 rounded-lg hover:bg-[#F1F5F9] text-[#0F172A]"
                      >
                        <ChevronLeft className="w-5 h-5" />
                      </button>
                      <div className="w-8 h-8 rounded-lg bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B]">
                        <Bot className="w-5 h-5" />
                      </div>
                      <div>
                        <h2 className="text-xs font-bold text-[#0F172A]">
                          {activePlutoQuiz ? 'Pluto Practice Quiz' : 'Pluto AI Assistant'}
                        </h2>
                        <p className="text-[10px] text-[#64748B]">
                          {activePlutoQuiz ? 'Self-Assessment (Unofficial)' : 'Academic Companion & Study Coach'}
                        </p>
                      </div>
                    </div>

                    <input
                      type="file"
                      ref={plutoFileInputRef}
                      onChange={handleFileUpload}
                      className="hidden"
                      accept="image/*,.pdf,.txt,.md"
                    />

                    {!activePlutoQuiz && (
                      <button
                        onClick={() => plutoFileInputRef.current?.click()}
                        className="px-2.5 py-1.5 rounded-lg bg-[#FEFCE8] hover:bg-[#FEF08A] text-[#B8860B] text-[10px] font-bold border border-[#FEF08A] flex items-center space-x-1"
                        title="Attach notes, photo or document"
                      >
                        <Paperclip className="w-3.5 h-3.5" />
                        <span>Attach Notes</span>
                      </button>
                    )}
                  </div>

                  {/* ACTIVE PLUTO PRACTICE QUIZ VIEW */}
                  {activePlutoQuiz ? (
                    <div className="flex-1 p-4 overflow-y-auto flex flex-col justify-between">
                      {!isQuizCompleted ? (
                        <div className="space-y-4">
                          {/* Progress & Header */}
                          <div>
                            <div className="flex items-center justify-between text-xs text-[#64748B] font-semibold pb-1.5">
                              <span>Question {quizIndex + 1} of {activePlutoQuiz.questions.length}</span>
                              <span className="text-[10px] bg-[#FEFCE8] px-2 py-0.5 rounded font-bold text-[#B8860B] border border-[#FEF08A]">
                                Practice Mode (Unofficial)
                              </span>
                            </div>
                            <div className="w-full bg-[#E2E8F0] h-1.5 rounded-full overflow-hidden">
                              <div
                                className="bg-[#B8860B] h-full transition-all duration-300 rounded-full"
                                style={{
                                  width: `${((quizIndex + (selectedQuizAnswer !== null ? 1 : 0)) / activePlutoQuiz.questions.length) * 100}%`
                                }}
                              />
                            </div>
                          </div>

                          {/* Question Card */}
                          <div className="bg-white rounded-xl p-4 border border-[#E2E8F0] shadow-2xs">
                            <span className="text-[10px] uppercase tracking-wider font-bold text-[#B8860B] mb-1 block">
                              Question {quizIndex + 1}
                            </span>
                            <h3 className="text-sm font-bold text-[#0F172A] leading-snug">
                              {activePlutoQuiz.questions[quizIndex]?.questionText}
                            </h3>
                          </div>

                          {/* Options A, B, C, D */}
                          <div className="space-y-2">
                            {activePlutoQuiz.questions[quizIndex]?.options.map((opt, optIdx) => {
                              const isAnswered = selectedQuizAnswer !== null;
                              const isCorrect = optIdx === activePlutoQuiz.questions[quizIndex].correctOptionIndex;
                              const isChosen = selectedQuizAnswer === optIdx;

                              let cardStyle = "bg-white border-[#E2E8F0] text-[#0F172A] hover:border-[#B8860B]";
                              if (isAnswered) {
                                if (isCorrect) {
                                  cardStyle = "bg-emerald-50 border-emerald-500 text-emerald-950 font-semibold shadow-2xs";
                                } else if (isChosen) {
                                  cardStyle = "bg-red-50 border-red-400 text-red-950 font-medium";
                                } else {
                                  cardStyle = "bg-[#F8FAFC] border-[#E2E8F0] text-[#64748B] opacity-60";
                                }
                              }

                              return (
                                <button
                                  key={optIdx}
                                  disabled={isAnswered}
                                  onClick={() => {
                                    setSelectedQuizAnswer(optIdx);
                                    setAnsweredMap(prev => ({ ...prev, [quizIndex]: optIdx }));
                                  }}
                                  className={`w-full text-left p-3 rounded-xl border text-xs transition-all flex items-start space-x-2.5 ${cardStyle}`}
                                >
                                  <span className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold border shrink-0 mt-0.5 ${
                                    isAnswered && isCorrect
                                      ? 'border-emerald-600 bg-emerald-600 text-white'
                                      : isAnswered && isChosen
                                      ? 'border-red-500 bg-red-500 text-white'
                                      : 'border-current'
                                  }`}>
                                    {String.fromCharCode(65 + optIdx)}
                                  </span>
                                  <span className="flex-1 leading-snug">{opt}</span>
                                </button>
                              );
                            })}
                          </div>

                          {/* Explanation Card */}
                          {selectedQuizAnswer !== null && (
                            <div className="bg-[#F8FAFC] rounded-xl p-3.5 border border-[#E2E8F0] text-xs">
                              <div className="flex items-center space-x-1.5 font-bold mb-1">
                                {selectedQuizAnswer === activePlutoQuiz.questions[quizIndex].correctOptionIndex ? (
                                  <>
                                    <Check className="w-4 h-4 text-emerald-600" />
                                    <span className="text-emerald-800">Correct!</span>
                                  </>
                                ) : (
                                  <>
                                    <X className="w-4 h-4 text-red-600" />
                                    <span className="text-red-800">Explanation:</span>
                                  </>
                                )}
                              </div>
                              <p className="text-[#334155] text-[11px] leading-relaxed">
                                {activePlutoQuiz.questions[quizIndex].explanation}
                              </p>
                            </div>
                          )}
                        </div>
                      ) : (
                        /* Quiz Finished Score Card & Review */
                        <div className="flex-1 flex flex-col items-center p-2 space-y-4">
                          <div className="w-14 h-14 rounded-2xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shadow-2xs">
                            <CheckCircle2 className="w-8 h-8" />
                          </div>
                          <div className="text-center">
                            <h3 className="text-base font-bold text-[#0F172A]">Practice Quiz Completed!</h3>
                            <p className="text-xs text-[#64748B] mt-0.5">{activePlutoQuiz.title}</p>
                          </div>

                          {/* Score and Stats */}
                          {(() => {
                            const totalQ = activePlutoQuiz.questions.length;
                            const correctQ = Object.entries(answeredMap).filter(([qIdx, ans]) => activePlutoQuiz.questions[Number(qIdx)]?.correctOptionIndex === ans).length;
                            const incorrectQ = totalQ - correctQ;
                            const pct = Math.round((correctQ / totalQ) * 100);

                            return (
                              <div className="w-full bg-white rounded-2xl border border-[#E2E8F0] p-4 shadow-2xs text-center">
                                <p className="text-[11px] text-[#64748B] font-medium">Practice Assessment Score</p>
                                <p className="text-3xl font-extrabold text-[#0F172A] my-1 font-mono">
                                  {correctQ} / {totalQ}
                                </p>
                                <p className="text-xs font-bold text-[#B8860B]">{pct}% Mastery on Practice</p>

                                <div className="grid grid-cols-2 gap-2 mt-3 pt-3 border-t border-[#E2E8F0]">
                                  <div className="bg-emerald-50 border border-emerald-200 rounded-lg py-1.5 px-2">
                                    <p className="text-[10px] text-emerald-800 font-bold">{correctQ} Correct</p>
                                  </div>
                                  <div className="bg-red-50 border border-red-200 rounded-lg py-1.5 px-2">
                                    <p className="text-[10px] text-red-800 font-bold">{incorrectQ} Incorrect</p>
                                  </div>
                                </div>
                              </div>
                            );
                          })()}

                          {/* Question by Question Review */}
                          <div className="w-full space-y-2 text-left">
                            <p className="text-xs font-bold text-[#0F172A]">Question Review</p>
                            {activePlutoQuiz.questions.map((q, idx) => {
                              const chosen = answeredMap[idx];
                              const isCorrect = chosen === q.correctOptionIndex;
                              return (
                                <div key={idx} className="bg-white rounded-xl p-3 border border-[#E2E8F0] text-xs">
                                  <div className="flex items-start justify-between gap-2">
                                    <p className="font-bold text-[#0F172A] flex-1">
                                      {idx + 1}. {q.questionText}
                                    </p>
                                    <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded shrink-0 ${
                                      isCorrect ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'
                                    }`}>
                                      {isCorrect ? 'Correct' : 'Incorrect'}
                                    </span>
                                  </div>
                                  <p className="text-[11px] text-[#64748B] mt-1.5">
                                    Your answer: <span className="font-semibold text-[#0F172A]">{q.options[chosen] || 'None'}</span>
                                  </p>
                                  {!isCorrect && (
                                    <p className="text-[11px] text-emerald-700 mt-0.5">
                                      Correct answer: <span className="font-semibold">{q.options[q.correctOptionIndex]}</span>
                                    </p>
                                  )}
                                  <p className="text-[10px] text-[#334155] mt-1 italic bg-[#F8FAFC] p-2 rounded border border-[#E2E8F0]">
                                    {q.explanation}
                                  </p>
                                </div>
                              );
                            })}
                          </div>

                          {/* Explicit Unofficial Notice */}
                          <div className="bg-[#F8FAFC] p-3 rounded-xl border border-[#E2E8F0] text-center w-full">
                            <p className="text-[10px] text-[#64748B] leading-relaxed">
                              🔒 <strong>Practice Assessment Notice</strong>: This quiz is for self-assessment only and does <strong>not</strong> change your official StudyPilot Topic Mastery, Chapter Completion, or Subject Progress. Only official 5-question topic tests update official progress.
                            </p>
                          </div>
                        </div>
                      )}

                      {/* Quiz Navigation Footer */}
                      <div className="pt-3 border-t border-[#E2E8F0] flex items-center justify-between">
                        {!isQuizCompleted ? (
                          <>
                            <button
                              onClick={() => setActivePlutoQuiz(null)}
                              className="text-xs text-[#64748B] hover:text-[#0F172A] font-medium"
                            >
                              Exit Quiz
                            </button>
                            <button
                              disabled={selectedQuizAnswer === null}
                              onClick={() => {
                                if (quizIndex + 1 < activePlutoQuiz.questions.length) {
                                  setQuizIndex(i => i + 1);
                                  setSelectedQuizAnswer(null);
                                } else {
                                  setIsQuizCompleted(true);
                                }
                              }}
                              className="px-4 py-2 rounded-xl bg-[#B8860B] text-white text-xs font-bold disabled:opacity-40 hover:bg-[#996F0A] transition-colors"
                            >
                              {quizIndex + 1 < activePlutoQuiz.questions.length ? 'Next Question →' : 'See Results'}
                            </button>
                          </>
                        ) : (
                          <div className="w-full flex space-x-2">
                            <button
                              onClick={() => {
                                setQuizIndex(0);
                                setSelectedQuizAnswer(null);
                                setAnsweredMap({});
                                setIsQuizCompleted(false);
                              }}
                              className="flex-1 py-2.5 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] text-[#B8860B] text-xs font-bold hover:bg-[#FEF08A] transition-colors flex items-center justify-center space-x-1"
                            >
                              <RotateCcw className="w-3.5 h-3.5" />
                              <span>Retake Quiz</span>
                            </button>
                            <button
                              onClick={() => setActivePlutoQuiz(null)}
                              className="flex-1 py-2.5 rounded-xl bg-[#B8860B] text-white text-xs font-bold hover:bg-[#996F0A] transition-colors"
                            >
                              Return to Chat
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  ) : (
                    /* NORMAL PLUTO CHAT VIEW */
                    <>
                      <div className="flex-1 p-4 space-y-3 overflow-y-auto">
                        {plutoMessages.map(msg => {
                          const isUser = msg.sender === 'Student';
                          return (
                            <div
                              key={msg.id}
                              className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}
                            >
                              <div
                                className={`max-w-[88%] rounded-2xl p-3 shadow-2xs ${
                                  isUser
                                    ? 'bg-[#B8860B] text-white rounded-br-xs text-xs'
                                    : 'bg-white text-[#0F172A] border border-[#E2E8F0] rounded-bl-xs'
                                }`}
                              >
                                <div className="text-[10px] font-semibold opacity-75 mb-1 flex items-center justify-between">
                                  <span>{msg.sender}</span>
                                  <span className="text-[9px] opacity-70 ml-2">{msg.time}</span>
                                </div>
                                {isUser ? (
                                  <p className="whitespace-pre-wrap leading-relaxed">{msg.content}</p>
                                ) : (
                                  renderMarkdownContent(msg.content)
                                )}
                              </div>
                            </div>
                          );
                        })}

                        {isPlutoThinking && (
                          <div className="flex justify-start">
                            <div className="bg-white text-[#B8860B] border border-[#E2E8F0] rounded-2xl rounded-bl-xs p-3 text-xs flex items-center space-x-2.5 shadow-2xs">
                              <span className="w-2.5 h-2.5 rounded-full bg-[#B8860B] animate-ping" />
                              <span className="font-semibold text-[11px]">Pluto is thinking...</span>
                            </div>
                          </div>
                        )}

                        {/* Error Banner with Retry */}
                        {plutoError && lastFailedPayload && !isPlutoThinking && (
                          <div className="bg-red-50 border border-red-200 rounded-xl p-3 text-xs flex items-center justify-between shadow-2xs">
                            <div className="flex items-center space-x-2 text-red-800">
                              <AlertTriangle className="w-4 h-4 shrink-0 text-red-600" />
                              <span className="text-[11px] leading-tight">{plutoError}</span>
                            </div>
                            <button
                              onClick={() => handleSendPluto(undefined, lastFailedPayload.isQuiz, lastFailedPayload.message)}
                              className="px-2.5 py-1 bg-red-600 text-white rounded-lg text-[10px] font-bold hover:bg-red-700 shrink-0 ml-2"
                            >
                              Retry
                            </button>
                          </div>
                        )}

                        <div ref={plutoMessagesEndRef} />
                      </div>

                      {/* Attachment Preview Banner */}
                      {plutoAttachment && (
                        <div className="mx-3 p-2.5 bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl flex items-center justify-between text-xs shadow-2xs">
                          <div className="flex items-center space-x-2 truncate">
                            <Paperclip className="w-3.5 h-3.5 text-[#B8860B] shrink-0" />
                            <span className="font-semibold text-[#0F172A] truncate text-[11px]">{plutoAttachment.name}</span>
                            <span className="text-[10px] bg-[#F1F5F9] text-[#64748B] px-1.5 py-0.5 rounded">
                              {plutoAttachment.isImage ? 'Image' : 'Document'}
                            </span>
                          </div>
                          <div className="flex items-center space-x-1.5 shrink-0">
                            <button
                              onClick={() => handleSendPluto(undefined, true)}
                              className="px-2.5 py-1 rounded-lg bg-[#B8860B] hover:bg-[#996F0A] text-white text-[10px] font-bold shadow-2xs flex items-center space-x-1"
                            >
                              <span>Quiz me from this</span>
                            </button>
                            <button
                              onClick={() => setPlutoAttachment(null)}
                              className="p-1 text-[#64748B] hover:text-red-600"
                              title="Remove attachment"
                            >
                              <X className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      )}

                      {/* Quick prompt suggestions */}
                      <div className="px-3 pt-2 flex items-center space-x-1.5 overflow-x-auto no-scrollbar">
                        <button
                          onClick={() => { setPlutoInput("How are you today?"); }}
                          className="px-2.5 py-1 rounded-lg bg-white border border-[#E2E8F0] hover:border-[#B8860B] text-[10px] text-[#64748B] whitespace-nowrap shadow-2xs"
                        >
                          How are you?
                        </button>
                        <button
                          onClick={() => { setPlutoInput("Motivate me."); }}
                          className="px-2.5 py-1 rounded-lg bg-white border border-[#E2E8F0] hover:border-[#B8860B] text-[10px] text-[#64748B] whitespace-nowrap shadow-2xs"
                        >
                          Motivate me
                        </button>
                        <button
                          onClick={() => { setPlutoInput("What is Newton's second law?"); }}
                          className="px-2.5 py-1 rounded-lg bg-white border border-[#E2E8F0] hover:border-[#B8860B] text-[10px] text-[#64748B] whitespace-nowrap shadow-2xs"
                        >
                          Newton's second law
                        </button>
                        <button
                          onClick={() => { setPlutoInput("How do I add a chapter in StudyPilot?"); }}
                          className="px-2.5 py-1 rounded-lg bg-white border border-[#E2E8F0] hover:border-[#B8860B] text-[10px] text-[#64748B] whitespace-nowrap shadow-2xs"
                        >
                          How to add a chapter
                        </button>
                      </div>

                      {/* Pluto Chat Input Form */}
                      <form onSubmit={handleSendPluto} className="p-3 bg-white border-t border-[#E2E8F0] flex items-center space-x-2">
                        <button
                          type="button"
                          onClick={() => plutoFileInputRef.current?.click()}
                          className="p-2 rounded-xl bg-[#FEFCE8] hover:bg-[#FEF08A] text-[#B8860B] border border-[#FEF08A] shrink-0"
                          title="Attach document, PDF, or image"
                        >
                          <Paperclip className="w-4 h-4" />
                        </button>

                        <input
                          type="text"
                          value={plutoInput}
                          onChange={e => setPlutoInput(e.target.value)}
                          placeholder={plutoAttachment ? "Ask about attachment or request quiz..." : "Ask Pluto to explain, motivate, or quiz..."}
                          className="flex-1 bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                        />

                        <button
                          type="submit"
                          disabled={isPlutoThinking || (!plutoInput.trim() && !plutoAttachment)}
                          className="p-2 rounded-xl bg-[#B8860B] text-white hover:bg-[#996F0A] disabled:opacity-40 transition-colors shadow-2xs"
                        >
                          <Send className="w-4 h-4" />
                        </button>
                      </form>
                    </>
                  )}
                </div>
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: SETTINGS */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'SETTINGS' && (
                <SettingsView
                  profile={profile}
                  subjects={subjects}
                  materials={materials}
                  guardianSettings={guardianSettings}
                  onUpdateGuardianSettings={setGuardianSettings}
                  onUpdateProfile={(updated) => {
                    setProfile(prev => prev ? { ...prev, ...updated } : null);
                  }}
                  onNavigateToSubjects={() => setCurrentScreen('SUBJECTS')}
                  onNavigateToMaterials={() => setCurrentScreen('MATERIALS')}
                  onNavigateToAbout={() => setCurrentScreen('ABOUT')}
                  onBack={() => setCurrentScreen('HOME')}
                  onTestGuardianAlarm={() => {
                    setIsTestingSettingsAlert(true);
                    if (guardianSettings.isAudioReminderEnabled) {
                      speakGuardianWarning();
                    }
                    if (guardianSettings.alertType === 'VOICE_AND_REVEILLE') {
                      setTimeout(() => {
                        playReveilleSound(guardianSettings.volumeLevel, 3500);
                      }, 1800);
                    }
                    if (guardianSettings.isVibrationEnabled) {
                      triggerGuardianVibration();
                    }
                    setTimeout(() => {
                      setIsTestingSettingsAlert(false);
                    }, 4500);
                  }}
                  isTestingAlert={isTestingSettingsAlert}
                  securitySettings={securitySettings}
                  onUpdateSecuritySettings={setSecuritySettings}
                  onLockAppNow={() => setIsAppLocked(true)}
                  authUser={authUser}
                  onSignOut={handleSignOut}
                  googleUser={googleUser}
                  onGoogleSignOut={handleGoogleSignOut}
                  onSwitchGoogleAccount={handleSwitchGoogleAccount}
                />
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: STUDY MATERIALS */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'MATERIALS' && (
                <StudyMaterialsView
                  materials={materials}
                  subjects={subjects}
                  chapters={chapters}
                  topics={topics}
                  onAddMaterial={handleAddMaterial}
                  onUpdateMaterial={handleUpdateMaterial}
                  onDeleteMaterial={handleDeleteMaterial}
                  onStudyWithGuardian={handleStudyWithGuardian}
                  onBack={() => setCurrentScreen('HOME')}
                />
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: ABOUT */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'ABOUT' && (
                <AboutView onBack={() => setCurrentScreen('SETTINGS')} />
              )}

              {/* ------------------------------------------------------------- */}
              {/* SCREEN: FOCUS SESSION (Real Focus Guardian Presence Monitoring) */}
              {/* ------------------------------------------------------------- */}
              {currentScreen === 'FOCUS_SESSION' && (
                <FocusGuardianSession
                  topic={selectedTopicForFocus}
                  subject={subjects.find(s => s.id === (chapters.find(c => c.id === selectedTopicForFocus?.chapterId)?.subjectId || selectedMaterialForFocus?.subjectId))}
                  chapter={chapters.find(c => c.id === (selectedTopicForFocus?.chapterId || selectedMaterialForFocus?.chapterId))}
                  material={selectedMaterialForFocus}
                  guardianSettings={guardianSettings}
                  onExitMaterial={() => setSelectedMaterialForFocus(null)}
                  onFinishSession={handleFinishFocusSession}
                  onExitSession={() => {
                    setIsSessionActive(false);
                    if (selectedMaterialForFocus) {
                      setCurrentScreen('MATERIALS');
                    } else {
                      setCurrentScreen('TOPICS');
                    }
                    setSelectedMaterialForFocus(null);
                  }}
                />
              )}
                </>
              )}
            </div>

            {/* Bottom Navigation Bar */}
            {googleUser && currentScreen !== 'ONBOARDING' && currentScreen !== 'FOCUS_SESSION' && (
              <BottomNavBar
                currentScreen={currentScreen as any}
                onNavigate={(screen) => setCurrentScreen(screen)}
              />
            )}

            {/* Android Navigation Bar (Back, Home, Recents) */}
            <div className="bg-white py-2.5 px-12 border-t border-[#E2E8F0] flex items-center justify-around">
              <button
                onClick={() => {
                  if (!googleUser) return;
                  if (currentScreen === 'CHAPTERS') setCurrentScreen('SUBJECTS');
                  else if (currentScreen === 'TOPICS') setCurrentScreen('CHAPTERS');
                  else if (currentScreen !== 'HOME' && currentScreen !== 'ONBOARDING') setCurrentScreen('HOME');
                }}
                className="w-4 h-4 border-l-2 border-b-2 border-[#64748B] rotate-45 transform"
                title="Back"
              ></button>
              <button
                onClick={() => {
                  if (!googleUser) return;
                  setCurrentScreen('HOME');
                }}
                className="w-3.5 h-3.5 rounded-full border-2 border-[#64748B]"
                title="Home"
              ></button>
              <button
                onClick={() => {}}
                className="w-3.5 h-3.5 border-2 border-[#64748B] rounded-xs"
                title="Recents"
              ></button>
            </div>
          </div>
        )}
      </main>

      {/* CRUD Modals */}
      {modalType && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-sm p-5 shadow-xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-[#0F172A]">
                {modalType === 'ADD_SUBJECT' && 'Add New Subject'}
                {modalType === 'RENAME_SUBJECT' && 'Rename Subject'}
                {modalType === 'DELETE_SUBJECT' && 'Delete Subject?'}
                {modalType === 'ADD_CHAPTER' && 'Add Chapter'}
                {modalType === 'RENAME_CHAPTER' && 'Rename Chapter'}
                {modalType === 'DELETE_CHAPTER' && 'Delete Chapter?'}
                {modalType === 'ADD_TOPIC' && 'Add Topic'}
                {modalType === 'RENAME_TOPIC' && 'Rename Topic'}
                {modalType === 'DELETE_TOPIC' && 'Delete Topic?'}
                {modalType === 'EDIT_PROFILE' && 'Edit Student Profile'}
              </h3>
              <button onClick={() => setModalType(null)} className="p-1 rounded-md text-[#64748B] hover:bg-[#F1F5F9]">
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Content for Add / Rename */}
            {(modalType === 'ADD_SUBJECT' || modalType === 'RENAME_SUBJECT' || modalType === 'ADD_CHAPTER' || modalType === 'RENAME_CHAPTER' || modalType === 'ADD_TOPIC' || modalType === 'RENAME_TOPIC') && (
              <div>
                <input
                  type="text"
                  autoFocus
                  value={modalInput}
                  onChange={e => setModalInput(e.target.value)}
                  placeholder="Enter name..."
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                />
              </div>
            )}

            {/* Content for Delete */}
            {(modalType === 'DELETE_SUBJECT' || modalType === 'DELETE_CHAPTER' || modalType === 'DELETE_TOPIC') && (
              <p className="text-xs text-[#64748B]">
                Are you sure you want to delete <strong className="text-[#0F172A]">{targetItem?.name}</strong>? This action cannot be undone.
              </p>
            )}

            {/* Content for Edit Profile */}
            {modalType === 'EDIT_PROFILE' && (
              <div className="space-y-3">
                <div>
                  <label className="block text-[11px] font-semibold text-[#64748B] mb-1">Country</label>
                  <input
                    type="text"
                    id="edit-country"
                    defaultValue={profile?.country || 'India'}
                    className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A]"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-[#64748B] mb-1">Education Board</label>
                  <input
                    type="text"
                    id="edit-system"
                    defaultValue={profile?.educationSystem || 'CBSE'}
                    className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A]"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-[#64748B] mb-1">Grade / Year</label>
                  <input
                    type="text"
                    id="edit-grade"
                    defaultValue={profile?.grade || 'Grade 10'}
                    className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A]"
                  />
                </div>
              </div>
            )}

            <div className="flex items-center justify-end space-x-2 pt-2">
              <button
                onClick={() => setModalType(null)}
                className="px-3 py-1.5 rounded-lg text-xs font-medium text-[#64748B] hover:bg-[#F1F5F9]"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  if (modalType === 'ADD_SUBJECT') handleAddSubject(modalInput);
                  else if (modalType === 'RENAME_SUBJECT' && targetItem) handleRenameSubject(targetItem.id, modalInput);
                  else if (modalType === 'DELETE_SUBJECT' && targetItem) handleDeleteSubject(targetItem.id);
                  else if (modalType === 'ADD_CHAPTER') handleAddChapter(modalInput);
                  else if (modalType === 'RENAME_CHAPTER' && targetItem) handleRenameChapter(targetItem.id, modalInput);
                  else if (modalType === 'DELETE_CHAPTER' && targetItem) handleDeleteChapter(targetItem.id);
                  else if (modalType === 'ADD_TOPIC') handleAddTopic(modalInput);
                  else if (modalType === 'RENAME_TOPIC' && targetItem) handleRenameTopic(targetItem.id, modalInput);
                  else if (modalType === 'DELETE_TOPIC' && targetItem) handleDeleteTopic(targetItem.id);
                  else if (modalType === 'EDIT_PROFILE') {
                    const country = (document.getElementById('edit-country') as HTMLInputElement)?.value || 'India';
                    const system = (document.getElementById('edit-system') as HTMLInputElement)?.value || 'CBSE';
                    const grade = (document.getElementById('edit-grade') as HTMLInputElement)?.value || 'Grade 10';
                    setProfile(prev => prev ? { ...prev, country, educationSystem: system, grade } : null);
                    setModalType(null);
                  }
                }}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold text-white shadow-xs ${
                  modalType?.startsWith('DELETE') ? 'bg-rose-600 hover:bg-rose-700' : 'bg-[#B8860B] hover:bg-[#996F0A]'
                }`}
              >
                {modalType?.startsWith('DELETE') ? 'Delete' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* App Security PIN Unlock Screen Overlay */}
      {googleUser && isAppLocked && securitySettings.isPinEnabled && securitySettings.pinHash && (
        <PinUnlockScreen
          storedPinHash={securitySettings.pinHash}
          biometricsEnabled={securitySettings.biometricsEnabled}
          onUnlock={() => {
            lastUserActivityRef.current = Date.now();
            setIsAppLocked(false);
          }}
        />
      )}
    </div>
  );
}
