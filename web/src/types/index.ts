export type VerificationResult = {
    userID: string;
    degreeType: string | null;
    verified: boolean;
    reason?: string;
}