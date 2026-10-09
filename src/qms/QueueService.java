package qms;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic layer: login, registration, token issue, queue ordering,
 * calling the next token, wait-time estimation and statistics.
 * All methods are synchronized and re-read the data files when another
 * running copy of the application changed them (so a student window and a
 * staff window can run side by side during a demo).
 */
public class QueueService {
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PRIOR_WEIGHT = 3;   // weight of the default service time in the estimate

    /** Summary numbers for the dashboard. */
    public static class Stats {
        public int total, waiting, serving, completed, skipped, cancelled;
        public double avgServiceMin, avgWaitMin;
    }

    private final DataStore store;
    private List<User> users;
    private List<Token> tokens;
    private long seenUsers, seenTokens;

    public QueueService(DataStore store) {
        this.store = store;
        users = store.loadUsers();
        tokens = store.loadTokens();
        seenUsers = store.usersModified();
        seenTokens = store.tokensModified();
        if (findUser("admin") == null) {
            users.add(new User("admin", "Administrator", hash("admin123"), User.Role.STAFF));
            saveUsers();
        }
    }

    // ---------------------------------------------------------------- helpers
    public static String now() { return LocalDateTime.now().format(FMT); }

    private static String today() { return LocalDate.now().toString(); }

    public static String hash(String pw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(pw.getBytes(StandardCharsets.UTF_8))) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void sync() {
        if (store.usersModified() != seenUsers) {
            users = store.loadUsers();
            seenUsers = store.usersModified();
        }
        if (store.tokensModified() != seenTokens) {
            tokens = store.loadTokens();
            seenTokens = store.tokensModified();
        }
    }

    private void saveUsers() {
        store.saveUsers(users);
        seenUsers = store.usersModified();
    }

    private void saveTokens() {
        store.saveTokens(tokens);
        seenTokens = store.tokensModified();
    }

    private User findUser(String id) {
        for (User u : users) if (u.id.equalsIgnoreCase(id)) return u;
        return null;
    }

    private static double minutesBetween(String from, String to) {
        return Duration.between(LocalDateTime.parse(from, FMT), LocalDateTime.parse(to, FMT)).getSeconds() / 60.0;
    }

    // --------------------------------------------------------- authentication
    public synchronized User login(String id, String password) {
        sync();
        User u = findUser(id.trim());
        return (u != null && u.passwordHash.equals(hash(password))) ? u : null;
    }

    public synchronized User registerStudent(String id, String name, String password) {
        sync();
        id = id.trim();
        name = name.trim();
        if (id.isEmpty() || name.isEmpty() || password.isEmpty())
            throw new IllegalArgumentException("Register number, name and password are required.");
        if (id.contains("|") || name.contains("|"))
            throw new IllegalArgumentException("The character '|' is not allowed.");
        if (password.length() < 4)
            throw new IllegalArgumentException("Password must have at least 4 characters.");
        if (findUser(id) != null)
            throw new IllegalArgumentException("This register number is already registered.");
        User u = new User(id, name, hash(password), User.Role.STUDENT);
        users.add(u);
        saveUsers();
        return u;
    }

    // ------------------------------------------------------------ token issue
    public synchronized Token issueToken(String studentId, String studentName, ServiceType service,
                                         boolean priority, boolean walkIn) {
        sync();
        if (!walkIn) {
            for (Token t : tokens)
                if (t.date.equals(today()) && t.service == service && t.isActive()
                        && t.studentId.equalsIgnoreCase(studentId))
                    throw new IllegalStateException("You already have an active token (" + t.label()
                            + ") for " + service.label + ".");
        }
        int max = 0;
        for (Token t : tokens)
            if (t.date.equals(today()) && t.service == service) max = Math.max(max, t.number);
        Token t = new Token();
        t.date = today();
        t.number = max + 1;
        t.service = service;
        t.studentId = studentId;
        t.studentName = studentName;
        t.priority = priority;
        t.status = Token.Status.WAITING;
        t.issuedAt = now();
        tokens.add(t);
        saveTokens();
        return t;
    }

