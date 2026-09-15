// src/components/AdaptivePlannerView.tsx
import React, { useState, useMemo } from 'react';
import {
  ChevronLeft,
  Calendar,
  Clock,
  CheckCircle,
  CheckCircle2,
  AlertTriangle,
  Play,
  RotateCcw,
  Sparkles,
  BookOpen,
  ArrowRight,
  TrendingUp,
  Target,
  FileCheck2,
  ChevronDown,
  ChevronUp,
  Award,
  HelpCircle,
  X,
  Check,
  Zap
} from 'lucide-react';
import { Topic, Chapter, Subject, StudySessionRecord, TopicTestRecord, UserProfile } from '../types';
import { generateAdaptiveDailyPlan, AdaptivePlanResult } from '../utils/planner';

interface AdaptivePlannerViewProps {
  topics: Topic[];
  chapters: Chapter[];
  subjects: Subject[];
  studySessions: StudySessionRecord[];
  testRecords: TopicTestRecord[];
  profile: UserProfile | null;
  onBack: () => void;
  onStartTopic: (topic: Topic) => void;
  onNavigateToChapter: (subjectId: string, chapterId: string) => void;
  onRecordTestResult: (topicId: string, score: number, totalQuestions?: number, correctAnswers?: number) => void;
  onUpdateTopicState?: (topicId: string, state: Topic['state']) => void;
}

