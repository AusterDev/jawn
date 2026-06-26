import type { APIRoute } from "astro";
import Redis from "ioredis";
import type { VerificationResult } from "../../types/index";

let redis: Redis;
try {
  redis = new Redis({
    host: process.env.REDIS_HOST,
    port: parseInt(process.env.REDIS_PORT || "6379", 10),
    password: process.env.REDIS_PASSWORD,
    maxRetriesPerRequest: 3,
    connectTimeout: 5000,
  });
  redis.on("error", (err) => console.error(err));
} catch (err) {
  console.error(err);
}

const ALLOWED_DEGREES = ["ds", "es", "aes"] as const;
const SESSION_TTL = 900;

export const GET: APIRoute = async ({ url, cookies }) => {
  const code = url.searchParams.get("code");
  const state = url.searchParams.get("state");
  const sessionId = state;

  const cookieOptions = {
    httpOnly: true,
    secure: !import.meta.env.DEV,
    sameSite: "strict" as const,
    path: "/",
    maxAge: 60 * 60,
  };

  const terminateWithRedirect = (destination: string, result: VerificationResult) => {
    cookies.set("result", JSON.stringify(result), cookieOptions);

    return new Response(
      `<!DOCTYPE html>
      <html>
        <head>
          <meta http-equiv="refresh" content="0;url=${encodeURI(destination)}">
        </head>
        <body>
          <script>window.location.href = "${encodeURI(destination)}";</script>
        </body>
      </html>`,
      { headers: { "Content-Type": "text/html; charset=utf-8" } }
    );
  };

  const fallbackUrl = new URL("/jawn/verify/finale", url.origin).toString();

  if (!code || !sessionId) {
    return Response.redirect(fallbackUrl, 302);
  }

  cookies.delete("result");

  try {
    const redisKey = `session:${sessionId}`;
    const rawSession = await redis.get(redisKey);

    if (!rawSession) {
      return Response.redirect(fallbackUrl, 302);
    }

    let session;
    try {
      session = JSON.parse(rawSession);
    } catch (e) {
      console.error(e);
      return Response.redirect(fallbackUrl, 302);
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 8000);

    let tokens;
    try {
      const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        signal: controller.signal,
        body: new URLSearchParams({
          code,
          client_id: process.env.GOOGLE_CLIENT_ID || "",
          client_secret: process.env.GOOGLE_CLIENT_SECRET || "",
          redirect_uri: `${url.origin}/jawn/verify`,
          grant_type: "authorization_code",
        }),
      });

      tokens = await tokenResponse.json();

      if (!tokenResponse.ok) {
        console.error(tokens);
        return Response.redirect(fallbackUrl, 302);
      }
    } catch {
      clearTimeout(timeoutId);
    }

    const profileResponse = await fetch("https://www.googleapis.com/oauth2/v2/userinfo", {
      headers: { Authorization: `Bearer ${tokens.access_token}` },
    });

    if (!profileResponse.ok) {
      return Response.redirect(fallbackUrl, 302);
    }

    const profile = await profileResponse.json();
    const email = profile.email ? profile.email.toLowerCase().trim() : "";

    let detectedDegree: string | null = null;
    const match = email.match(/^[^@]+@([^.]+)\.study\.iitm\.ac\.in$/);
    
    if (match) {
      const subdomain = match[1];
      if ((ALLOWED_DEGREES as readonly string[]).includes(subdomain)) {
        detectedDegree = subdomain;
      }
    }

    const pipeline = redis.pipeline();
    
    if (!detectedDegree) {
      session.verified = false;
      session.degree_type = "NONE";

      pipeline.setex(redisKey, SESSION_TTL, JSON.stringify(session));
      pipeline.del(`user_session:${session.user_id}`);
      pipeline.publish("user_verified", sessionId);
      await pipeline.exec();

      return terminateWithRedirect("/jawn/verify/finale", {
        userID: session.user_id,
        degreeType: null,
        verified: false,
        reason: "Your student email is not assigned by IIT Madras. Please use your authorized student email address."
      });
    }

    session.verified = true;
    session.degree_type = detectedDegree;

    pipeline.setex(redisKey, SESSION_TTL, JSON.stringify(session));
    pipeline.del(`user_session:${session.user_id}`);
    pipeline.publish("user_verified", sessionId);
    await pipeline.exec();

    return terminateWithRedirect("/jawn/verify/finale", {
      userID: session.user_id,
      degreeType: detectedDegree,
      verified: true,
    });

  } catch (error) {
    console.error(error);
    return Response.redirect(fallbackUrl, 302);
  }
};