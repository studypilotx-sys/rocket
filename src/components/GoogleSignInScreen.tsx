import React from 'react';
import { AuthScreen } from './AuthScreen';
import { AuthUser } from '../types';

interface GoogleSignInScreenProps {
  onSignInSuccess: (user: AuthUser) => void;
  initialError?: string | null;
}

// Proxies to AuthScreen to ensure full Email + Password authentication is always used
export function GoogleSignInScreen({ onSignInSuccess, initialError }: GoogleSignInScreenProps) {
  return <AuthScreen onSignInSuccess={onSignInSuccess} initialError={initialError} />;
}
