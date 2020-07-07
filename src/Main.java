import com.oracle.tools.packager.IOUtils;

import java.io.*;
import java.lang.Runtime;
import java.util.*;

public class Main {
    static final int SUBPROBLEMSNUMBER = 5;
    static int fileNumber = 0;

    public static void main(String[] args){
        long start = System.currentTimeMillis();
        constraintGrouper( "RP_files/logistic_eq/eqs/AM_s=0/logistic_equationN=20.txt");
        long end = System.currentTimeMillis();
        System.out.println("It took "+ (end-start) + " milliseconds to complete the run while grouping constraints");

        start = System.currentTimeMillis();
        regularRunner( "RP_files/logistic_eq/eqs/AM_s=0_branching/logistic_equationN=20.txt");
        end = System.currentTimeMillis();
        System.out.println("It took "+ (end-start) + " milliseconds to complete the run without grouping constraints");
    }

    public static void regularRunner(String oldFileName){
        try{
            Runtime.getRuntime().exec("realpaver "+oldFileName);
        }catch(IOException e){
            System.out.println("error: " + e.getMessage());
        }
    }

    public static void constraintGrouper(String oldFileName){
        //create a hashmap to list of domain and add hashmap to queue
        Queue<HashMap> domainQueue = new LinkedList<HashMap>();
        domainQueue.add(domainsFromFile(oldFileName));

        //subproblems are in an array of arraylists
        ArrayList<String>[] constraintGroupList = new ArrayList[SUBPROBLEMSNUMBER];
        subproblemsFromFile(oldFileName, constraintGroupList);

        //create a place to store solutions
        Queue<HashMap> solutions = new LinkedList<HashMap>();

        //create a string with the first few lines of text
        String constants = constantsFromFile(oldFileName);

        BufferedWriter myBufferedWriter;
        String newFileName;

        //domain loop
        try {
            while (!domainQueue.isEmpty()) {
                HashMap<String,float[]> domain = domainQueue.remove();
                //subproblem loop
                for (int i = 0; i < SUBPROBLEMSNUMBER; i++) {
                    fileNumber += 1;
                    newFileName = "NewFiles/"+ fileNumber + ".txt";
                    myBufferedWriter = new BufferedWriter(new FileWriter(newFileName));
                    myBufferedWriter.write(constants);

                    //copy domain to new file
                    myBufferedWriter.write("\nVARIABLES\n");
                    int entryCounter = 0;
                    for (Map.Entry variableDomain : domain.entrySet()) {
                        entryCounter += 1;
                        String key = (String) variableDomain.getKey();
                        float[] range = (float[]) variableDomain.getValue();
                        if(entryCounter == domain.size()){
                            myBufferedWriter.write(key + " in [" + range[0] + ", " + range[1] + "];\n");
                        }else {
                            myBufferedWriter.write(key + " in [" + range[0] + ", " + range[1] + "],\n");
                        }
                    }

                    //copy constraints to new file
                    myBufferedWriter.write("\nCONSTRAINTS\n");
                    for (int j = 0; j < constraintGroupList[i].size(); j++) {
                        myBufferedWriter.write(constraintGroupList[i].get(j)+"\n");
                    }
                    myBufferedWriter.close();

                    int variableCount = domain.size();
                    domain.clear();

                    Process p = Runtime.getRuntime().exec("realpaver " + newFileName);
                    InputStream s = p.getInputStream();
                    BufferedReader myBufferedReader = new BufferedReader(new InputStreamReader(s));
                    String line, variable;
                    boolean startOfOuterBox = false;
                    while ((line = myBufferedReader.readLine()) != null) {
                        line.toUpperCase();
                        if (line.contains("INITIAL BOX")) {
                            startOfOuterBox = false;
                        }
                        if (line.contains("OUTER BOX")) {
                            startOfOuterBox = true;
                        }
                        float start, end;
                        //add each domain from the output
                        if (startOfOuterBox && line.contains("x")) {
                            variable = line.substring(line.indexOf("x"), line.indexOf(" i"));
                            start = Float.valueOf(line.substring(line.indexOf("[") + 1, line.indexOf(",")));
                            end = Float.valueOf(line.substring(line.indexOf(",") + 1, line.indexOf("]")));
                            float[] range = {start, end};
                            domain.put(variable, range);
                        }
                        //if not the last constraint set, stop when you reach ;
                        if ((fileNumber%SUBPROBLEMSNUMBER!=0) && startOfOuterBox && line.contains(";")) {
                            break;
                        }
                        //if the last in the constraint set, continue after all variables added to domain
                        float precision;
                        if ((fileNumber%SUBPROBLEMSNUMBER==0) && startOfOuterBox && line.contains("precision")) {
                            precision = Float.valueOf(line.substring(line.indexOf(":") + 2, line.indexOf(",")));
                            //if precision is too big we have to find a variable to disect
                            if (precision > 10e-4) {
                                System.out.println("precision too large, we divide the domain");
                                //go through the domainList (just created), look for the variable and replace it with
                                //the first half then add it to domain queue
                                int counter = 0;
                                //copy the domain to have two domains to add to the queue
                                HashMap<String,float[]> domain2 = new HashMap<String,float[]>();
                                for (Map.Entry variableName : domain.entrySet()) {
                                    variable = (String) variableName.getKey();
                                    float[] range = (float[])variableName.getValue();
                                    domain2.put(variable,range);
                                }
                                //split a random variable's range in half
                                Random rand = new Random();
                                int variableToDissect = rand.nextInt(variableCount - 1);
                                for (Map.Entry variableName : domain.entrySet()) {
                                    if (counter == variableToDissect) {
                                        variable = (String) variableName.getKey();
                                        float[] range = (float[])variableName.getValue();
                                        float midpoint = (range[1]-range[0])/2;
                                        domain.remove(variable);
                                        domain2.remove(variable);
                                        domain.put(variable,range);
                                        float[] range2 = {midpoint,range[1]};
                                        range[1] = midpoint;
                                        domain2.put(variable,range2);
                                        domain.put(variable,range);
                                    }
                                }
                                domainQueue.add(domain);
                                domainQueue.add(domain2);
                            }else{
                                //when precision is small enough, add solution to our queue for solutions
                                solutions.add(domain);
                            }
                        }
                    }
                }
            }
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
        int solutionNumber = 0;
        while(!solutions.isEmpty()) {
            solutionNumber += 1;
            System.out.println("Solution number: " + solutionNumber);
            HashMap<String, float[]> solution = solutions.remove();
            for (Map.Entry solutionDomain : solution.entrySet()) {
                String key = (String) solutionDomain.getKey();
                float[] range = (float[]) solutionDomain.getValue();
                System.out.println(key + " in [" + range[0] + ", " + range[1] + "]");
            }
        }
        System.out.println();
    }

    public static HashMap<String,float[]> domainsFromFile(String fileName){
        HashMap<String, float[]> domainList = new HashMap<>();
        try {
            BufferedReader myBufferedReader = new BufferedReader(new FileReader(fileName));
            String line, variable;
            boolean startOfVariablesList = false;
            while ((line = myBufferedReader.readLine()) != null) {
                line.toUpperCase();
                if (line.contains("VARIABLES")) {
                    startOfVariablesList = true;
                }
                float start, end;
                if (startOfVariablesList && line.contains("x")) {
                    variable = line.substring(line.indexOf("x"), line.indexOf(" "));
                    start = Float.valueOf(line.substring(line.indexOf("[") + 1, line.indexOf(",")));
                    end = Float.valueOf(line.substring(line.indexOf(",") + 1, line.indexOf("]")));
                    float[] range = {start, end};
                    domainList.put(variable, range);
                }
                if (startOfVariablesList && line.contains(";")) {
                    break;
                }
            }

            /*for (Map.Entry domain : domainList.entrySet()) {
                String key = (String) domain.getKey();
                float[] range = (float[]) domain.getValue();
                System.out.println(key + " : [" + range[0] + "," + range[1] + "]");
            }
             */
        }catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
        return domainList;
    }

    public static void subproblemsFromFile(String fileName,ArrayList<String>[] constraintGroupList){
        try {
            BufferedReader myBufferedReader = new BufferedReader(new FileReader(fileName));
            String line;
            for(int i = 0; i < SUBPROBLEMSNUMBER; i++) {
                constraintGroupList[i] = new ArrayList<>();
            }
            boolean startOfConstraintsList = false;
            int counter = 0;
            while ((line = myBufferedReader.readLine()) != null) {
                line.toUpperCase();
                if (line.contains("CONSTRAINTS")) {
                    startOfConstraintsList = true;
                }
                if (startOfConstraintsList && line.contains("x")) {
                    counter += 1;
                    constraintGroupList[counter/SUBPROBLEMSNUMBER].add(line);
                }
                if (startOfConstraintsList && line.contains(";")) {
                    break;
                }
            }
            for(int i = 0 ; i < SUBPROBLEMSNUMBER; i++){
                for(int j = 0 ; j < constraintGroupList[i].size(); j++){
                    if (j == (constraintGroupList[i].size() - 1)){
                        line = constraintGroupList[i].get(j).replace(",",";");
                        constraintGroupList[i].remove(j);
                        constraintGroupList[i].add(j,line);
                    }
                }
            }
        }catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

    public static String constantsFromFile(String fileName){
        boolean startOfConstants = false;
        String constants = "";
        try {
            BufferedReader myBufferedReader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = myBufferedReader.readLine()) != null) {
                constants += line;
                constants += "\n";
                line.toUpperCase();
                if(line.contains("CONSTANTS")){
                    startOfConstants = true;
                }
                if(startOfConstants && line.contains(";")){
                    break;
                }
            }
        }catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
        return constants;
    }
}
