import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExcelReader {
    static ArrayList<Unit> unitList = new ArrayList<>();
    public static enum Condition {AND, OR}
    private void ExcelReader() {}

    public static ArrayList<Unit> read() {
        try {
            Workbook workbook = WorkbookFactory.create(new File("./data/ScheduleOfUndergraduateUnits.xlsx"));
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                //Relevant data types within the unit list
                Unit.DataScope dataScope;
                String unitCode;
                int creditPoints;
                Unit.UnitType unitType;
                List<String> NCCW;
                boolean[] whenOffered = new boolean[3];
                String department;
                ArrayList<String> cotaught;
                boolean captsone;

                //Data scope
                String cellValue = dataFormatter.formatCellValue(row.getCell(0));
                if (cellValue.equals("Undergraduate"))
                    dataScope = Unit.DataScope.Undergrad;
                else dataScope = Unit.DataScope.Postgrad;

                //Unit Code
                cellValue = dataFormatter.formatCellValue(row.getCell(1));
                unitCode = cellValue.trim();

                //Credit Points
                cellValue = dataFormatter.formatCellValue(row.getCell(3));
                creditPoints = Integer.parseInt(cellValue);

                //Unit Type
                cellValue = dataFormatter.formatCellValue(row.getCell(5));
                if (cellValue.equals("PACE"))
                    unitType = Unit.UnitType.PACE;
                else unitType = Unit.UnitType.NA;

                //NCCW
                cellValue = dataFormatter.formatCellValue(row.getCell(8));
                cellValue = cellValue.replaceAll(" ", "");
                NCCW = Arrays.asList(cellValue.split(","));

                //When Offered
                cellValue = dataFormatter.formatCellValue(row.getCell(9));
                if (cellValue.contains("S1"))
                    whenOffered[0] = true;
                if (cellValue.contains("S2"))
                    whenOffered[1] = true;
                if (cellValue.contains("S3"))
                    whenOffered[2] = true;
                unitList.add(new Unit(dataScope, unitCode, creditPoints, unitType, NCCW, whenOffered));
            }

            for (Row row : sheet) { //Second parse for pre and corequisites
                if (row.getRowNum() == 0) continue;
                Unit unit = unitList.get(row.getRowNum() - 1);
                if(unit == null || unit.getLegacyUnit() == true) {
                    System.out.println("Unit not found or uknown");
                    continue;
                }
                String cell = dataFormatter.formatCellValue(row.getCell(6)); //Prerequisite cell

                Matcher m = Pattern.compile("[A-Z]{4}\\d{4}").matcher(cell);
                ArrayList<Unit> reqList = new ArrayList<>();
                while(m.find())
                    reqList.add(getUnitByCode(m.group()));
                unit.setRequisiteList(reqList);
                //Do prerequisite things
                try {
                    unit.setPrerequisite(requisiteParse(cell));
                } catch (Exception e) {
                    //Req parse failed
                    System.out.println("Prereq parse failed on "+unit.getUnitCode());
                }

                cell = dataFormatter.formatCellValue(row.getCell(7)); //Corequisite cell
                //Do corequisite things
                try {
                    unit.setCorequisite(requisiteParse(cell));
                } catch (Exception e) {
                    //Req parse failed
                    System.out.println("Coreq parse failed on "+unit.getUnitCode());
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: "+e);
            e.printStackTrace();
        }
        return unitList;
    }

    //Returns a requisite object parsed from the given pre or co requisite string
    public static Requisite requisiteParse(String cell) {
        Requisite requisite = null;
        Requisite.Expr expr = null;
        Requisite.Expr nextExpr;
        cell = cell.replaceAll("\\[", "(");
        cell = cell.replaceAll("\\]", ")");
        if(cell.isEmpty())
            return null;

        //With each iteration, take the first requisite of the string and add it to the data structure
        //Remove the parsed portion of the string for the next iteration until empty.
        while (!cell.isEmpty()) {
            cell = cell.trim();
            int idx = Integer.MAX_VALUE; //Char index of first earliest expression in the string
            int offset = 0; //End point of the initially parsed substring
            nextExpr = null;

            //Find earliest occurence of an expression
            int f;
            f = find(cell, "or");
            if(f != -1) {
                idx = f;
                offset = idx + 2;
                nextExpr = Requisite.Expr.OR;
            }
            f = find(cell, "and");
            if(f != -1 && f < idx) {
                idx = f;
                offset = idx + 3;
                nextExpr = Requisite.Expr.AND;
            }
            f = find(cell, "including");
            if(f != -1 && f < idx) {
                idx = f;
                offset = idx + 9;
                nextExpr = Requisite.Expr.AND;
            }

            //Do something with the req
            //This section of code is responsible for building requisites and their sublists, and does so by making recursive calls to this method
            //No next expression exists
            if(nextExpr == null) {
                //A list already exists
                if(expr != null  && requisite != null) {
                    //Add the remaining cell to the branch as an individual req
                    String s = cell;
                    s = clean(s);
                    requisite.addToBranch(requisiteParse(s));
                } else {
                    //No next expression or previous list, treat as individual req
                    requisite = individualRequisiteParse(cell);
                }

            } else if (nextExpr != expr) {//We are either at the first requisite of a list/sublist, or the next expression does not match the current
                //We are at the first requisite of a list/sublist
                if(expr == null) {
                    requisite =  new Requisite(nextExpr);
                    expr = nextExpr;
                    String s = cell.substring(0, idx-1);
                    s = clean(s);
                    requisite.addToBranch(requisiteParse(s));
                } else { //The next expression does not match the current
                    if(nextExpr == Requisite.Expr.AND) {
                        //OR transitioning to AND

                    } else {
                        //AND transitioning to OR
                    }
                }
            } else { //Next expression is equal to current
                String s = cell.substring(0, idx-1);
                s = clean(s);
                requisite.addToBranch(requisiteParse(s));
            }

            //Remove parsed parts of string for next iteration
            if(nextExpr == null) {
                 cell = "";
            } else {
                cell = cell.substring(offset+1);
            }


        }
        return requisite;
    }

    //Returns the index of the given expr within the given string, while ignoring a few key phrases
    //Ignores brackets, ignores "or" if " above" follows, ignores "or" if contained in a phrase like "10cp in ACST or AFIN units at 3000 level"
    public static int find(String cell, String expr) {
        int count = 0;
        for (int i = 0; i < cell.length(); i++) {
            if(cell.charAt(i) == '(')
                count++;
            if(cell.charAt(i) == ')')
                count--;
            if(count == 0 && cell.substring(i).startsWith(expr)) {
                if(expr == "or" && cell.substring(i).startsWith("or above"))
                    continue;
                if(expr == "or") {
                    Pattern p = Pattern.compile("^(or [A-Z]{3,4} )+units at");
                    Matcher m = p.matcher(cell.substring(i));
                    if(m.find())
                        continue;
                }
                return i;
            }
        }
        return -1;
    }

    //Removes whitespace and both front and end brackets.
    public static String clean(String s) {
        s = s.trim();
        if(s.startsWith("(") && s.endsWith(")")) {
            s = s.substring(1,s.length()-1);
        }
        return s;
    }


    //Build an Individual Requisite instance from the given cell
    public static Requisite individualRequisiteParse(String cell) {
        //Attempts to fix unbalanced brackets
        if(cell.contains("(") && !cell.contains(")"))
            cell = cell.replaceAll("\\(", "");
        else if(cell.contains(")") && !cell.contains("("))
            cell = cell.replaceAll("\\)", "");
        cell = cell.trim();

        //Check individual requisite against pre defined regular expressions
        Matcher m;
        Pattern p;

        //Credit Point at certain level  Eg. "50cp at 1000 level"
        p = Pattern.compile("^\\d+cp at \\d+ level");
        m = p.matcher(cell);
        if(m.find()) {
            String s = "";
            Matcher ms = Pattern.compile("\\d+").matcher(cell);
            while(ms.find()) {
                s += ms.group() + ",";
            }
            return new Requisite(s, Requisite.RequisiteType.CPAtLevel);
        }

        //Admission Eg. "Admission to (BEng(Hons) or BCom)"
        p = Pattern.compile("^([Aa]dmission to \\(?)?[BM][A-Z][a-z]{2,}\\S*");
        m = p.matcher(cell);
        if(m.find()) {
            p = Pattern.compile("^[Aa]dmission to \\(");
            m = p.matcher(cell);
            if(m.find()) {
                cell = cell.substring(m.end() - 1);
                cell = clean(cell);
                return requisiteParse(cell);
            }
            p = Pattern.compile("[BM][A-Z][a-z]{2,}\\S*");
            m = p.matcher(cell);
            if(m.find()) {
                cell = cell.substring(m.start(), m.end());
                return new Requisite(cell, Requisite.RequisiteType.Admission);
            }

        }

        //Unit with a grade requirement Eg."COMP1000(Cr)"
        p = Pattern.compile("^[A-Z]{3,4}\\d{3,4}\\(\\w*\\)$");
        m = p.matcher(cell);
        if(m.find()) {
            return new Requisite(cell, Requisite.RequisiteType.UnitWithGrade);
        }

        //Unit Eg."COMP1000"
        p = Pattern.compile("^[A-Z]{3,4}\\d{3,4}$");
        m = p.matcher(cell);
        if(m.find()) {
            return new Requisite(cell, Requisite.RequisiteType.UNIT);
        }

        //Multiple units Eg."MATH130-MATH135"
        p = Pattern.compile("^[A-Z]{3,4}\\d{3,4}-[A-Z]{3,4}\\d{3,4}$");
        m = p.matcher(cell);
        if(m.find()) {
            String s = "";
            String unitname;
            int lower,upper;
            unitname = cell.substring(0,4);
            lower = Integer.parseInt(cell.substring(4,cell.indexOf("-")));
            upper = Integer.parseInt(cell.substring(cell.indexOf("-")+5));
            for(int i = lower; i <= upper; i++) {
                s+= unitname + i;
                if(i < upper)
                    s+= " or ";
            }
            return requisiteParse(s);
        }

        //Permission by special approval
        p = Pattern.compile("^[Pp]ermission by special approval$");
        m = p.matcher(cell);
        if(m.find()) {
            return new Requisite(cell, Requisite.RequisiteType.Permission);
        }

        //Credit point requirement by itself Eg. "40cp"
        p = Pattern.compile("^\\d+cp$");
        m = p.matcher(cell);
        if(m.find()) {
            return new Requisite(cell, Requisite.RequisiteType.CP);
        }

        //Credit point requirement from unit(s) topic at a certain level (eg. 10cp in ANTH OR MATH units at 2000 level)
        p = Pattern.compile("^\\d+cp (in|from) [\\w+\\s+]+units at \\d+ level");
        m = p.matcher(cell);
        if(m.find()) {
            Pattern ps = Pattern.compile("cp (in|from)");
            m = ps.matcher(cell);
            if(m.find()) {
                int cp = Integer.parseInt(cell.substring(0, cell.indexOf("cp")));
                int level = Integer.parseInt(cell.substring(cell.indexOf("units at") + 9, cell.indexOf(" level")));
                String s = cell.substring(m.end()+1, cell.indexOf(" units"));
                ArrayList<String> as = new ArrayList<>(Arrays.asList(s.split(" or ")));
                return new Requisite(as, cp, level);
            }

        }

        //Credit point requirement from unit(s) topic (eg. 10cp in ANTH OR MATH units)
        p = Pattern.compile("^\\d+cp (in|from) [\\w+\\s+]+units");
        m = p.matcher(cell);
        if(m.find()) {
            Pattern ps = Pattern.compile("cp (in|from)");
            m = ps.matcher(cell);
            if(m.find()) {
                int cp = Integer.parseInt(cell.substring(0, m.start()));
                String s = cell.substring(m.end()+1, cell.indexOf(" units"));
                ArrayList<String> as = new ArrayList<>(Arrays.asList(s.split(" or ")));
                return new Requisite(as, cp);
            }

        }

        //Credit point requirement from a list of units Eg. "10cp from (COMP1000 or COMP1025)"
        p = Pattern.compile("^\\d+cp from ");
        m = p.matcher(cell);
        if(m.find()) {
            int cp = Integer.parseInt(cell.substring(0, cell.indexOf("cp from")));
            String s = cell.substring(m.end());
            s = clean(s);
            Requisite r = ExcelReader.requisiteParse(s);
            return new Requisite(r, cp, Requisite.RequisiteType.CPFromUnits);
        }

        //Default case, no match
        return null;
    }

    public static Unit getUnitByCode(String code) {
        for(Unit unit : unitList) {
            if (unit.getUnitCode().equals(code))
                return unit;
        }
        Unit u = new Unit(code);
        unitList.add(u);
        return u;
    }
}


