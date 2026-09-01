import React, { createContext, useContext, useState, useEffect } from 'react';
import { UserProfile, DriverProfile, UserRole } from '../types';
import { auth, isFirebaseConfigured } from '../services/firebase';
import { onAuthStateChanged, signOut as fbSignOut, signInWithEmailAndPassword, createUserWithEmailAndPassword } from 'firebase/auth';
import { storageService } from '../services/storageService';

interface AuthContextType {
  currentUser: UserProfile | DriverProfile | null;
  activeRole: UserRole;
  isDriver: boolean;
  switchRole: (role: UserRole) => void;
  loginAsDemoClient: () => void;
  loginAsDemoDriver: () => void;
  loginWithEmail: (email: string, pass: string) => Promise<void>;
  registerWithEmail: (name: string, email: string, pass: string, role: UserRole) => Promise<void>;
  logout: () => void;
}

const DEFAULT_CLIENT_USER: UserProfile = {
  id: 'client_demo_1',
  name: 'Awa Traoré',
  email: 'awa.traore@gmail.com',
  phone: '+225 07 88 12 34 56',
  role: 'client',
  avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
  defaultCity: 'Abidjan',
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [activeRole, setActiveRole] = useState<UserRole>('client');
  const [currentUser, setCurrentUser] = useState<UserProfile | DriverProfile | null>(DEFAULT_CLIENT_USER);

  useEffect(() => {
    // Sync driver profile if role is driver
    if (activeRole === 'driver') {
      setCurrentUser(storageService.getDriverProfile());
    } else {
      setCurrentUser(DEFAULT_CLIENT_USER);
    }
  }, [activeRole]);

  useEffect(() => {
    if (isFirebaseConfigured() && auth) {
      const unsubscribe = onAuthStateChanged(auth, (firebaseUser) => {
        if (firebaseUser) {
          setCurrentUser((prev) => ({
            id: firebaseUser.uid,
            name: firebaseUser.displayName || 'Utilisateur WÀNDÉ',
            email: firebaseUser.email || '',
            phone: firebaseUser.phoneNumber || '+225 07 00 00 00 00',
            role: activeRole,
            avatarUrl: firebaseUser.photoURL || undefined,
          }));
        }
      });
      return () => unsubscribe();
    }
  }, [activeRole]);

  const switchRole = (role: UserRole) => {
    setActiveRole(role);
    if (role === 'driver') {
      setCurrentUser(storageService.getDriverProfile());
    } else {
      setCurrentUser(DEFAULT_CLIENT_USER);
    }
  };

  const loginAsDemoClient = () => {
    setActiveRole('client');
    setCurrentUser(DEFAULT_CLIENT_USER);
  };

  const loginAsDemoDriver = () => {
    setActiveRole('driver');
    setCurrentUser(storageService.getDriverProfile());
  };

  const loginWithEmail = async (email: string, pass: string) => {
    if (isFirebaseConfigured() && auth) {
      await signInWithEmailAndPassword(auth, email, pass);
    } else {
      // Mock local fallback
      setCurrentUser({
        id: `user_${Date.now()}`,
        name: email.split('@')[0],
        email,
        phone: '+225 07 88 99 00 11',
        role: activeRole,
      });
    }
  };

  const registerWithEmail = async (name: string, email: string, pass: string, role: UserRole) => {
    if (isFirebaseConfigured() && auth) {
      await createUserWithEmailAndPassword(auth, email, pass);
    }
    setActiveRole(role);
    setCurrentUser({
      id: `user_${Date.now()}`,
      name,
      email,
      phone: '+225 07 88 99 00 11',
      role,
    });
  };

  const logout = () => {
    if (isFirebaseConfigured() && auth) {
      fbSignOut(auth);
    }
    setCurrentUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        activeRole,
        isDriver: activeRole === 'driver',
        switchRole,
        loginAsDemoClient,
        loginAsDemoDriver,
        loginWithEmail,
        registerWithEmail,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
