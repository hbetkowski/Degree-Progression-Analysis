import java.util.ArrayList;

public class Main {
    static ArrayList<Unit> unitList;

    /**
     * The main method controlling how the program runs.
     * First populates the unit list using the ExcelReader class then schedules based on provided parameters.
     *
     * Scheduler.java is the static class that performs the scheduling and returns an instance of a Schedule.java
     * A Schedule.java contains a list of Session.java, containing a session number and list of units. This is the resulting structure.
     *
     * Providing Scheduler.run() with a transcript, degree(curriculum), a start session, a schedule approach and a logs flag will return the resulting schedule.
     * transcript: String containing units currently completed by a student, separated by commas.
     * curriculum: String containing the units to be completed, separated by commas.
     * start session:  1 or 2
     * schedule approach: Enumerated type, either CriticalPath or RequisiteCount
     * logs flag: true to display scheduling logs at each iteration.
     *
     * schedule.print() can be used to display the final resulting schedule. Required to see result if logs flag is false.
     *
     */
    public static void main(String[] args) {
        unitList = ExcelReader.read(); //Read raw data

        Schedule schedule;
        String transcript = "COMP1000,ENGG1000,COMP1050,COMP1300";
        String curriculum = "ENGG1000,ENGG1050,ENGG2000,ENGG2050,ENGG3000,ENGG3050,ENGG4099,ENGG4001,COMP1000,COMP1010,COMP1050,COMP1300,COMP1350,MATH1007," +
                "COMP2000,COMP2010,COMP2050,COMP2100,COMP2250,MATH2907,COMP3000,COMP3010,COMP3100,COMP4000,COMP4050,COMP4060,COMP4092,COMP4093";
        int startSession = 2;

        try {
            schedule = Scheduler.run(transcript, curriculum, startSession, Scheduler.PriorityApproach.RequisiteCount, false);
            schedule.print();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Returns a Unit object relating to the given unit code
    public static Unit getUnitByCode(String code) {
        for(Unit unit : unitList) {
            if (unit.getUnitCode().equals(code))
                return unit;
        }
        System.out.println("couldnt find unit: "+code);
        Unit u = new Unit(code);
        unitList.add(u);
        return u;
    }

    //Prints all stored unit info to the console - was used to check parse results
    public static void printUnitInfo() {
        for(Unit u : unitList) {
            if(u.getUnitCode().startsWith("ENGG") || u.getUnitCode().startsWith("COMP")) {
                System.out.println("UNIT:" + u.getUnitCode());
                if (!u.getLegacyUnit()) {
                    try {
                        System.out.print("Prereqs: ");
                        if (u.getPrerequisite() != null)
                            System.out.println(u.getPrerequisite().printString());
                        else System.out.println();
                        System.out.print("Coreqs: ");
                        if (u.getCorequisite() != null)
                            System.out.println(u.getCorequisite().printString());
                        else System.out.println();
                        System.out.print("Offered: ");
                        boolean[] w = u.getWhenOffered();
                        for (int k = 0; k < w.length; k++)
                            if (w[k]) System.out.print("S" + (k + 1) + " ");
                        System.out.println();
                    } catch (Exception e) {
                        System.out.println("Error getting requisites");
                    }
                } else {
                    System.out.println("Legacy unit");
                }
                System.out.println("----");
            }
        }
    }
}
