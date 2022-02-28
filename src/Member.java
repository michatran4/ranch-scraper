import java.util.LinkedList;

/**
 * Represents a student.
 */
public class Member {
    private final String urlId, name, email;
    private final LinkedList<String> groups;
    private int grad, clubs;

    public Member(String id, String n, LinkedList<String> g, String e) {
        urlId = id;
        name = n;
        groups = g;
        email = e;
    }

    /**
     * @return the graduation year
     */
    public int getGrad() {
        return grad;
    }

    /**
     * Set the graduation year after making sure the member only belongs in one gradation group.
     * @param year the graduation year
     */
    public void setGrad(int year) {
        grad = year;
    }

    /**
     * @return the amount of real clubs one is in
     */
    public int getClubs() {
        return clubs;
    }

    /**
     * Set the amount of real clubs one is in, after filtering from fake clubs.
     * @param num the number
     */
    public void setClubs(int num) {
        clubs = num;
    }

    /**
     * @return the student's name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the student's email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the group ids or names that the student belongs to
     */
    public LinkedList<String> getGroups() {
        return groups;
    }

    @Override
    public String toString() {
        return "[" + name + ", " + urlId + "]";
    }
}
