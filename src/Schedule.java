import java.util.ArrayList;

/**
 * Result to be returned from the Scheduler.run() method
 * Contains a list of Sessions, which each refer to a block of the schedule.
 */
public class Schedule {
    ArrayList<Session> sessions;

    public Schedule() {
        sessions = new ArrayList<>();
    }

    public void add(ArrayList<Unit> units, int session) {
        sessions.add(new Session(session, units));
    }

    public void add(Session session) {
        sessions.add(session);
    }

    public void print() {
        for(Session session : sessions) {
            session.print();
        }
    }


}
