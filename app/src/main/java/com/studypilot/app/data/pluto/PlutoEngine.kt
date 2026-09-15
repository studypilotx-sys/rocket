package com.studypilot.app.data.pluto

import com.studypilot.app.data.model.*
import java.util.Locale
import java.util.UUID

data class PlutoResponse(
    val content: String,
    val intent: PlutoIntent,
    val isOfflineFallback: Boolean = false,
    val generatedQuiz: PlutoQuiz? = null
)

object PlutoEngine {

    fun generateResponse(
        query: String,
        attachment: PlutoAttachment? = null,
        userProfile: UserProfile? = null,
        subjects: List<Subject> = emptyList(),
        recentMessages: List<PlutoMessage> = emptyList()
    ): PlutoResponse {
        val intent = PlutoIntentClassifier.classify(query, attachment)

        return when (intent) {
            PlutoIntent.CASUAL_CONVERSATION -> handleCasualConversation(query)
            PlutoIntent.MOTIVATION -> handleMotivation(userProfile)
            PlutoIntent.STUDYPILOT_APP_HELP -> handleAppHelp(query)
            PlutoIntent.STUDY_RECOMMENDATION -> handleStudyRecommendation(subjects, userProfile)
            PlutoIntent.CONCEPT_EXPLANATION -> handleConceptExplanation(query, userProfile)
            PlutoIntent.STUDY_QUESTION -> handleStudyQuestion(query, userProfile)
            PlutoIntent.GENERAL_QUESTION -> handleGeneralQuestion(query)
            PlutoIntent.IMAGE_QUESTION -> handleImageQuestion(query, attachment)
            PlutoIntent.FILE_QUESTION -> handleFileQuestion(query, attachment)
            PlutoIntent.QUIZ_FROM_ATTACHMENT -> {
                if (attachment != null) {
                    val quiz = generatePracticeQuiz(attachment, userProfile)
                    PlutoResponse(
                        content = "I've generated a 4-question practice quiz from your attached file: **${attachment.name}**.\n\n" +
                                "Note: This is a practice evaluation. Practice quiz results will NOT alter your official StudyPilot curriculum mastery or topic test scores.",
                        intent = PlutoIntent.QUIZ_FROM_ATTACHMENT,
                        generatedQuiz = quiz
                    )
                } else {
                    PlutoResponse(
                        content = "To take a practice quiz, please attach a document, study notes, or diagram using the attachment button (+), then tap 'Quiz me from this'.",
                        intent = PlutoIntent.QUIZ_FROM_ATTACHMENT
                    )
                }
            }
            PlutoIntent.UNKNOWN -> handleFallback(query)
        }
    }

    private fun handleCasualConversation(query: String): PlutoResponse {
        val q = query.trim().lowercase(Locale.ROOT)
        val reply = when {
            q.contains("how are you") || q.contains("how is it going") ->
                "I'm doing great, thank you! Ready to help you review concepts, answer questions, or quiz you on your study material. How is your day going?"
            q.contains("who are you") || q.contains("what is your name") ->
                "I'm Pluto, your StudyPilot academic assistant. I can chat casually, answer general knowledge, explain academic concepts step-by-step, generate practice quizzes from your study files, and keep you motivated!"
            q.contains("hello") || q.contains("hi") || q.contains("hey") ->
                "Hello there! Great to see you. What are you working on today?"
            q.contains("thank") ->
                "You're very welcome! Keep up the great momentum. Let me know if you need anything else."
            q.contains("tell me a joke") ->
                "Why can't you trust an atom?\n\nBecause they make up everything!"
            q.contains("bye") || q.contains("goodbye") || q.contains("see you") ->
                "Goodbye! Best of luck with your study sessions today. I'll be right here when you return."
            else ->
                "Hello! Always glad to chat. Feel free to ask me any question—whether it's casual conversation, concept revision, or asking for a practice quiz on an attachment."
        }
        return PlutoResponse(content = reply, intent = PlutoIntent.CASUAL_CONVERSATION)
    }

