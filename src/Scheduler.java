import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * There are three sections to the Scheduler class.
 * 1. Building unit priorities through critical paths
 * 2. Building unit priorities through the number of prerequisite occurences
 * 3. Scheduling the remaining curriculum based on built priorities
 *
 * We have the option of selecting either option 1 or 2 to build these priorities
 * at which point the scheduler is run to produce an output.
 * Either buildCriticalPaths() or requisiteCount() can be used to call options 1 or 2 respectively.
 * Both options will perform scheduling after priorities have been assigned.
 */

public class Scheduler {

    static Schedule schedule = new Schedule(); //The resulting schedule to be built.

    static ArrayList<Unit> unitCurriculum = new ArrayList<>(); //Curriculum of units to be completed
    static ArrayList<Unit> preCompletedUnits = new ArrayList<>(); //Units completed at the time of schedule
    static ArrayList<Unit> completedUnits = new ArrayList<>(); //To be filled with units as they are scheduled
    static ArrayList<Unit> pendingUnits = new ArrayList<>(); //Units pending to be scheduled
    static ArrayList<Integer> priorities = new ArrayList<>(); //Priorities for each unit
    static ArrayList<ArrayList<Unit>> criticalPath = new ArrayList<>(); //Critical paths of each unit
    static String courseName = "BEng(Hons)";

    static int cpPerSession = 0;
    static int unitsPerSession = 0;
    static int session = 1;
    static int attempt = 0;
    static int emptyAvailability;
    static int timeToComplete;
    static boolean log;

    enum PriorityApproach { CriticalPath, RequisiteCount }


    /**
     *  Parent method to create a schedule. Should be called from Main.java or elsewhere.
     *      * transcript: String containing units currently completed by a student, separated by commas.
     *      * curriculum: String containing the units to be completed, separated by commas.
     *      * start session:  1 or 2
     *      * schedule approach: Enumerated type, either CriticalPath or RequisiteCount
     *      * logs flag: true to display scheduling logs at each iteration.
     *  Returns: resulting schedule.
     */

    public static Schedule run(String transcript, String curriculum, int startSession, PriorityApproach approach, Boolean printLogs) {
        initialiseInputs(transcript, curriculum);
        log = printLogs;
        session = startSession;
        if(session > 2 || session < 1)
            session = 1;
        if(approach.equals(PriorityApproach.CriticalPath))
            buildCriticalPaths();
        else if(approach.equals(PriorityApproach.RequisiteCount))
            requisiteCount();

        if(log)
            printScheduleAnalysis();

        return schedule;
    }

    /**
     * SECTION 1: Building unit priorities through critical paths
     */
    //Calculates the priority of each unit based on a critcal path approach.
    //Populates the "priorities" list.
    //This method finds the unit with the highest unit code in the curriculum, builds all critical paths that can occur from this unit
    //This is repeated for the next highest unit code, until every unit in the curriculum exists within a critical path.
    private static void buildCriticalPaths() {
        ArrayList<Unit> curriculumCopy = new ArrayList<>(unitCurriculum);

       while(!curriculumCopy.isEmpty()) {
            Unit highest = null;
            for (Unit u : curriculumCopy) { //Get the unit with the highest code
                if (highest == null)
                    highest = u;
                else if (u.getLevel() > highest.getLevel())
                    highest = u;
            }
            criticalPath.addAll(getCriticalPath(highest));
            for(ArrayList<Unit> cpath : criticalPath)
                curriculumCopy.removeAll(cpath);
       }

        //Apply weighting from critical paths to the "priorities" array.
        // As each unit appears in atleast one critical path, we can take the *deepest* value for each unit, and apply this depth to the priority.
       for(Unit unit : unitCurriculum) {
           int highest = 0;
           for(ArrayList<Unit> cpath : criticalPath) {
               for(Unit u : cpath) {
                   if(u.equals(unit) && cpath.indexOf(u) > highest)
                       highest = cpath.indexOf(u);
               }
           }
           priorities.add(highest);
       }
       if(log)
         printCriticalPaths();
       sortByPriority();
       schedule();
    }

