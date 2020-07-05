import com.oracle.tools.packager.IOUtils;

import java.io.*;
import java.lang.Runtime;
import java.util.*;

public class Main {
    static final int SUBPROBLEMSNUMBER = 5;
    public static void main(String[]args){
        String oldFileName = "RP_files/logistic_eq/eqs/AM_s=0/logistic_equationN=20.txt";

        //create a hashmap to list of domain and add hashmap to queue
        Queue<HashMap> domainQueue = new LinkedList<HashMap>();
        domainQueue.add(domainsFromFile(oldFileName));

        //subproblems are in an array of arraylists
        ArrayList<String>[] constraintGroupList = new ArrayList[SUBPROBLEMSNUMBER];
        subproblemsFromFile(oldFileName, constraintGroupList);

        //create a string with the first few lines of text
        String constants = constantsFromFile(oldFileName);

        BufferedWriter myBufferedWriter;
        String newFileName;
        int fileNumber = 0;

        //domain loop
        try {
            while (!domainQueue.isEmpty()) {
                HashMap<String,float[]> domain = domainQueue.remove();
                //subproblem loop
                for (int i = 0; i < SUBPROBLEMSNUMBER; i++) {
                    //create new file (preferably no random names, so they get reused)
                    newFileName = "NewFiles/"+ fileNumber + ".txt";
                    fileNumber += 1;
                    myBufferedWriter = new BufferedWriter(new FileWriter(newFileName));
                    myBufferedWriter.write(constants);
                    myBufferedWriter.write("\n");
                    myBufferedWriter.write("VARIABLES");
                    //copy domain to new file
                    for (Map.Entry variableDomain : domain.entrySet()) {
                        String key = (String) variableDomain.getKey();
                        float[] range = (float[]) variableDomain.getValue();
                        myBufferedWriter.write(key + " in [" + range[0] + ", " + range[1] + "]");
                    }
                    //copy constraints to new file
                    for (int j = 0; j < constraintGroupList[i].size(); j++) {
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
            float start, end;
            boolean startOfVariablesList = false;
            while ((line = myBufferedReader.readLine()) != null) {
                line.toUpperCase();
                if (line.contains("VARIABLES")) {
                    startOfVariablesList = true;
                }
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
                    constraintGroupList[counter%SUBPROBLEMSNUMBER].add(line);
                }
                if (startOfConstraintsList && line.contains(";")) {
                    break;
                }
                counter += 1;
            }
            for(int i = 0 ; i < SUBPROBLEMSNUMBER; i++){
                System.out.println(i);
                for(int j = 0 ; j < constraintGroupList[i].size(); j++){
                    if (j == constraintGroupList[i].size() - 1){
                        constraintGroupList[i].get(j).replace(",",";");
                    }
                    System.out.println(constraintGroupList[i].get(j));
                }
                System.out.println();
            }
        }catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

    public static String constantsFromFile(String fileName){
        String constants = "";
        try {
            BufferedReader myBufferedReader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = myBufferedReader.readLine()) != null) {
                constants += line;
                constants += "\n";
            }
            System.out.println(constants);
        }catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
        return constants;
    }
}
