import express from "express";
import fs from "fs";
import path from "path";
import crypto from "crypto";

export interface StoredUser {
  id: string;
  email: string;
  name: string;
  passwordHash: string;
  salt: string;
  createdAt: number;
  resetCode?: string;
  resetCodeExpiresAt?: number;
}

export interface ActiveSession {
  token: string;
  userId: string;
  email: string;
  name: string;
  createdAt: number;
  expiresAt: number;
}

const DATA_DIR = path.join(process.cwd(), "data");
const USERS_FILE = path.join(DATA_DIR, "users.json");
const SESSIONS_FILE = path.join(DATA_DIR, "sessions.json");
const SESSION_DURATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

// Helper: Ensure storage directory exists
function ensureDataDir() {
  if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  }
}

// Helper: Load users from storage or seed initial default user
function initUserStore(): StoredUser[] {
  ensureDataDir();
  try {
    if (fs.existsSync(USERS_FILE)) {
      const raw = fs.readFileSync(USERS_FILE, "utf-8");
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed;
      }
    }
  } catch (err) {
    console.warn("Could not read users database, seeding default user:", err);
  }

  // Pre-seed default student account with salted SHA-256 hash
  const defaultSalt = crypto.randomBytes(16).toString("hex");
  const defaultHash = crypto.createHash("sha256").update(defaultSalt + "Password123!").digest("hex");
  const initialUsers: StoredUser[] = [
    {
      id: "student_default_1",
      email: "student@studypilot.edu",
      name: "Alex Student",
      passwordHash: defaultHash,
      salt: defaultSalt,
      createdAt: Date.now(),
    },
  ];

  try {
    fs.writeFileSync(USERS_FILE, JSON.stringify(initialUsers, null, 2), "utf-8");
  } catch (e) {
    console.error("Error writing initial users.json:", e);
  }
  return initialUsers;
}

// Helper: Load active sessions from disk
function initSessionStore(): ActiveSession[] {
  ensureDataDir();
  try {
    if (fs.existsSync(SESSIONS_FILE)) {
      const raw = fs.readFileSync(SESSIONS_FILE, "utf-8");
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        // Filter out expired sessions
        const now = Date.now();
        return parsed.filter((s) => s.expiresAt > now);
      }
    }
  } catch (err) {
    console.warn("Could not read sessions file:", err);
  }
  return [];
}

let users: StoredUser[] = initUserStore();
let sessions: ActiveSession[] = initSessionStore();

function saveUsers(): void {
  ensureDataDir();
  try {
    fs.writeFileSync(USERS_FILE, JSON.stringify(users, null, 2), "utf-8");
  } catch (e) {
    console.error("Failed to save users to file:", e);
  }
}

function saveSessions(): void {
  ensureDataDir();
  try {
    fs.writeFileSync(SESSIONS_FILE, JSON.stringify(sessions, null, 2), "utf-8");
  } catch (e) {
    console.error("Failed to save sessions to file:", e);
  }
}

// Helper: Create a new session
function createSession(user: StoredUser): ActiveSession {
  const token = "stk_" + crypto.randomBytes(32).toString("hex");
  const session: ActiveSession = {
    token,
    userId: user.id,
    email: user.email,
    name: user.name,
    createdAt: Date.now(),
    expiresAt: Date.now() + SESSION_DURATION_MS,
  };
  sessions.push(session);
  saveSessions();
  return session;
}

// Helper: Invalidate user sessions
function invalidateUserSessions(userId: string): void {
  sessions = sessions.filter((s) => s.userId !== userId);
  saveSessions();
}

// Helper: Extract bearer token from Authorization header or body
function extractToken(req: express.Request): string | null {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith("Bearer ")) {
    return authHeader.substring(7).trim();
  }
  const customHeader = req.headers["x-auth-token"];
  if (typeof customHeader === "string" && customHeader.trim()) {
    return customHeader.trim();
  }
  if (req.body && typeof req.body.token === "string" && req.body.token.trim()) {
    return req.body.token.trim();
  }
  return null;
}

// Helper: Find valid session by token
function findSession(token: string | null): ActiveSession | null {
  if (!token) return null;
  const now = Date.now();
  const session = sessions.find((s) => s.token === token && s.expiresAt > now);
  return session || null;
}

export const authRouter = express.Router();

// GET /api/auth/status
authRouter.get("/status", (_req, res) => {
  res.json({
    authType: "email_password",
    features: ["signup", "login", "forgot_password", "reset_password", "change_password", "logout", "me"],
    registeredUsersCount: users.length,
    activeSessionsCount: sessions.length,
  });
});

