import type { APIRoute } from "astro";
import Redis from "ioredis";
import type { VerificationResult } from "../../types/index";

const redis = new Redis({
  host: process.env.REDIS_HOST,
  port: parseInt(process.env.REDIS_PORT!, 10) || 6379,
  password: process.env.REDIS_PASSWORD,
});

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
      `<html>
      <head>
        <meta http-equiv="refresh" content="0;url=${destination}">
      </head>
      <body>
        <script>window.location.href = "${destination}";</script>
      </body>
    </html>`,
      {
        headers: { "Content-Type": "text/html" },
      }
    );
  };

  if (!code || !sessionId) {
    return Response.redirect(new URL("/jawn/verify/finale", url.origin), 302);
  }

  cookies.delete("result");

  try {
    const redisKey = `session:${sessionId}`;
    const rawSession = await redis.get(redisKey);

    if (!rawSession) {
      return Response.redirect(new URL("/jawn/verify/finale", url.origin), 302);
    }

    const session = JSON.parse(rawSession);

    const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        code,
        client_id: import.meta.env.GOOGLE_CLIENT_ID || "",
        client_secret: import.meta.env.GOOGLE_CLIENT_SECRET || "",
        redirect_uri: `${url.origin}/jawn/verify`,
        grant_type: "authorization_code",
      }),
    });

    const tokens = await tokenResponse.json();

    if (!tokenResponse.ok) {
      console.error("Google Token Exchange Failure:", tokens);
      return Response.redirect(new URL("/jawn/verify/finale", url.origin), 302);
    }

    const profileResponse = await fetch("https://www.googleapis.com/oauth2/v2/userinfo", {
      headers: { Authorization: `Bearer ${tokens.access_token}` },
    });

    const profile = await profileResponse.json();
    const email = profile.email ? profile.email.toLowerCase() : "";

    const allowedDegrees = ["ds", "es", "aes"];
    let detectedDegree: string | null = null;

    if (email && email.endsWith(".study.iitm.ac.in")) {
      const domainPart = email.split("@")[1];
      const subdomain = domainPart.replace(".study.iitm.ac.in", "");

      if (allowedDegrees.includes(subdomain)) {
        detectedDegree = subdomain;
      }
    }


    if (!detectedDegree) {
      session.verified = false;
      session.degree_type = "NONE";

      await Promise.all([
        redis.setex(redisKey, 900, JSON.stringify(session)),
        redis.del(`user_session:${session.user_id}`),
        redis.publish("user_verified", sessionId) 
      ]);

      return terminateWithRedirect("/jawn/verify/finale", {
        userID: session.user_id,
        degreeType: null,
        verified: false,
        reason: "Your student email is not assigned by IIT Madras. Please use your authorized student email address."
      });

    } else {
      session.verified = true;
      session.degree_type = detectedDegree;

      await Promise.all([
        redis.setex(redisKey, 900, JSON.stringify(session)),
        redis.del(`user_session:${session.user_id}`),
        redis.publish("user_verified", sessionId) 
      ]);

      return terminateWithRedirect(`/jawn/verify/finale`, {
        userID: session.user_id,
        degreeType: detectedDegree,
        verified: true,
      });
    }

  } catch (error) {
    console.error("Callback core API route execution error:", error);
    return Response.redirect(new URL("/jawn/verify/finale", url.origin), 302);
  }
};