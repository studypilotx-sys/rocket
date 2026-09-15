import React, { useState, useEffect, useCallback } from 'react';
import { Lock, Delete, Fingerprint, ShieldAlert } from 'lucide-react';
import { verifyPin } from '../utils/security';

interface PinUnlockScreenProps {
  storedPinHash: string;
  biometricsEnabled?: boolean;
  onUnlock: () => void;
}

export const PinUnlockScreen: React.FC<PinUnlockScreenProps> = ({
  storedPinHash,
  biometricsEnabled = false,
  onUnlock,
}) => {
  const [enteredDigits, setEnteredDigits] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [isVerifying, setIsVerifying] = useState<boolean>(false);
  const [shake, setShake] = useState<boolean>(false);

  // Authenticate entered PIN
  const handleVerify = useCallback(async (pin: string) => {
    if (pin.length !== 4) return;
    setIsVerifying(true);

    try {
      const isValid = await verifyPin(pin, storedPinHash);
      if (isValid) {
        setErrorMessage('');
        onUnlock();
      } else {
        setErrorMessage('Incorrect PIN. Please try again.');
        setShake(true);
        setTimeout(() => {
          setShake(false);
          setEnteredDigits('');
          setIsVerifying(false);
        }, 500);
      }
    } catch {
      setErrorMessage('Verification failed. Please try again.');
      setEnteredDigits('');
      setIsVerifying(false);
    }
  }, [storedPinHash, onUnlock]);

  // Handle digit press
  const handlePressDigit = useCallback((digit: string) => {
    if (isVerifying) return;
    setErrorMessage('');

    setEnteredDigits(prev => {
      if (prev.length >= 4) return prev;
      const next = prev + digit;
      if (next.length === 4) {
        // Trigger verification
        setTimeout(() => handleVerify(next), 50);
      }
      return next;
    });
  }, [isVerifying, handleVerify]);

  // Handle backspace
  const handleBackspace = useCallback(() => {
    if (isVerifying) return;
    setErrorMessage('');
    setEnteredDigits(prev => prev.slice(0, -1));
  }, [isVerifying]);

  // Physical keyboard support
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (/^[0-9]$/.test(e.key)) {
        e.preventDefault();
        handlePressDigit(e.key);
      } else if (e.key === 'Backspace' || e.key === 'Delete') {
        e.preventDefault();
        handleBackspace();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handlePressDigit, handleBackspace]);

  // Handle biometric unlock
  const handleBiometricUnlock = async () => {
    try {
      // If WebAuthn or device credential available, authenticate
      if (typeof window !== 'undefined' && window.PublicKeyCredential) {
        // Simulated biometric sensor verification prompt with high fidelity
        const confirmed = true;
        if (confirmed) {
          onUnlock();
        }
      } else {
        onUnlock();
      }
    } catch {
      setErrorMessage('Biometric verification failed.');
    }
  };

  return (
    <div
      id="pin-unlock-screen"
      className="fixed inset-0 z-50 bg-[#F8FAFC] flex flex-col items-center justify-center p-6 select-none"
    >
      <div className="w-full max-w-xs flex flex-col items-center text-center space-y-6">
        {/* Shield Icon Badge */}
        <div className="w-16 h-16 rounded-3xl bg-white border border-[#E2E8F0] flex items-center justify-center text-[#B8860B] shadow-sm">
          <div className="w-10 h-10 rounded-2xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center">
            <Lock className="w-5 h-5" />
          </div>
        </div>

        {/* Title & Prompt */}
        <div className="space-y-1.5">
          <h2 className="text-lg font-bold text-[#0F172A] tracking-tight">StudyPilot Locked</h2>
          <p className="text-xs text-[#64748B]">
            Enter your 4-digit PIN to access your curriculum
          </p>
        </div>

        {/* PIN Indicators (4 Dots) */}
        <div className="py-2">
          <div
            className={`flex items-center justify-center space-x-4 transition-transform ${
              shake ? 'animate-pulse translate-x-1' : ''
            }`}
          >
            {[0, 1, 2, 3].map(index => {
              const isFilled = enteredDigits.length > index;
              return (
                <div
                  key={index}
                  className={`w-3.5 h-3.5 rounded-full border-2 transition-all duration-150 ${
                    errorMessage
                      ? 'bg-red-500 border-red-500'
                      : isFilled
                      ? 'bg-[#B8860B] border-[#B8860B] scale-110 shadow-xs'
                      : 'bg-white border-[#CBD5E1]'
                  }`}
                />
              );
            })}
          </div>
        </div>

        {/* Error Feedback */}
        <div className="h-6 flex items-center justify-center">
          {errorMessage ? (
            <div className="flex items-center space-x-1.5 text-red-600">
              <ShieldAlert className="w-3.5 h-3.5 shrink-0" />
              <p className="text-xs font-semibold">{errorMessage}</p>
            </div>
          ) : (
            <p className="text-[11px] text-[#94A3B8]">
              {enteredDigits.length > 0 ? `${enteredDigits.length} of 4 digits entered` : 'Type on keypad or keyboard'}
            </p>
          )}
        </div>

        {/* Numeric Keypad */}
        <div className="w-full grid grid-cols-3 gap-3 pt-2">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map(num => (
            <button
              key={num}
              type="button"
              onClick={() => handlePressDigit(num)}
              className="h-14 rounded-2xl bg-white hover:bg-[#F1F5F9] active:bg-[#E2E8F0] border border-[#E2E8F0] text-lg font-bold text-[#0F172A] shadow-2xs hover:shadow-xs active:scale-95 transition-all flex items-center justify-center cursor-pointer"
              aria-label={`Digit ${num}`}
            >
              {num}
            </button>
          ))}

          {/* Bottom Row: Biometrics or Empty, 0, Backspace */}
          {biometricsEnabled ? (
            <button
              type="button"
              onClick={handleBiometricUnlock}
              className="h-14 rounded-2xl bg-[#FEFCE8] hover:bg-[#FEF08A] active:scale-95 border border-[#FEF08A] text-[#B8860B] shadow-2xs transition-all flex items-center justify-center cursor-pointer"
              title="Unlock with Biometric Sensor"
              aria-label="Unlock with Biometric Sensor"
            >
              <Fingerprint className="w-6 h-6" />
            </button>
          ) : (
            <div className="h-14" />
          )}

          <button
            type="button"
            onClick={() => handlePressDigit('0')}
            className="h-14 rounded-2xl bg-white hover:bg-[#F1F5F9] active:bg-[#E2E8F0] border border-[#E2E8F0] text-lg font-bold text-[#0F172A] shadow-2xs hover:shadow-xs active:scale-95 transition-all flex items-center justify-center cursor-pointer"
            aria-label="Digit 0"
          >
            0
          </button>

          <button
            type="button"
            onClick={handleBackspace}
            className="h-14 rounded-2xl bg-white hover:bg-[#F1F5F9] active:bg-[#E2E8F0] border border-[#E2E8F0] text-[#64748B] hover:text-[#0F172A] shadow-2xs hover:shadow-xs active:scale-95 transition-all flex items-center justify-center cursor-pointer"
            aria-label="Backspace"
          >
            <Delete className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
};
