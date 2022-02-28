import java.util.LinkedList;

/**
 * Represents a group that students may be in.
 */
public class Group {
    private final String id;
    private final LinkedList<Member> students;
    private String name;

    public Group(String s, LinkedList<Member> list) {
        id = s;
        students = list;
        name = "";
    }

    /**
     * Sets the name of the group, if it has an id.
     * @param name provided name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the list of students in the group, for writing
     */
    public LinkedList<Member> getStudents() {
        return students;
    }

    /**
     * @return the amount of members in the group, for sorting
     */
    public int getMemberCount() {
        return students.size();
    }

    @Override
    public String toString() {
        return (name.equals("") ? id : name) + ", " + students.size();
    }
}
