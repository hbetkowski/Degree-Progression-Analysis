import java.util.ArrayList;

/**
 * Class used for pre and co requisite data
 * A requisite can be either: a unit, a credit point requirement at a certain level,
 * an admission, an OR field utilising a sublist of OR possibilities, an AND field uiliising a sublist of AND requisites
 */
public class Requisite {

    //A requisite can become one of the following data structures
    Unit unit;
    CreditPointRequirement creditPointRequirement;
    Admission admission;
    ArrayList<Requisite> sublist;
    String grade;

    //Defines which of the data structures the requisite will be
    RequisiteType requisiteType;
    Expr expr;

    //Defines the type of requisite
    //OR should read as SUBLIST - needs refactoring
    public enum RequisiteType {
        Sublist,
        UNIT,
        UnitWithGrade,
        CPAtLevel, //CP above a certain level
        CPFromUnits, //CP from a list of units
        CP, //CP only
        CPFromUnitGroup, //CP from unit group eg. ANTH ENGG MATH
        CPFromUnitGroupAtLevel, //CP from a unit group above a certain level
        Admission,
        Permission,
        Error
    }

    //Defines the expression used for the sublist of requisites
    public enum Expr {
        AND,
        OR
    }

    //Construct a requisite that's not a sublist (cpreq, admis, unit)
    public Requisite(String input, RequisiteType requisiteType) {
        switch(requisiteType) {
            case UNIT:
                this.requisiteType = RequisiteType.UNIT;
                unit = ExcelReader.getUnitByCode(input);
                break;
            case Admission:
                this.requisiteType = RequisiteType.Admission;
                admission = new Admission(input);
                break;
            case CPAtLevel:
                this.requisiteType = RequisiteType.CPAtLevel;
                String[] arr = input.split(",");
                creditPointRequirement = new CreditPointRequirement(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
                break;
            case CP:
                this.requisiteType = requisiteType;
                String s = input.substring(0,input.indexOf("cp"));
                creditPointRequirement = new CreditPointRequirement(Integer.parseInt(s));
                break;
            case UnitWithGrade:
                this.requisiteType = RequisiteType.UnitWithGrade;
                String u = input.substring(0,input.indexOf("("));
                String g = input.substring(input.indexOf("(") + 1, input.indexOf(")"));
                unit = ExcelReader.getUnitByCode(u);
                grade = g;
                break;
            case Permission:
                this.requisiteType = RequisiteType.Permission;
                break;
            case Error:
                this.requisiteType = RequisiteType.Error;
        }
    }

    //Construct a requisite of type CREDITPOINTFROM
    public Requisite(Requisite requisite, int creditPoints, RequisiteType type) {
            requisiteType = type;
            creditPointRequirement = new CreditPointRequirement(requisite, creditPoints);
    }

    //Constructor for a requisite that is a sublist of requisites
    //Creates an empty sublist
    public Requisite(Expr expr) {
        this.expr = expr;
        this.requisiteType = RequisiteType.Sublist;
        sublist = new ArrayList<>();
    }

    //Constructor for a requisite that is a sublist of requisites
    //Adds reference to the given sublist
    public Requisite(ArrayList<Requisite> requisites, Expr expr) {
        this.requisiteType = RequisiteType.Sublist;
        this.expr = expr;
        sublist = requisites;
    }

    //Construct a CPFromUnitGroupAtLevel requisite
    public Requisite(ArrayList<String> unitGroups, int cp, int level)  {
        requisiteType = RequisiteType.CPFromUnitGroupAtLevel;
        creditPointRequirement = new CreditPointRequirement(unitGroups, cp, level);
    }

    //Construct a CPFromUnitGroup requisite
    public Requisite(ArrayList<String> unitGroups, int cp)  {
        requisiteType = RequisiteType.CPFromUnitGroup;
        creditPointRequirement = new CreditPointRequirement(unitGroups, cp);
    }


    //Class structure for a credit point requisite
    class CreditPointRequirement {
         int creditPoints;
         int level;
         Requisite requisite;
         ArrayList<String> unitGroup;


        public CreditPointRequirement(ArrayList<String> unitGroup, int creditPoints, int level) {
            this.unitGroup = unitGroup;
            this.creditPoints = creditPoints;
            this.level = level;
        }
        public CreditPointRequirement(ArrayList<String> unitGroup, int creditPoints) {
            this.unitGroup = unitGroup;
            this.creditPoints = creditPoints;
        }
        public CreditPointRequirement(int creditPoints, int level) {
            this.creditPoints = creditPoints;
            this.level = level;
        }
        public CreditPointRequirement(int creditPoints) {this.creditPoints =  creditPoints;}

        public CreditPointRequirement(Requisite requisite, int creditPoints) {
            this.creditPoints = creditPoints;
            this.requisite = requisite;
        }


         public ArrayList<String> getUnitGroup() {return unitGroup;}
         public int getCreditPoints() { return creditPoints; }
         public int getLevel() { return level; } //Requires null check
        public Requisite getRequisite() { return requisite; }
    }


    //Class structure for an admission requisite
    class Admission {
        String degree;

        public String getDegree() {
            return degree;
        }
        public Admission(String degree)  { this.degree = degree; }
    }


    //Assumes the requisite type is of SUBLIST(OR)
    //Adds a requisite to the sublist
    public void addToBranch(Requisite requisite) {
        if(requisiteType != RequisiteType.Sublist)
            return;
        if(sublist == null)
            sublist = new ArrayList<>();
        sublist.add(requisite);
    }

    //Convert requisite to string
    public  String printString() {
        String s = "";
        if(this == null) return s;
        switch (this.getRequisiteType()) {
            case UNIT:
                s = this.getUnit().getUnitCode();
                break;
            case Admission:
                s = "Admission to " + admission.getDegree();
                break;
            case CPAtLevel:
                s = this.getCreditPointRequirement().getCreditPoints() + "cp at " + this.getCreditPointRequirement().getLevel() + " level";
                break;
            case CPFromUnits:
                s = creditPointRequirement.getCreditPoints() + " credit points from " + creditPointRequirement.getRequisite().printString();
                break;
            case CP:
                s = getCreditPointRequirement().getCreditPoints() + "cp";
                break;
            case CPFromUnitGroupAtLevel:
                s = creditPointRequirement.getCreditPoints() + "cp in ";
                ArrayList<String> alist = creditPointRequirement.getUnitGroup();
                for(int i = 0; i < alist.size(); i++) {
                    s+= alist.get(i);
                    if(i+1 < alist.size())
                        s+= "/";
                }
                s+= " units at " + creditPointRequirement.getLevel() + " level";
                break;
            case CPFromUnitGroup:
                s = creditPointRequirement.getCreditPoints() + "cp in ";
                alist = creditPointRequirement.getUnitGroup();
                for(int i = 0; i < alist.size(); i++) {
                    s+= alist.get(i);
                    if(i+1 < alist.size())
                        s+= "/";
                }
                s+= " units";
                break;
            case UnitWithGrade:
                s = unit.getUnitCode() + "[" + grade + "]";
                break;
            case Permission:
                s = "Permission by special approval";
                break;
            case Sublist:
                s+= "[";
                for(Requisite req : this.getSublist()) {
                    if(req == null)
                        s += "ERROR";
                    else s += req.printString();
                    if(this.getSublist().indexOf(req) != this.getSublist().size() - 1)
                        s += this.getExpr() == Requisite.Expr.OR ? " or " : " and ";
                }
                s+= "]";
                break;
        }
        return s;
    }

    //Getters
    public Expr getExpr() { return expr; }// == Expr.AND ? true : false; }
    public CreditPointRequirement getCreditPointRequirement() {
        return creditPointRequirement;
    }
    public RequisiteType getRequisiteType() { return requisiteType; }
    public Unit getUnit() { return unit; }
    public Admission getAdmission() { return admission; }
    public ArrayList<Requisite> getSublist() { return sublist;  }

}
