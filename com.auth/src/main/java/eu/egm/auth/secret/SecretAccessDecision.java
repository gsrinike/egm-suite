package eu.egm.auth.secret;

public record SecretAccessDecision(boolean allowed, String reason) {
    public static SecretAccessDecision grant() {
        return new SecretAccessDecision(true, "Allowed");
    }

    public static SecretAccessDecision denied(String reason) {
        return new SecretAccessDecision(false, reason);
    }
}