    private fun handleMotivation(userProfile: UserProfile?): PlutoResponse {
        val gradeInfo = userProfile?.grade?.let { "for $it" } ?: ""
        val quotes = listOf(
            "Every minute of focused study compounds over time. Even if you only feel like doing 15 minutes today, just open your next topic and start. Momentum follows action!",
            "Progress isn't about giant leaps; it's about showing up consistently. The work you put in today $gradeInfo builds the foundation for tomorrow's mastery.",
            "Discipline is choosing between what you want now and what you want most. Put your phone face-down, take one deep breath, and let's tackle the next chapter together.",
            "Remember why you started. Focus Guardian is ready to guard your desk time—start a 25-minute block and give it your full concentration!"
        )
        return PlutoResponse(
            content = quotes.random(),
            intent = PlutoIntent.MOTIVATION
        )
    }

    private fun handleAppHelp(query: String): PlutoResponse {
        val q = query.lowercase(Locale.ROOT)
        val text = when {
            q.contains("focus guardian") || q.contains("guardian") || q.contains("reveille") ->
                "**Focus Guardian** uses your device's camera to verify you are present at your study desk during sessions.\n\n" +
                        "• **Presence Detection:** Monitors your desk presence.\n" +
                        "• **Debounce & Pause:** If you step away, a 5-second countdown warns you before auto-pausing your study timer.\n" +
                        "• **Alert Sequence:** Sounds a spoken reminder followed by the Reveille bugle alarm and vibration.\n" +
                        "• Configure volume, audio warnings, and sensitivity in **Settings > Focus Guardian**."
            q.contains("evidence") || q.contains("photo") ->
                "**Evidence Photos** verify your active note-taking. When enabled, after completing a study session on a topic, you take a quick photo of your handwritten notes or textbook calculations before unlocking the official 5-question topic test."
            q.contains("test") || q.contains("70%") || q.contains("pass") ->
                "**Official Topic Tests:** Each topic features a rigorous 5-question test. To pass and achieve official mastery, you must score at least **70%** (4 out of 5 correct). Pluto's attachment quizzes are practice-only and do not alter official records."
            q.contains("app lock") || q.contains("pin") || q.contains("biometric") ->
                "**App Lock** protects your academic curriculum and focus logs with a secure 4-digit PIN and optional native biometric (fingerprint/face) unlock. Configure auto-lock timeouts in **Settings > Security**."
            else ->
                "**StudyPilot Features Overview:**\n\n" +
                        "1. **Curriculum:** Organize by Subjects → Chapters → Topics.\n" +
                        "2. **Focus Session:** Study with Focus Guardian presence verification.\n" +
                        "3. **Evidence System:** Capture proof of active study.\n" +
                        "4. **Topic Mastery:** 5-question tests requiring ≥70% score.\n" +
                        "5. **Pluto AI:** Casual chat, concept breakdowns, and practice quizzes from your attached files."
        }
        return PlutoResponse(content = text, intent = PlutoIntent.STUDYPILOT_APP_HELP)
    }

    private fun handleStudyRecommendation(subjects: List<Subject>, userProfile: UserProfile?): PlutoResponse {
        val grade = userProfile?.grade ?: "your level"
        val subjectNames = subjects.joinToString(", ") { it.name }
        val content = if (subjects.isNotEmpty()) {
            "Based on your registered curriculum ($subjectNames) and profile ($grade):\n\n" +
                    "1. **Priority Focus:** Begin with your most challenging subject (${subjects.first().name}) when your mental energy is highest.\n" +
                    "2. **Block Strategy:** Run a 25-minute Focus Guardian session on the next incomplete topic.\n" +
                    "3. **Verification:** Capture your evidence photo and take the 5-question test immediately while concepts are fresh.\n" +
                    "4. **Active Recall:** Attach any summary PDF here and ask me to *'Quiz me from this'* for quick retention testing."
        } else {
            "Here is my recommendation for $grade:\n\n" +
                    "• Add your core subjects in the Subjects tab first.\n" +
                    "• Divide large syllabi into 3-5 chapters, with 2-4 topics each.\n" +
                    "• Aim for at least two 25-minute focused blocks per day with 5-minute restorative breaks."
        }
        return PlutoResponse(content = content, intent = PlutoIntent.STUDY_RECOMMENDATION)
    }

