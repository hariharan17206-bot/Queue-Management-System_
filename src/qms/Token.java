package qms;

/** One queue token issued to a student (or a walk-in visitor). */
public class Token {
    public enum Status { WAITING, SERVING, COMPLETED, SKIPPED, CANCELLED }

    public String date;            // yyyy-MM-dd  (numbering restarts every day)
    public int number;             // 1, 2, 3 ... per service per day
    public ServiceType service;
    public String studentId;
    public String studentName;
    public boolean priority;       // senior citizen / differently-abled / emergency
    public Status status = Status.WAITING;
    public String counter = "-";
    public String issuedAt = "";
    public String calledAt = "";
    public String completedAt = "";

    /** Token label shown to the user, e.g. L-004. */
    public String label() {
        return service.prefix + "-" + String.format("%03d", number);
    }

    public boolean isActive() {
        return status == Status.WAITING || status == Status.SERVING;
    }

    private static String clean(String s) {
        return s.replace('|', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    public String toRecord() {
        return String.join("|", date, String.valueOf(number), service.name(), clean(studentId),
                clean(studentName), String.valueOf(priority), status.name(), counter,
                issuedAt, calledAt, completedAt);
    }

    public static Token fromRecord(String line) {
        String[] p = line.split("\\|", -1);
        Token t = new Token();
        t.date = p[0];
        t.number = Integer.parseInt(p[1]);
        t.service = ServiceType.valueOf(p[2]);
        t.studentId = p[3];
        t.studentName = p[4];
        t.priority = Boolean.parseBoolean(p[5]);
        t.status = Status.valueOf(p[6]);
        t.counter = p[7];
        t.issuedAt = p[8];
        t.calledAt = p[9];
        t.completedAt = p[10];
        return t;
    }
}