// GET /api/auth/me - Validate current session token
authRouter.get("/me", (req, res) => {
  const token = extractToken(req);
  if (!token) {
    return res.status(401).json({ error: "No authentication token provided." });
  }

  const session = findSession(token);
  if (!session) {
    return res.status(401).json({ error: "Session invalid or expired. Please sign in again." });
  }

  const user = users.find((u) => u.id === session.userId);
  if (!user) {
    return res.status(401).json({ error: "User account associated with session was not found." });
  }

  return res.json({
    success: true,
    user: {
      id: user.id,
      email: user.email,
      name: user.name,
      signedInAt: session.createdAt,
      token: session.token,
    },
  });
});

// POST /api/auth/signup
authRouter.post("/signup", (req, res) => {
  try {
    const { email, password, name } = req.body || {};
    if (!email || !password || !name) {
      return res.status(400).json({ error: "Full name, email address, and password are required." });
    }

    const cleanEmail = String(email).trim().toLowerCase();
    const cleanName = String(name).trim();
    const cleanPassword = String(password);

    if (cleanName.length < 2) {
      return res.status(400).json({ error: "Please enter your full name (at least 2 characters)." });
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(cleanEmail)) {
      return res.status(400).json({ error: "Please enter a valid email address." });
    }

    if (cleanPassword.length < 6) {
      return res.status(400).json({ error: "Password must be at least 6 characters long." });
    }

    const existingUser = users.find((u) => u.email === cleanEmail);
    if (existingUser) {
      return res.status(409).json({ error: "An account with this email already exists. Please log in instead." });
    }

    // Salted SHA-256 hashing
    const salt = crypto.randomBytes(16).toString("hex");
    const passwordHash = crypto.createHash("sha256").update(salt + cleanPassword).digest("hex");

    const newUser: StoredUser = {
      id: "user_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7),
      email: cleanEmail,
      name: cleanName,
      passwordHash,
      salt,
      createdAt: Date.now(),
    };

    users.push(newUser);
    saveUsers();

    // Create session
    const session = createSession(newUser);

    return res.status(201).json({
      success: true,
      message: "Account created successfully!",
      user: {
        id: newUser.id,
        email: newUser.email,
        name: newUser.name,
        signedInAt: session.createdAt,
        token: session.token,
      },
      token: session.token,
    });
  } catch (err: any) {
    console.error("Signup error:", err);
    return res.status(500).json({ error: "An unexpected error occurred during sign up. Please try again." });
  }
});

// POST /api/auth/login
authRouter.post("/login", (req, res) => {
  try {
    const { email, password } = req.body || {};
    if (!email || !password) {
      return res.status(400).json({ error: "Email address and password are required." });
    }

    const cleanEmail = String(email).trim().toLowerCase();
    const cleanPassword = String(password);

    const user = users.find((u) => u.email === cleanEmail);
    if (!user) {
      return res.status(401).json({ error: "No account found with this email. Please check your spelling or sign up." });
    }

    // Strict salted SHA-256 password hash comparison - NO universal credentials or bypasses
    const computedHash = crypto.createHash("sha256").update(user.salt + cleanPassword).digest("hex");
    if (computedHash !== user.passwordHash) {
      return res.status(401).json({ error: "Incorrect password. Please verify your password or use 'Forgot Password'." });
    }

    // Create active session
    const session = createSession(user);

    return res.json({
      success: true,
      message: "Login successful!",
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        signedInAt: session.createdAt,
        token: session.token,
      },
      token: session.token,
    });
  } catch (err: any) {
    console.error("Login error:", err);
    return res.status(500).json({ error: "An unexpected error occurred during login. Please try again." });
  }
});

// POST /api/auth/forgot-password
authRouter.post("/forgot-password", (req, res) => {
  try {
    const { email } = req.body || {};
    if (!email) {
      return res.status(400).json({ error: "Email address is required." });
    }

    const cleanEmail = String(email).trim().toLowerCase();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(cleanEmail)) {
      return res.status(400).json({ error: "Please enter a valid email address." });
    }

    const user = users.find((u) => u.email === cleanEmail);
    if (!user) {
      return res.status(404).json({ error: "No student account found with this email address." });
    }

    // Generate cryptographically secure 6-digit numeric recovery verification code
    const resetCode = crypto.randomInt(100000, 999999).toString();
    user.resetCode = resetCode;
    user.resetCodeExpiresAt = Date.now() + 15 * 60 * 1000; // 15 min validity
    saveUsers();

    return res.json({
      success: true,
      message: `A 6-digit recovery code has been generated for ${cleanEmail}.`,
      resetCode,
      expiresInMinutes: 15,
    });
  } catch (err: any) {
    console.error("Forgot password error:", err);
    return res.status(500).json({ error: "Failed to process forgot password request." });
  }
});

