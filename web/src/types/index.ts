export type VerificationResult = {
    user_id: string;
    degree_type: string | null;
    verified: boolean;
    reason: string | null;
}