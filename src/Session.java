import java.util.ArrayList;


/**
 * Session Class
 * Refers to an instance of the Schedule class, where each entry of a Schedule is an instance of a Session
 * Holds session number and List of units within the session
 */
public class Session {
    private int session;
    private ArrayList<Unit> units;

    public Session(int session, ArrayList<Unit> units) {
        this.session = session;
        this.units = units;
    }

    public void print() {
        System.out.print("Session "+session+": ");
        for(Unit unit : units)
            System.out.print(unit.getUnitCode() + " ");
        System.out.println();
    }
}
