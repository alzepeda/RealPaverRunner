import com.oracle.tools.packager.IOUtils;

import java.io.*;
import java.lang.Runtime;
import java.util.*;

public class Main {
    static final int SUBPROBLEMSNUMBER = 5;
    static int fileNumber = 0;

    public static void main(String[]args){
        String oldFileName = "RP_files/logistic_eq/eqs/AM_s=0/logistic_equationN=20.txt";

        //create a hashmap to list of domain and add hashmap to queue
        Queue<HashMap> domainQueue = new LinkedList<HashMap>();
        domainQueue.add(domainsFromFile(oldFileName));

        //subproblems are in an array of arraylists
        ArrayList<String>[] constraintGroupList = new ArrayList[SUBPROBLEMSNUMBER];
        subproblemsFromFile(oldFileName, constraintGroupList);

        //create a place to store solutions
        ArrayList<String> solutions = new ArrayList();

        //create a string with the first few lines of text
        String constants = constantsFromFile(oldFileName);

        BufferedWriter myBufferedWriter;
        String newFileName;

        //domain loop
        try {
            while (!domainQueue.isEmpty()) {
                HashMap<String,float[]> domain = domainQueue.remove();
                //copy domain into an easier data structure where you can modify after each round
                //subproblem loop
                for (int i = 0; i < SUBPROBLEMSNUMBER; i++) {
                    //create new file (preferably no random names, so they get reused)
                    fileNumber += 1;
                    newFileName = "NewFiles/"+ fileNumber + ".txt";
                    myBufferedWriter = new BufferedWriter(new FileWriter(newFileName));
                    myBufferedWriter.write(constants);

                    //copy domain to new file
                    myBufferedWriter.write("\nVARIABLES\n");
                    int entryCounter = 0;

                    //it doesn't write the domain to the file the 2+ time
                    for (Map.Entry variableDomain : domain.entrySet()) {
                        System.out.println("hi");
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

                    //run realpaver on the file then save the domains found as a new domain
                    domain = domainsFromOutput("realpaver " + newFileName);
                    for (Map.Entry domainItem : domain.entrySet()) {
                        String key = (String) domainItem.getKey();
                        float[] range = (float[]) domainItem.getValue();
                        System.out.println(key + " : [" + range[0] + "," + range[1] + "]");
                    }
                }
                //check precision, then divide the domain or send it to a list of solutions
            }
        }catch(IOException e){

        }
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

    public static HashMap<String,float[]> domainsFromOutput(String commandName){
        HashMap<String, float[]> domainList = new HashMap<>();
        try {
            System.out.println(commandName);
            Process p = Runtime.getRuntime().exec(commandName);
            InputStream s = p.getInputStream();
            BufferedReader myBufferedReader = new BufferedReader(new InputStreamReader(s));
            String line, variable;
            boolean startOfVariablesList = false;
            while ((line = myBufferedReader.readLine()) != null) {
                System.out.println(line);
                line.toUpperCase();
                if (line.contains("INITIAL BOX")) {
                    startOfVariablesList = false;
                }
                if (line.contains("OUTER BOX")) {
                    startOfVariablesList = true;
                }
                float start, end;
                if (startOfVariablesList && line.contains("x")) {
                    variable = line.substring(line.indexOf("x"), line.indexOf(" i"));
                    start = Float.valueOf(line.substring(line.indexOf("[") + 1, line.indexOf(",")));
                    end = Float.valueOf(line.substring(line.indexOf(",") + 1, line.indexOf("]")));
                    float[] range = {start, end};
                    domainList.put(variable, range);
                }
                if (startOfVariablesList && line.contains(";")) {
                    break;
                }
            }

            for (Map.Entry domain : domainList.entrySet()) {
                String key = (String) domain.getKey();
                float[] range = (float[]) domain.getValue();
                System.out.println(key + " : [" + range[0] + "," + range[1] + "]");
            }
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