    //Recursively builds a list of all critical paths that exist within the remaining curriculum.
    //Returns a list of lists representing the critical path of all the given unit and all of its requisites within the curriculum.
    private static ArrayList<ArrayList<Unit>> getCriticalPath (Unit unit){
        ArrayList<ArrayList<Unit>> result = new ArrayList<>();
        ArrayList<Unit> path = new ArrayList<>();
        if(unit.getRequisiteList() == null || unit.getRequisiteList().isEmpty()) {
            path.add(unit);
            result.add(path);
            return result;
        }
        for(Unit req : unit.getRequisiteList()) {
            if(unitCurriculum.contains(req)) {
                for (ArrayList<Unit> sublist : getCriticalPath(req)) {
                    path = new ArrayList<>();
                    path.add(unit);
                    path.addAll(sublist);
                    result.add(path);
                }
            }
        }
        if(result.isEmpty()) {
            path.add(unit);
            result.add(path);
        }

        return result;
    }

    //Prints all critical paths of the stored curriculum
    public static void printCriticalPaths() {
        System.out.println("CRITICAL PATHS:");
        int i = 0;
        for(ArrayList<Unit> a : criticalPath) {
            System.out.print(i++ + ": ");
            for(Unit u : a) {
                System.out.print(u.getUnitCode()+ " ");
            }
            System.out.println();
        }
    }

    /**
     * SECTION 2: Building unit priorities through the number of prerequisite occurrences.
     */

    //Main method used for the priority approach to scheduling
    //Finds the priority of each unit using helper methods within the section
    private static void requisiteCount() {
        for(Unit u : unitCurriculum) {
            priorities.add(getPriority(u));
        }
        cascadePriorities();
        sortByPriority();
        schedule();
    }

    private static void sortByPriority() {
        ArrayList<Unit> copy = new ArrayList<>(unitCurriculum);
        unitCurriculum = new ArrayList<>();
        int bound = Collections.max(priorities);
        if(log) System.out.println("UNIT PRIORITIES:");
        while(bound >= 0) {
            for(int i = 0; i < priorities.size(); i++) {
                if (priorities.get(i) == bound) {
                    unitCurriculum.add(copy.get(i));
                    if(log) System.out.println(copy.get(i).getUnitCode() + ": " + priorities.get(i));
                }
            }
            bound--;
        }
    }


    //Performs priority inversion on related units within a curriculum. Results in a more accurate priority.
    private static void cascadePriorities() {
        ArrayList<Integer> copyPriorities = new ArrayList<>(priorities); //Holds the unmodified priority for each unit

        for(int primary = 0; primary < unitCurriculum.size(); primary++) {
            for(int secondary = 0; secondary < unitCurriculum.size(); secondary++) {
                if(primary == secondary)
                    continue;
                Requisite prerequisite = unitCurriculum.get(secondary).getPrerequisite();
                if(cascadePrioritiesHelper(prerequisite, unitCurriculum.get(primary))) {
                    int newPriority = priorities.get(primary) + copyPriorities.get(secondary);
                    priorities.set(primary, newPriority);
                }

            }
        }
    }

    //Returns true if the the prerequisite contains the unit code
    private static boolean cascadePrioritiesHelper(Requisite prerequisite, Unit unit) {
        if(prerequisite == null)
            return false;
        switch (prerequisite.getRequisiteType()) {
            case UNIT:
                if(prerequisite.getUnit().equals(unit))
                    return true;
                break;
            case Sublist:
                for(Requisite r : prerequisite.getSublist()) {
                    if(cascadePrioritiesHelper(r, unit))
                        return true;
                }
        }
        return false;
    }

