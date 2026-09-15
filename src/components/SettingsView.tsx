import React, { useState } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  User,
  BookOpen,
  Sliders,
  Shield,
  Folder,
  Bell,
  Lock,
  Palette,
  Info,
  Edit3,
  Check,
  Volume2,
  AlertCircle,
  ExternalLink,
  Smartphone
} from 'lucide-react';
import {
  UserProfile,
  Subject,
  GuardianSettings,
  StudyMaterialItem,
  AppSecuritySettings,
  DEFAULT_SECURITY_SETTINGS,
  AutoLockTimeout,
  AuthUser,
  GoogleAuthUser
} from '../types';
import { hashPin, verifyPin } from '../utils/security';

interface SettingsViewProps {
  profile: UserProfile | null;
  subjects: Subject[];
  materials: StudyMaterialItem[];
  guardianSettings: GuardianSettings;
  onUpdateGuardianSettings: (updater: (prev: GuardianSettings) => GuardianSettings) => void;
  onUpdateProfile: (updated: Partial<UserProfile>) => void;
  onNavigateToSubjects: () => void;
  onNavigateToMaterials: () => void;
  onNavigateToAbout: () => void;
  onBack: () => void;
  onTestGuardianAlarm: () => void;
  isTestingAlert: boolean;
  securitySettings?: AppSecuritySettings;
  onUpdateSecuritySettings?: (settings: AppSecuritySettings) => void;
  onLockAppNow?: () => void;
  authUser?: AuthUser | null;
  onSignOut?: () => void;
  googleUser?: GoogleAuthUser | null;
  onGoogleSignOut?: () => void;
  onSwitchGoogleAccount?: () => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  profile,
  subjects,
  materials,
  guardianSettings,
  onUpdateGuardianSettings,
  onUpdateProfile,
  onNavigateToSubjects,
  onNavigateToMaterials,
  onNavigateToAbout,
  onBack,
  onTestGuardianAlarm,
  isTestingAlert,
  securitySettings,
  onUpdateSecuritySettings,
  onLockAppNow,
  authUser,
  onSignOut,
  googleUser,
  onGoogleSignOut,
  onSwitchGoogleAccount,
}) => {
  const activeUser = authUser || googleUser;
  const handleUserSignOut = onSignOut || onGoogleSignOut || onSwitchGoogleAccount;

  // Change Password state
  const [isChangePasswordModalOpen, setIsChangePasswordModalOpen] = useState(false);
  const [currentPasswordInput, setCurrentPasswordInput] = useState('');
  const [newPasswordInput, setNewPasswordInput] = useState('');
  const [confirmNewPasswordInput, setConfirmNewPasswordInput] = useState('');
  const [changePasswordError, setChangePasswordError] = useState('');
  const [changePasswordSuccess, setChangePasswordSuccess] = useState('');
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  // Notifications local state
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [sessionReminders, setSessionReminders] = useState(true);
  const [streakAlerts, setStreakAlerts] = useState(true);
  const [retentionReviews, setRetentionReviews] = useState(true);
  const [topicEvaluations, setTopicEvaluations] = useState(true);
  const [testReminderSent, setTestReminderSent] = useState(false);

  // Security state backed by parent props or local fallback
  const [localSecurity, setLocalSecurity] = useState<AppSecuritySettings>(() => {
    return securitySettings || DEFAULT_SECURITY_SETTINGS;
  });

  const activeSecurity = securitySettings || localSecurity;
  const updateSecurity = (updated: AppSecuritySettings) => {
    setLocalSecurity(updated);
    if (onUpdateSecuritySettings) {
      onUpdateSecuritySettings(updated);
    }
  };

  // PIN Management Modals State
  const [securityModalMode, setSecurityModalMode] = useState<'NONE' | 'SETUP_PIN' | 'CHANGE_PIN' | 'DISABLE_PIN'>('NONE');
  const [pinCurrentInput, setPinCurrentInput] = useState('');
  const [pinNewInput, setPinNewInput] = useState('');
  const [pinConfirmInput, setPinConfirmInput] = useState('');
  const [pinErrorMessage, setPinErrorMessage] = useState('');
  const [pinSuccessMessage, setPinSuccessMessage] = useState('');
  const [isProcessingPin, setIsProcessingPin] = useState(false);

  // Edit Profile Modal
  const [isEditProfileModalOpen, setIsEditProfileModalOpen] = useState(false);
  const [editCountry, setEditCountry] = useState(profile?.country || 'India');
  const [editBoard, setEditBoard] = useState(profile?.educationSystem || 'CBSE');
  const [editGrade, setEditGrade] = useState(profile?.grade || 'Grade 10');
  const [editName, setEditName] = useState(profile?.name || 'Student');
  const [editDailyTarget, setEditDailyTarget] = useState(profile?.dailyTargetMinutes || 120);

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    onUpdateProfile({
      country: editCountry,
      educationSystem: editBoard,
      grade: editGrade,
      name: editName,
      dailyTargetMinutes: editDailyTarget,
    });
    setIsEditProfileModalOpen(false);
  };

  const handleSendTestNotification = () => {
    setTestReminderSent(true);
    setTimeout(() => setTestReminderSent(false), 4000);
  };

  return (
    <div className="p-5 space-y-4 pb-12">
      {/* Header */}
      <div className="flex items-center space-x-2">
        <button
          id="settings-back-btn"
          onClick={onBack}
          className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#0F172A] transition-colors"
          aria-label="Back to Dashboard"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <h1 className="text-lg font-bold text-[#0F172A]">Settings</h1>
      </div>

      {testReminderSent && (
        <div className="bg-[#F0FDF4] border border-[#BBF7D0] p-3 rounded-xl text-xs text-[#16A34A] font-semibold flex items-center space-x-2 animate-fadeIn">
          <Bell className="w-4 h-4 shrink-0" />
          <span>Test Academic Reminder sent: "Physics revision scheduled for 5:00 PM today."</span>
        </div>
      )}

      {/* ========================================================= */}
      {/* STUDENT ACCOUNT AUTHENTICATION */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 space-y-3 shadow-2xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3 min-w-0">
            {activeUser?.picture ? (
              <img
                src={activeUser.picture}
                alt={activeUser.name || 'Student'}
                className="w-10 h-10 rounded-full border border-[#CBD5E1] object-cover shrink-0"
                referrerPolicy="no-referrer"
              />
            ) : (
              <div className="w-10 h-10 rounded-full bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] font-bold text-sm shrink-0">
                {(activeUser?.name || profile?.name || 'S').charAt(0).toUpperCase()}
              </div>
            )}
            <div className="min-w-0">
              <div className="flex items-center space-x-1.5">
                <h2 className="text-xs font-bold text-[#0F172A] truncate">
                  {activeUser?.name || profile?.name || 'Student Account'}
                </h2>
                <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold bg-[#DCFCE7] text-[#15803D] border border-[#BBF7D0]">
                  Active
                </span>
              </div>
              <p className="text-[10px] text-[#64748B] truncate">
                {activeUser?.email || profile?.email || 'Authenticated'}
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-1.5 shrink-0">
            <button
              id="change-password-modal-trigger-btn"
              onClick={() => {
                setChangePasswordError('');
                setChangePasswordSuccess('');
                setCurrentPasswordInput('');
                setNewPasswordInput('');
                setConfirmNewPasswordInput('');
                setIsChangePasswordModalOpen(true);
              }}
              className="text-[11px] font-semibold text-[#B8860B] hover:text-[#996F0A] px-2.5 py-1.5 rounded-lg hover:bg-[#FEFCE8] border border-[#FEF08A] transition-colors"
              title="Change account password"
            >
              Password
            </button>
            {handleUserSignOut && (
              <button
                id="student-signout-btn"
                onClick={handleUserSignOut}
                className="text-[11px] font-semibold text-[#EF4444] hover:text-[#DC2626] px-2.5 py-1.5 rounded-lg hover:bg-[#FEF2F2] border border-[#FECACA] transition-colors"
                title="Sign out of student account"
              >
                Sign Out
              </button>
            )}
          </div>
        </div>

        <div className="pt-2 border-t border-[#F1F5F9] flex items-center justify-between text-[10px] text-[#64748B]">
          <span className="flex items-center space-x-1">
            <span className="w-1.5 h-1.5 rounded-full bg-[#22C55E]"></span>
            <span>Email & Password Protection</span>
          </span>
          <span className="font-mono text-[9px] text-[#94A3B8]">StudyPilot Auth</span>
        </div>
      </div>

      {/* ========================================================= */}
      {/* SECTION 1: Student Profile */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 space-y-3 shadow-2xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shadow-2xs">
              <User className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-xs font-bold text-[#0F172A]">Student Profile</h2>
              <p className="text-[10px] text-[#64748B]">Academic identity & grade</p>
            </div>
          </div>

          <button
            id="edit-profile-btn"
            onClick={() => {
              setEditCountry(profile?.country || 'India');
              setEditBoard(profile?.educationSystem || 'CBSE');
              setEditGrade(profile?.grade || 'Grade 10');
              setEditName(profile?.name || 'Student');
              setEditDailyTarget(profile?.dailyTargetMinutes || 120);
              setIsEditProfileModalOpen(true);
            }}
            className="text-xs font-bold text-[#B8860B] hover:underline"
          >
            Edit Profile
          </button>
        </div>

        <div className="pt-2 border-t border-[#F1F5F9] grid grid-cols-2 gap-2 text-xs">
          <div>
            <p className="text-[10px] text-[#64748B]">Student Name</p>
            <p className="font-bold text-[#0F172A]">{profile?.name || 'Student'}</p>
          </div>
          <div>
            <p className="text-[10px] text-[#64748B]">Country</p>
            <p className="font-bold text-[#0F172A]">{profile?.country || 'India'}</p>
          </div>
          <div>
            <p className="text-[10px] text-[#64748B]">Education Board</p>
            <p className="font-bold text-[#0F172A]">{profile?.educationSystem || 'CBSE'}</p>
          </div>
          <div>
            <p className="text-[10px] text-[#64748B]">Current Grade</p>
            <p className="font-bold text-[#0F172A]">{profile?.grade || 'Grade 10'}</p>
          </div>
          {(activeUser?.email || profile?.email || profile?.googleEmail) && (
            <div className="col-span-2">
              <p className="text-[10px] text-[#64748B]">Student Email</p>
              <p className="font-medium text-[#0F172A] truncate font-mono text-[11px]">
                {activeUser?.email || profile?.email || profile?.googleEmail}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* ========================================================= */}
      {/* SECTION 2: Curriculum */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 space-y-3 shadow-2xs">
        <div className="flex items-center space-x-2.5">
          <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shadow-2xs">
            <BookOpen className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-xs font-bold text-[#0F172A]">Curriculum</h2>
            <p className="text-[10px] text-[#64748B]">Subjects, chapters & syllabus structure</p>
          </div>
        </div>

        <div className="pt-2 border-t border-[#F1F5F9] flex items-center justify-between text-xs">
          <div>
            <p className="font-semibold text-[#0F172A]">{subjects.length} Subjects Configured</p>
            <p className="text-[10px] text-[#64748B]">Includes Physics, Chemistry, Math & Biology</p>
          </div>
          <button
            id="manage-curriculum-btn"
            onClick={onNavigateToSubjects}
            className="px-3 py-1.5 rounded-xl bg-[#F8FAFC] hover:bg-[#F1F5F9] border border-[#E2E8F0] text-[#0F172A] text-xs font-bold flex items-center space-x-1 transition-colors"
          >
            <span>Manage</span>
            <ChevronRight className="w-3.5 h-3.5 text-[#B8860B]" />
          </button>
        </div>
      </div>

      {/* ========================================================= */}
      {/* SECTION 3: Study Settings */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 space-y-3 shadow-2xs">
        <div className="flex items-center space-x-2.5">
          <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shadow-2xs">
            <Sliders className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-xs font-bold text-[#0F172A]">Study Settings</h2>
            <p className="text-[10px] text-[#64748B]">Daily targets & test requirements</p>
          </div>
        </div>

        <div className="pt-2 border-t border-[#F1F5F9] space-y-3">
          <div>
            <div className="flex items-center justify-between text-xs mb-1.5">
              <span className="font-semibold text-[#0F172A]">Daily Study Target</span>
              <span className="font-bold text-[#B8860B]">{profile?.dailyTargetMinutes || 120} min/day</span>
            </div>
            <div className="grid grid-cols-5 gap-1">
              {[60, 90, 120, 180, 240].map(mins => (
                <button
                  key={mins}
                  onClick={() => onUpdateProfile({ dailyTargetMinutes: mins })}
                  className={`py-1 rounded-lg text-[10px] font-bold border transition-colors ${
                    (profile?.dailyTargetMinutes || 120) === mins
                      ? 'bg-[#B8860B] text-white border-[#B8860B]'
                      : 'bg-white text-[#64748B] border-[#E2E8F0] hover:bg-[#F8FAFC]'
                  }`}
                >
                  {mins}m
                </button>
              ))}
            </div>
          </div>

          <div className="flex items-center justify-between pt-1 border-t border-[#F1F5F9]">
            <div>
              <p className="text-xs font-semibold text-[#0F172A]">Evidence Photo Required</p>
              <p className="text-[10px] text-[#64748B]">Verify handwritten notes before test unlock</p>
            </div>
            <input
              type="checkbox"
              checked={profile?.evidenceRequired !== false}
              onChange={e => onUpdateProfile({ evidenceRequired: e.target.checked })}
              className="w-4 h-4 accent-[#22C55E] rounded cursor-pointer"
            />
          </div>
        </div>
      </div>

      {/* ========================================================= */}
      {/* SECTION 4: Focus Guardian */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 space-y-3 shadow-2xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shadow-2xs">
              <Shield className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-xs font-bold text-[#0F172A]">Focus Guardian</h2>
              <p className="text-[10px] text-[#64748B]">On-device desk presence monitoring</p>
            </div>
          </div>

          <input
            type="checkbox"
            checked={guardianSettings.isGuardianEnabled}
            onChange={e =>
              onUpdateGuardianSettings(prev => ({ ...prev, isGuardianEnabled: e.target.checked }))
            }
            className="w-4 h-4 accent-[#22C55E] rounded cursor-pointer"
          />
        </div>

        {guardianSettings.isGuardianEnabled && (
          <div className="space-y-3 pt-2 border-t border-[#F1F5F9]">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-[#0F172A]">Spoken Audio Reminder</p>
                <p className="text-[10px] text-[#64748B]">Voice prompt when desk is empty</p>
              </div>
              <input
                type="checkbox"
                checked={guardianSettings.isAudioReminderEnabled}
                onChange={e =>
                  onUpdateGuardianSettings(prev => ({ ...prev, isAudioReminderEnabled: e.target.checked }))
                }
                className="w-4 h-4 accent-[#22C55E] rounded cursor-pointer"
              />
            </div>

            <div>
              <p className="text-[11px] font-semibold text-[#0F172A] mb-1.5">Alert Sequence Type</p>
              <div className="grid grid-cols-3 gap-1.5">
                {[
                  { id: 'VOICE_AND_REVEILLE', label: 'Voice + Reveille' },
                  { id: 'VOICE_ONLY', label: 'Voice Only' },
                  { id: 'OFF', label: 'Muted' },
                ].map(item => (
                  <button
                    key={item.id}
                    onClick={() =>
                      onUpdateGuardianSettings(prev => ({ ...prev, alertType: item.id as any }))
                    }
                    className={`py-1.5 px-2 rounded-lg text-[10px] font-bold border transition-colors ${
                      guardianSettings.alertType === item.id
                        ? 'bg-[#B8860B] text-white border-[#B8860B]'
                        : 'bg-white text-[#64748B] border-[#E2E8F0] hover:bg-[#F8FAFC]'
                    }`}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="text-[11px] font-semibold text-[#0F172A] mb-1.5">Reveille Horn Volume</p>
              <div className="grid grid-cols-3 gap-1.5">
                {[
                  { id: 'LOW', label: 'Low (40%)' },
                  { id: 'MEDIUM', label: 'Medium (70%)' },
                  { id: 'HIGH', label: 'High (100%)' },
                ].map(item => (
                  <button
                    key={item.id}
                    onClick={() =>
                      onUpdateGuardianSettings(prev => ({ ...prev, volumeLevel: item.id as any }))
                    }
                    className={`py-1.5 px-2 rounded-lg text-[10px] font-bold border transition-colors ${
                      guardianSettings.volumeLevel === item.id
                        ? 'bg-[#B8860B] text-white border-[#B8860B]'
                        : 'bg-white text-[#64748B] border-[#E2E8F0] hover:bg-[#F8FAFC]'
                    }`}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-[#0F172A]">Haptic Vibration</p>
                <p className="text-[10px] text-[#64748B]">Vibrate device during alarms</p>
              </div>
              <input
                type="checkbox"
                checked={guardianSettings.isVibrationEnabled}
                onChange={e =>
                  onUpdateGuardianSettings(prev => ({ ...prev, isVibrationEnabled: e.target.checked }))
                }
                className="w-4 h-4 accent-[#22C55E] rounded cursor-pointer"
              />
            </div>

            <button
              id="test-guardian-alarm-btn"
              disabled={isTestingAlert}
              onClick={onTestGuardianAlarm}
              className="w-full py-2 px-3 rounded-xl border border-[#B8860B] text-[#B8860B] hover:bg-[#FEFCE8] text-xs font-bold flex items-center justify-center space-x-2 transition-colors disabled:opacity-50"
            >
              <Volume2 className="w-3.5 h-3.5" />
              <span>{isTestingAlert ? 'Testing Bugle & Speech...' : 'Test Alarm & Vibration (3s)'}</span>
            </button>
          </div>
        )}
      </div>

      {/* ========================================================= */}
      {/* SECTION 5: Study Materials */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 space-y-3 shadow-2xs">
        <div className="flex items-center space-x-2.5">
          <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shadow-2xs">
            <Folder className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-xs font-bold text-[#0F172A]">Study Materials</h2>
            <p className="text-[10px] text-[#64748B]">Local files, lecture videos & notes repository</p>
          </div>
        </div>

        <div className="pt-2 border-t border-[#F1F5F9] flex items-center justify-between text-xs">
          <div>
            <p className="font-semibold text-[#0F172A]">{materials.length} Local Materials</p>
            <p className="text-[10px] text-[#64748B]">Stored on-device • Private & offline</p>
          </div>

          <button
            id="settings-materials-btn"
            onClick={onNavigateToMaterials}
            className="px-3 py-1.5 rounded-xl bg-[#F8FAFC] hover:bg-[#F1F5F9] border border-[#E2E8F0] text-[#0F172A] text-xs font-bold flex items-center space-x-1 transition-colors"
          >
            <span>Open Library</span>
            <ChevronRight className="w-3.5 h-3.5 text-[#B8860B]" />
          </button>
        </div>
      </div>

      {/* ========================================================= */}
      {/* SECTION 6: Notifications */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 space-y-3 shadow-2xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shadow-2xs">
              <Bell className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-xs font-bold text-[#0F172A]">Notifications</h2>
              <p className="text-[10px] text-[#64748B]">Session reminders & streak alerts</p>
            </div>
          </div>

          <input
            type="checkbox"
            checked={notificationsEnabled}
            onChange={e => setNotificationsEnabled(e.target.checked)}
            className="w-4 h-4 accent-[#22C55E] rounded cursor-pointer"
          />
        </div>

        {notificationsEnabled && (
          <div className="space-y-2.5 pt-2 border-t border-[#F1F5F9] text-xs">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-semibold text-[#0F172A]">Daily Study Reminders</p>
                <p className="text-[10px] text-[#64748B]">Alerts to hit daily targets</p>
              </div>
              <input
                type="checkbox"
                checked={sessionReminders}
                onChange={e => setSessionReminders(e.target.checked)}
                className="w-3.5 h-3.5 accent-[#22C55E] rounded"
              />
            </div>

            <div className="flex items-center justify-between">
              <div>
                <p className="font-semibold text-[#0F172A]">Active Streak Alerts</p>
                <p className="text-[10px] text-[#64748B]">Evening reminder to preserve streak</p>
              </div>
              <input
                type="checkbox"
                checked={streakAlerts}
                onChange={e => setStreakAlerts(e.target.checked)}
                className="w-3.5 h-3.5 accent-[#22C55E] rounded"
              />
            </div>

            <div className="flex items-center justify-between">
              <div>
                <p className="font-semibold text-[#0F172A]">Concept Retention Reviews</p>
                <p className="text-[10px] text-[#64748B]">Spaced repetition reminders</p>
              </div>
              <input
                type="checkbox"
                checked={retentionReviews}
                onChange={e => setRetentionReviews(e.target.checked)}
                className="w-3.5 h-3.5 accent-[#22C55E] rounded"
              />
            </div>

            <div className="flex items-center justify-between">
              <div>
                <p className="font-semibold text-[#0F172A]">Topic Test Evaluations</p>
                <p className="text-[10px] text-[#64748B]">Prompts to test studied chapters</p>
              </div>
              <input
                type="checkbox"
                checked={topicEvaluations}
                onChange={e => setTopicEvaluations(e.target.checked)}
                className="w-3.5 h-3.5 accent-[#22C55E] rounded"
              />
            </div>

            <button
              onClick={handleSendTestNotification}
              className="w-full py-1.5 px-3 rounded-xl bg-[#F8FAFC] hover:bg-[#F1F5F9] border border-[#E2E8F0] text-[#0F172A] text-xs font-bold transition-colors mt-1"
            >
              Send Sample Reminder
            </button>
          </div>
        )}
      </div>

      {/* ========================================================= */}
      {/* SECTION 7: Security */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 space-y-3 shadow-2xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shadow-2xs">
              <Lock className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-xs font-bold text-[#0F172A]">Security</h2>
              <p className="text-[10px] text-[#64748B]">App lock PIN & biometric authentication</p>
            </div>
          </div>

          <input
            type="checkbox"
            checked={activeSecurity.isPinEnabled}
            onChange={e => {
              const shouldEnable = e.target.checked;
              if (shouldEnable) {
                setSecurityModalMode('SETUP_PIN');
                setPinNewInput('');
                setPinConfirmInput('');
                setPinErrorMessage('');
              } else {
                setSecurityModalMode('DISABLE_PIN');
                setPinCurrentInput('');
                setPinErrorMessage('');
              }
            }}
            className="w-4 h-4 accent-[#22C55E] rounded cursor-pointer"
          />
        </div>

        {pinSuccessMessage && (
          <div className="p-2 rounded-xl bg-emerald-50 border border-emerald-100 flex items-center space-x-1.5 text-[11px] text-emerald-700 font-semibold">
            <Check className="w-3.5 h-3.5 shrink-0" />
            <span>{pinSuccessMessage}</span>
          </div>
        )}

        {activeSecurity.isPinEnabled && (
          <div className="space-y-3 pt-2 border-t border-[#F1F5F9] text-xs">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-semibold text-[#0F172A]">4-Digit Passcode</p>
                <p className="text-[10px] text-[#64748B]">Current PIN: •••• (Secured)</p>
              </div>
              <div className="flex items-center space-x-2">
                {onLockAppNow && (
                  <button
                    type="button"
                    onClick={onLockAppNow}
                    className="px-2.5 py-1 rounded-lg bg-[#F8FAFC] border border-[#E2E8F0] hover:bg-[#F1F5F9] text-[10px] font-bold text-[#0F172A] flex items-center space-x-1 transition-colors"
                  >
                    <Lock className="w-3 h-3 text-[#B8860B]" />
                    <span>Lock Now</span>
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => {
                    setSecurityModalMode('CHANGE_PIN');
                    setPinCurrentInput('');
                    setPinNewInput('');
                    setPinConfirmInput('');
                    setPinErrorMessage('');
                  }}
                  className="text-xs font-bold text-[#B8860B] hover:underline"
                >
                  Change PIN
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between">
              <div>
                <p className="font-semibold text-[#0F172A]">Biometric Fingerprint</p>
                <p className="text-[10px] text-[#64748B]">Unlock using device sensor</p>
              </div>
              <input
                type="checkbox"
                checked={activeSecurity.biometricsEnabled}
                onChange={e => updateSecurity({ ...activeSecurity, biometricsEnabled: e.target.checked })}
                className="w-3.5 h-3.5 accent-[#22C55E] rounded cursor-pointer"
              />
            </div>

            <div>
              <p className="text-[11px] font-semibold text-[#0F172A] mb-1.5">Auto-Lock Timeout</p>
              <div className="grid grid-cols-4 gap-1">
                {[
                  { id: 'IMMEDIATE', label: 'Instant' },
                  { id: '1_MIN', label: '1 min' },
                  { id: '5_MIN', label: '5 min' },
                  { id: '15_MIN', label: '15 min' },
                ].map(item => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => updateSecurity({ ...activeSecurity, autoLockTimeout: item.id as AutoLockTimeout })}
                    className={`py-1 rounded-lg text-[10px] font-bold border transition-colors ${
                      activeSecurity.autoLockTimeout === item.id
                        ? 'bg-[#B8860B] text-white border-[#B8860B]'
                        : 'bg-white text-[#64748B] border-[#E2E8F0] hover:bg-[#F8FAFC]'
                    }`}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* ========================================================= */}
      {/* SECTION 8: Appearance */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 space-y-3 shadow-2xs">
        <div className="flex items-center space-x-2.5">
          <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shadow-2xs">
            <Palette className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-xs font-bold text-[#0F172A]">Appearance</h2>
            <p className="text-[10px] text-[#64748B]">StudyPilot official palette</p>
          </div>
        </div>

        <div className="pt-2 border-t border-[#F1F5F9] space-y-2.5 text-xs">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-[#0F172A]">Official Theme</span>
            <span className="text-[11px] font-bold text-[#B8860B]">White, Silver, Gold & Parrot Green</span>
          </div>

          <div className="flex items-center space-x-2 pt-1">
            <div className="flex-1 p-2 rounded-xl bg-white border border-[#E2E8F0] text-center shadow-2xs">
              <span className="w-3 h-3 rounded-full bg-white border border-[#CBD5E1] inline-block mb-0.5" />
              <p className="text-[9px] font-mono text-[#0F172A]">White</p>
            </div>
            <div className="flex-1 p-2 rounded-xl bg-white border border-[#E2E8F0] text-center shadow-2xs">
              <span className="w-3 h-3 rounded-full bg-[#E2E8F0] border border-[#CBD5E1] inline-block mb-0.5" />
              <p className="text-[9px] font-mono text-[#64748B]">Silver</p>
            </div>
            <div className="flex-1 p-2 rounded-xl bg-white border border-[#E2E8F0] text-center shadow-2xs">
              <span className="w-3 h-3 rounded-full bg-[#D4AF37] inline-block mb-0.5" />
              <p className="text-[9px] font-mono text-[#B8860B]">Gold</p>
            </div>
            <div className="flex-1 p-2 rounded-xl bg-white border border-[#E2E8F0] text-center shadow-2xs">
              <span className="w-3 h-3 rounded-full bg-[#22C55E] inline-block mb-0.5" />
              <p className="text-[9px] font-mono text-[#16A34A]">Parrot</p>
            </div>
          </div>
        </div>
      </div>

      {/* ========================================================= */}
      {/* SECTION 9: About */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 shadow-2xs">
        <button
          id="settings-about-btn"
          onClick={onNavigateToAbout}
          className="w-full text-left flex items-center justify-between"
        >
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shadow-2xs">
              <Info className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-xs font-bold text-[#0F172A]">About StudyPilot</h2>
              <p className="text-[10px] text-[#64748B]">Version 1.0.0 • Created by Mohammad Fahad</p>
            </div>
          </div>

          <ChevronRight className="w-4 h-4 text-[#B8860B]" />
        </button>
      </div>

      {/* Edit Profile Modal */}
      {isEditProfileModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-sm p-5 shadow-xl space-y-4">
            <h3 className="text-sm font-bold text-[#0F172A]">Edit Student Profile</h3>

            <form onSubmit={handleSaveProfile} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-[#0F172A] mb-1">Student Name</label>
                <input
                  type="text"
                  value={editName}
                  onChange={e => setEditName(e.target.value)}
                  className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                  required
                />
              </div>

              <div>
                <label className="block font-semibold text-[#0F172A] mb-1">Country</label>
                <select
                  value={editCountry}
                  onChange={e => setEditCountry(e.target.value)}
                  className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                >
                  <option value="India">India</option>
                  <option value="United States">United States</option>
                  <option value="United Kingdom">United Kingdom</option>
                  <option value="Canada">Canada</option>
                  <option value="Australia">Australia</option>
                  <option value="Singapore">Singapore</option>
                  <option value="UAE">United Arab Emirates</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-[#0F172A] mb-1">Education Board</label>
                <select
                  value={editBoard}
                  onChange={e => setEditBoard(e.target.value)}
                  className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                >
                  <option value="CBSE">CBSE (Central Board)</option>
                  <option value="ICSE">ICSE / ISC</option>
                  <option value="State Board">State Board</option>
                  <option value="International">International (IB / Cambridge)</option>
                  <option value="Other">Other National Curriculum</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-[#0F172A] mb-1">Grade / Year</label>
                <select
                  value={editGrade}
                  onChange={e => setEditGrade(e.target.value)}
                  className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
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

              <div className="pt-2 flex items-center justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setIsEditProfileModalOpen(false)}
                  className="px-4 py-2 border border-[#E2E8F0] text-[#64748B] hover:bg-[#F8FAFC] rounded-xl text-xs font-bold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-[#B8860B] hover:bg-[#A17608] text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  Save Profile
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* SECURITY / PIN MODALS */}
      {/* ========================================================= */}

      {/* 1. SETUP PIN MODAL */}
      {securityModalMode === 'SETUP_PIN' && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-sm p-5 shadow-2xl space-y-4 animate-in fade-in">
            <div className="flex items-center space-x-2.5">
              <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shrink-0">
                <Lock className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-[#0F172A]">Create 4-Digit PIN</h3>
                <p className="text-[10px] text-[#64748B]">Set up app lock passcode</p>
              </div>
            </div>

            <form
              onSubmit={async (e) => {
                e.preventDefault();
                setPinErrorMessage('');
                if (pinNewInput.length !== 4) {
                  setPinErrorMessage('PIN must be exactly 4 digits.');
                  return;
                }
                if (pinNewInput !== pinConfirmInput) {
                  setPinErrorMessage('PINs do not match. Please re-enter.');
                  return;
                }
                setIsProcessingPin(true);
                try {
                  const hash = await hashPin(pinNewInput);
                  updateSecurity({
                    ...activeSecurity,
                    isPinEnabled: true,
                    pinHash: hash,
                  });
                  setSecurityModalMode('NONE');
                  setPinSuccessMessage('PIN protection successfully enabled.');
                  setTimeout(() => setPinSuccessMessage(''), 3500);
                } catch {
                  setPinErrorMessage('Failed to set PIN. Please try again.');
                } finally {
                  setIsProcessingPin(false);
                }
              }}
              className="space-y-3"
            >
              <div>
                <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                  Enter 4-Digit PIN
                </label>
                <input
                  type="password"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  maxLength={4}
                  autoFocus
                  value={pinNewInput}
                  onChange={(e) => setPinNewInput(e.target.value.replace(/\D/g, ''))}
                  placeholder="••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-center text-base font-mono tracking-widest text-[#0F172A] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                  Confirm 4-Digit PIN
                </label>
                <input
                  type="password"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  maxLength={4}
                  value={pinConfirmInput}
                  onChange={(e) => setPinConfirmInput(e.target.value.replace(/\D/g, ''))}
                  placeholder="••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-center text-base font-mono tracking-widest text-[#0F172A] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                />
              </div>

              {pinErrorMessage && (
                <div className="p-2 rounded-lg bg-red-50 border border-red-100 flex items-center space-x-1.5 text-xs text-red-600 font-medium">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{pinErrorMessage}</span>
                </div>
              )}

              <div className="pt-2 flex items-center justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setSecurityModalMode('NONE')}
                  className="px-4 py-2 border border-[#E2E8F0] text-[#64748B] hover:bg-[#F8FAFC] rounded-xl text-xs font-bold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={pinNewInput.length !== 4 || pinConfirmInput.length !== 4 || isProcessingPin}
                  className="px-5 py-2 bg-[#B8860B] hover:bg-[#A17608] disabled:opacity-50 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  {isProcessingPin ? 'Setting PIN...' : 'Enable App Lock'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 2. CHANGE PIN MODAL */}
      {securityModalMode === 'CHANGE_PIN' && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-sm p-5 shadow-2xl space-y-4 animate-in fade-in">
            <div className="flex items-center space-x-2.5">
              <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shrink-0">
                <Lock className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-[#0F172A]">Change Passcode PIN</h3>
                <p className="text-[10px] text-[#64748B]">Authenticate and set new PIN</p>
              </div>
            </div>

            <form
              onSubmit={async (e) => {
                e.preventDefault();
                setPinErrorMessage('');
                if (pinCurrentInput.length !== 4) {
                  setPinErrorMessage('Please enter your current 4-digit PIN.');
                  return;
                }
                if (pinNewInput.length !== 4) {
                  setPinErrorMessage('New PIN must be 4 digits.');
                  return;
                }
                if (pinNewInput !== pinConfirmInput) {
                  setPinErrorMessage('New PINs do not match.');
                  return;
                }

                setIsProcessingPin(true);
                try {
                  const isValid = await verifyPin(pinCurrentInput, activeSecurity.pinHash);
                  if (!isValid) {
                    setPinErrorMessage('Incorrect current PIN. Cannot update.');
                    setIsProcessingPin(false);
                    return;
                  }

                  const newHash = await hashPin(pinNewInput);
                  updateSecurity({
                    ...activeSecurity,
                    pinHash: newHash,
                  });
                  setSecurityModalMode('NONE');
                  setPinSuccessMessage('PIN changed successfully.');
                  setTimeout(() => setPinSuccessMessage(''), 3500);
                } catch {
                  setPinErrorMessage('Failed to update PIN. Please try again.');
                } finally {
                  setIsProcessingPin(false);
                }
              }}
              className="space-y-3"
            >
              <div>
                <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                  Current PIN
                </label>
                <input
                  type="password"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  maxLength={4}
                  autoFocus
                  value={pinCurrentInput}
                  onChange={(e) => setPinCurrentInput(e.target.value.replace(/\D/g, ''))}
                  placeholder="••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-center text-base font-mono tracking-widest text-[#0F172A] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                  New 4-Digit PIN
                </label>
                <input
                  type="password"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  maxLength={4}
                  value={pinNewInput}
                  onChange={(e) => setPinNewInput(e.target.value.replace(/\D/g, ''))}
                  placeholder="••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-center text-base font-mono tracking-widest text-[#0F172A] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                  Confirm New PIN
                </label>
                <input
                  type="password"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  maxLength={4}
                  value={pinConfirmInput}
                  onChange={(e) => setPinConfirmInput(e.target.value.replace(/\D/g, ''))}
                  placeholder="••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-center text-base font-mono tracking-widest text-[#0F172A] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                />
              </div>

              {pinErrorMessage && (
                <div className="p-2 rounded-lg bg-red-50 border border-red-100 flex items-center space-x-1.5 text-xs text-red-600 font-medium">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{pinErrorMessage}</span>
                </div>
              )}

              <div className="pt-2 flex items-center justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setSecurityModalMode('NONE')}
                  className="px-4 py-2 border border-[#E2E8F0] text-[#64748B] hover:bg-[#F8FAFC] rounded-xl text-xs font-bold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={pinCurrentInput.length !== 4 || pinNewInput.length !== 4 || pinConfirmInput.length !== 4 || isProcessingPin}
                  className="px-5 py-2 bg-[#B8860B] hover:bg-[#A17608] disabled:opacity-50 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  {isProcessingPin ? 'Updating...' : 'Save New PIN'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 3. DISABLE PIN MODAL */}
      {securityModalMode === 'DISABLE_PIN' && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-sm p-5 shadow-2xl space-y-4 animate-in fade-in">
            <div className="flex items-center space-x-2.5">
              <div className="w-8 h-8 rounded-xl bg-red-50 border border-red-100 flex items-center justify-center text-red-600 shrink-0">
                <Lock className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-[#0F172A]">Disable PIN Protection</h3>
                <p className="text-[10px] text-[#64748B]">Authenticate to turn off app lock</p>
              </div>
            </div>

            <form
              onSubmit={async (e) => {
                e.preventDefault();
                setPinErrorMessage('');
                if (pinCurrentInput.length !== 4) {
                  setPinErrorMessage('Please enter your 4-digit PIN.');
                  return;
                }

                setIsProcessingPin(true);
                try {
                  const isValid = await verifyPin(pinCurrentInput, activeSecurity.pinHash);
                  if (!isValid) {
                    setPinErrorMessage('Incorrect PIN. App lock remains enabled.');
                    setIsProcessingPin(false);
                    return;
                  }

                  updateSecurity({
                    ...activeSecurity,
                    isPinEnabled: false,
                    pinHash: '',
                  });
                  setSecurityModalMode('NONE');
                  setPinSuccessMessage('PIN protection disabled.');
                  setTimeout(() => setPinSuccessMessage(''), 3500);
                } catch {
                  setPinErrorMessage('Verification failed. Please try again.');
                } finally {
                  setIsProcessingPin(false);
                }
              }}
              className="space-y-3"
            >
              <p className="text-xs text-[#64748B] leading-relaxed">
                Enter your current 4-digit PIN to confirm removing passcode security from StudyPilot.
              </p>

              <div>
                <input
                  type="password"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  maxLength={4}
                  autoFocus
                  value={pinCurrentInput}
                  onChange={(e) => setPinCurrentInput(e.target.value.replace(/\D/g, ''))}
                  placeholder="••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-center text-base font-mono tracking-widest text-[#0F172A] focus:bg-white focus:outline-none focus:border-red-500"
                />
              </div>

              {pinErrorMessage && (
                <div className="p-2 rounded-lg bg-red-50 border border-red-100 flex items-center space-x-1.5 text-xs text-red-600 font-medium">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{pinErrorMessage}</span>
                </div>
              )}

              <div className="pt-2 flex items-center justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setSecurityModalMode('NONE')}
                  className="px-4 py-2 border border-[#E2E8F0] text-[#64748B] hover:bg-[#F8FAFC] rounded-xl text-xs font-bold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={pinCurrentInput.length !== 4 || isProcessingPin}
                  className="px-5 py-2 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  {isProcessingPin ? 'Verifying...' : 'Disable App Lock'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 4. CHANGE ACCOUNT PASSWORD MODAL */}
      {isChangePasswordModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-sm p-5 shadow-2xl space-y-4 animate-in fade-in">
            <div className="flex items-center space-x-2.5">
              <div className="w-8 h-8 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shrink-0">
                <Lock className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-[#0F172A]">Change Account Password</h3>
                <p className="text-[10px] text-[#64748B]">Update your login credentials</p>
              </div>
            </div>

            <form
              onSubmit={async (e) => {
                e.preventDefault();
                setChangePasswordError('');
                setChangePasswordSuccess('');

                if (!currentPasswordInput) {
                  setChangePasswordError('Please enter your current password.');
                  return;
                }
                if (newPasswordInput.length < 6) {
                  setChangePasswordError('New password must be at least 6 characters long.');
                  return;
                }
                if (newPasswordInput !== confirmNewPasswordInput) {
                  setChangePasswordError('New passwords do not match.');
                  return;
                }

                const token = activeUser?.token;
                if (!token) {
                  setChangePasswordError('You must have an active authenticated session to change your password.');
                  return;
                }
                const userEmail = activeUser?.email || profile?.email;
                if (!userEmail) {
                  setChangePasswordError('Active student account email not found.');
                  return;
                }

                setIsChangingPassword(true);
                try {
                  const res = await fetch('/api/auth/change-password', {
                    method: 'POST',
                    headers: {
                      'Content-Type': 'application/json',
                      Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({
                      email: userEmail,
                      currentPassword: currentPasswordInput,
                      newPassword: newPasswordInput,
                    }),
                  });

                  const data = await res.json();
                  if (!res.ok || !data.success) {
                    throw new Error(data.error || 'Failed to update password.');
                  }

                  setChangePasswordSuccess('Password updated successfully!');
                  setTimeout(() => {
                    setIsChangePasswordModalOpen(false);
                    setCurrentPasswordInput('');
                    setNewPasswordInput('');
                    setConfirmNewPasswordInput('');
                    setChangePasswordSuccess('');
                  }, 1200);
                } catch (err: any) {
                  setChangePasswordError(err.message || 'Error changing password.');
                } finally {
                  setIsChangingPassword(false);
                }
              }}
              className="space-y-3"
            >
              <div>
                <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                  Current Password
                </label>
                <input
                  type="password"
                  required
                  value={currentPasswordInput}
                  onChange={(e) => setCurrentPasswordInput(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                  New Password (min 6 chars)
                </label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={newPasswordInput}
                  onChange={(e) => setNewPasswordInput(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                  Confirm New Password
                </label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={confirmNewPasswordInput}
                  onChange={(e) => setConfirmNewPasswordInput(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                />
              </div>

              {changePasswordError && (
                <div className="p-2.5 rounded-xl bg-red-50 border border-red-200 text-xs text-red-600 font-medium flex items-center space-x-1.5">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{changePasswordError}</span>
                </div>
              )}

              {changePasswordSuccess && (
                <div className="p-2.5 rounded-xl bg-[#F0FDF4] border border-[#BBF7D0] text-xs text-[#16A34A] font-medium flex items-center space-x-1.5">
                  <Check className="w-4 h-4 shrink-0" />
                  <span>{changePasswordSuccess}</span>
                </div>
              )}

              <div className="pt-2 flex items-center justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setIsChangePasswordModalOpen(false)}
                  className="px-4 py-2 border border-[#E2E8F0] text-[#64748B] hover:bg-[#F8FAFC] rounded-xl text-xs font-bold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isChangingPassword}
                  className="px-5 py-2 bg-[#B8860B] hover:bg-[#A17608] disabled:opacity-50 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  {isChangingPassword ? 'Saving...' : 'Update Password'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
