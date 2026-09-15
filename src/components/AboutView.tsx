import React from 'react';
import { ChevronLeft, School, Calendar, BookOpen, ShieldCheck, FileText, CheckCircle2, TrendingUp, Bot } from 'lucide-react';

interface AboutViewProps {
  onBack: () => void;
}

export const AboutView: React.FC<AboutViewProps> = ({ onBack }) => {
  const capabilities = [
    {
      title: 'Adaptive Study Planning',
      desc: 'Schedules balanced daily focus targets and dynamically recalibrates your workload across curriculum subjects.',
      icon: Calendar,
    },
    {
      title: 'Subject & Topic Organization',
      desc: 'Hierarchical breakdown of subjects, chapters, and topics tailored to your education board and grade syllabus.',
      icon: BookOpen,
    },
    {
      title: 'Focus Guardian',
      desc: 'Privacy-first on-device presence monitoring that protects study time and sounds alerts without cloud video streaming.',
      icon: ShieldCheck,
    },
    {
      title: 'Study Materials',
      desc: 'Local repository for video lectures, PDFs, notes, and study documents linked directly to your syllabus topics.',
      icon: FileText,
    },
    {
      title: 'Practice & Official Tests',
      desc: 'Evidence verification, active recall quizzes, and timed syllabus evaluations to validate true conceptual understanding.',
      icon: CheckCircle2,
    },
    {
      title: 'Progress Tracking',
      desc: 'Transparent topic mastery ratings, active focus streaks, and empirical study metrics without vanity statistics.',
      icon: TrendingUp,
    },
    {
      title: 'Pluto Assistant',
      desc: 'Academic companion for concept explanations, quiz generation, and motivational study guidance.',
      icon: Bot,
    },
  ];

  return (
    <div className="p-5 space-y-4 pb-10">
      {/* Header */}
      <div className="flex items-center space-x-2">
        <button
          id="about-back-btn"
          onClick={onBack}
          className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#0F172A] transition-colors"
          aria-label="Back to Settings"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <h1 className="text-lg font-bold text-[#0F172A]">About StudyPilot</h1>
      </div>

      {/* Hero Card */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-6 text-center shadow-xs">
        <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-[#FEFCE8] to-[#FEF9C3] border border-[#FDE047]/60 flex items-center justify-center text-[#B8860B] mx-auto mb-3 shadow-2xs">
          <School className="w-7 h-7" />
        </div>
        <h2 className="text-xl font-bold text-[#0F172A]">StudyPilot</h2>
        <p className="text-xs font-semibold text-[#16A34A] mt-0.5 flex items-center justify-center gap-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-[#22C55E]" />
          <span>Version 1.0.0 • Academic Edition</span>
        </p>

        <div className="my-4 border-t border-[#E2E8F0]" />

        <p className="text-xs text-[#334155] leading-relaxed font-normal">
          StudyPilot is a personal academic companion designed to help students organize their curriculum, plan study sessions, focus during study, learn from their own study materials, test their understanding, and track meaningful academic progress.
        </p>
      </div>

      {/* Core Capabilities */}
      <div className="space-y-2.5">
        <h3 className="text-xs font-bold text-[#0F172A] uppercase tracking-wider px-1">
          Core Capabilities
        </h3>

        <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 divide-y divide-[#F1F5F9] shadow-xs">
          {capabilities.map((cap, i) => {
            const Icon = cap.icon;
            return (
              <div key={i} className={`flex items-start space-x-3.5 ${i === 0 ? 'pb-3.5' : i === capabilities.length - 1 ? 'pt-3.5' : 'py-3.5'}`}>
                <div className="w-8 h-8 rounded-lg bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shrink-0 mt-0.5 shadow-2xs">
                  <Icon className="w-4 h-4" />
                </div>
                <div>
                  <h4 className="text-xs font-bold text-[#0F172A]">{cap.title}</h4>
                  <p className="text-[11px] text-[#64748B] leading-relaxed mt-0.5">{cap.desc}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Creator Attribution */}
      <div className="bg-[#F8FAFC] rounded-2xl border border-[#E2E8F0] p-4 text-center">
        <p className="text-sm font-bold text-[#0F172A]">Created by Mohammad Fahad</p>
        <p className="text-[11px] text-[#64748B] mt-0.5">Designed for disciplined, purposeful academic learning.</p>
      </div>
    </div>
  );
};
