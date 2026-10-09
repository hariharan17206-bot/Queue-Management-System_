package qms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access layer. Users and tokens are stored in plain text files so the
 * data survives restarts. This class can be replaced by a JDBC / MySQL version
 * without touching the rest of the application.
 */
public class DataStore {
    private final Path usersFile;
    private final Path tokensFile;

    public DataStore(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create data folder: " + e.getMessage(), e);
        }
        usersFile = dir.resolve("users.txt");
        tokensFile = dir.resolve("tokens.txt");
    }

    public List<User> loadUsers() {
        List<User> list = new ArrayList<>();
        for (String line : readLines(usersFile)) list.add(User.fromRecord(line));
        return list;
    }

    public void saveUsers(List<User> users) {
        List<String> lines = new ArrayList<>();
        for (User u : users) lines.add(u.toRecord());
        writeLines(usersFile, lines);
    }

    public List<Token> loadTokens() {
        List<Token> list = new ArrayList<>();
        for (String line : readLines(tokensFile)) list.add(Token.fromRecord(line));
        return list;
    }

    public void saveTokens(List<Token> tokens) {
        List<String> lines = new ArrayList<>();
        for (Token t : tokens) lines.add(t.toRecord());
        writeLines(tokensFile, lines);
    }

    public long usersModified() { return modified(usersFile); }

    public long tokensModified() { return modified(tokensFile); }

    private long modified(Path p) {
        try {
            return Files.exists(p) ? Files.getLastModifiedTime(p).toMillis() : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }

    private List<String> readLines(Path p) {
        List<String> out = new ArrayList<>();
        if (!Files.exists(p)) return out;
        try {
            for (String s : Files.readAllLines(p, StandardCharsets.UTF_8))
                if (!s.isBlank()) out.add(s);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + p + ": " + e.getMessage(), e);
        }
        return out;
    }

    private void writeLines(Path p, List<String> lines) {
        try {
            Files.write(p, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write " + p + ": " + e.getMessage(), e);
        }
    }
}
