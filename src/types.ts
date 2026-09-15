export type ScreenType =
  | 'ONBOARDING'
  | 'HOME'
  | 'SUBJECTS'
  | 'CHAPTERS'
  | 'TOPICS'
  | 'PLANNER'
  | 'PROGRESS'
  | 'PLUTO'
  | 'SETTINGS'
  | 'ABOUT'
  | 'MATERIALS'
  | 'FOCUS_SESSION';

export interface AuthUser {
  id: string;
  email: string;
  name: string;
  picture?: string;
  signedInAt: number;
  token?: string;
}

// Deprecated alias for backwards compatibility
export type GoogleAuthUser = AuthUser;

export interface UserProfile {
  country: string;
  educationSystem: string;
  grade: string;
  name?: string;
  dailyTargetMinutes?: number;
  evidenceRequired?: boolean;
  email?: string;
  googleEmail?: string;
  googleDisplayName?: string;
  googlePhotoUrl?: string;
}

export interface Topic {
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

export interface TopicTestRecord {
  id: string;
  topicId: string;
  topicName: string;
  subjectName: string;
  chapterName?: string;
  score: number; // percentage 0 - 100
  passed: boolean;
  timestamp: number;
  totalQuestions?: number;
  correctAnswers?: number;
  notes?: string;
}

export type PlannerPriorityLevel = 'HIGH' | 'MEDIUM' | 'LOW';

export interface PlannerRecommendation {
  topic: Topic;
  subject: Subject;
  chapter: Chapter;
  priorityScore: number;
  priorityLevel: PlannerPriorityLevel;
  reason: string;
  recommendedMinutes: number;
  previousScore?: number;
  attempts?: number;
  failedAttempts?: number;
}

export interface Chapter {
  id: string;
  subjectId: string;
  name: string;
  orderIndex: number;
  estimatedMinutes: number;
}

export interface Subject {
  id: string;
  name: string;
  orderIndex: number;
  colorHex: string;
}

export interface GuardianSettings {
  isGuardianEnabled: boolean;
  isAudioReminderEnabled: boolean;
  alertType: 'VOICE_ONLY' | 'VOICE_AND_REVEILLE' | 'OFF';
  volumeLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  isVibrationEnabled: boolean;
}

export interface StudySessionRecord {
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

export interface ChatMessage {
  id: string;
  sender: 'user' | 'Pluto';
  content: string;
  time: string;
  attachmentName?: string;
}

export type MaterialType = 'VIDEO' | 'PDF' | 'IMAGE' | 'DOCUMENT' | 'NOTES';
export type MaterialCategoryFilter = 'ALL' | 'VIDEOS' | 'PDFS' | 'NOTES' | 'IMAGES' | 'DOCUMENTS';

export interface StudyMaterialItem {
  id: string;
  name: string;
  type: MaterialType;
  uriOrPath: string; // URL, Base64 data URL, or text content
  fileSize?: string;
  subjectId: string;
  chapterId: string;
  topicId?: string;
  dateAdded: number;
  lastOpened?: number;
  durationMinutes?: number;
  notes?: string;
}

export type AutoLockTimeout = 'IMMEDIATE' | '1_MIN' | '5_MIN' | '15_MIN';

export interface AppSecuritySettings {
  isPinEnabled: boolean;
  pinHash: string; // SHA-256 hashed 4-digit PIN, never stored or shown in plaintext
  autoLockTimeout: AutoLockTimeout;
  biometricsEnabled: boolean;
}

export const DEFAULT_SECURITY_SETTINGS: AppSecuritySettings = {
  isPinEnabled: false,
  pinHash: '',
  autoLockTimeout: '1_MIN',
  biometricsEnabled: false,
};
