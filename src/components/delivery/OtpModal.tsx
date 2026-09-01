import React, { useState } from 'react';
import { KeyRound, Check, X, ShieldAlert, Sparkles } from 'lucide-react';
import confetti from 'canvas-confetti';

interface OtpModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (otp: string) => Promise<void>;
  recipientName: string;
  expectedOtpForDemo?: string;
}

export const OtpModal: React.FC<OtpModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  recipientName,
  expectedOtpForDemo,
}) => {
  const [digits, setDigits] = useState<string[]>(['', '', '', '']);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleDigitChange = (index: number, value: string) => {
    const char = value.slice(-1);
    if (!/^\d*$/.test(char)) return;

    const newDigits = [...digits];
    newDigits[index] = char;
    setDigits(newDigits);
    setErrorMsg(null);

    // Auto-focus next input
    if (char && index < 3) {
      const nextInput = document.getElementById(`otp-input-${index + 1}`);
      nextInput?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      const prevInput = document.getElementById(`otp-input-${index - 1}`);
      prevInput?.focus();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const enteredOtp = digits.join('');
    if (enteredOtp.length < 4) {
      setErrorMsg('Veuillez saisir les 4 chiffres du code OTP');
      return;
    }

    try {
      setIsSubmitting(true);
      await onConfirm(enteredOtp);
      confetti({
        particleCount: 80,
        spread: 70,
        origin: { y: 0.6 },
        colors: ['#0066FF', '#16A34A', '#FFB800'],
      });
      onClose();
    } catch (err: any) {
      setErrorMsg(err?.message || 'Code OTP incorrect');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
      <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-200 animate-slide-up space-y-4 text-center">
        <div className="w-14 h-14 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center mx-auto ring-8 ring-blue-50/50">
          <KeyRound className="w-7 h-7" />
        </div>

        <div>
          <h3 className="font-extrabold text-slate-900 text-lg">
            Validation de la livraison
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Demandez le code secret à 4 chiffres à <strong className="text-slate-800">{recipientName}</strong> pour débloquer votre paiement.
          </p>
        </div>

        {/* Demo Hint Helper */}
        {expectedOtpForDemo && (
          <div className="bg-amber-50 border border-amber-200 rounded-xl p-2.5 text-xs text-amber-800 flex items-center justify-between">
            <span>💡 Code démo destinataire :</span>
            <button
              type="button"
              onClick={() => setDigits(expectedOtpForDemo.split(''))}
              className="font-black bg-amber-200/80 px-2 py-0.5 rounded text-amber-900 hover:bg-amber-300"
            >
              {expectedOtpForDemo} (Remplir)
            </button>
          </div>
        )}

        {/* 4 Digit Inputs */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="flex justify-center gap-3">
            {digits.map((digit, idx) => (
              <input
                key={idx}
                id={`otp-input-${idx}`}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={digit}
                onChange={(e) => handleDigitChange(idx, e.target.value)}
                onKeyDown={(e) => handleKeyDown(idx, e)}
                className="w-12 h-14 text-center text-2xl font-black bg-slate-50 border-2 border-slate-200 rounded-2xl focus:border-blue-600 focus:bg-blue-50/30 focus:outline-none transition-all"
              />
            ))}
          </div>

          {errorMsg && (
            <p className="text-xs text-red-600 font-bold bg-red-50 py-1.5 px-3 rounded-lg border border-red-100">
              {errorMsg}
            </p>
          )}

          <div className="flex gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-3 rounded-xl border border-slate-200 text-slate-700 font-bold text-sm hover:bg-slate-50"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={isSubmitting || digits.join('').length < 4}
              className="flex-1 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-sm shadow-md shadow-emerald-600/25 disabled:opacity-50 flex items-center justify-center gap-1.5"
            >
              <Check className="w-4 h-4 stroke-[3]" />
              <span>Valider ({digits.join('').length}/4)</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
