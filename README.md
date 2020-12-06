MAIN FOLDERS/FILES:

1) Main program (source code):

    ./src/main/java/cs454/ConcurrencyRepairTool.java

2) Sources (programs to be improved):

    ./src/main/sources

3) Compilation folder:

    ./target

4) Output folder (improved programs):

    ./output


COMPILATION:

1) Add maven to PATH:

    $ source mvn_path

2) Change dir to project:

    $ cd project

3) Compile:

    $ mvn clean install


EXECUTION:

1) Run with Java JDK11 (must have been previously installed):

    $ java -jar target/project-1.0-SNAPSHOT-shaded.jar [arg1] [arg2] [arg3] [arg4]

Where:

    arg1: Integer (0<x): Population Size (per program improved).

    arg2: Integer (0<x): Number of fitness evaluations (per program improved).

    arg3: Float (0<x<=1): Percentage of the population to be replaced on each generation (offsprings).

    arg4: Integer (0<=x): Number of mutations performed for each offspring.

Default for testing:

    $ java -jar target/project-1.0-SNAPSHOT-shaded.jar [arg1] [arg2] 0.4 1      ////////// NOT DEFINED YET