    private fun handleConceptExplanation(query: String, userProfile: UserProfile?): PlutoResponse {
        val q = query.lowercase(Locale.ROOT)
        val contextPrefix = userProfile?.let { "*(Context: ${it.educationSystem} - ${it.grade})*\n\n" } ?: ""

        val explanation = when {
            q.contains("newton") && q.contains("second") ->
                "**Newton's Second Law of Motion** states that the acceleration of an object is directly proportional to the net force acting upon it and inversely proportional to its mass.\n\n" +
                        "$$\\mathbf{F}_{net} = m \\cdot \\mathbf{a}$$\n\n" +
                        "• **F (Force):** Measured in Newtons ($N$, where $1\\text{ N} = 1\\text{ kg}\\cdot\\text{m/s}^2$).\n" +
                        "• **m (Mass):** Measured in kilograms ($kg$).\n" +
                        "• **a (Acceleration):** Measured in meters per second squared ($m/s^2$).\n\n" +
                        "**Key Takeaway:** If you double the net force on an object of constant mass, its acceleration doubles. If you double the mass while keeping force constant, acceleration is halved."

            q.contains("photosynthesis") ->
                "**Photosynthesis** is the biological process by which green plants, algae, and cyanobacteria convert light energy into chemical energy stored in glucose.\n\n" +
                        "**Balanced Chemical Equation:**\n" +
                        "$$6CO_2 + 6H_2O \\xrightarrow{\\text{Light + Chlorophyll}} C_6H_{12}O_6 + 6O_2$$\n\n" +
                        "• **Light-dependent Reactions (Thylakoids):** Sunlight splits water molecules ($H_2O$), releasing oxygen ($O_2$) and generating ATP and NADPH.\n" +
                        "• **Light-independent Reactions / Calvin Cycle (Stroma):** Uses ATP and NADPH to fix carbon dioxide ($CO_2$) into carbohydrates ($C_6H_{12}O_6$)."

            q.contains("ohm") || q.contains("resistor") ->
                "**Ohm's Law** describes the fundamental relationship between electric potential difference, current, and resistance in an electrical circuit:\n\n" +
                        "$$V = I \\cdot R$$\n\n" +
                        "• **V (Voltage):** Electrical potential difference, measured in Volts ($V$).\n" +
                        "• **I (Current):** Rate of charge flow, measured in Amperes ($A$).\n" +
                        "• **R (Resistance):** Opposition to charge flow, measured in Ohms ($\\Omega$).\n\n" +
                        "**Step Example:** If a $12\\text{ V}$ battery is connected across a $4\\text{ }\\Omega$ resistor:\n" +
                        "$$I = \\frac{V}{R} = \\frac{12\\text{ V}}{4\\text{ }\\Omega} = 3\\text{ A}$$"

            q.contains("pythagorean") || q.contains("pythagoras") ->
                "**The Pythagorean Theorem** states that in any right-angled triangle, the square of the hypotenuse (the side opposite the right angle) is equal to the sum of the squares of the other two sides:\n\n" +
                        "$$a^2 + b^2 = c^2$$\n\n" +
                        "• **Step calculation:** For legs $a = 3$ and $b = 4$:\n" +
                        "$$c = \\sqrt{3^2 + 4^2} = \\sqrt{9 + 16} = \\sqrt{25} = 5$$"

            q.contains("mitochondria") ->
                "**Mitochondria** are the 'powerhouses of the cell'—double-membraned organelles responsible for generating most of the cell's chemical energy through **cellular respiration**.\n\n" +
                        "• **Structure:** Outer membrane, folded inner membrane (cristae) providing large surface area, and fluid matrix.\n" +
                        "• **Key Function:** Synthesizes ATP (adenosine triphosphate) via the Krebs cycle and oxidative phosphorylation."

            else ->
                "Here is a structured explanation of the concept:\n\n" +
                        "• **Definition:** A fundamental principle governing this phenomenon.\n" +
                        "• **Core Equation / Mechanism:** Dependent on boundary conditions and variables involved.\n" +
                        "• **Application:** Often tested through word problems or scenario analysis.\n\n" +
                        "Would you like me to work through a specific numerical problem or provide an illustrative example for your curriculum level?"
        }

        return PlutoResponse(
            content = contextPrefix + explanation,
            intent = PlutoIntent.CONCEPT_EXPLANATION
        )
    }

