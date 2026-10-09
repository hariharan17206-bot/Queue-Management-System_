package qms;

/** A registered user: either a STUDENT or a STAFF (admin) member. */
public class User {
    public enum Role { STUDENT, STAFF }

    public String id;
    public String name;
    public String passwordHash;
    public Role role;

    public User(String id, String name, String passwordHash, Role role) {
        this.id = id;
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String toRecord() {
        return id + "|" + name + "|" + passwordHash + "|" + role;
    }

    public static User fromRecord(String line) {
        String[] p = line.split("\\|", -1);
        return new User(p[0], p[1], p[2], Role.valueOf(p[3]));
    }
}
