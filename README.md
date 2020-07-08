# RealPaverRunner

This file is meant to compare the run-times of a constraint splitting technique and the regular technique employed by realpaver.

**How to Run**
Download the files and open with your preferred Jave IDE. I used IntelliJ with my compiler set to javac. I am running on JDK 9.0.1 and use only the external libraries it provides. Run on Main.java.

**Things to Change**
The user is encouraged to modify the files that are being tested. It is important to pay attention and send the branching files only to the standard realpaver runner. The user is also able to modify the number of subsets the constraints are divided into.

**Expected Output**
When running the standard realpaver, there should only be one line detailing the length of time it took to run realpaver. When running the modified program, the user should expect one line for each subproblem and then a final line describing the length of time it took to run the whole modified program.
