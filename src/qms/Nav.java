package qms;

/** Navigation callbacks between the screens. */
public interface Nav {
    void showStudent(User u);
    void showStaff(User u);
    void openBoard();
    void logout();
}
