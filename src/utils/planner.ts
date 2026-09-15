// src/utils/planner.ts
import { Topic, Chapter, Subject, StudySessionRecord, TopicTestRecord, PlannerRecommendation, PlannerPriorityLevel } from '../types';

/**
 * Checks if a timestamp represents a moment during today (local calendar date).
 */
export function isToday(timestamp: number): boolean {
  if (!timestamp) return false;
  const d = new Date(timestamp);
  const now = new Date();
  return (
    d.getDate() === now.getDate() &&
    d.getMonth() === now.getMonth() &&
    d.getFullYear() === now.getFullYear()
  );
}

/**
 * Calculates strictly active study minutes completed today.
 * Excludes break seconds, paused seconds, and absence interruptions.
 */
export function calculateTodayStudyTime(studySessions: StudySessionRecord[]): {
  activeSeconds: number;
  activeMinutes: number;
  breakSeconds: number;
  pauseSeconds: number;
  sessionCount: number;
} {
  const todaySessions = studySessions.filter((s) => isToday(s.timestamp || (s as any).completedAt || 0));

  let activeSeconds = 0;
  let breakSeconds = 0;
  let pauseSeconds = 0;

  for (const session of todaySessions) {
    activeSeconds += session.activeSeconds || 0;
    breakSeconds += session.breakSeconds || 0;
    pauseSeconds += session.pauseSeconds || 0;
  }

  return {
    activeSeconds,
    activeMinutes: Math.floor(activeSeconds / 60),
    breakSeconds,
    pauseSeconds,
    sessionCount: todaySessions.length,
  };
}

/**
 * Evaluates priority score and generates an objective reason for a topic.
 *
 * Priority Hierarchy:
 * 1. Repeated failed test attempts (Highest urgency)
 * 2. Failed previous tests (Score < 70%)
 * 3. Explicitly marked NEEDS_REVIEW
 * 4. Weak topics / low test scores
 * 5. Difficult unfinished topics
 * 6. Important unfinished topics
 * 7. In progress / currently being studied
 * 8. Not studied recently (stale knowledge)
 * 9. Remaining topics in current active chapter
 * 10. Topics not started (curriculum order)
 * 11. Mastered/Passed topics (Review only)
 */