    //Returns the number of times the given unit appears in the prerequisites of a curriculums units
    private static int getPriority(Unit u) {
        int priority = 0;
        for(Unit unit : unitCurriculum) {
            Requisite current = unit.getPrerequisite();
            if(current == null)
                continue;
            switch (current.getRequisiteType()) {
                case UNIT:
                    if(current.getUnit().equals(u))
                        priority++;
                    break;
                case Sublist:
                    for(Requisite r : current.getSublist()) {
                        priority += getPriorityHelper(u, r);
                    }
            }
        }
        return priority;
    }

    //Helper method for getPriority(), allows for easier traversal of the requisite sublist structure.
    private static int getPriorityHelper(Unit u, Requisite r) {
        int priority = 0;
        switch (r.getRequisiteType()) {
            case UNIT:
                if(r.getUnit().equals(u))
                    priority++;
                break;
            case Sublist:
                for(Requisite req : r.getSublist()) {
                    priority += getPriorityHelper(u, req);
                }
        }

        return priority;
    }

    /**
     * SECTION 3: Scheduling the remaining curriculum based on built priorities
     */

    //Converts the string inputs that make up a transcript and course, to their relative objects.
    private static void initialiseInputs(String transcript, String curriculum) {
        ArrayList<String> t = new ArrayList<>(Arrays.asList(transcript.split(",")));
        ArrayList<String> c = new ArrayList<>(Arrays.asList(curriculum.split(","))); //Holds curriculum in string format pre conversion
        c.removeAll(t);
        for(String s : c) {
            Unit u = Main.getUnitByCode(s);
            unitCurriculum.add(u);
        }
        for(String s : t) {
            Unit u = Main.getUnitByCode(s);
            preCompletedUnits.add(u);
        }
    }

    //The main method called that performs the scheduling. Other methods in this section are helper methods of this parent method.
    //Unit's are attempted to be scheduled in the order of their priority as long as prerequisite conditions have been met.
    //For each time period (session) unit's are placed into a temporary schedule to correctly verify prerequisites.
    //Once a a session from a temporary schedule is full or cannot be added to, it is placed into the confirmed schedule.
    private static void schedule() {
        if(log) System.out.println("Attempting schedule:");
        boolean scheduled = false;
        for(Unit unit : unitCurriculum) {
            if(completedUnits.contains(unit) || pendingUnits.contains(unit) || preCompletedUnits.contains(unit)) //If the unit has been completed/scheduled
                continue;

            Requisite prereq = unit.getPrerequisite();

            if(prereq == null || verifyPrerequisites(prereq)) { //No prerequisites exist / Prerequisites have been met
                if(unit.getWhenOffered()[session - 1] == true || allFalse(unit.getWhenOffered())) { //The unit is offered in the session we are attempting to schedule
                    scheduled = true;
                    attempt = 0;
                    pendingUnits.add(unit);
                    cpPerSession += unit.getCreditPoints();
                    unitsPerSession += 1;
                    if(log) System.out.println("Scheduling: " + unit.getUnitCode() + " in S" + session + ". Session CP: " + cpPerSession + ", Session Unit(s): " + unitsPerSession);
                }
                if(cpPerSession >= 40 || unitsPerSession >= 4) {
                    if(session == 1)
                        session = 2;
                    else session = 1;
                    cpPerSession = 0;
                    unitsPerSession = 0;
                    schedulePending();
                }

            } else { //Prerequisites not met - Ignore and retry with new unit. Logic is handled here through recursive calls of the schedule() method. Most likely won't add anything here.

            }
        }
        unitCurriculum.removeAll(completedUnits);

        if(log) {
            schedule.print();
            printRemainingCurriculum();
        }

        if(!unitCurriculum.isEmpty() && scheduled) //Progress has been made this iteration, schedule again
            schedule();
        else if (attempt == 0) { //No progress made, change session and retry
            schedulePending();
            if(session == 1)
                session = 2;
            else session = 1;
            cpPerSession = 0;
            unitsPerSession = 0;
            attempt = 1;
            schedule();
        } else if(!unitCurriculum.isEmpty() && log) { //No progress made over two sessions, cannot schedule.
            System.out.println("Unable to Schedule: "+unitCurriculum);
        }
    }

