import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

/**
 * Analyze student data.
 * This includes students that have graduated, but have used the platform before graduating.
 */
public class IndexAnalyzer {
    private static final LinkedList<String> faculty = new LinkedList<>();
    private static final LinkedList<String> fakeClubs = new LinkedList<>();
    private static final ArrayList<Member> members = new ArrayList<>(); // some were placeholders
    private static final ArrayList<Member> activeStudents = new ArrayList<>(); // real members
    private static JSONObject groupIds;

    public IndexAnalyzer() throws FileNotFoundException {
        Scanner s = new Scanner(new File("index.json"));
        JSONObject object = new JSONObject(s.nextLine());

        // index faculty web pages
        s = new Scanner(new File("teachers.json"));
        JSONArray array = new JSONArray(s.nextLine());
        for (Object o: array.toList()) {
            faculty.add(((String) o).toLowerCase());
        }

        // index fake clubs for filtering
        s = new Scanner(new File("fake-clubs.json"));
        array = new JSONArray(s.nextLine());
        for (Object o: array.toList()) {
            fakeClubs.add(((String) o));
        }

        // get graduation groups
        groupIds = object.getJSONObject("group_id"); // only key in the index that's not an integer
        HashMap<String, Integer> graduationMap = new HashMap<>();
        for (String key: groupIds.keySet()) { // place all the graduating class ids+years into a map
            String value = (String) groupIds.get(key);
            if (value.matches("Class of \\d{4}.*: Cy Ranch HS") && !value.contains("Seniors")) {
                value = value.substring(value.indexOf("20"), value.indexOf(":"));
                int year = Integer.parseInt(value);
                graduationMap.put(key, year);
            }
        }

        // loop through members
        for (String id: object.keySet()) { // loop through all members
            if (id.matches("\\d+")) { // make sure if it's an actual member (just an integer)
                JSONObject info = object.getJSONObject(id);
                String name = info.getString("name");
                LinkedList<String> groups = new LinkedList<>();
                try { // some may have group display on private settings
                    for (Object o: info.getJSONArray("groups").toList()) {
                        groups.add((String) o);
                    }
                }
                catch (Exception ignored) {
                    //System.out.println(name + " (" + id + ") has private groups on.");
                }
                String email = "";
                try { // some may not have an email
                    email = info.getString("email");
                }
                catch (Exception ignored) {}
                Member member = new Member(id, name, groups, email);
                members.add(member);
                // extra stuff for ease of computing data
                // determine the graduation year by seeing if the graduation year id is in it
                int year = 0;
                for (String yearId: graduationMap.keySet()) {
                    if (groups.contains(yearId)) { // groups is json array
                        if (year == 0) { // determine if this is the first time setting the year
                            year = graduationMap.get(yearId);
                        }
                        else { // special case where stupid admins gave 2 graduating classes
                            year = Integer.MAX_VALUE;
                            //System.out.println(name + " (" + id + ") is in multiple graduating classes.");
                            break;
                        }
                    }
                }
                member.setGrad(year); // year of 0 means not a cy ranch student
                // determining the number of clubs someone is in. one is announcements group
                // seniors of 2021 will likely not have been in any clubs
                if (groups.size() > 0) {
                    member.setClubs(getRealClubs(groups));
                }
                else { // people with private settings are in 0 groups
                    member.setClubs(0);
                }
                // by default, members are 'active'
                // set one as inactive if graduating year is 0 (not at the school, or class of 2020)
                // there is no class of 2020 group
                // MAX_VALUE doesn't have an effect
                if (year != 0) {
                    activeStudents.add(member); // will be further pruned
                }
                //else: System.out.println(name + " (" + id + ") does not go to Cy Ranch.");
            }
        }
    }

    /**
     * Further prunes faculty from students by using faculty page checks and last names.
     */
    public static void removeFaculty() {
        activeStudents.removeIf(member -> {
            // two main checks: if the name is in the faculty pages, or if they have a good email
            String name = member.getName().toLowerCase();
            if (faculty.contains(name)) {
                return true;
            }
            /*
            some members have a @cfisd.net email, but it's invalid.
            people can set invalid emails, so it needs to be checked.
            this regex is just an okay check; it suffices for my data.
            trust it because there are a lot of bad names.
            check further if the surname has all caps. regular students can't set that
             */
            if (member.getEmail().matches(".*[^1-9]@cfisd.net")) {
                return member.getName().split(" +")[1].matches("^[^a-z]*$");
            }
            return false; // normal member, don't prune
        });
    }

    /**
     * Format groups of the student that is in the most clubs, so it can be determined which clubs
     * should be put towards his club count.
     *
     * @param groups the groups one is in
     * @return a formatted output of the groups one is in
     */
    private static String formatGroups(LinkedList<String> groups) {
        StringBuilder output = new StringBuilder();
        for (String s: groups) {
            if (fakeClubs.contains(s)) {
                output.append("(excluded) ");
            }
            if (s.matches("\\d+")) { // if group is a number, lookup the id
                output.append(s).append(" - ").append(groupIds.get(s)).append("\n");
            }
            else { // just add the group name
                output.append(s).append("\n");
            }
        }
        return output.substring(0, output.length() - 1); // rid of last newline
    }