    public synchronized Token issueWalkIn(String name, ServiceType service, boolean priority) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Visitor name is required.");
        return issueToken("WALK-IN", name.trim(), service, priority, true);
    }

    // ---------------------------------------------------------------- queries
    /** Waiting tokens of a service in serving order: priority first, then token number. */
    public synchronized List<Token> waitingList(ServiceType s) {
        sync();
        return tokens.stream()
                .filter(t -> t.date.equals(today()) && t.service == s && t.status == Token.Status.WAITING)
                .sorted(Comparator.comparing((Token t) -> !t.priority).thenComparingInt(t -> t.number))
                .collect(Collectors.toList());
    }

    public synchronized List<Token> servingList(ServiceType s) {
        sync();
        return tokens.stream()
                .filter(t -> t.date.equals(today()) && t.service == s && t.status == Token.Status.SERVING)
                .sorted(Comparator.comparing((Token t) -> t.counter))
                .collect(Collectors.toList());
    }

    public synchronized Token servingAt(ServiceType s, String counter) {
        for (Token t : servingList(s)) if (t.counter.equals(counter)) return t;
        return null;
    }

    /** Today's tokens of a service, ordered for the staff table. */
    public synchronized List<Token> queueView(ServiceType s) {
        sync();
        List<Token> out = new ArrayList<>(servingList(s));
        out.addAll(waitingList(s));
        tokens.stream()
                .filter(t -> t.date.equals(today()) && t.service == s && !t.isActive())
                .sorted(Comparator.comparingInt((Token t) -> t.number))
                .forEach(out::add);
        return out;
    }

    public synchronized List<Token> tokensOfStudent(String studentId) {
        sync();
        return tokens.stream()
                .filter(t -> t.studentId.equalsIgnoreCase(studentId))
                .sorted(Comparator.comparing((Token t) -> t.issuedAt).reversed())
                .collect(Collectors.toList());
    }

    /** 0 = being served now, 1 = next in line, ... , -1 = not in the queue. */
    public synchronized int position(Token t) {
        if (t.status == Token.Status.SERVING) return 0;
        if (t.status != Token.Status.WAITING) return -1;
        List<Token> w = waitingList(t.service);
        for (int i = 0; i < w.size(); i++)
            if (w.get(i).number == t.number) return i + 1;
        return -1;
    }

    /** Smoothed average service time: blends the default time with today's real service times. */
    public synchronized double avgServiceMinutes(ServiceType s) {
        sync();
        double sum = 0;
        int n = 0;
        for (Token t : tokens)
            if (t.date.equals(today()) && t.service == s && t.status == Token.Status.COMPLETED
                    && !t.calledAt.isEmpty() && !t.completedAt.isEmpty()) {
                sum += minutesBetween(t.calledAt, t.completedAt);
                n++;
            }
        return (s.defaultMinutes * PRIOR_WEIGHT + sum) / (PRIOR_WEIGHT + n);
    }

    /** Estimated waiting time in minutes for a waiting token (0 if not waiting). */
    public synchronized int estimatedWait(Token t) {
        int pos = position(t);
        if (pos <= 0) return 0;
        double perToken = avgServiceMinutes(t.service) / t.service.counters;
        return (int) Math.ceil((pos - 1) * perToken + perToken / 2.0);
    }

    /** Estimated wait for a student who would join this queue right now. */
    public synchronized int estimatedWaitIfJoined(ServiceType s) {
        int ahead = waitingList(s).size();
        double perToken = avgServiceMinutes(s) / s.counters;
        return (int) Math.ceil(ahead * perToken + perToken / 2.0);
    }

    public synchronized Stats stats(ServiceType s) {
        sync();
        Stats st = new Stats();
        double waitSum = 0;
        int waitN = 0;
        for (Token t : tokens) {
            if (!t.date.equals(today()) || (s != null && t.service != s)) continue;
            st.total++;
            switch (t.status) {
                case WAITING: st.waiting++; break;
                case SERVING: st.serving++; break;
                case COMPLETED: st.completed++; break;
                case SKIPPED: st.skipped++; break;
                case CANCELLED: st.cancelled++; break;
            }
            if (!t.calledAt.isEmpty()) {
                waitSum += minutesBetween(t.issuedAt, t.calledAt);
                waitN++;
            }
        }
        st.avgWaitMin = waitN == 0 ? 0 : waitSum / waitN;
        if (s != null) st.avgServiceMin = avgServiceMinutes(s);
        return st;
    }

    // ------------------------------------------------------- staff operations
    public synchronized Token callNext(ServiceType s, String counter) {
        sync();
        if (servingAt(s, counter) != null)
            throw new IllegalStateException(counter + " is already serving " + servingAt(s, counter).label()
                    + ". Complete or skip it first.");
        List<Token> w = waitingList(s);
        if (w.isEmpty()) throw new IllegalStateException("No one is waiting in the " + s.label + " queue.");
        Token t = w.get(0);
        t.status = Token.Status.SERVING;
        t.counter = counter;
        t.calledAt = now();
        saveTokens();
        return t;
    }

    public synchronized void complete(Token t) {
        sync();
        if (t.status != Token.Status.SERVING) throw new IllegalStateException("Only a token being served can be completed.");
        t.status = Token.Status.COMPLETED;
        t.completedAt = now();
        saveTokens();
    }

    /** Student did not come to the counter (no-show). */
    public synchronized void skip(Token t) {
        sync();
        if (t.status != Token.Status.SERVING) throw new IllegalStateException("Only a token being served can be skipped.");
        t.status = Token.Status.SKIPPED;
        t.completedAt = now();
        saveTokens();
    }

    /** Staff decide priority (after checking ID). Only a waiting token can be changed. */
    public synchronized void setPriority(Token t, boolean priority) {
        sync();
        if (t.status != Token.Status.WAITING) throw new IllegalStateException("Priority can be changed only for a waiting token.");
        t.priority = priority;
        saveTokens();
    }

    public synchronized void cancel(Token t) {
        sync();
        if (t.status != Token.Status.WAITING) throw new IllegalStateException("Only a waiting token can be cancelled.");
        t.status = Token.Status.CANCELLED;
        t.completedAt = now();
        saveTokens();
    }

    public synchronized void clearToday() {
        sync();
        tokens.removeIf(t -> t.date.equals(today()));
        saveTokens();
    }

    /** Fills the queues with sample data so the system can be demonstrated quickly. */
    public synchronized void seedDemo() {
        String[] names = {"Arun Kumar", "Divya S", "Karthik R", "Meena P", "Naveen M", "Priya D", "Rahul V",
                "Sneha K", "Vignesh T", "Yamini B", "Surya N", "Harini L", "Dinesh G", "Kavya M", "Ajay S"};
        int n = 0;
        for (ServiceType s : ServiceType.values()) {
            Token done = issueToken("DEMO", names[n++ % names.length], s, false, true);
            done.status = Token.Status.COMPLETED;
            done.counter = "Counter 1";
            LocalDateTime base = LocalDateTime.now().minusMinutes(30);
            done.issuedAt = base.format(FMT);
            done.calledAt = base.plusMinutes(8).format(FMT);
            done.completedAt = base.plusMinutes(8 + s.defaultMinutes).format(FMT);
            for (int i = 0; i < 3; i++)
                issueToken("DEMO", names[n++ % names.length], s, s == ServiceType.FEES && i == 2, true);
        }
        saveTokens();
    }

    /** Multi-line description used by the detail panels. */
    public synchronized String describe(Token t) {
        int pos = position(t);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Token       : %s%n", t.label()));
        sb.append(String.format("Service     : %s%n", t.service.label));
        sb.append(String.format("Name        : %s (%s)%n", t.studentName, t.studentId));
        sb.append(String.format("Type        : %s%n", t.priority ? "PRIORITY" : "NORMAL"));
        sb.append(String.format("Status      : %s%n", t.status));
        sb.append(String.format("Counter     : %s%n", t.counter));
        sb.append(String.format("Issued at   : %s%n", t.issuedAt));
        if (!t.calledAt.isEmpty()) sb.append(String.format("Called at   : %s%n", t.calledAt));
        if (!t.completedAt.isEmpty()) sb.append(String.format("Closed at   : %s%n", t.completedAt));
        if (t.status == Token.Status.WAITING)
            sb.append(String.format("Position    : %d in queue (%d ahead)%nEst. wait   : about %d min%n",
                    pos, pos - 1, estimatedWait(t)));
        if (t.status == Token.Status.SERVING)
            sb.append(String.format("Please go to %s now.%n", t.counter));
        return sb.toString();
    }
}