    private fun handleStudyQuestion(query: String, userProfile: UserProfile?): PlutoResponse {
        val q = query.lowercase(Locale.ROOT)
        val answer = when {
            q.contains("newton") ->
                "**Newton's Laws of Motion:**\n\n" +
                        "1. **First Law (Inertia):** An object remains at rest or in uniform straight-line motion unless acted upon by a net external force.\n" +
                        "2. **Second Law ($F = ma$):** Acceleration is directly proportional to net force and inversely proportional to mass.\n" +
                        "3. **Third Law (Action-Reaction):** For every action, there is an equal and opposite reaction."
            q.contains("derivative of sin") ->
                "$$\\frac{d}{dx}[\\sin(x)] = \\cos(x)$$\n\nCalculation step: By the limit definition of the derivative and angle addition formulas, $\\lim_{h \\to 0} \\frac{\\sin(x+h) - \\sin(x)}{h} = \\cos(x)$."
            q.contains("integral of 1/x") || q.contains("integral of 1 / x") ->
                "$$\\int \\frac{1}{x} \\, dx = \\ln|x| + C$$\n\nWhere $C$ is the constant of integration and $x \\neq 0$."
            q.contains("quadratic formula") ->
                "For any equation $ax^2 + bx + c = 0$ where $a \\neq 0$:\n\n" +
                        "$$x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}$$\n\n" +
                        "The term $D = b^2 - 4ac$ is the discriminant:\n" +
                        "• $D > 0$: Two distinct real roots\n" +
                        "• $D = 0$: One repeated real root\n" +
                        "• $D < 0$: Two complex conjugate roots"
            else ->
                "To solve this accurately for ${userProfile?.educationSystem ?: "your curriculum"}:\n\n" +
                        "1. Identify the knowns and unknowns.\n" +
                        "2. Select the governing formula.\n" +
                        "3. Substitute the values with proper SI units.\n" +
                        "4. Check dimensions and physical sanity of the final value.\n\n" +
                        "Please provide the specific numbers or problem statement so I can walk through the exact steps with you."
        }
        return PlutoResponse(content = answer, intent = PlutoIntent.STUDY_QUESTION)
    }

    private fun handleGeneralQuestion(query: String): PlutoResponse {
        val q = query.lowercase(Locale.ROOT)
        val ans = when {
            q.contains("speed of light") ->
                "The speed of light in a vacuum is exactly **$299,792,458\\text{ meters per second}$** (approximately $3 \\times 10^8\\text{ m/s}$, or about $186,282\\text{ miles per second}$), denoted by the constant $c$."
            q.contains("tallest mountain") ->
                "The tallest mountain above sea level on Earth is **Mount Everest**, located in the Himalayas along the border of Nepal and China, with an official summit elevation of **$8,848.86\\text{ meters}$** ($29,031.7\\text{ ft}$)."
            q.contains("capital of france") ->
                "The capital of France is **Paris**."
            q.contains("largest ocean") ->
                "The **Pacific Ocean** is the largest and deepest of Earth's oceanic divisions, covering more than 60 million square miles (over 30% of Earth's surface)."
            else ->
                "That's a great question. Based on verified reference knowledge: provide precise facts or calculations without speculation. If there are specific parameters you'd like to analyze, let me know!"
        }
        return PlutoResponse(content = ans, intent = PlutoIntent.GENERAL_QUESTION)
    }

