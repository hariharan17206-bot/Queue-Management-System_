package qms;

/**
 * The college services that have their own queue.
 * Each service has a token prefix letter, a default service time (minutes)
 * and a number of service counters.
 */
public enum ServiceType {
    OFFICE("Office", "O", 8, 2),
    LIBRARY("Library", "L", 5, 2),
    FEES("Fees Section", "F", 6, 2),
    CERTIFICATES("Certificates", "C", 10, 2),
    ADMIN("Administrative Services", "A", 7, 2);

    public final String label;
    public final String prefix;
    public final int defaultMinutes;
    public final int counters;

    ServiceType(String label, String prefix, int defaultMinutes, int counters) {
        this.label = label;
        this.prefix = prefix;
        this.defaultMinutes = defaultMinutes;
        this.counters = counters;
    }

    @Override
    public String toString() {
        return label;
    }
}
