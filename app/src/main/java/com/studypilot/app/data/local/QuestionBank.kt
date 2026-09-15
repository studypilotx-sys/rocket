package com.studypilot.app.data.local

import com.studypilot.app.data.model.TestQuestion
import java.util.UUID

object QuestionBank {

    fun getQuestionsForTopic(topicId: String, topicName: String): List<TestQuestion> {
        val trimmed = topicName.trim().lowercase()

        val specificQuestions = when {
            // Physics: Rectilinear Motion & Vectors
            trimmed.contains("vector") || trimmed.contains("rectilinear") -> listOf(
                QuestionDraft(
                    "Which of the following physical quantities is a vector?",
                    "Mass", "Velocity", "Temperature", "Speed",
                    1, "Velocity possesses both magnitude and a defined spatial direction, qualifying it as a vector quantity."
                ),
                QuestionDraft(
                    "If two vectors of magnitudes 3 N and 4 N act at a right angle (90°), what is the magnitude of their resultant?",
                    "7 N", "1 N", "5 N", "12 N",
                    2, "By the Pythagorean theorem: R = sqrt(3^2 + 4^2) = sqrt(9 + 16) = 5 N."
                ),
                QuestionDraft(
                    "In uniform rectilinear motion, what is the acceleration of the object?",
                    "Zero", "Constant positive value", "Equal to gravitational acceleration (g)", "Continuously variable",
                    0, "Uniform rectilinear motion implies constant velocity in a straight line, meaning acceleration (dv/dt) is strictly zero."
                ),
                QuestionDraft(
                    "What does the area under a velocity-time graph represent for an object in linear motion?",
                    "Total acceleration", "Net force applied", "Instantaneous power", "Displacement",
                    3, "The definite integral of velocity with respect to time gives the displacement: s = integral(v dt)."
                ),
                QuestionDraft(
                    "Can a particle have zero displacement after moving a total distance of 100 meters?",
                    "No, displacement must equal distance", "Yes, if it returns to its starting location", "Only if time is negative", "No, distance cannot exceed displacement",
                    1, "Displacement is the vector connecting the initial to final position; if the particle returns to its start, displacement is zero."
                )
            )

            // Physics: Speed, Velocity & Acceleration
            trimmed.contains("speed") || trimmed.contains("acceleration") -> listOf(
                QuestionDraft(
                    "How is instantaneous acceleration mathematically defined?",
                    "Rate of change of displacement with time", "Rate of change of velocity with time (dv/dt)", "Product of velocity and mass", "Distance divided by elapsed time",
                    1, "Instantaneous acceleration is the first time derivative of velocity: a = dv/dt."
                ),
                QuestionDraft(
                    "A car accelerates uniformly from rest to 20 m/s in 5 seconds. What is its acceleration?",
                    "2 m/s²", "4 m/s²", "10 m/s²", "100 m/s²",
                    1, "Using a = (v - u) / t: a = (20 - 0) / 5 = 4 m/s²."
                ),
                QuestionDraft(
                    "Can an object have zero velocity while experiencing non-zero acceleration?",
                    "No, impossible", "Yes, at the peak height of a vertically projected object", "Only in circular motion", "Only when moving in negative direction",
                    1, "At the zenith of vertical projectile motion, instantaneous velocity is zero while gravitational acceleration g remains -9.8 m/s²."
                ),
                QuestionDraft(
                    "What is the SI unit of acceleration?",
                    "m/s", "m·s", "m/s²", "kg·m/s²",
                    2, "Acceleration measures the change in velocity (m/s) per second (s), giving m/s²."
                ),
                QuestionDraft(
                    "Under constant acceleration a with initial velocity u, what is the kinematic relation for displacement s?",
                    "s = u·t + 0.5·a·t²", "s = u·t + a·t", "s = 2·a·t²", "s = (u + a) / t",
                    0, "The standard kinematic equation for displacement under uniform acceleration is s = u*t + (1/2)*a*t^2."
                )
            )

            // Physics: Newton's Laws
            trimmed.contains("newton") || trimmed.contains("laws of motion") -> listOf(
                QuestionDraft(
                    "According to Newton's First Law, what happens to a body in uniform motion when net external force is zero?",
                    "It abruptly halts", "It continues in uniform motion in a straight line", "It accelerates in the direction of momentum", "Its mass increases",
                    1, "Newton's First Law states an object remains at rest or in uniform straight-line motion unless acted upon by a net external force."
                ),
                QuestionDraft(
                    "Newton's Second Law defines force as which of the following?",
                    "Rate of change of momentum (dp/dt)", "Product of velocity and distance", "Mass times displacement", "Ratio of energy to time",
                    0, "F = dp/dt. When mass is constant, this simplifies to F = m*a."
                ),
                QuestionDraft(
                    "What is the net force required to accelerate a 5 kg mass at 3 m/s²?",
                    "15 N", "0.6 N", "1.67 N", "8 N",
                    0, "F = m * a = 5 kg * 3 m/s² = 15 N."
                ),
                QuestionDraft(
                    "Newton's Third Law states that action and reaction forces:",
                    "Act on the same object and cancel each other", "Act on different objects with equal magnitude and opposite directions", "Occur at different points in time", "Are only present during acceleration",
                    1, "Action-reaction pairs act on two separate interacting bodies, which is why they do not cancel into zero force."
                ),
                QuestionDraft(
                    "What property of an object quantifies its inertia in linear motion?",
                    "Volume", "Inertial Mass", "Weight", "Density",
                    1, "Mass is the intrinsic quantitative measure of an object's inertia (resistance to changes in motion)."
                )
            )

            // Physics: Work, Energy & Power
            trimmed.contains("work") || trimmed.contains("energy") || trimmed.contains("power") -> listOf(
                QuestionDraft(
                    "What is the SI unit of work and energy?",
                    "Watt", "Joule", "Pascal", "Newton",
                    1, "The Joule (J) is equivalent to one Newton-meter (N·m) and is the standard SI unit of energy and work."
                ),
                QuestionDraft(
                    "According to the Work-Energy Theorem, net work done on a particle equals:",
                    "Change in potential energy", "Change in kinetic energy", "Total power transferred", "Initial momentum",
                    1, "W_net = Delta K = K_final - K_initial."
                ),
                QuestionDraft(
                    "What is the kinetic energy of a 2 kg body traveling at 10 m/s?",
                    "20 J", "200 J", "100 J", "50 J",
                    2, "K = 0.5 * m * v² = 0.5 * 2 * (10²) = 100 J."
                ),
                QuestionDraft(
                    "How is power mathematically defined in terms of work W and time t?",
                    "P = W * t", "P = dW / dt", "P = W² / t", "P = F * t",
                    1, "Power is the time rate at which work is performed: P = dW/dt."
                ),
                QuestionDraft(
                    "In an isolated system governed solely by conservative forces, what quantity remains conserved?",
                    "Total Mechanical Energy", "Kinetic energy only", "Potential energy only", "Acceleration",
                    0, "When only conservative forces act, the sum of kinetic and potential energy (Mechanical Energy) is strictly conserved."
                )
            )

            // Chemistry: Atomic Structure / Bohr / Quantum
            trimmed.contains("atomic") || trimmed.contains("bohr") || trimmed.contains("quantum") -> listOf(
                QuestionDraft(
                    "What does the principal quantum number (n) primarily specify?",
                    "Electron orbital shape", "Main energy shell and average orbital distance", "Spin direction", "Spatial orientation",
                    1, "The principal quantum number n represents the primary energy level or shell."
                ),
                QuestionDraft(
                    "According to Bohr's model of the hydrogen atom, electron angular momentum is quantized as:",
                    "m*v*r = n*h / (2*pi)", "m*v*r = n*h", "m*v*r = 2*pi*n / h", "m*v*r = h / (4*pi)",
                    0, "Bohr postulated L = mvr = nh / (2pi), where n is an integer."
                ),
                QuestionDraft(
                    "How many electrons can maximally occupy a single orbital?",
                    "1", "2", "6", "10",
                    1, "By the Pauli Exclusion Principle, an orbital can hold a maximum of 2 electrons with opposite spins (+1/2, -1/2)."
                ),
                QuestionDraft(
                    "What orbital shape corresponds to the azimuthal quantum number l = 1?",
                    "Spherical (s)", "Dumbbell (p)", "Double dumbbell (d)", "Complex (f)",
                    1, "l = 0 denotes s orbitals (spherical), and l = 1 denotes p orbitals (dumbbell-shaped)."
                ),
                QuestionDraft(
                    "According to Hund's Rule of Maximum Multiplicity:",
                    "Orbitals of equal energy are singly occupied before pairing occurs", "Electrons fill highest energy levels first", "No two electrons have identical quantum numbers", "Light behaves as a wave only",
                    0, "Electrons enter degenerate orbitals singly with parallel spins prior to electron pairing."
                )
            )

            // Chemistry: Bonding / VSEPR / Hybridization
            trimmed.contains("bond") || trimmed.contains("vsepr") || trimmed.contains("hybrid") -> listOf(
                QuestionDraft(
                    "What molecular geometry is predicted by VSEPR theory for methane (CH4)?",
                    "Trigonal planar", "Linear", "Tetrahedral", "Bent",
                    2, "CH4 has 4 bonding pairs and 0 lone pairs around carbon, giving an ideal tetrahedral geometry (109.5°)."
                ),
                QuestionDraft(
                    "What hybridization does the central carbon atom exhibit in ethene (C2H4)?",
                    "sp", "sp²", "sp³", "sp³d",
                    1, "Each carbon in C2H4 forms 3 sigma bonds and 1 pi bond, resulting in sp² hybridization with ~120° bond angles."
                ),
                QuestionDraft(
                    "What type of bond is formed by the lateral (side-by-side) overlap of atomic p-orbitals?",
                    "Sigma (sigma) bond", "Pi (pi) bond", "Ionic bond", "Hydrogen bond",
                    1, "End-to-end overlap produces sigma bonds, whereas lateral overlap forms pi bonds."
                ),
                QuestionDraft(
                    "Why is the bond angle in water (H2O) approximately 104.5° rather than 109.5°?",
                    "Electronegativity of hydrogen", "Lone pair - lone pair repulsion exceeds bond pair repulsion", "Carbon atom hybridization", "Absence of sigma bonds",
                    1, "In VSEPR theory, lone pairs occupy more space and exert stronger repulsive forces than bonding pairs, compressing the H-O-H angle."
                ),
                QuestionDraft(
                    "What distinguishes an ionic bond from a covalent bond?",
                    "Complete transfer of valence electrons vs shared electron pairs", "Covalent bonds only exist in metals", "Ionic bonds involve no charge difference", "Covalent bonds always dissolve in benzene",
                    0, "Ionic bonds involve electrostatic attraction between ions after electron transfer, whereas covalent bonds involve electron sharing."
                )
            )

            // Mathematics: Calculus / Derivatives / Integrals
            trimmed.contains("derivative") || trimmed.contains("calculus") || trimmed.contains("limit") -> listOf(
                QuestionDraft(
                    "What is the first derivative of f(x) = x³ with respect to x?",
                    "3x²", "3x", "x² / 3", "6x",
                    0, "Using the power rule d/dx(x^n) = n*x^(n-1), d/dx(x³) = 3x²."
                ),
                QuestionDraft(
                    "At a critical point where f'(x) = 0 and f''(x) > 0, what does f(x) possess?",
                    "Local Maximum", "Local Minimum", "Point of discontinuity", "Vertical asymptote",
                    1, "By the Second Derivative Test, a zero first derivative paired with a positive second derivative confirms a local minimum (concave up)."
                ),
                QuestionDraft(
                    "What is the derivative of sin(x) with respect to x?",
                    "-cos(x)", "cos(x)", "tan(x)", "-sin(x)",
                    1, "Standard trigonometric differentiation yields d/dx [sin(x)] = cos(x)."
                ),
                QuestionDraft(
                    "According to the Chain Rule, what is the derivative of f(g(x))?",
                    "f'(g(x)) * g'(x)", "f'(x) * g'(x)", "f'(g'(x))", "f(x) + g'(x)",
                    0, "The Chain Rule states d/dx [f(g(x))] = f'(g(x)) * g'(x)."
                ),
                QuestionDraft(
                    "What does the limit lim (x -> 0) of [sin(x) / x] evaluate to in radians?",
                    "0", "1", "Infinity", "Undefined",
                    1, "A foundational trigonometric limit proven geometrically or via L'Hopital's rule is lim (x->0) [sin(x)/x] = 1."
                )
            )

            // Mathematics: Integration
            trimmed.contains("integr") -> listOf(
                QuestionDraft(
                    "What is the indefinite integral of 2x with respect to x?",
                    "x² + C", "2x² + C", "x + C", "2 + C",
                    0, "integral(2x dx) = 2 * (x² / 2) + C = x² + C."
                ),
                QuestionDraft(
                    "The Fundamental Theorem of Calculus connects differentiation with:",
                    "Matrix transformations", "Definite integration as accumulation", "Vector cross products", "Permutations",
                    1, "It establishes that integration is the inverse operation of differentiation and evaluates definite integrals via antiderivatives."
                ),
                QuestionDraft(
                    "What is the integral of 1/x dx for x > 0?",
                    "x² + C", "ln(x) + C", "-1/x² + C", "e^x + C",
                    1, "By definition, the antiderivative of 1/x is natural logarithm ln|x| + C."
                ),
                QuestionDraft(
                    "What does the definite integral of f(x) >= 0 between a and b represent geometrically?",
                    "The slope at the midpoint", "The area between the curve f(x) and the x-axis from a to b", "The perimeter of the domain", "The maximum value of f(x)",
                    1, "Definite integration of a non-negative function yields the exact geometric area under the curve."
                ),
                QuestionDraft(
                    "What is the value of definite integral from 0 to 1 of (3x²) dx?",
                    "1", "3", "0.5", "6",
                    0, "Antiderivative of 3x² is x³. Evaluating from 0 to 1: 1³ - 0³ = 1."
                )
            )

            // Biology: Cell / Genetics / DNA
            trimmed.contains("cell") || trimmed.contains("dna") || trimmed.contains("genetic") -> listOf(
                QuestionDraft(
                    "Which cellular organelle is known as the site of aerobic ATP synthesis?",
                    "Endoplasmic reticulum", "Mitochondria", "Golgi apparatus", "Ribosome",
                    1, "Mitochondria generate cellular ATP via the citric acid cycle and oxidative phosphorylation."
                ),
                QuestionDraft(
                    "In DNA, which nitrogenous base pairs with Adenine through two hydrogen bonds?",
                    "Cytosine", "Thymine", "Guanine", "Uracil",
                    1, "Adenine pairs selectively with Thymine via 2 hydrogen bonds in DNA (Uracil replaces Thymine in RNA)."
                ),
                QuestionDraft(
                    "During which stage of mitosis do sister chromatids separate toward opposite spindle poles?",
                    "Prophase", "Metaphase", "Anaphase", "Telophase",
                    2, "Anaphase is characterized by the cleavage of cohesin and migration of sister chromatids to opposite poles."
                ),
                QuestionDraft(
                    "What is the phenotypic ratio in a standard Mendelian monohybrid cross of two heterozygous individuals (Aa x Aa)?",
                    "1:1", "3:1 dominant to recessive", "9:3:3:1", "1:2:1",
                    1, "A standard monohybrid cross gives genotypes 1 AA : 2 Aa : 1 aa, presenting a 3:1 dominant to recessive phenotype ratio."
                ),
                QuestionDraft(
                    "What enzyme is primarily responsible for synthesizing new DNA strands during replication?",
                    "DNA Polymerase", "RNA Primase", "Amylase", "Lipase",
                    0, "DNA Polymerase synthesizes complementary DNA strands by adding deoxynucleotides in the 5' to 3' direction."
                )
            )

            else -> null
        }

        val drafts = specificQuestions ?: generateContextualQuestions(topicName)

        return drafts.mapIndexed { index, draft ->
            TestQuestion(
                id = UUID.randomUUID().toString(),
                topicId = topicId,
                questionNumber = index + 1,
                questionText = draft.question,
                optionA = draft.optionA,
                optionB = draft.optionB,
                optionC = draft.optionC,
                optionD = draft.optionD,
                correctOptionIndex = draft.correctOptionIndex,
                explanation = draft.explanation
            )
        }
    }