export const AdaptivePlannerView: React.FC<AdaptivePlannerViewProps> = ({
  topics,
  chapters,
  subjects,
  studySessions,
  testRecords,
  profile,
  onBack,
  onStartTopic,
  onNavigateToChapter,
  onRecordTestResult,
  onUpdateTopicState,
}) => {
  const dailyTargetMinutes = profile?.dailyTargetMinutes || 120;

  // Real adaptive plan calculation
  const planResult: AdaptivePlanResult = useMemo(() => {
    return generateAdaptiveDailyPlan(
      topics,
      chapters,
      subjects,
      studySessions,
      testRecords,
      dailyTargetMinutes
    );
  }, [topics, chapters, subjects, studySessions, testRecords, dailyTargetMinutes]);

  const [isReviewOpen, setIsReviewOpen] = useState(false);
  const [testingTopic, setTestingTopic] = useState<Topic | null>(null);

  // Quick assessment test state for the testing topic
  const [testScoreInput, setTestScoreInput] = useState<number>(75);
  const [activeQuestionIndex, setActiveQuestionIndex] = useState<number>(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, number>>({});
  const [testSubmitted, setTestSubmitted] = useState<boolean>(false);
  const [quickTestMode, setQuickTestMode] = useState<'QUIZ' | 'DIRECT_SCORE'>('QUIZ');

  // Generate 4 sample assessment questions for the selected topic
  const currentTopicQuestions = useMemo(() => {
    if (!testingTopic) return [];
    return [
      {
        id: 1,
        question: `What is the core physical or mathematical principle behind ${testingTopic.name}?`,
        options: [
          'Direct rate of variation with proportional constants',
          'Inverse conservation equilibrium under boundary conditions',
          'Invariant symmetric transformation along standard axes',
          'Oscillatory damping through dissipation resistance',
        ],
        correct: 0,
      },
      {
        id: 2,
        question: `When analyzing problems in "${testingTopic.name}", which initial step is essential?`,
        options: [
          'Assume zero dissipation and discard initial state conditions',
          'Identify reference frame, known quantities, and governing laws',
          'Combine scalar and vector magnitudes without directional signs',
          'Use arbitrary dimensional units and normalize later',
        ],
        correct: 1,
      },
      {
        id: 3,
        question: `Which common misconception frequently leads to errors in ${testingTopic.name}?`,
        options: [
          'Applying dynamic relationships in purely non-inertial systems',
          'Confusing instantaneous quantities with cumulative averages',
          'Treating vector components independently',
          'Verifying dimensional homogeneity before calculations',
        ],
        correct: 1,
      },
      {
        id: 4,
        question: `How is the principle of ${testingTopic.name} verified in standard laboratory experiments?`,
        options: [
          'By measuring correlated parameters with controlled sensors and plotting regression',
          'By assuming theoretical values equal experimental outcome',
          'By discarding non-ideal data points without error analysis',
          'By isolating the observer from physical interactions',
        ],
        correct: 0,
      },
    ];
  }, [testingTopic]);

  const handleOpenTest = (topic: Topic) => {
    setTestingTopic(topic);
    setActiveQuestionIndex(0);
    setSelectedAnswers({});
    setTestSubmitted(false);
    setTestScoreInput(topic.lastScore || 70);
  };

  const handleCloseTest = () => {
    setTestingTopic(null);
    setTestSubmitted(false);
  };

  const handleSubmitQuizTest = () => {
    if (!testingTopic) return;
    let correct = 0;
    const total = currentTopicQuestions.length;
    currentTopicQuestions.forEach((q, idx) => {
      if (selectedAnswers[idx] === q.correct) {
        correct++;
      }
    });
    const calculatedScore = Math.round((correct / total) * 100);
    onRecordTestResult(testingTopic.id, calculatedScore, total, correct);
    setTestSubmitted(true);
  };

  const handleQuickScoreSubmit = (score: number) => {
    if (!testingTopic) return;
    onRecordTestResult(testingTopic.id, score, 5, Math.round((score / 100) * 5));
    setTestSubmitted(true);
  };

  const progressPercent = Math.min(100, Math.round((planResult.todayActiveMinutes / planResult.dailyTargetMinutes) * 100));

  return (
    <div className="flex-1 flex flex-col justify-between bg-[#F8FAFC]">
      <div className="p-4 sm:p-5 space-y-4">
        {/* Header Navigation */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <button
              id="planner-back-btn"
              onClick={onBack}
              className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#0F172A] transition-colors"
              title="Back to Dashboard"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <div>
              <h1 className="text-lg font-bold text-[#0F172A] flex items-center space-x-1.5">
                <span>Adaptive Study Planner</span>
              </h1>
              <p className="text-[10px] text-[#B8860B] font-semibold">
                Personalized syllabus schedule based on real performance
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-1 bg-[#FEFCE8] border border-[#FEF08A] px-2.5 py-1 rounded-lg">
            <Sparkles className="w-3.5 h-3.5 text-[#B8860B]" />
            <span className="text-[11px] font-bold text-[#B8860B]">AI Adaptive</span>
          </div>
        </div>

        {/* 1. DAILY STUDY TARGET CARD */}
        <div className="bg-white rounded-2xl border border-[#E2E8F0] p-4 sm:p-5 shadow-2xs">
          <div className="flex items-center justify-between pb-2 border-b border-[#E2E8F0]/80">
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 rounded-lg bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B]">
                <Target className="w-4 h-4" />
              </div>
              <div>
                <p className="text-xs font-bold text-[#0F172A]">Daily Study Target</p>
                <p className="text-[10px] text-[#64748B]">Active study time tracking</p>
              </div>
            </div>

            <div className="text-right">
              <span className="text-sm font-extrabold text-[#0F172A]">
                {planResult.todayActiveMinutes}m
              </span>
              <span className="text-xs text-[#64748B]"> / {planResult.dailyTargetMinutes}m</span>
            </div>
          </div>

          {/* Progress Bar */}
          <div className="mt-3">
            <div className="flex items-center justify-between text-[10px] mb-1 font-medium">
              <span className="text-[#64748B]">{progressPercent}% of daily goal completed</span>
              <span className={planResult.isTargetAchieved ? 'text-emerald-600 font-bold' : 'text-[#B8860B] font-bold'}>
                {planResult.isTargetAchieved ? 'Target Achieved! 🎉' : `${planResult.remainingMinutes}m remaining`}
              </span>
            </div>
            <div className="w-full h-2.5 bg-[#F1F5F9] rounded-full overflow-hidden border border-[#E2E8F0]">
              <div
                className={`h-full transition-all duration-500 rounded-full ${
                  planResult.isTargetAchieved ? 'bg-emerald-500' : 'bg-[#B8860B]'
                }`}
                style={{ width: `${progressPercent}%` }}
              />
            </div>
          </div>

          {/* Quick Metrics Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 mt-3 pt-3 border-t border-[#E2E8F0]/70 text-center">
            <div className="bg-[#F8FAFC] p-2 rounded-xl border border-[#E2E8F0]">
              <p className="text-[10px] text-[#64748B]">Remaining Today</p>
              <p className="text-sm font-bold text-[#0F172A] mt-0.5 font-mono">
                {planResult.remainingMinutes} min
              </p>
            </div>

            <div className="bg-[#F8FAFC] p-2 rounded-xl border border-[#E2E8F0]">
              <p className="text-[10px] text-[#64748B]">Recommended</p>
              <p className="text-sm font-bold text-[#B8860B] mt-0.5 font-mono">
                {planResult.todayPlan.length} topics
              </p>
            </div>

            <div className="bg-[#F8FAFC] p-2 rounded-xl border border-[#E2E8F0] col-span-2 sm:col-span-1">
              <p className="text-[10px] text-[#64748B]">Est. Study Time</p>
              <p className="text-sm font-bold text-[#0F172A] mt-0.5 font-mono">
                ~{planResult.totalRecommendedMinutes} min
              </p>
            </div>
          </div>

          <div className="mt-2.5 flex items-center justify-between text-[9px] text-[#64748B] bg-[#F8FAFC] px-2.5 py-1.5 rounded-lg border border-[#E2E8F0]">
            <span className="flex items-center space-x-1">
              <Clock className="w-3 h-3 text-[#B8860B]" />
              <span>Active study time only</span>
            </span>
            <span>Breaks & pauses strictly excluded</span>
          </div>
        </div>

        {/* 2. TODAY'S RECOMMENDED STUDY PLAN */}
        <div>
          <div className="flex items-center justify-between mb-2 px-1">
            <div>
              <h2 className="text-xs font-bold text-[#0F172A] uppercase tracking-wider">Today's Study Plan</h2>
              <p className="text-[10px] text-[#64748B]">
                Prioritized based on test results, review needs & curriculum sequence
              </p>
            </div>
            {planResult.todayPlan.length > 0 && (
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-[#FEFCE8] text-[#B8860B] border border-[#FEF08A]">
                {planResult.todayPlan.length} Tasks Scheduled
              </span>
            )}
          </div>

          {/* EMPTY STATE: ALL CAUGHT UP */}
          {planResult.isAllCaughtUp && (
            <div className="bg-white rounded-2xl border border-[#E2E8F0] p-6 text-center shadow-2xs">
              <div className="w-12 h-12 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-600 mx-auto mb-3">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-[#0F172A]">You're all caught up!</h3>
              <p className="text-xs text-[#64748B] mt-1">
                All topics in your current curriculum are mastered or passed.
              </p>
              <p className="text-[11px] text-[#334155] mt-2 leading-relaxed">
                Great job! You can review previously mastered topics below or take practice tests to keep your knowledge sharp.
              </p>
              {planResult.reviewTopics.length > 0 && (
                <button
                  onClick={() => setIsReviewOpen(true)}
                  className="mt-4 inline-flex items-center space-x-1.5 px-3.5 py-2 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] text-[#B8860B] text-xs font-bold hover:bg-[#FEF08A] transition-colors"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Review Mastered Topics ({planResult.reviewTopics.length})</span>
                </button>
              )}
            </div>
          )}

          {/* EMPTY STATE: TARGET ACHIEVED FOR TODAY */}
          {!planResult.isAllCaughtUp && planResult.isTargetAchieved && planResult.todayPlan.length === 0 && (
            <div className="bg-white rounded-2xl border border-emerald-200 p-5 text-center shadow-2xs">
              <div className="w-12 h-12 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-600 mx-auto mb-2">
                <Award className="w-6 h-6" />
              </div>
              <h3 className="text-sm font-bold text-[#0F172A]">Daily Goal Achieved!</h3>
              <p className="text-xs text-[#64748B] mt-1">
                You have completed your daily target of {planResult.dailyTargetMinutes} minutes of active study today.
              </p>
              <p className="text-[10px] text-[#334155] mt-1.5">
                Take a healthy rest, or explore optional review topics below.
              </p>
            </div>
          )}

          {/* LIST OF RECOMMENDED TOPICS */}
          {planResult.todayPlan.length > 0 && (
            <div className="space-y-3">
              {planResult.todayPlan.map((item, index) => {
                const isHighPriority = item.priorityLevel === 'HIGH';
                const isFailed = item.previousScore !== undefined && item.previousScore < 70;
                const isReview = item.topic.state === 'NEEDS_REVIEW';

                return (
                  <div
                    key={item.topic.id}
                    id={`planner-item-${item.topic.id}`}
                    className="bg-white rounded-xl border border-[#E2E8F0] p-4 shadow-2xs hover:border-[#CBD5E1] transition-all"
                  >
                    {/* Top Row: Index + Subject & Priority Badges */}
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center space-x-2">
                        <span className="w-6 h-6 rounded-full bg-[#0F172A] text-white flex items-center justify-center text-xs font-extrabold shrink-0 font-mono">
                          {index + 1}
                        </span>
                        <div className="flex items-center space-x-1.5">
                          <span
                            className="w-2.5 h-2.5 rounded-full shrink-0"
                            style={{ backgroundColor: item.subject.colorHex || '#B8860B' }}
                          />
                          <span className="text-xs font-bold text-[#0F172A]">{item.subject.name}</span>
                        </div>
                      </div>

                      <div className="flex items-center space-x-1.5">
                        {isHighPriority ? (
                          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-md text-[10px] font-bold bg-amber-50 text-amber-900 border border-amber-200">
                            <AlertTriangle className="w-3 h-3 text-amber-600" />
                            <span>High Priority</span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-md text-[10px] font-bold bg-slate-100 text-slate-700 border border-slate-200">
                            <span>Medium Priority</span>
                          </span>
                        )}

                        <span className="text-[10px] font-bold text-[#64748B] bg-[#F1F5F9] px-2 py-0.5 rounded-md border border-[#E2E8F0]">
                          ~{item.recommendedMinutes}m
                        </span>
                      </div>
                    </div>

                    {/* Chapter & Topic Title */}
                    <div className="mt-2.5 pl-8">
                      <p className="text-[11px] text-[#64748B] font-medium">
                        Chapter: <span className="text-[#334155]">{item.chapter.name}</span>
                      </p>
                      <h3 className="text-sm font-bold text-[#0F172A] mt-0.5">{item.topic.name}</h3>

                      {/* Reason Callout Banner */}
                      <div
                        className={`mt-2 p-2.5 rounded-lg border text-xs flex items-start space-x-2 ${
                          isFailed || isReview
                            ? 'bg-amber-50/70 border-amber-200 text-amber-900'
                            : 'bg-[#F8FAFC] border-[#E2E8F0] text-[#334155]'
                        }`}
                      >
                        <Zap className="w-3.5 h-3.5 text-[#B8860B] shrink-0 mt-0.5" />
                        <div className="text-[11px] leading-snug">
                          <span className="font-semibold text-[#0F172A]">Reason: </span>
                          <span>{item.reason}</span>
                        </div>
                      </div>

                      {/* Performance & Status Indicators */}
                      <div className="flex flex-wrap items-center gap-2 mt-2 pt-2 border-t border-[#E2E8F0]/60">
                        <span className="text-[10px] px-1.5 py-0.5 rounded bg-[#F8FAFC] text-[#64748B] font-semibold border border-[#E2E8F0]">
                          Status: {item.topic.state}
                        </span>

                        {item.previousScore !== undefined && (
                          <span
                            className={`text-[10px] px-1.5 py-0.5 rounded font-semibold border ${
                              item.previousScore >= 70
                                ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                                : 'bg-red-50 text-red-800 border-red-200'
                            }`}
                          >
                            Previous Score: {item.previousScore}%
                          </span>
                        )}

                        {item.attempts !== undefined && (
                          <span className="text-[10px] px-1.5 py-0.5 rounded bg-[#F8FAFC] text-[#64748B] font-semibold border border-[#E2E8F0]">
                            {item.attempts} {item.attempts === 1 ? 'attempt' : 'attempts'}
                          </span>
                        )}
                      </div>

                      {/* Interactive Action Buttons */}
                      <div className="flex items-center space-x-2 mt-3">
                        <button
                          id={`start-study-topic-${item.topic.id}`}
                          onClick={() => onStartTopic(item.topic)}
                          className="flex-1 py-2 px-3 rounded-xl bg-[#B8860B] text-white hover:bg-[#996F0A] text-xs font-bold flex items-center justify-center space-x-1.5 shadow-2xs transition-colors"
                        >
                          <Play className="w-3.5 h-3.5 fill-current" />
                          <span>{item.topic.state === 'STUDYING' ? 'Continue Studying' : 'Start Studying'}</span>
                        </button>

                        <button
                          id={`test-topic-btn-${item.topic.id}`}
                          onClick={() => handleOpenTest(item.topic)}
                          className="py-2 px-3 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] text-[#B8860B] hover:bg-[#FEF08A] text-xs font-bold flex items-center space-x-1 transition-colors"
                          title="Take Quick Assessment"
                        >
                          <FileCheck2 className="w-3.5 h-3.5" />
                          <span>Assess</span>
                        </button>

                        <button
                          onClick={() => onNavigateToChapter(item.subject.id, item.chapter.id)}
                          className="p-2 rounded-xl hover:bg-[#F1F5F9] text-[#64748B] border border-[#E2E8F0] transition-colors"
                          title="View in Chapter"
                        >
                          <BookOpen className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* 3. OPTIONAL REVIEW & MASTERED TOPICS SECTION */}
        {planResult.reviewTopics.length > 0 && (
          <div className="bg-white rounded-2xl border border-[#E2E8F0] overflow-hidden shadow-2xs">
            <button
              onClick={() => setIsReviewOpen(!isReviewOpen)}
              className="w-full p-4 flex items-center justify-between text-left hover:bg-[#F8FAFC] transition-colors"
            >
              <div className="flex items-center space-x-2.5">
                <div className="w-7 h-7 rounded-lg bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-600">
                  <CheckCircle className="w-3.5 h-3.5" />
                </div>
                <div>
                  <h3 className="text-xs font-bold text-[#0F172A]">Mastered Topics & Revision</h3>
                  <p className="text-[10px] text-[#64748B]">
                    {planResult.reviewTopics.length} completed topics available for optional review
                  </p>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                  Mastered
                </span>
                {isReviewOpen ? <ChevronUp className="w-4 h-4 text-[#64748B]" /> : <ChevronDown className="w-4 h-4 text-[#64748B]" />}
              </div>
            </button>

            {isReviewOpen && (
              <div className="p-4 pt-0 border-t border-[#E2E8F0] space-y-2.5 mt-2">
                <p className="text-[10px] text-[#64748B] italic mb-1">
                  Mastered topics are never forced into your daily plan. You can start a refresher session or retest anytime:
                </p>
                {planResult.reviewTopics.map((rev) => (
                  <div
                    key={rev.topic.id}
                    className="bg-[#F8FAFC] p-3 rounded-xl border border-[#E2E8F0] flex items-center justify-between"
                  >
                    <div>
                      <div className="flex items-center space-x-1.5">
                        <span
                          className="w-2 h-2 rounded-full"
                          style={{ backgroundColor: rev.subject.colorHex || '#B8860B' }}
                        />
                        <span className="text-[10px] font-bold text-[#64748B]">{rev.subject.name}</span>
                      </div>
                      <p className="text-xs font-bold text-[#0F172A] mt-0.5">{rev.topic.name}</p>
                      <div className="flex items-center space-x-2 text-[10px] text-[#64748B] mt-0.5">
                        <span>Score: {rev.previousScore ?? 85}%</span>
                        <span>•</span>
                        <span>{rev.chapter.name}</span>
                      </div>
                    </div>

                    <div className="flex items-center space-x-1">
                      <button
                        onClick={() => onStartTopic(rev.topic)}
                        className="px-2.5 py-1 rounded-lg bg-white border border-[#E2E8F0] text-[#0F172A] hover:bg-[#F1F5F9] text-[11px] font-bold flex items-center space-x-1 shadow-2xs"
                      >
                        <RotateCcw className="w-3 h-3 text-[#B8860B]" />
                        <span>Review</span>
                      </button>
                      <button
                        onClick={() => handleOpenTest(rev.topic)}
                        className="px-2.5 py-1 rounded-lg bg-[#FEFCE8] border border-[#FEF08A] text-[#B8860B] hover:bg-[#FEF08A] text-[11px] font-bold"
                      >
                        Retest
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Informational Footer */}
        <div className="text-center py-2 text-[10px] text-[#64748B]">
          StudyPilot Adaptive Algorithm adapts dynamically after every focus session & test score.
        </div>
      </div>

      {/* QUICK ASSESSMENT / TEST MODAL */}
      {testingTopic && (
        <div className="fixed inset-0 z-50 bg-[#0F172A]/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-5 border border-[#E2E8F0] shadow-xl space-y-4 max-h-[90vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="flex items-center justify-between pb-3 border-b border-[#E2E8F0]">
              <div className="flex items-center space-x-2">
                <div className="w-8 h-8 rounded-lg bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B]">
                  <FileCheck2 className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-[#0F172A]">Topic Assessment</h3>
                  <p className="text-[10px] text-[#64748B] truncate max-w-[200px]">{testingTopic.name}</p>
                </div>
              </div>
              <button
                onClick={handleCloseTest}
                className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#64748B]"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {!testSubmitted ? (
              <div className="space-y-4">
                {/* Mode Selector */}
                <div className="flex rounded-xl bg-[#F1F5F9] p-1 text-xs font-bold">
                  <button
                    onClick={() => setQuickTestMode('QUIZ')}
                    className={`flex-1 py-1.5 rounded-lg transition-all ${
                      quickTestMode === 'QUIZ'
                        ? 'bg-white text-[#0F172A] shadow-2xs'
                        : 'text-[#64748B] hover:text-[#0F172A]'
                    }`}
                  >
                    4-Question Quiz
                  </button>
                  <button
                    onClick={() => setQuickTestMode('DIRECT_SCORE')}
                    className={`flex-1 py-1.5 rounded-lg transition-all ${
                      quickTestMode === 'DIRECT_SCORE'
                        ? 'bg-white text-[#0F172A] shadow-2xs'
                        : 'text-[#64748B] hover:text-[#0F172A]'
                    }`}
                  >
                    Record Test Score
                  </button>
                </div>

                {quickTestMode === 'QUIZ' ? (
                  <div className="space-y-3">
                    <div className="flex items-center justify-between text-[11px] text-[#64748B]">
                      <span>Question {activeQuestionIndex + 1} of {currentTopicQuestions.length}</span>
                      <span>Pass mark: 70% (3/4)</span>
                    </div>

                    <div className="bg-[#F8FAFC] p-3.5 rounded-xl border border-[#E2E8F0]">
                      <p className="text-xs font-bold text-[#0F172A] leading-relaxed">
                        {currentTopicQuestions[activeQuestionIndex]?.question}
                      </p>

                      <div className="space-y-2 mt-3">
                        {currentTopicQuestions[activeQuestionIndex]?.options.map((opt, optIdx) => {
                          const isSelected = selectedAnswers[activeQuestionIndex] === optIdx;
                          return (
                            <button
                              key={optIdx}
                              onClick={() => setSelectedAnswers({ ...selectedAnswers, [activeQuestionIndex]: optIdx })}
                              className={`w-full text-left p-2.5 rounded-xl border text-xs transition-all flex items-start space-x-2 ${
                                isSelected
                                  ? 'bg-[#FEFCE8] border-[#FEF08A] text-[#B8860B] font-bold shadow-2xs'
                                  : 'bg-white border-[#E2E8F0] text-[#334155] hover:bg-[#F1F5F9]'
                              }`}
                            >
                              <span
                                className={`w-4 h-4 rounded-full flex items-center justify-center text-[10px] font-mono border shrink-0 mt-0.5 ${
                                  isSelected
                                    ? 'border-[#B8860B] bg-[#B8860B] text-white'
                                    : 'border-slate-300 text-slate-500'
                                }`}
                              >
                                {String.fromCharCode(65 + optIdx)}
                              </span>
                              <span className="flex-1">{opt}</span>
                            </button>
                          );
                        })}
                      </div>
                    </div>

                    {/* Navigation buttons */}
                    <div className="flex items-center justify-between pt-2">
                      <button
                        disabled={activeQuestionIndex === 0}
                        onClick={() => setActiveQuestionIndex((i) => Math.max(0, i - 1))}
                        className="px-3 py-1.5 rounded-lg border border-[#E2E8F0] text-xs font-semibold text-[#64748B] disabled:opacity-40"
                      >
                        Previous
                      </button>

                      {activeQuestionIndex < currentTopicQuestions.length - 1 ? (
                        <button
                          onClick={() => setActiveQuestionIndex((i) => i + 1)}
                          className="px-4 py-1.5 rounded-lg bg-[#0F172A] text-white text-xs font-bold hover:bg-slate-800"
                        >
                          Next →
                        </button>
                      ) : (
                        <button
                          id="submit-quiz-assessment-btn"
                          disabled={Object.keys(selectedAnswers).length < currentTopicQuestions.length}
                          onClick={handleSubmitQuizTest}
                          className="px-4 py-1.5 rounded-lg bg-[#B8860B] text-white text-xs font-bold hover:bg-[#996F0A] disabled:opacity-40"
                        >
                          Submit & Update Planner
                        </button>
                      )}
                    </div>
                  </div>
                ) : (
                  /* DIRECT SCORE MODE */
                  <div className="space-y-3">
                    <p className="text-xs text-[#64748B]">
                      Enter the test score achieved on this topic's official examination or worksheet. Scores below 70% automatically mark the topic for urgent review.
                    </p>

                    <div>
                      <div className="flex items-center justify-between text-xs mb-1">
                        <span className="font-semibold text-[#0F172A]">Test Score:</span>
                        <span className="font-mono font-bold text-sm text-[#B8860B]">{testScoreInput}%</span>
                      </div>
                      <input
                        id="test-score-slider"
                        type="range"
                        min="0"
                        max="100"
                        step="5"
                        value={testScoreInput}
                        onChange={(e) => setTestScoreInput(Number(e.target.value))}
                        className="w-full accent-[#B8860B]"
                      />
                      <div className="flex justify-between text-[10px] text-[#64748B] mt-1">
                        <span>0% (Fail)</span>
                        <span>70% (Pass mark)</span>
                        <span>100% (Mastery)</span>
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-2">
                      <button
                        onClick={() => handleQuickScoreSubmit(45)}
                        className="p-2 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs font-bold hover:bg-red-100"
                      >
                        Fail (45%)
                      </button>
                      <button
                        onClick={() => handleQuickScoreSubmit(60)}
                        className="p-2 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs font-bold hover:bg-amber-100"
                      >
                        Needs Review (60%)
                      </button>
                      <button
                        onClick={() => handleQuickScoreSubmit(90)}
                        className="p-2 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold hover:bg-emerald-100"
                      >
                        Pass (90%)
                      </button>
                    </div>

                    <button
                      id="submit-direct-score-btn"
                      onClick={() => handleQuickScoreSubmit(testScoreInput)}
                      className="w-full py-2.5 rounded-xl bg-[#B8860B] text-white text-xs font-bold hover:bg-[#996F0A] transition-colors"
                    >
                      Record {testScoreInput}% & Update Planner
                    </button>
                  </div>
                )}
              </div>
            ) : (
              /* RESULT SUMMARY AFTER TEST */
              <div className="text-center py-4 space-y-3">
                <div
                  className={`w-12 h-12 rounded-2xl flex items-center justify-center mx-auto ${
                    (testingTopic.lastScore || 0) >= 70
                      ? 'bg-emerald-50 border border-emerald-200 text-emerald-600'
                      : 'bg-amber-50 border border-amber-200 text-amber-600'
                  }`}
                >
                  {(testingTopic.lastScore || 0) >= 70 ? (
                    <Check className="w-6 h-6" />
                  ) : (
                    <AlertTriangle className="w-6 h-6" />
                  )}
                </div>

                <div>
                  <h4 className="text-sm font-bold text-[#0F172A]">
                    {(testingTopic.lastScore || 0) >= 70 ? 'Topic Passed!' : 'Review Recommended'}
                  </h4>
                  <p className="text-xs text-[#64748B] mt-1">
                    Recorded score: <span className="font-bold text-[#0F172A]">{testingTopic.lastScore}%</span>
                  </p>
                </div>

                <div className="bg-[#F8FAFC] p-3 rounded-xl border border-[#E2E8F0] text-[11px] text-[#334155] text-left">
                  {(testingTopic.lastScore || 0) >= 70 ? (
                    <p>
                      🎉 This topic has achieved passing mastery ({testingTopic.lastScore}%). It has been removed from urgent daily review and added to your Mastered Topics list.
                    </p>
                  ) : (
                    <p>
                      ⚠️ With a score of {testingTopic.lastScore}%, this topic has been placed at <strong>HIGH PRIORITY</strong> in your Adaptive Planner for concept reinforcement.
                    </p>
                  )}
                </div>

                <button
                  id="close-test-result-btn"
                  onClick={handleCloseTest}
                  className="w-full py-2.5 rounded-xl bg-[#0F172A] text-white text-xs font-bold hover:bg-slate-800 transition-colors"
                >
                  View Updated Planner
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
