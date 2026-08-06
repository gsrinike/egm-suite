package eu.egm.srv.cnm.services.service;

import eu.egm.data.cnm.cgmes.CgmesProfileKind;
import eu.egm.data.cnm.common.CnmFileProcessingRequested;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orders RDF metadata work so shared boundary profiles are extracted before
 * TSO-specific profiles. Requests with the same profile priority retain their
 * creation-time order.
 */
final class CnmFileProcessingPriority {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    static final Comparator<PrioritizedRequest> COMPARATOR = Comparator
            .comparingInt((PrioritizedRequest request) -> profilePriority(request.event().fileName()))
            .thenComparing(request -> timestamp(request.event().requestedAt()))
            .thenComparingLong(PrioritizedRequest::sequence);

    private CnmFileProcessingPriority() {
    }

    static PrioritizedRequest prioritize(CnmFileProcessingRequested event) {
        return new PrioritizedRequest(event, SEQUENCE.incrementAndGet());
    }

    static int profilePriority(String fileName) {
        CgmesProfileKind kind = CgmesProfileKind.fromCode(profileCode(fileName));
        return switch (kind) {
            case BOUNDARY_EQUIPMENT -> 1;
            case BOUNDARY_TOPOLOGY -> 2;
            case EQUIPMENT -> 3;
            case TOPOLOGY -> 4;
            case STEADY_STATE_HYPOTHESIS -> 5;
            case STATE_VARIABLES -> 6;
            case UNKNOWN -> 99;
            default -> 10;
        };
    }

    private static long timestamp(Instant instant) {
        return instant == null ? Long.MAX_VALUE : instant.toEpochMilli();
    }

    private static String profileCode(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        String baseName = fileName.replace('\\', '/');
        int slash = baseName.lastIndexOf('/');
        if (slash >= 0) {
            baseName = baseName.substring(slash + 1);
        }
        int dot = baseName.lastIndexOf('.');
        String stem = dot > 0 ? baseName.substring(0, dot) : baseName;
        String[] parts = stem.split("_");
        if (parts.length < 3) {
            return stem.toUpperCase(Locale.ROOT);
        }
        if (stem.contains("__")) {
            return parts[Math.max(parts.length - 2, 0)].toUpperCase(Locale.ROOT);
        }
        return parts[Math.max(parts.length - 2, 0)].toUpperCase(Locale.ROOT);
    }

    record PrioritizedRequest(CnmFileProcessingRequested event, long sequence) {
    }
}