    /**
     * This does club count analysis after sorting by club counts.
     */
    public static void analyzeMemberClubs() {
        int clubs = activeStudents.get(0).getClubs();
        System.out.println("The most clubs a member is in is " + clubs + " clubs.");
        System.out.println("The following are the students in the most clubs (that have groups):");
        for (Member student: activeStudents) { // get ALL the students with the highest club count
            if (student.getClubs() == clubs) {
                System.out.println(student);
            }
            else {
                break;
            }
        }
        clubCountsToCSV();
    }

    /**
     * Writes member club counts to a csv file.
     */
    private static void clubCountsToCSV() {
        StringBuilder output = new StringBuilder();
        for (Member student: activeStudents) {
            output.append(student.getName()).append(",").append(student.getClubs()).append("\n");
        }
        try {
            FileWriter writer = new FileWriter("member-club-counts.csv");
            writer.write(output.toString());
            writer.close();
            System.out.println("Club counts have been written to member-club-counts.csv");
        }
        catch (Exception ignored) {}
    }

    /**
     * This does group analysis with the amount of members in each.
     */
    public static void analyzeGroupMembership() {
        HashMap<String, LinkedList<Member>> map = new HashMap<>();
        // use hashmap for quick lookup and indexing, then create group objects
        for (Member student: activeStudents) {
            for (String group: student.getGroups()) {
                LinkedList<Member> students;
                if (map.containsKey(group)) {
                    students = map.get(group);
                }
                else {
                    students = new LinkedList<>();
                }
                students.add(student);
                map.put(group, students);
            }
        }
        LinkedList<Group> groups = new LinkedList<>();
        for (String id: map.keySet()) {
            Group g = new Group(id, map.get(id));
            if (id.matches("\\d+")) { // if group is a number, lookup the id
                g.setName((String) groupIds.get(id));
            }
            groups.add(g);
        }

        groups.sort((group, t1) -> { // reverse order, higher member count first
            return Integer.compare(t1.getMemberCount(), group.getMemberCount());
        });

        System.out.println("The top group is " + groups.get(0) + " members");
        groupCountsToCSV(groups);
        writeGroups(groups);
    }

    /**
     * Writes the club counts to a csv file.
     */
    private static void groupCountsToCSV(LinkedList<Group> groups) {
        StringBuilder output = new StringBuilder();
        for (Group group: groups) {
            output.append(group.toString()).append("\n");
        }
        try {
            FileWriter writer = new FileWriter("membership-data.csv");
            writer.write(output.toString());
            writer.close();
            System.out.println("Group counts have been written to membership-data.csv");
        }
        catch (Exception ignored) {}
    }

    /**
     * Log all members that belong to groups.
     *
     * @param groups groups, that contain members
     */
    private static void writeGroups(LinkedList<Group> groups) {
        StringBuilder output = new StringBuilder();
        for (int i = 6; i < groups.size(); i++) { // first 5 consist of graduation + announcements
            output.append(groups.get(i).toString());
            if (groups.get(i).getMemberCount() > 1) {
                output.append(" members");
            }
            else {
                output.append(" member");
            }
            output.append("\n\n");
            for (Member student: groups.get(i).getStudents()) {
                output.append(student.getName()).append("\n");
            }
            output.append("\n");
        }
        try {
            FileWriter writer = new FileWriter("groups.txt");
            writer.write(output.toString());
            writer.close();
            System.out.println("All group data has been written to groups.txt");
        }
        catch (Exception ignored) {}
    }

    /**
     * Writes the json into a readable text file.
     */
    private static void writeDatabase() {
        StringBuilder output = new StringBuilder();
        for (Member member: activeStudents) { // before sorting
            output.append(member.getName()).append("\n");
            for (String s: member.getGroups()) {
                if (s.matches("\\d+")) {
                    output.append(groupIds.get(s));
                }
                else {
                    output.append(s);
                }
                output.append("\n");
            }
            output.append("\n");
        }
        try {
            FileWriter writer = new FileWriter("data.txt");
            writer.write(output.toString());
            writer.close();
            System.out.println("All data has been written to data.txt");
        }
        catch (Exception ignored) {}
    }

    public static void main(String[] args) throws FileNotFoundException {
        new IndexAnalyzer();
        System.out.println("There are " + members.size() + " in the announcements group.");
        removeFaculty();
        System.out.println("There are " + activeStudents.size() + " students.");
        writeDatabase();
        activeStudents.sort((member, t1) -> { // reverse order, higher amount of clubs first
            return Integer.compare(t1.getClubs(), member.getClubs());
        });
        System.out.println();
        analyzeMemberClubs();
        System.out.println();
        analyzeGroupMembership();
    }

    /**
     * Gets the number of clubs not included in the 'fake clubs' blacklist.
     *
     * @param groups the groups one is in
     * @return the number of real clubs
     */
    public int getRealClubs(LinkedList<String> groups) {
        LinkedList<String> temp = new LinkedList<>(groups);
        try {
            temp.removeIf(fakeClubs::contains);
        }
        catch (Exception ignored) {}
        return temp.size();
    }
}
