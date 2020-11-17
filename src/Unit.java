import java.util.*;
import java.util.ArrayList;

/**
 * Unit class represents an instance of a Unit and all relevant scheduling data.
 * This includes datascope, unitcode, unit level, credit points, pre and co requisites, offering times, NCCW
 */
public class Unit {

    public enum DataScope {Undergrad, Postgrad}
    public enum UnitType  {PACE, NA}

    DataScope dataScope;
    String unitCode;
    int level;
    int creditPoints;
    UnitType unitType;
    Requisite prerequisite;
    Requisite corequisite;
    List<String> NCCW;
    boolean[] whenOffered;
    String department;
    ArrayList<Unit> cotaught;
    boolean captsone;
    boolean legacyUnit = false;
    ArrayList<Unit> requisiteList;

    public Unit(DataScope dataScope, String unitCode, int creditPoints, UnitType unitType, List<String> NCCW, boolean[] whenOffered) {
        this.dataScope = dataScope;
        this.unitCode = unitCode;
        this.creditPoints = creditPoints;
        this.unitType = unitType;
        this.NCCW = NCCW;
        this.whenOffered = whenOffered;
        try {
            level = Integer.parseInt(unitCode.substring(4));
        } catch(Exception e) {
            level = -1;
        }
    }

    public Unit(String code) {
        this.unitCode = code;
        legacyUnit = true;
        try {
            level = Integer.parseInt(code.substring(4));
        } catch(Exception e) {
            level = -1;
        }
    }

    public Unit() {}


    public void addPrerequisite(Requisite requisite) {
        this.prerequisite = requisite;;
    }

    public void setPrerequisite(Requisite prerequisite) {
        this.prerequisite = prerequisite;
    }

    public void setCorequisite(Requisite corequisite) {
        this.corequisite = corequisite;
    }

    public void setRequisiteList(ArrayList<Unit> requisiteList) {
        this.requisiteList = requisiteList;
    }

    public void addCorequisite(Requisite requisite) { this.corequisite = requisite; }

    public Requisite getCorequisite() { return corequisite; }

    public ArrayList<Unit> getRequisiteList() { return requisiteList;}

    public Requisite getPrerequisite() { return prerequisite; }

    public boolean[] getWhenOffered() { return whenOffered; }

    public int getLevel() { return level; }

    public int getCreditPoints() { return creditPoints; }

    public boolean getLegacyUnit() { return legacyUnit; }
    public String getUnitCode() { return unitCode; }
}
