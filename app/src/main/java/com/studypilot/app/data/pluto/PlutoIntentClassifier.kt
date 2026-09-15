package com.studypilot.app.data.pluto

import com.studypilot.app.data.model.PlutoAttachment
import com.studypilot.app.data.model.PlutoIntent
import java.util.Locale

object PlutoIntentClassifier {

    fun classify(
        query: String,
        attachment: PlutoAttachment? = null
    ): PlutoIntent {
        val clean = query.trim().lowercase(Locale.ROOT)

        // 1. Explicit Quiz request
        if (clean.contains("quiz me") || clean.contains("test me") ||
            clean.contains("quiz from this") || clean.contains("create quiz") ||
            clean.contains("generate quiz") || clean.contains("practice test") ||
            (attachment != null && clean.contains("quiz"))
        ) {
            return PlutoIntent.QUIZ_FROM_ATTACHMENT
        }

        // 2. Attachment-specific questions (ONLY if query specifically refers to attachment)
        if (attachment != null) {
            val refersToAttachment = clean.contains("this image") ||
                    clean.contains("in this photo") ||
                    clean.contains("this diagram") ||
                    clean.contains("this picture") ||
                    clean.contains("this file") ||
                    clean.contains("this document") ||
                    clean.contains("this pdf") ||
                    clean.contains("in the document") ||
                    clean.contains("summarize this") ||
                    clean.contains("read this") ||
                    clean.contains("explain this page") ||
                    clean.contains("what is written here") ||
                    clean == "explain this" ||
                    clean == "what is this"

            if (refersToAttachment) {
                return if (attachment.isImage) PlutoIntent.IMAGE_QUESTION else PlutoIntent.FILE_QUESTION
            }
        }

        // 3. Casual conversation
        val casualTriggers = listOf(
            "hello", "hi", "hey", "how are you", "who are you", "good morning",
            "good afternoon", "good evening", "what's up", "whats up",
            "thank you", "thanks", "bye", "goodbye", "see you", "nice to meet you",
            "what is your name", "tell me a joke", "how is it going"
        )
        if (casualTriggers.any { clean == it || clean.startsWith("$it ") || clean.endsWith(" $it") }) {
            return PlutoIntent.CASUAL_CONVERSATION
        }

        // 4. Motivation
        val motivationTriggers = listOf(
            "motivate", "motivation", "tired", "stressed", "procrastinating",
            "can't focus", "cant focus", "give up", "hard to study", "cheer me up",
            "inspire", "lazy", "overwhelmed", "no energy", "bored"
        )
        if (motivationTriggers.any { clean.contains(it) }) {
            return PlutoIntent.MOTIVATION
        }

        // 5. StudyPilot app help
        val appHelpTriggers = listOf(
            "studypilot", "focus guardian", "guardian", "study timer", "how does test work",
            "evidence photo", "pass mark", "70%", "how to add subject", "adaptive planner",
            "reveille", "absence detection", "app lock", "notification"
        )
        if (appHelpTriggers.any { clean.contains(it) }) {
            return PlutoIntent.STUDYPILOT_APP_HELP
        }

        // 6. Study recommendation
        val recommendationTriggers = listOf(
            "what should i study", "recommend", "study plan", "schedule",
            "how long should i study", "pomodoro", "best way to learn", "routine"
        )
        if (recommendationTriggers.any { clean.contains(it) }) {
            return PlutoIntent.STUDY_RECOMMENDATION
        }

        // 7. Concept explanation
        val conceptTriggers = listOf(
            "explain", "why does", "how does", "derive", "mechanism of",
            "difference between", "define", "concept of", "proof of", "intuition behind"
        )
        if (conceptTriggers.any { clean.startsWith(it) || clean.contains(" $it ") }) {
            return PlutoIntent.CONCEPT_EXPLANATION
        }

        // 8. Study questions (Academic subjects & topics)
        val studyKeywords = listOf(
            "newton", "force", "gravity", "velocity", "acceleration", "derivative", "integral",
            "calculus", "algebra", "geometry", "trigonometry", "photosynthesis", "cell", "dna",
            "rna", "mitochondria", "atom", "molecule", "reaction", "periodic table", "acid",
            "base", "thermodynamics", "ohm's law", "resistor", "voltage", "current", "magnetism",
            "history", "revolution", "constitution", "geography", "latitude", "longitude",
            "solve", "calculate", "theorem", "equation", "formula"
        )
        if (studyKeywords.any { clean.contains(it) }) {
            return PlutoIntent.STUDY_QUESTION
        }

        // 9. General Question
        if (clean.startsWith("what is") || clean.startsWith("who is") ||
            clean.startsWith("where is") || clean.startsWith("when was") ||
            clean.startsWith("which is") || clean.startsWith("why is")
        ) {
            return PlutoIntent.GENERAL_QUESTION
        }

        return PlutoIntent.UNKNOWN
    }
}
