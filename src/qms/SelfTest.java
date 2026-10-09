package qms;

import java.nio.file.Files;
import java.nio.file.Path;

/** Console self-test of the queue logic (no GUI). Run it from Eclipse as a Java Application. */
public class SelfTest {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) passed++; else failed++;
    }

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("qms-test");
        QueueService q = new QueueService(new DataStore(tmp));

        check("admin login works", q.login("admin", "admin123") != null);
        check("wrong password rejected", q.login("admin", "wrong") == null);
        q.registerStudent("S1", "Student One", "pass1");
        check("student can register and log in", q.login("S1", "pass1") != null);
        try { q.registerStudent("S1", "Dup", "pass1"); check("duplicate register rejected", false); }
        catch (IllegalArgumentException e) { check("duplicate register rejected", true); }

        Token a = q.issueToken("S1", "Student One", ServiceType.LIBRARY, false, false);
        check("first token is L-001", a.label().equals("L-001"));
        Token b = q.issueWalkIn("Visitor", ServiceType.LIBRARY, false);
        Token c = q.issueWalkIn("Senior Citizen", ServiceType.LIBRARY, false);
        q.setPriority(c, true);   // staff decide priority after checking ID
        check("numbers increase (L-003)", c.label().equals("L-003"));
        check("staff can set priority", c.priority);
        check("priority token goes first", q.position(c) == 1 && q.position(a) == 2 && q.position(b) == 3);
        check("estimated wait grows with position", q.estimatedWait(b) > q.estimatedWait(c));
        try { q.issueToken("S1", "Student One", ServiceType.LIBRARY, false, false); check("one active token per service", false); }
        catch (IllegalStateException e) { check("one active token per service", true); }
        check("other service starts at 001", q.issueToken("S1", "Student One", ServiceType.FEES, false, false).label().equals("F-001"));

        Token called = q.callNext(ServiceType.LIBRARY, "Counter 1");
        check("callNext serves the priority token", called == c && c.status == Token.Status.SERVING);
        try { q.callNext(ServiceType.LIBRARY, "Counter 1"); check("busy counter blocked", false); }
        catch (IllegalStateException e) { check("busy counter blocked", true); }
        Token second = q.callNext(ServiceType.LIBRARY, "Counter 2");
        check("second counter serves next in order", second == a);
        q.complete(c);
        check("complete works", c.status == Token.Status.COMPLETED);
        q.skip(a);
        check("skip works", a.status == Token.Status.SKIPPED);
        q.cancel(b);
        check("cancel works", b.status == Token.Status.CANCELLED);

        QueueService.Stats st = q.stats(ServiceType.LIBRARY);
        check("stats are correct", st.total == 3 && st.completed == 1 && st.skipped == 1 && st.cancelled == 1);

        QueueService q2 = new QueueService(new DataStore(tmp));
        check("data persists after restart", q2.tokensOfStudent("S1").size() == 2);

        q.seedDemo();
        check("demo data added", q.stats(null).total > 10);
        q.clearToday();
        check("clear today works", q.stats(null).total == 0);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