// POST /api/auth/reset-password
authRouter.post("/reset-password", (req, res) => {
  try {
    const { email, resetCode, newPassword } = req.body || {};
    if (!email || !resetCode || !newPassword) {
      return res.status(400).json({ error: "Email, 6-digit verification code, and new password are required." });
    }

    const cleanEmail = String(email).trim().toLowerCase();
    const cleanCode = String(resetCode).trim();
    const cleanPassword = String(newPassword);

    if (cleanPassword.length < 6) {
      return res.status(400).json({ error: "New password must be at least 6 characters long." });
    }

    const user = users.find((u) => u.email === cleanEmail);
    if (!user) {
      return res.status(404).json({ error: "Account not found." });
    }

    if (!user.resetCode || user.resetCode !== cleanCode) {
      return res.status(400).json({ error: "Invalid verification code. Please check the 6-digit code and try again." });
    }

    if (!user.resetCodeExpiresAt || Date.now() > user.resetCodeExpiresAt) {
      return res.status(400).json({ error: "This verification code has expired. Please request a new code." });
    }

    // Set new password with fresh salt
    const newSalt = crypto.randomBytes(16).toString("hex");
    const newHash = crypto.createHash("sha256").update(newSalt + cleanPassword).digest("hex");

    user.salt = newSalt;
    user.passwordHash = newHash;
    delete user.resetCode;
    delete user.resetCodeExpiresAt;
    saveUsers();

    // Invalidate existing sessions so user must sign in with new password
    invalidateUserSessions(user.id);

    return res.json({
      success: true,
      message: "Your password has been successfully reset! Please sign in with your new password.",
    });
  } catch (err: any) {
    console.error("Reset password error:", err);
    return res.status(500).json({ error: "Failed to reset password. Please try again." });
  }
});

// POST /api/auth/change-password
authRouter.post("/change-password", (req, res) => {
  try {
    // Require authenticated session
    const token = extractToken(req);
    if (!token) {
      return res.status(401).json({ error: "Authentication required. Please sign in to change password." });
    }

    const session = findSession(token);
    if (!session) {
      return res.status(401).json({ error: "Session expired or invalid. Please sign in again." });
    }

    const { currentPassword, newPassword, email } = req.body || {};
    if (!currentPassword || !newPassword) {
      return res.status(400).json({ error: "Current password and new password are required." });
    }

    // Locate user by session userId
    const user = users.find((u) => u.id === session.userId);
    if (!user) {
      return res.status(404).json({ error: "User account not found." });
    }

    // If an email was explicitly sent, verify it belongs to the session
    if (email && String(email).trim().toLowerCase() !== user.email) {
      return res.status(403).json({ error: "Cannot change password for another account." });
    }

    // Verify current password
    const currentHash = crypto.createHash("sha256").update(user.salt + String(currentPassword)).digest("hex");
    if (currentHash !== user.passwordHash) {
      return res.status(401).json({ error: "Current password is incorrect." });
    }

    if (String(newPassword).length < 6) {
      return res.status(400).json({ error: "New password must be at least 6 characters long." });
    }

    if (String(newPassword) === String(currentPassword)) {
      return res.status(400).json({ error: "New password must be different from your current password." });
    }

    // Set new password with fresh salt
    const newSalt = crypto.randomBytes(16).toString("hex");
    user.salt = newSalt;
    user.passwordHash = crypto.createHash("sha256").update(newSalt + String(newPassword)).digest("hex");
    saveUsers();

    return res.json({
      success: true,
      message: "Password updated successfully!",
    });
  } catch (err: any) {
    console.error("Change password error:", err);
    return res.status(500).json({ error: "Failed to change password." });
  }
});

// POST /api/auth/logout
authRouter.post("/logout", (req, res) => {
  const token = extractToken(req);
  if (token) {
    sessions = sessions.filter((s) => s.token !== token);
    saveSessions();
  }
  return res.json({
    success: true,
    message: "Logged out successfully.",
  });
});