export function evaluateTopicPriority(
  topic: Topic,
  chapter: Chapter | undefined,
  subject: Subject | undefined,
  recentSessions: StudySessionRecord[],
  testRecords: TopicTestRecord[]
): {
  priorityScore: number;
  priorityLevel: PlannerPriorityLevel;
  reason: string;
} {
  // Find test records for this topic
  const topicTests = testRecords.filter((t) => t.topicId === topic.id);
  const latestTest = topicTests.length > 0 ? topicTests[0] : null;

  const failedAttempts = topic.failedAttempts || topicTests.filter((t) => !t.passed).length;
  const lastScore = topic.lastScore ?? latestTest?.score;

  // Find recent study sessions for this topic
  const topicSessions = recentSessions.filter((s) => s.topicId === topic.id);
  const lastSession = topicSessions.length > 0 ? topicSessions[0] : null;
  const lastStudiedAt = topic.lastStudiedAt || lastSession?.timestamp;

  const now = Date.now();
  const daysSinceStudied = lastStudiedAt ? (now - lastStudiedAt) / (1000 * 60 * 60 * 24) : null;

  // 1. REPEATED FAILED ATTEMPTS (Highest Urgency)
  if (failedAttempts >= 2) {
    return {
      priorityScore: 2000 + failedAttempts * 100,
      priorityLevel: 'HIGH',
      reason: `Repeated test struggles — ${failedAttempts} failed attempts. Urgent reinforcement needed.`,
    };
  }

  // 2. FAILED PREVIOUS TEST
  if ((lastScore !== undefined && lastScore < 70) || (latestTest && !latestTest.passed)) {
    const scoreText = lastScore !== undefined ? ` (Score: ${lastScore}%)` : '';
    return {
      priorityScore: 1600 + (70 - (lastScore ?? 50)) * 5,
      priorityLevel: 'HIGH',
      reason: `Failed previous test${scoreText} — review core concepts before retesting.`,
    };
  }

  // 3. TOPIC MARKED NEEDS_REVIEW
  if (topic.state === 'NEEDS_REVIEW') {
    const scoreText = lastScore !== undefined ? ` — previous test score ${lastScore}%` : '';
    return {
      priorityScore: 1400,
      priorityLevel: 'HIGH',
      reason: `Needs review${scoreText} — scheduled for knowledge reinforcement.`,
    };
  }

  // 4. WEAK TEST SCORE (e.g. 50 - 69%)
  if (lastScore !== undefined && lastScore < 75 && topic.state !== 'PASSED' && topic.state !== 'COMPLETED') {
    return {
      priorityScore: 1200 + (75 - lastScore) * 5,
      priorityLevel: 'HIGH',
      reason: `Low test score (${lastScore}%) — targeted revision recommended.`,
    };
  }

  // 5. DIFFICULT TOPIC
  if (topic.difficulty === 'HARD' && topic.state !== 'PASSED' && topic.state !== 'COMPLETED') {
    return {
      priorityScore: 950 - topic.orderIndex * 2,
      priorityLevel: 'HIGH',
      reason: 'Challenging curriculum topic — requires focused attention and practice.',
    };
  }

  // 6. IMPORTANT UNFINISHED TOPIC
  if (topic.isImportant && topic.state !== 'PASSED' && topic.state !== 'COMPLETED') {
    return {
      priorityScore: 900 - topic.orderIndex * 2,
      priorityLevel: 'HIGH',
      reason: 'Core foundational topic — high exam importance.',
    };
  }

  // 7. CURRENTLY BEING STUDIED (In progress)
  if (topic.state === 'STUDYING') {
    return {
      priorityScore: 800 - topic.orderIndex * 2,
      priorityLevel: 'MEDIUM',
      reason: 'Currently in progress — continue active study momentum.',
    };
  }

  // 8. NOT STUDIED RECENTLY (> 3 days since last session or unfinished stale)
  if (daysSinceStudied !== null && daysSinceStudied >= 3 && topic.state !== 'PASSED' && topic.state !== 'COMPLETED') {
    const days = Math.floor(daysSinceStudied);
    return {
      priorityScore: 700 + Math.min(100, days * 10),
      priorityLevel: 'MEDIUM',
      reason: `Not studied recently (${days} days ago) — refresh concepts to retain mastery.`,
    };
  }

  // 9. REMAINING TOPICS IN CURRENT CHAPTER
  // (Check if any session was recorded in this chapter recently)
  const isChapterActive = recentSessions.some((s) => {
    return s.topicName && s.topicName === topic.name;
  });
  if (isChapterActive && topic.state === 'NOT_STARTED') {
    return {
      priorityScore: 600 - topic.orderIndex * 5,
      priorityLevel: 'MEDIUM',
      reason: 'Continue current chapter — build on sequential chapter context.',
    };
  }

  // 10. NEW TOPIC (NOT STARTED)
  if (topic.state === 'NOT_STARTED') {
    // Foundational topics (orderIndex 0 or 1) have higher priority
    const baseScore = topic.orderIndex === 0 ? 550 : 500 - topic.orderIndex * 5;
    return {
      priorityScore: baseScore,
      priorityLevel: topic.orderIndex === 0 ? 'HIGH' : 'MEDIUM',
      reason: topic.orderIndex === 0 ? 'New chapter foundation — not started.' : 'New topic — not started.',
    };
  }

  // 11. EVIDENCE REQUIRED OR TEST AVAILABLE
  if (topic.state === 'TEST_AVAILABLE' || topic.state === 'EVIDENCE_REQUIRED') {
    return {
      priorityScore: 750,
      priorityLevel: 'MEDIUM',
      reason: 'Ready for assessment test — verify understanding.',
    };
  }

  // 12. MASTERED / COMPLETED TOPICS (Low Priority / Optional Review)
  const daysSinceTest = latestTest ? (now - latestTest.timestamp) / (1000 * 60 * 60 * 24) : null;
  const daysText = daysSinceTest && daysSinceTest >= 5 ? ` (${Math.floor(daysSinceTest)} days since test)` : '';
  return {
    priorityScore: 100 + (lastScore || 80),
    priorityLevel: 'LOW',
    reason: `Topic mastered${daysText} — optional review.`,
  };
}

export interface AdaptivePlanResult {
  dailyTargetMinutes: number;
  todayActiveMinutes: number;
  todayActiveSeconds: number;
  todayBreakSeconds: number;
  todayPauseSeconds: number;
  remainingMinutes: number;
  isTargetAchieved: boolean;
  isAllCaughtUp: boolean;
  todayPlan: PlannerRecommendation[];
  totalRecommendedMinutes: number;
  otherUnfinishedCount: number;
  reviewTopics: PlannerRecommendation[];
}

/**
 * Generates the real adaptive study plan based on student profile, curriculum,
 * study history, and verified test results.
 */