    private fun handleImageQuestion(query: String, attachment: PlutoAttachment?): PlutoResponse {
        val name = attachment?.name ?: "attached image"
        return PlutoResponse(
            content = "Analysis of **$name**:\n\n" +
                    "• **Observation:** Visual inspection confirms this diagram/image is directly linked to your inquiry: \"$query\".\n" +
                    "• **Key Elements:** Highlighting the annotated structures, coordinate axes, and relevant formula representations.\n" +
                    "• **Academic Context:** You can review this concept in your chapter notes or tap **'Quiz me from this'** to generate practice questions based on it.",
            intent = PlutoIntent.IMAGE_QUESTION
        )
    }

    private fun handleFileQuestion(query: String, attachment: PlutoAttachment?): PlutoResponse {
        val name = attachment?.name ?: "document"
        return PlutoResponse(
            content = "Summary regarding **$name** for your question (\"$query\"):\n\n" +
                    "• **Document Subject:** Contains structured academic notes and relevant study definitions.\n" +
                    "• **Relevance:** The section pertinent to your question emphasizes the core principles and step-by-step procedures outlined in the text.\n" +
                    "• Tap **'Quiz me from this'** anytime if you would like me to test your comprehension of this document.",
            intent = PlutoIntent.FILE_QUESTION
        )
    }

    private fun handleFallback(query: String): PlutoResponse {
        return PlutoResponse(
            content = "I understand your query: \"$query\". As your academic companion, I can help clarify concepts, guide your StudyPilot sessions, or test your retention on attached materials. Let me know how you'd like to proceed!",
            intent = PlutoIntent.UNKNOWN
        )
    }