    private fun generateContextualQuestions(topicName: String): List<QuestionDraft> {
        val cleanName = topicName.trim()
        return listOf(
            QuestionDraft(
                "What is the primary governing principle of '$cleanName'?",
                "Empirical observation and fundamental physical/theoretical conservation laws",
                "Arbitrary historical convention with no testable predictive power",
                "Random fluctuations without mathematical definition",
                "Isolated qualitative descriptions without real-world utility",
                0,
                "Study of $cleanName rests on rigorously established conservation laws and empirical scientific methodology."
            ),
            QuestionDraft(
                "In the context of '$cleanName', how are key dependent variables systematically determined?",
                "By measuring initial boundary conditions and evaluating governing equations",
                "By guessing values without reference to initial states",
                "By assuming all variables remain completely invariant in all scenarios",
                "By ignoring units of measurement and dimensional consistency",
                0,
                "Analyzing $cleanName requires establishing precise boundary conditions and solving the corresponding analytical relationships."
            ),
            QuestionDraft(
                "Which analytical condition is essential when validating problems involving '$cleanName'?",
                "Ensuring dimensional homogeneity across all terms in the formulation",
                "Assuming friction or resistance is always infinite",
                "Excluding numerical constants from all calculations",
                "Disregarding the conservation of mass and energy",
                0,
                "Dimensional homogeneity is a critical constraint verifying that equations representing $cleanName are mathematically and physically sound."
            ),
            QuestionDraft(
                "When evaluating experiments or systems regarding '$cleanName', what role does control verification play?",
                "Isolating variables to identify precise cause-and-effect relationships",
                "Increasing random external noise in the data collection process",
                "Preventing repetition of empirical measurements",
                "Eliminating all theoretical interpretations",
                0,
                "Controlled verification allows students and researchers to confirm specific mechanisms underlying $cleanName."
            ),
            QuestionDraft(
                "What is a principal real-world application of mastering '$cleanName'?",
                "Predicting system behaviors and optimizing engineering or academic problem solutions",
                "Eliminating the necessity for standard measurement instruments",
                "Proving that scientific laws change randomly over time",
                "Avoiding analytical modeling in applied disciplines",
                0,
                "Mastery of $cleanName provides predictive insight and foundational problem-solving tools in academic and applied environments."
            )
        )
    }

    private data class QuestionDraft(
        val question: String,
        val optionA: String,
        val optionB: String,
        val optionC: String,
        val optionD: String,
        val correctOptionIndex: Int,
        val explanation: String
    )
}
