package com.swiftapp_backend.paymentservice.shared;

/** Exception messages may contain provider responses, credentials or request bodies. */
public final class SafeFailureDetails {
    private SafeFailureDetails() {}

    public static String describe(Throwable failure) {
        StringBuilder details = new StringBuilder();
        for (int cause = 0; failure != null && cause < 4; cause++, failure = failure.getCause()) {
            if (!details.isEmpty()) details.append("; caused by ");
            details.append(failure.getClass().getName());
            StackTraceElement[] frames = failure.getStackTrace();
            for (int frame = 0; frame < Math.min(frames.length, 6); frame++) {
                details.append(" at ").append(frames[frame]);
            }
        }
        return details.toString();
    }
}
