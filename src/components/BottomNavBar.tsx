import React from 'react';
import { Home, BookOpen, Calendar, TrendingUp, Bot } from 'lucide-react';
import { ScreenType } from '../types';

interface BottomNavBarProps {
  currentScreen: ScreenType;
  onNavigate: (screen: ScreenType) => void;
}

export const BottomNavBar: React.FC<BottomNavBarProps> = ({ currentScreen, onNavigate }) => {
  const tabs: Array<{ id: ScreenType; label: string; icon: React.FC<{ className?: string }> }> = [
    { id: 'HOME', label: 'Home', icon: Home },
    { id: 'SUBJECTS', label: 'Subjects', icon: BookOpen },
    { id: 'PLANNER', label: 'Planner', icon: Calendar },
    { id: 'PROGRESS', label: 'Progress', icon: TrendingUp },
    { id: 'PLUTO', label: 'Pluto', icon: Bot },
  ];

  return (
    <nav
      id="main-bottom-navigation"
      aria-label="Main Navigation"
      className="bg-white border-t border-[#E2E8F0] px-2 py-1.5 flex items-center justify-around z-20 shrink-0"
    >
      {tabs.map(tab => {
        const Icon = tab.icon;
        const isActive =
          currentScreen === tab.id ||
          (tab.id === 'SUBJECTS' && (currentScreen === 'CHAPTERS' || currentScreen === 'TOPICS'));

        return (
          <button
            key={tab.id}
            id={`nav-tab-${tab.id.toLowerCase()}`}
            onClick={() => onNavigate(tab.id)}
            className={`flex flex-col items-center justify-center py-1 px-3 rounded-xl transition-all duration-150 min-w-[56px] ${
              isActive
                ? 'text-[#B8860B] font-bold'
                : 'text-[#64748B] hover:text-[#0F172A] font-medium'
            }`}
          >
            <div className={`p-1 rounded-lg transition-colors ${isActive ? 'bg-[#FEFCE8] text-[#B8860B]' : ''}`}>
              <Icon className="w-4 h-4" />
            </div>
            <span className="text-[10px] tracking-tight mt-0.5">{tab.label}</span>
            {isActive && (
              <span className="w-1.5 h-1.5 rounded-full bg-[#22C55E] mt-0.5 shadow-xs" />
            )}
          </button>
        );
      })}
    </nav>
  );
};
