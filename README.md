MAIN FOLDERS/FILES:

1) Main program (source code):

    /project/src/main/java/cs454/ConcurrencyRepairTool.java

2) Sources (programs to be improved):

    /project/src/main/sources

3) Compilation folder:

    /project/target

4) Output folder (improved programs):

    /project/output


COMPILATION:

1) Add maven to PATH:

    $ source mvn_path

2) Change dir to project:

    $ cd project

3) Compile:

    $ mvn clean install


EXECUTION (from project folder):

1) Run with Java JDK11 (must have been previously installed):

    $ java -jar target/project-1.0-SNAPSHOT-shaded.jar [arg1] [arg2] [arg3] [arg4] [arg5] [arg6] [arg7]

Where:

    arg1: Integer (1<x): Population Size (per program improved). At least two because two parents
                         are assumed to exist always.

    arg2: Integer (0<x): Number of fitness evaluations (per program improved).

    arg3: Float (0<x<=1): Percentage of the population to be replaced on each generation (offsprings).

    arg4: Integer (0<=x): Number of mutations performed for each offspring.

    arg5: Integer (0<x): Maximum number of helper threads to be created to perform fitness evaluations.

    arg6: Integer (0<x): Number of tests performed per program in order to obtain its fitness.

    arg7: Integer (0<x): Maximum time -in seconds- a test can take (timeout).

Default for testing:

    $ java -jar target/project-1.0-SNAPSHOT-shaded.jar 20 100 0.4 1 30 20 5    // REVISION REQUIRED