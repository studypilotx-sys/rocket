import React, { useState } from 'react';
import {
  Shield,
  CheckCircle2,
  AlertCircle,
  Mail,
  Lock,
  Eye,
  EyeOff,
  User,
  ArrowLeft,
  KeyRound,
  Sparkles,
  ArrowRight
} from 'lucide-react';
import { AuthUser } from '../types';

interface AuthScreenProps {
  onSignInSuccess: (user: AuthUser) => void;
  initialError?: string | null;
}

type AuthMode = 'LOGIN' | 'SIGNUP' | 'FORGOT_PASSWORD';

export function AuthScreen({ onSignInSuccess, initialError }: AuthScreenProps) {
  const [authMode, setAuthMode] = useState<AuthMode>('LOGIN');

  // Form states
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Forgot password flow states
  const [forgotStep, setForgotStep] = useState<1 | 2>(1);
  const [resetCode, setResetCode] = useState('');
  const [generatedCodeNotice, setGeneratedCodeNotice] = useState<string | null>(null);

  // Status & Feedback
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(initialError || null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  // Handle Login
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    const cleanEmail = email.trim().toLowerCase();
    if (!cleanEmail) {
      setErrorMessage('Please enter your email address.');
      return;
    }
    if (!emailRegex.test(cleanEmail)) {
      setErrorMessage('Please enter a valid email address (e.g., student@example.com).');
      return;
    }
    if (!password) {
      setErrorMessage('Please enter your password.');
      return;
    }

    setIsLoading(true);
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: cleanEmail,
          password,
        }),
      });

      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Login failed. Please verify your credentials.');
      }

      onSignInSuccess(data.user);
    } catch (err: any) {
      setErrorMessage(err.message || 'Unable to connect to login server.');
    } finally {
      setIsLoading(false);
    }
  };

  // Handle Sign Up
  const handleSignUp = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    const cleanName = name.trim();
    const cleanEmail = email.trim().toLowerCase();

    if (!cleanName || cleanName.length < 2) {
      setErrorMessage('Please enter your full student name (at least 2 characters).');
      return;
    }
    if (!cleanEmail || !emailRegex.test(cleanEmail)) {
      setErrorMessage('Please enter a valid email address (e.g., student@example.com).');
      return;
    }
    if (password.length < 6) {
      setErrorMessage('Password must be at least 6 characters long.');
      return;
    }
    if (password !== confirmPassword) {
      setErrorMessage('Passwords do not match. Please verify your password entry.');
      return;
    }

    setIsLoading(true);
    try {
      const res = await fetch('/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: cleanName,
          email: cleanEmail,
          password,
        }),
      });

      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Sign up failed. Please check your details.');
      }

      onSignInSuccess(data.user);
    } catch (err: any) {
      setErrorMessage(err.message || 'Unable to register account.');
    } finally {
      setIsLoading(false);
    }
  };

  // Handle Forgot Password - Step 1 (Request Code)
  const handleRequestResetCode = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);
    setGeneratedCodeNotice(null);

    const cleanEmail = email.trim().toLowerCase();
    if (!cleanEmail || !emailRegex.test(cleanEmail)) {
      setErrorMessage('Please enter a valid registered email address.');
      return;
    }

    setIsLoading(true);
    try {
      const res = await fetch('/api/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: cleanEmail }),
      });

      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Could not find an account with this email.');
      }

      setForgotStep(2);
      if (data.resetCode) {
        setGeneratedCodeNotice(data.resetCode);
      }
      setSuccessMessage('A 6-digit verification code has been issued.');
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to send reset code.');
    } finally {
      setIsLoading(false);
    }
  };

  // Handle Forgot Password - Step 2 (Reset Password)
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    const cleanCode = resetCode.trim();
    if (!cleanCode || cleanCode.length !== 6 || !/^\d{6}$/.test(cleanCode)) {
      setErrorMessage('Please enter the valid 6-digit numeric verification code.');
      return;
    }
    if (password.length < 6) {
      setErrorMessage('New password must be at least 6 characters long.');
      return;
    }
    if (password !== confirmPassword) {
      setErrorMessage('Passwords do not match. Please verify your new password.');
      return;
    }

    setIsLoading(true);
    try {
      const res = await fetch('/api/auth/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: email.trim().toLowerCase(),
          resetCode: cleanCode,
          newPassword: password,
        }),
      });

      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Failed to reset password. Please check your code.');
      }

      setSuccessMessage('Password reset successfully! Redirecting to sign in...');
      setTimeout(() => {
        setAuthMode('LOGIN');
        setForgotStep(1);
        setResetCode('');
        setPassword('');
        setConfirmPassword('');
        setGeneratedCodeNotice(null);
      }, 1500);
    } catch (err: any) {
      setErrorMessage(err.message || 'Password reset failed.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="w-full flex-1 flex flex-col justify-between p-6 overflow-y-auto bg-white">
      <div>
        {/* App Header Badge */}
        <div className="flex items-center justify-between mb-3">
          <div className="inline-block px-2.5 py-1 rounded bg-[#FEFCE8] text-[#B8860B] border border-[#FEF08A] text-xs font-bold tracking-wider">
            STUDYPILOT
          </div>
          <div className="flex items-center space-x-1.5 text-[11px] font-semibold text-[#64748B]">
            <KeyRound className="w-3.5 h-3.5 text-[#B8860B]" />
            <span>Secure Student Access</span>
          </div>
        </div>

        {/* Dynamic Titles */}
        {authMode === 'LOGIN' && (
          <div>
            <h1 className="text-2xl font-bold text-[#0F172A] tracking-tight">Welcome to StudyPilot</h1>
            <p className="text-xs text-[#64748B] mt-1.5 leading-relaxed">
              Sign in with your email and password to access your syllabus, study sessions, and Pluto AI mentor.
            </p>
          </div>
        )}

        {authMode === 'SIGNUP' && (
          <div>
            <h1 className="text-2xl font-bold text-[#0F172A] tracking-tight">Create Student Account</h1>
            <p className="text-xs text-[#64748B] mt-1.5 leading-relaxed">
              Register your account to manage syllabus checklists, earn study mastery badges, and build focus streaks.
            </p>
          </div>
        )}

        {authMode === 'FORGOT_PASSWORD' && (
          <div>
            <button
              type="button"
              onClick={() => {
                setAuthMode('LOGIN');
                setForgotStep(1);
                setErrorMessage(null);
                setSuccessMessage(null);
              }}
              className="inline-flex items-center space-x-1 text-xs text-[#64748B] hover:text-[#0F172A] font-semibold mb-2"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Back to Sign In</span>
            </button>
            <h1 className="text-2xl font-bold text-[#0F172A] tracking-tight">
              {forgotStep === 1 ? 'Reset Password' : 'Enter Verification Code'}
            </h1>
            <p className="text-xs text-[#64748B] mt-1.5 leading-relaxed">
              {forgotStep === 1
                ? 'Enter your student email address to receive a secure 6-digit password reset code.'
                : `Enter the 6-digit code issued for ${email} and choose your new password.`}
            </p>
          </div>
        )}

        {/* Tab Switcher (Sign In vs Create Account) */}
        {authMode !== 'FORGOT_PASSWORD' && (
          <div className="mt-5 p-1 bg-[#F1F5F9] rounded-xl flex items-center border border-[#E2E8F0]">
            <button
              type="button"
              id="auth-tab-login"
              onClick={() => {
                setAuthMode('LOGIN');
                setErrorMessage(null);
                setSuccessMessage(null);
              }}
              className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all text-center ${
                authMode === 'LOGIN'
                  ? 'bg-white text-[#0F172A] shadow-xs'
                  : 'text-[#64748B] hover:text-[#0F172A]'
              }`}
            >
              Sign In
            </button>
            <button
              type="button"
              id="auth-tab-signup"
              onClick={() => {
                setAuthMode('SIGNUP');
                setErrorMessage(null);
                setSuccessMessage(null);
              }}
              className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all text-center ${
                authMode === 'SIGNUP'
                  ? 'bg-white text-[#0F172A] shadow-xs'
                  : 'text-[#64748B] hover:text-[#0F172A]'
              }`}
            >
              Create Account
            </button>
          </div>
        )}

        {/* Notification Feedback Banners */}
        {errorMessage && (
          <div className="mt-4 p-3 rounded-xl bg-red-50 border border-red-200 text-xs text-red-700 flex items-start space-x-2 animate-fadeIn">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-red-500" />
            <div className="flex-1 font-medium">{errorMessage}</div>
          </div>
        )}

        {successMessage && (
          <div className="mt-4 p-3 rounded-xl bg-[#F0FDF4] border border-[#BBF7D0] text-xs text-[#16A34A] flex items-start space-x-2 animate-fadeIn">
            <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5 text-[#22C55E]" />
            <div className="flex-1 font-medium">{successMessage}</div>
          </div>
        )}

        {/* 1. SIGN IN FORM */}
        {authMode === 'LOGIN' && (
          <form onSubmit={handleLogin} className="mt-5 space-y-4">
            <div>
              <label className="block text-xs font-semibold text-[#0F172A] mb-1.5">
                Email Address
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#94A3B8]">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  id="login-email-input"
                  type="email"
                  required
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="student@example.com"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl pl-9 pr-3 py-2.5 text-xs text-[#0F172A] placeholder-[#94A3B8] focus:bg-white focus:outline-none focus:border-[#B8860B] transition-colors"
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-[#0F172A]">
                  Password
                </label>
                <button
                  type="button"
                  id="forgot-password-btn"
                  onClick={() => {
                    setAuthMode('FORGOT_PASSWORD');
                    setForgotStep(1);
                    setErrorMessage(null);
                    setSuccessMessage(null);
                  }}
                  className="text-[11px] font-semibold text-[#B8860B] hover:text-[#996F0A] transition-colors"
                >
                  Forgot Password?
                </button>
              </div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#94A3B8]">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  id="login-password-input"
                  type={showPassword ? 'text' : 'password'}
                  required
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl pl-9 pr-10 py-2.5 text-xs text-[#0F172A] placeholder-[#94A3B8] focus:bg-white focus:outline-none focus:border-[#B8860B] transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-[#94A3B8] hover:text-[#64748B]"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              id="submit-login-btn"
              type="submit"
              disabled={isLoading}
              className="w-full bg-[#B8860B] hover:bg-[#996F0A] text-white font-bold py-3 px-4 rounded-xl text-xs transition-all shadow-xs flex items-center justify-center space-x-2 disabled:opacity-50"
            >
              {isLoading ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  <span>Signing In...</span>
                </>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        )}

        {/* 2. SIGN UP FORM */}
        {authMode === 'SIGNUP' && (
          <form onSubmit={handleSignUp} className="mt-5 space-y-3.5">
            <div>
              <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                Full Name
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#94A3B8]">
                  <User className="w-4 h-4" />
                </div>
                <input
                  id="signup-name-input"
                  type="text"
                  required
                  autoComplete="name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Alex Kumar"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl pl-9 pr-3 py-2 text-xs text-[#0F172A] placeholder-[#94A3B8] focus:bg-white focus:outline-none focus:border-[#B8860B] transition-colors"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                Email Address
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#94A3B8]">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  id="signup-email-input"
                  type="email"
                  required
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="student@example.com"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl pl-9 pr-3 py-2 text-xs text-[#0F172A] placeholder-[#94A3B8] focus:bg-white focus:outline-none focus:border-[#B8860B] transition-colors"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                Create Password (min. 6 characters)
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#94A3B8]">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  id="signup-password-input"
                  type={showPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  autoComplete="new-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl pl-9 pr-10 py-2 text-xs text-[#0F172A] placeholder-[#94A3B8] focus:bg-white focus:outline-none focus:border-[#B8860B] transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-[#94A3B8] hover:text-[#64748B]"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                Confirm Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#94A3B8]">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  id="signup-confirm-password-input"
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl pl-9 pr-10 py-2 text-xs text-[#0F172A] placeholder-[#94A3B8] focus:bg-white focus:outline-none focus:border-[#B8860B] transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-[#94A3B8] hover:text-[#64748B]"
                >
                  {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              id="submit-signup-btn"
              type="submit"
              disabled={isLoading}
              className="w-full bg-[#B8860B] hover:bg-[#996F0A] text-white font-bold py-3 px-4 rounded-xl text-xs transition-all shadow-xs flex items-center justify-center space-x-2 disabled:opacity-50 mt-2"
            >
              {isLoading ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  <span>Creating Account...</span>
                </>
              ) : (
                <>
                  <span>Create Student Account</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        )}

        {/* 3. FORGOT PASSWORD FLOW */}
        {authMode === 'FORGOT_PASSWORD' && (
          <div className="mt-5 space-y-4">
            {forgotStep === 1 ? (
              <form onSubmit={handleRequestResetCode} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-[#0F172A] mb-1.5">
                    Registered Email Address
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#94A3B8]">
                      <Mail className="w-4 h-4" />
                    </div>
                    <input
                      id="forgot-email-input"
                      type="email"
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="student@example.com"
                      className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl pl-9 pr-3 py-2.5 text-xs text-[#0F172A] placeholder-[#94A3B8] focus:bg-white focus:outline-none focus:border-[#B8860B] transition-colors"
                    />
                  </div>
                </div>

                <button
                  id="request-reset-code-btn"
                  type="submit"
                  disabled={isLoading}
                  className="w-full bg-[#B8860B] hover:bg-[#996F0A] text-white font-bold py-3 px-4 rounded-xl text-xs transition-all shadow-xs flex items-center justify-center space-x-2 disabled:opacity-50"
                >
                  {isLoading ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                      <span>Sending Code...</span>
                    </>
                  ) : (
                    <>
                      <span>Generate Reset Code</span>
                      <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>
              </form>
            ) : (
              <form onSubmit={handleResetPassword} className="space-y-3.5">
                {generatedCodeNotice && (
                  <div className="p-3 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] text-[#854D0E] text-xs">
                    <p className="font-bold flex items-center space-x-1.5">
                      <Sparkles className="w-3.5 h-3.5 text-[#B8860B]" />
                      <span>Verification Code Issued</span>
                    </p>
                    <p className="mt-1 font-mono text-sm tracking-widest font-bold text-[#B8860B] bg-white px-2 py-1 rounded border border-[#FDE047] inline-block">
                      {generatedCodeNotice}
                    </p>
                    <p className="text-[10px] text-[#A16207] mt-1">
                      Enter the 6-digit code above into the verification field below.
                    </p>
                  </div>
                )}

                <div>
                  <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                    6-Digit Verification Code
                  </label>
                  <input
                    id="reset-code-input"
                    type="text"
                    required
                    maxLength={6}
                    value={resetCode}
                    onChange={(e) => setResetCode(e.target.value.replace(/\D/g, ''))}
                    placeholder="123456"
                    className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl px-3 py-2 text-center text-base font-mono tracking-widest text-[#0F172A] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                    New Password (min. 6 characters)
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#94A3B8]">
                      <Lock className="w-4 h-4" />
                    </div>
                    <input
                      id="reset-new-password-input"
                      type={showPassword ? 'text' : 'password'}
                      required
                      minLength={6}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl pl-9 pr-10 py-2 text-xs text-[#0F172A] placeholder-[#94A3B8] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute inset-y-0 right-0 pr-3 flex items-center text-[#94A3B8] hover:text-[#64748B]"
                    >
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-[#0F172A] mb-1">
                    Confirm New Password
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#94A3B8]">
                      <Lock className="w-4 h-4" />
                    </div>
                    <input
                      id="reset-confirm-password-input"
                      type={showConfirmPassword ? 'text' : 'password'}
                      required
                      minLength={6}
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl pl-9 pr-10 py-2 text-xs text-[#0F172A] placeholder-[#94A3B8] focus:bg-white focus:outline-none focus:border-[#B8860B]"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute inset-y-0 right-0 pr-3 flex items-center text-[#94A3B8] hover:text-[#64748B]"
                    >
                      {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                <button
                  id="submit-reset-password-btn"
                  type="submit"
                  disabled={isLoading}
                  className="w-full bg-[#B8860B] hover:bg-[#996F0A] text-white font-bold py-3 px-4 rounded-xl text-xs transition-all shadow-xs flex items-center justify-center space-x-2 disabled:opacity-50"
                >
                  {isLoading ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                      <span>Saving New Password...</span>
                    </>
                  ) : (
                    <>
                      <span>Save New Password & Sign In</span>
                      <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>
              </form>
            )}
          </div>
        )}
      </div>

      {/* Feature & Security Highlights */}
      <div className="mt-8 pt-5 border-t border-[#F1F5F9] space-y-2.5">
        <div className="flex items-start space-x-2.5 p-2.5 rounded-xl bg-[#F8FAFC] border border-[#E2E8F0]">
          <Shield className="w-4 h-4 text-[#B8860B] shrink-0 mt-0.5" />
          <div className="text-xs">
            <p className="font-semibold text-[#0F172A]">Secure Account Architecture</p>
            <p className="text-[11px] text-[#64748B] mt-0.5 leading-relaxed">
              Passwords are protected with salted cryptographic hashing. Your academic syllabus and notes are kept private to your student profile.
            </p>
          </div>
        </div>

        <div className="flex items-start space-x-2.5 p-2.5 rounded-xl bg-[#F8FAFC] border border-[#E2E8F0]">
          <CheckCircle2 className="w-4 h-4 text-[#22C55E] shrink-0 mt-0.5" />
          <div className="text-xs">
            <p className="font-semibold text-[#0F172A]">Dual Protection with App PIN</p>
            <p className="text-[11px] text-[#64748B] mt-0.5 leading-relaxed">
              Combine your email account login with an optional 4-digit App PIN for rapid device screen locking.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
