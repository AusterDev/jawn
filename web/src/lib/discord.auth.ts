const DISCORD_AUTH_API = "https://discord.com/api/oauth2";
const DISCORD_API = "https://discord.com/api/v10";

interface DiscordTokenResponse {
    access_token?: string;
    error?: string;
}

interface DiscordUserResponse {
    id?: string;
    username?: string;
}

function buildAuthHeader(token: string) {
    return {
        "Authorization": `Bearer ${token}`,
    };
}

export function buildOAuth2Url(sessionID: string): string {
    const params = new URLSearchParams({
        client_id: process.env.DISCORD_CLIENT_ID!,
        redirect_uri: `${process.env.WEB_HOST!}/callback/discord`,
        response_type: "code",
        scope: "identify",
        state: sessionID,
    });
    
    return `${DISCORD_AUTH_API}/authorize?${params.toString()}`;
}

async function exchangeCode(code: string): Promise<string | null> {
    const response = await fetch(`${DISCORD_AUTH_API}/token`, {
        method: "POST",
        body: new URLSearchParams({
            client_id: process.env.DISCORD_CLIENT_ID!,
            client_secret: process.env.DISCORD_CLIENT_SECRET!,
            code,
            grant_type: "authorization_code",
            redirect_uri: `${process.env.WEB_HOST!}/callback/discord`,
        }).toString(),
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
    });

    if (!response.ok) {
        return null;
    }

    const data = (await response.json()) as DiscordTokenResponse;
    return data.access_token ?? null;
}

async function getCurrentUser(token: string): Promise<DiscordUserResponse | null> {
    const response = await fetch(`${DISCORD_API}/users/@me`, {
        method: "GET",
        headers: buildAuthHeader(token),
    });

    if (!response.ok) {
        return null;
    }

    return (await response.json()) as DiscordUserResponse;
}

export async function verifyAccount(userID: string, code: string): Promise<boolean> {
    const token = await exchangeCode(code);
    if (!token) throw new Error("Failed to exchange code for token");

    const user = await getCurrentUser(token);
    if (!user || !user.id) throw new Error("Failed to fetch valid Discord user data");

    return user.id === userID;
}