    //Returns true if the given Requisite has been met based on completed and assumed to be completed units.
    //Returns false otherwise.
    private static boolean verifyPrerequisites(Requisite head) {
        int count;
        switch (head.getRequisiteType()) {
            case UNIT:
               if (completedUnits.contains(head.getUnit()) || preCompletedUnits.contains(head.getUnit()))
                    return true;
                break;
            case Sublist:
                if (head.getExpr() == Requisite.Expr.OR) { //For each sub requisite, return true if any are true
                    for(Requisite r : head.getSublist()) {
                        if(verifyPrerequisites(r))
                            return true;
                    }
                } else if (head.getExpr() == Requisite.Expr.AND) { //For each sub requisite, return false if ANY are false
                    for (Requisite r : head.getSublist()) {
                        if(!verifyPrerequisites(r))
                            return false;
                    }
                    return true;
                }
                break;
            case CPAtLevel:
                int level = head.getCreditPointRequirement().level;
                int creditPoints = head.getCreditPointRequirement().creditPoints;
                count = 0;
                for(Unit unit : completedUnits) {
                    int i = Integer.parseInt(unit.getUnitCode().substring(4));
                    if(i >= level)
                        count += unit.getCreditPoints();
                }
                for(Unit unit : preCompletedUnits) {
                    int i = Integer.parseInt(unit.getUnitCode().substring(4));
                    if(i >= level)
                        count += unit.getCreditPoints();
                }
                if(count >= creditPoints)
                    return true;
                return false;
            case CPFromUnits:
                return true;
            case CPFromUnitGroup:
                return true;
            case CPFromUnitGroupAtLevel:
                return true;
            case CP:
                count = 0;
                for(Unit unit : completedUnits) {
                    count += unit.getCreditPoints();
                }
                if(count >= head.getCreditPointRequirement().creditPoints)
                    return true;
                return false;
            case UnitWithGrade:
                return true;
            case Permission:
               return true;
            case Admission:
                if(head.getAdmission().getDegree().equals(courseName))
                    return true;
                return false;

        }

        return false;
    }


    //Returns true if a unit has no offerings.
    //Due to Macquaries new curriculum, multiple late classes have no offering dates as no students have made it that far in curriculum.
    // In this case we will assume both offering dates to be available so a realist schedule can be formed.
    private static boolean allFalse(boolean[] whenOffered) {
        for(boolean b : whenOffered) {
            if (b == true)
                return false;
        }
        return true;
    }

    //Schedules units in the pending schedule to the confirmed schedule.
    private static void schedulePending() {
        int ea = 40; //empty availability begins at 40cp (max per session)
        for(Unit unit : pendingUnits) {
            completedUnits.add(unit);
            ea -= unit.getCreditPoints();
        }
        emptyAvailability += ea;
        timeToComplete++;
        schedule.add(new Session(session, pendingUnits));
        pendingUnits = new ArrayList<>();
    }

    public static void printScheduleAnalysis() {
        float cp = 0;
        for(Unit unit : completedUnits) {
            cp += unit.getCreditPoints();
        }
        System.out.println("Unscaled Empty Availability: "+ emptyAvailability);
        System.out.println("Total Credit Points: "+(int)cp);
        System.out.println("Empty Availability: "+emptyAvailability / cp);
        System.out.println("Time to Complete: "+timeToComplete+" sessions.");
    }

    private static void printRemainingCurriculum() {
        String s = "Remaining curriculum: ";
        for(Unit u : unitCurriculum)
            s+= u.getUnitCode() + ", ";
        System.out.println(s + "\n");

    }
}
