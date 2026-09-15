import express from "express";
import path from "path";
import fs from "fs";
import crypto from "crypto";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI, Type } from "@google/genai";
import { authRouter } from "./server/auth";

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(express.json({ limit: "25mb" }));

  // Lazy AI client initialization
  let aiClient: GoogleGenAI | null = null;
  function getAiClient(): GoogleGenAI | null {
    if (!aiClient && process.env.GEMINI_API_KEY) {
      aiClient = new GoogleGenAI({
        apiKey: process.env.GEMINI_API_KEY,
        httpOptions: {
          headers: {
            "User-Agent": "aistudio-build",
          },
        },
      });
    }
    return aiClient;
  }

  // Helper to extract a human-readable clean error message
  function extractCleanErrorMessage(error: any): string {
    if (!error) return "Failed to communicate with Pluto AI service";
    const rawMsg = error.message || String(error);
    try {
      const parsed = JSON.parse(rawMsg);
      if (parsed?.error?.message) {
        const innerMsg = parsed.error.message;
        if (innerMsg.includes("Quota exceeded") || innerMsg.includes("RESOURCE_EXHAUSTED") || parsed?.error?.code === 429) {
          return "Gemini API request rate limit reached. Please wait a few seconds and tap Retry.";
        }
        return innerMsg;
      }
    } catch {
      // not JSON formatted
    }
    if (rawMsg.includes("quota") || rawMsg.includes("RESOURCE_EXHAUSTED") || error?.status === 429) {
      return "Gemini API request rate limit reached. Please wait a few seconds and tap Retry.";
    }
    if (error?.status === 503 || rawMsg.includes("high demand") || rawMsg.includes("UNAVAILABLE")) {
      return "AI service is temporarily handling high traffic. Please retry in a moment.";
    }
    return rawMsg;
  }

  // Helper to generate content with model fallback
  async function generateWithFallback(client: GoogleGenAI, params: any) {
    // gemini-3.1-flash-lite offers high free-tier quota and fast latency, followed by gemini-3.8-flash and gemini-flash-latest
    const candidateModels = ["gemini-3.1-flash-lite", "gemini-3.8-flash", "gemini-flash-latest"];
    let lastError: any = null;

    for (const modelName of candidateModels) {
      try {
        const result = await client.models.generateContent({
          ...params,
          model: modelName,
        });
        return { response: result, modelUsed: modelName };
      } catch (err: any) {
        lastError = err;
        const cleanErr = extractCleanErrorMessage(err);
        console.warn(`Pluto generation with ${modelName} encountered: ${cleanErr}. Attempting fallback model...`);
      }
    }
    throw lastError || new Error("Failed to generate response from Gemini AI models.");
  }

  // API routes FIRST
  // Health check endpoint
  app.get("/api/health", (_req, res) => {
    res.json({
      status: "ok",
      hasGeminiKey: Boolean(process.env.GEMINI_API_KEY),
      authType: "email_password",
      app: "StudyPilot",
    });
  });

  // Authentication routes (Email & Password: signup, login, forgot-password, reset-password, change-password, logout)
  app.use("/api/auth", authRouter);

  app.post("/api/pluto/chat", async (req, res) => {
    try {
      const {
        message,
        attachment,
        userProfile,
        subjects,
        generateQuiz,
        conversationHistory,
      } = req.body;

      const client = getAiClient();

      if (!client) {
        // Explicit configuration error per requirements: Do NOT pretend Pluto works
        return res.status(503).json({
          error: "GEMINI_API_KEY is not configured.",
          reply: "⚠️ **Configuration Error**: `GEMINI_API_KEY` is not configured on the server. Pluto requires an active Gemini API key to deliver live AI responses, analyze attachments, and generate practice quizzes. Please verify your API key in project settings.",
          isConfigMissing: true,
        });
      }

      // Compact student curriculum context (selective retrieval)
      const subjectNames = Array.isArray(subjects) && subjects.length > 0
        ? subjects.map((s: any) => s.name).filter(Boolean).join(", ")
        : "General Curriculum";

      // Comprehensive, precise system instruction
      const systemInstruction = `You are Pluto, the intelligent AI academic companion and study mentor inside StudyPilot (created by Mohammad Fahad).
Your persona is encouraging, intellectually clear, patient, and academically rigorous.
You explain complex topics clearly with structured Markdown, bold key terms, and bullet points.

Current Student Profile:
- Education System: ${userProfile?.educationSystem || "General Secondary/High School"}
- Grade/Level: ${userProfile?.grade || "Standard"}
- Registered Subjects: ${subjectNames}

StudyPilot Application Knowledge (Use ONLY when the user asks about the app itself):
1. App Structure: StudyPilot is a Phase 1 Native Android study management application built by Mohammad Fahad.
2. Adding Curriculum Content:
   - To add a Subject: Navigate to the Subjects screen from the bottom navigation or Home, and tap "+ New Subject".
   - To add a Chapter: Tap on any Subject to view its Chapters, then tap the "+ New Chapter" button at the top right of the screen.
   - To add a Topic: Open any Chapter and tap the "+ New Topic" button to create individual study topics.
3. Focus Guardian:
   - Hardware front-camera presence monitoring running on-device computer vision.
   - Detects PRESENT, ABSENT, or UNKNOWN states automatically.
   - Active study timer automatically pauses when the student is absent.
   - Absence triggers a native voice warning, followed by the synthesized Reveille bugle alarm and phone vibration if absence persists.
   - Draggable floating Picture-in-Picture (PiP) window lets the student study materials (videos, notes, PDFs) while Guardian continues presence tracking in the background.
4. Study Materials:
   - Holds study notes, uploaded PDFs, and video lectures. Offers a "Study with Guardian" button to launch study materials with the live floating camera.
5. Practice Quizzes vs. Official Tests:
   - Official Topic Mastery: Requires taking the 5-question official topic mastery test (passing score 70%+). This updates official topic completion, chapter mastery, and subject progress.
   - Pluto Practice Quizzes: Lightweight self-assessment quizzes generated on-the-fly from attachments or questions. They NEVER modify official topic completion, subject progress, or chapter mastery.

Directives:
- Casual conversation (e.g., "How are you?"): Be friendly and concise in 1-2 sentences. Do not force academic curriculum into casual small talk.
- Motivation (e.g., "Motivate me"): Provide empowering, practical advice for focus, discipline, and building momentum.
- Academic/concept questions (e.g., "What is Newton's second law?"): Provide step-by-step explanations, formulas, units, and intuitive examples.
- Attachment handling:
  * If an attachment (image, PDF, document, or text) is provided AND the user asks a question about it (or asks to explain/summarize it), thoroughly analyze the attachment content and answer specifically.
  * If an attachment is provided BUT the user asks an unrelated question (e.g. "What is Newton's second law?" or "How are you?"), answer the user's question directly without forcing the attachment into the response.
  * If "Quiz me from this" is requested, generate a 4-question practice quiz based on the material.`;

      // 1. PRACTICE QUIZ GENERATION FLOW
      if (generateQuiz) {
        const quizParts: any[] = [];

        if (attachment?.base64Data && attachment?.mimeType) {
          quizParts.push({
            inlineData: {
              mimeType: attachment.mimeType,
              data: attachment.base64Data,
            },
          });
        }

        const quizPrompt = `Generate an engaging, high-quality 4-question multiple-choice practice quiz based on:
Attachment Name: "${attachment?.name || 'Study Material'}"
${attachment?.contentSnippet ? `Text Content: "${attachment.contentSnippet.slice(0, 3000)}"` : ''}
${message ? `User Guidance/Topic: "${message}"` : ''}

Rules:
1. Generate exactly 4 multiple-choice questions testing conceptual understanding and active recall.
2. Each question MUST provide exactly 4 distinct options.
3. correctOptionIndex must be an integer (0, 1, 2, or 3) corresponding to the correct answer.
4. explanation must provide an educational explanation of why that answer is correct.
5. title should be a clear, concise title for this practice quiz.`;

        quizParts.push({ text: quizPrompt });

        const { response: quizResponse } = await generateWithFallback(client, {
          contents: { parts: quizParts },
          config: {
            systemInstruction,
            responseMimeType: "application/json",
            responseSchema: {
              type: Type.OBJECT,
              properties: {
                title: { type: Type.STRING },
                questions: {
                  type: Type.ARRAY,
                  items: {
                    type: Type.OBJECT,
                    properties: {
                      id: { type: Type.INTEGER },
                      questionText: { type: Type.STRING },
                      options: {
                        type: Type.ARRAY,
                        items: { type: Type.STRING },
                      },
                      correctOptionIndex: { type: Type.INTEGER },
                      explanation: { type: Type.STRING },
                    },
                    required: ["id", "questionText", "options", "correctOptionIndex", "explanation"],
                  },
                },
              },
              required: ["title", "questions"],
            },
          },
        });

        const quizText = quizResponse.text?.trim() || "{}";
        let parsedQuiz: any = null;
        try {
          parsedQuiz = JSON.parse(quizText);
        } catch (parseErr) {
          console.error("Quiz JSON parse error:", parseErr, quizText);
          throw new Error("Failed to parse the generated practice quiz. Please try again.");
        }

        if (!parsedQuiz.questions || !Array.isArray(parsedQuiz.questions) || parsedQuiz.questions.length === 0) {
          throw new Error("Pluto did not receive sufficient quiz questions from the material. Please try again.");
        }

        return res.json({
          reply: `I have analyzed your material (**${attachment?.name || 'Study Material'}**) and prepared a 4-question practice quiz to test your active recall.`,
          quiz: parsedQuiz,
          isOffline: false,
        });
      }

      // 2. STANDARD CONVERSATIONAL & MULTIMODAL QUERY FLOW
      const contents: any[] = [];

      // Include compact past turns (last 4 max)
      if (Array.isArray(conversationHistory) && conversationHistory.length > 0) {
        for (const msg of conversationHistory.slice(-4)) {
          if (msg.content && typeof msg.content === "string") {
            contents.push({
              role: msg.sender === "Student" ? "user" : "model",
              parts: [{ text: msg.content }],
            });
          }
        }
      }

      // Add current turn with attachment if provided
      const currentParts: any[] = [];
      if (attachment?.base64Data && attachment?.mimeType) {
        currentParts.push({
          inlineData: {
            mimeType: attachment.mimeType,
            data: attachment.base64Data,
          },
        });
      }

      const promptText = message?.trim() || (attachment ? `Please review this attached file (${attachment.name}) and give me a summary of key concepts.` : "Hello Pluto!");
      currentParts.push({ text: promptText });
      contents.push({ role: "user", parts: currentParts });

      const { response: aiResponse } = await generateWithFallback(client, {
        contents,
        config: {
          systemInstruction,
        },
      });

      const responseText = aiResponse.text?.trim();

      return res.json({
        reply: responseText || "I'm here to help you study! What concept would you like to review?",
        isOffline: false,
      });
    } catch (error: any) {
      console.error("Pluto API error:", error);
      const cleanError = extractCleanErrorMessage(error);
      const isQuota = error?.status === 429 || cleanError.includes("rate limit") || cleanError.includes("quota");
      const isOverloaded = error?.status === 503 || cleanError.includes("high demand") || cleanError.includes("traffic");

      let userFriendlyMessage = `⚠️ **Pluto AI Notice**: ${cleanError}`;
      if (isQuota) {
        userFriendlyMessage = "⚠️ **Rate Limit Notice**: Gemini API request rate limit reached. Please wait a few seconds and tap Retry.";
      } else if (isOverloaded) {
        userFriendlyMessage = "⚠️ **Temporary High Demand**: The AI model is currently handling high traffic. Please retry in a few moments.";
      }

      return res.status(500).json({
        error: cleanError,
        reply: userFriendlyMessage,
      });
    }
  });

  // Vite middleware for development
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (_req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`StudyPilot server listening on port ${PORT}`);
  });
}

startServer();