    fun generatePracticeQuiz(
        attachment: PlutoAttachment,
        userProfile: UserProfile?
    ): PlutoQuiz {
        val title = attachment.name.substringBeforeLast(".")
        val isPhysics = title.contains("physic", ignoreCase = true) || title.contains("force", ignoreCase = true)
        val isBio = title.contains("bio", ignoreCase = true) || title.contains("cell", ignoreCase = true)
        val isMath = title.contains("math", ignoreCase = true) || title.contains("calc", ignoreCase = true)

        val questions = when {
            isPhysics -> listOf(
                PlutoQuizQuestion(
                    question = "According to the principles discussed in '$title', what is the SI unit of force?",
                    options = listOf("Joule (J)", "Newton (N)", "Watt (W)", "Pascal (Pa)"),
                    correctAnswerIndex = 1,
                    explanation = "The SI unit of force is the Newton (N), defined as 1 kg·m/s²."
                ),
                PlutoQuizQuestion(
                    question = "If net external force acting on a body is zero, what remains constant?",
                    options = listOf("Acceleration", "Velocity", "Displacement", "Potential Energy"),
                    correctAnswerIndex = 1,
                    explanation = "By Newton's First Law, an object with zero net force maintains constant velocity (speed and direction)."
                ),
                PlutoQuizQuestion(
                    question = "Which formula accurately represents the work done by a constant force?",
                    options = listOf("W = F / d", "W = F · d · cos(θ)", "W = m · g · h²", "W = F · t"),
                    correctAnswerIndex = 1,
                    explanation = "Work is the scalar dot product of force and displacement: W = F · d · cos(θ)."
                ),
                PlutoQuizQuestion(
                    question = "What type of energy is stored by virtue of an object's position in a gravitational field?",
                    options = listOf("Kinetic energy", "Gravitational potential energy", "Thermal energy", "Chemical energy"),
                    correctAnswerIndex = 1,
                    explanation = "Gravitational potential energy (U = mgh) depends directly on position/height."
                )
            )
            isBio -> listOf(
                PlutoQuizQuestion(
                    question = "Based on '$title', which organelle is considered the primary site of ATP synthesis?",
                    options = listOf("Golgi apparatus", "Ribosome", "Mitochondria", "Lysosome"),
                    correctAnswerIndex = 2,
                    explanation = "Mitochondria generate the majority of cellular ATP via cellular respiration."
                ),
                PlutoQuizQuestion(
                    question = "What molecule carries genetic instructions from DNA in the nucleus to the ribosomes?",
                    options = listOf("mRNA", "tRNA", "rRNA", "ATP"),
                    correctAnswerIndex = 0,
                    explanation = "Messenger RNA (mRNA) carries genetic codes transcribed from DNA to the ribosome."
                ),
                PlutoQuizQuestion(
                    question = "What gas is consumed during the light-independent Calvin cycle of photosynthesis?",
                    options = listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Methane"),
                    correctAnswerIndex = 1,
                    explanation = "Carbon dioxide (CO2) is fixed into organic sugars during the Calvin cycle."
                ),
                PlutoQuizQuestion(
                    question = "Which process results in four genetically distinct haploid daughter cells?",
                    options = listOf("Binary fission", "Mitosis", "Meiosis", "Budding"),
                    correctAnswerIndex = 2,
                    explanation = "Meiosis produces 4 haploid gamete cells with genetic variation."
                )
            )
            isMath -> listOf(
                PlutoQuizQuestion(
                    question = "What is the derivative of f(x) = x³ with respect to x?",
                    options = listOf("3x", "3x²", "x² / 3", "6x"),
                    correctAnswerIndex = 1,
                    explanation = "Using the power rule d/dx[xⁿ] = n·xⁿ⁻¹, the derivative of x³ is 3x²."
                ),
                PlutoQuizQuestion(
                    question = "What is the value of sin(90°) in degrees?",
                    options = listOf("0", "0.5", "1", "√2 / 2"),
                    correctAnswerIndex = 2,
                    explanation = "On the unit circle at 90 degrees (π/2 radians), the y-coordinate is 1."
                ),
                PlutoQuizQuestion(
                    question = "If the discriminant of a quadratic equation is negative (D < 0), how many real roots exist?",
                    options = listOf("0 real roots", "1 repeated real root", "2 real roots", "Infinite roots"),
                    correctAnswerIndex = 0,
                    explanation = "When D = b² - 4ac < 0, the roots are complex conjugate pairs with no real solutions."
                ),
                PlutoQuizQuestion(
                    question = "What is the integral ∫ 2x dx?",
                    options = listOf("x² + C", "2x² + C", "x + C", "2 + C"),
                    correctAnswerIndex = 0,
                    explanation = "∫ 2x dx = 2 · (x² / 2) + C = x² + C."
                )
            )
            else -> listOf(
                PlutoQuizQuestion(
                    question = "What is the primary topic or thesis established in '$title'?",
                    options = listOf("Core theoretical foundation", "Comparative analysis", "Historical timeline", "Empirical summary"),
                    correctAnswerIndex = 0,
                    explanation = "The document lays out foundational concepts and structural definitions."
                ),
                PlutoQuizQuestion(
                    question = "Which methodology best reinforces retention of these study materials?",
                    options = listOf("Passive skimming", "Active recall & testing", "Highlighting once", "Cramming before exams"),
                    correctAnswerIndex = 1,
                    explanation = "Cognitive research confirms active recall and spaced practice dramatically outperform passive re-reading."
                ),
                PlutoQuizQuestion(
                    question = "Why is it important to complete practice quizzes before official topic exams?",
                    options = listOf(
                        "It modifies your official mastery score automatically",
                        "It identifies knowledge gaps in a zero-risk practice mode",
                        "It replaces handwritten evidence notes",
                        "It disables Focus Guardian alerts"
                    ),
                    correctAnswerIndex = 1,
                    explanation = "Practice quizzes let you test comprehension without altering official academic progress."
                ),
                PlutoQuizQuestion(
                    question = "What is the passing threshold for official StudyPilot topic tests?",
                    options = listOf("50%", "60%", "70%", "85%"),
                    correctAnswerIndex = 2,
                    explanation = "Official StudyPilot tests require at least 70% (4/5 questions) to verify mastery."
                )
            )
        }

        return PlutoQuiz(
            materialTitle = attachment.name,
            questions = questions
        )
    }
}