export function generateAdaptiveDailyPlan(
  topics: Topic[],
  chapters: Chapter[],
  subjects: Subject[],
  studySessions: StudySessionRecord[],
  testRecords: TopicTestRecord[],
  dailyTargetMinutes: number
): AdaptivePlanResult {
  const { activeMinutes, activeSeconds, breakSeconds, pauseSeconds } = calculateTodayStudyTime(studySessions);
  const remainingMinutes = Math.max(0, dailyTargetMinutes - activeMinutes);
  const isTargetAchieved = remainingMinutes === 0 && activeMinutes > 0;

  // Chapter and Subject maps for rapid lookup
  const chapterMap = new Map<string, Chapter>();
  for (const c of chapters) chapterMap.set(c.id, c);

  const subjectMap = new Map<string, Subject>();
  for (const s of subjects) subjectMap.set(s.id, s);

  // Evaluate all topics
  const allEvaluations: PlannerRecommendation[] = [];

  for (const topic of topics) {
    const chapter = chapterMap.get(topic.chapterId);
    if (!chapter) continue;
    const subject = subjectMap.get(chapter.subjectId);
    if (!subject) continue;

    const { priorityScore, priorityLevel, reason } = evaluateTopicPriority(
      topic,
      chapter,
      subject,
      studySessions,
      testRecords
    );

    // Associated test results
    const topicTests = testRecords.filter((t) => t.topicId === topic.id);
    const lastScore = topic.lastScore ?? (topicTests.length > 0 ? topicTests[0].score : undefined);
    const attempts = topic.testAttempts ?? topicTests.length;
    const failedAttempts = topic.failedAttempts ?? topicTests.filter((t) => !t.passed).length;

    allEvaluations.push({
      topic,
      subject,
      chapter,
      priorityScore,
      priorityLevel,
      reason,
      recommendedMinutes: topic.estimatedMinutes || 25,
      previousScore: lastScore,
      attempts: attempts > 0 ? attempts : undefined,
      failedAttempts: failedAttempts > 0 ? failedAttempts : undefined,
    });
  }

  // Separate unfinished/review candidate topics from mastered topics
  const unfinishedCandidates = allEvaluations.filter(
    (item) => item.topic.state !== 'PASSED' && item.topic.state !== 'COMPLETED'
  );

  const masteredCandidates = allEvaluations.filter(
    (item) => item.topic.state === 'PASSED' || item.topic.state === 'COMPLETED'
  );

  // Sort unfinished candidates strictly by priorityScore descending
  // Tie breakers: subject orderIndex, chapter orderIndex, topic orderIndex
  unfinishedCandidates.sort((a, b) => {
    if (b.priorityScore !== a.priorityScore) {
      return b.priorityScore - a.priorityScore;
    }
    if (a.subject.orderIndex !== b.subject.orderIndex) {
      return a.subject.orderIndex - b.subject.orderIndex;
    }
    if (a.chapter.orderIndex !== b.chapter.orderIndex) {
      return a.chapter.orderIndex - b.chapter.orderIndex;
    }
    return a.topic.orderIndex - b.topic.orderIndex;
  });

  // Sort mastered candidates: oldest tested / lowest passing score first for review
  masteredCandidates.sort((a, b) => {
    const scoreA = a.previousScore ?? 100;
    const scoreB = b.previousScore ?? 100;
    return scoreA - scoreB;
  });

  const isAllCaughtUp = unfinishedCandidates.length === 0;

  // Build the Daily Plan packing topics into available remaining study time
  const todayPlan: PlannerRecommendation[] = [];
  let totalRecommendedMinutes = 0;

  if (remainingMinutes > 0) {
    for (const candidate of unfinishedCandidates) {
      const topicTime = candidate.recommendedMinutes;

      // Pack topic if it fits within remaining time
      if (totalRecommendedMinutes + topicTime <= remainingMinutes) {
        todayPlan.push(candidate);
        totalRecommendedMinutes += topicTime;
      } else if (todayPlan.length === 0) {
        // If even the very first high-priority topic exceeds remainingMinutes (e.g. 15 min left for 25 min topic),
        // adjust the recommended session time to fit the exact remaining time today!
        todayPlan.push({
          ...candidate,
          recommendedMinutes: remainingMinutes,
          reason: `${candidate.reason} (Targeted ${remainingMinutes}m session to complete daily goal)`,
        });
        totalRecommendedMinutes += remainingMinutes;
        break;
      }
    }
  }

  const otherUnfinishedCount = Math.max(0, unfinishedCandidates.length - todayPlan.length);

  return {
    dailyTargetMinutes,
    todayActiveMinutes: activeMinutes,
    todayActiveSeconds: activeSeconds,
    todayBreakSeconds: breakSeconds,
    todayPauseSeconds: pauseSeconds,
    remainingMinutes,
    isTargetAchieved,
    isAllCaughtUp,
    todayPlan,
    totalRecommendedMinutes,
    otherUnfinishedCount,
    reviewTopics: masteredCandidates,
  };
}
