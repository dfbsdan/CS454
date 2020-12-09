package cs454;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////TEMPORARY CODE(?)
/*
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
*/
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.*;
import com.github.javaparser.printer.PrettyPrinter;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.*;

/**
 * Main Class
*/
public class ConcurrencyRepairTool
{
    public static void main( String[] args )
    {
        if(args.length != 8) { ////////////////////////////////////////////////////////////////////////////////////////MAY CHANGE
            throw new IllegalArgumentException("Wrong amount of arguments.");
        }
        int populationSize = Integer.parseInt(args[0]);
        if(populationSize <= 1) {
            throw new IllegalArgumentException("The population size has to be greater than 1.");
        }
        int fitnessEvaluations = Integer.parseInt(args[1]);
        if(fitnessEvaluations <= 0) {
            throw new IllegalArgumentException("The amount of fitness evaluations has to be greater than 0.");
        }
        float offsprings = Float.parseFloat(args[2]);
        if(offsprings <= 0 || offsprings > 1) {
            throw new IllegalArgumentException("Incorrect offspring percentage.");
        }
        int mutationCnt = Integer.parseInt(args[3]);
        if(mutationCnt < 0) {
            throw new IllegalArgumentException("The amount of mutations has to be greater or equal to 0.");
        }
        int poolSize = Integer.parseInt(args[4]);
        if(poolSize <= 0) {
            throw new IllegalArgumentException("The thread pool size has to be greater than 0.");
        }
        int testCnt = Integer.parseInt(args[5]);
        if(testCnt <= 0) {
            throw new IllegalArgumentException("The number of tests has to be greater than 0");
        }
        int timeout = Integer.parseInt(args[6]);
        if(timeout <= 0) {
            throw new IllegalArgumentException("The timeout has to be greater than 0");
        }
        float k = Float.parseFloat(args[7]);
        if(k <= 0 || k > 1) {
            throw new IllegalArgumentException("Incorrect population size percentage.");
        }

        // JavaParser has a minimal logging class that normally logs nothing.
        // Let's ask it to write to standard out:
        Log.setAdapter(new Log.StandardOutStandardErrorAdapter());

        ReparationTool repTool = new ReparationTool(populationSize, fitnessEvaluations, offsprings, mutationCnt, poolSize, testCnt, timeout, k);
        repTool.repairAll();
        repTool.finish();
    }
}

class ReparationTool
{
    // Threadpool used to perform fitness evaluations
    private final ExecutorService pool;
    // The path of the Maven module/project which contains the ConcurrencyRepairTool class.
    // appended with a path to "output".
    private final Path output = CodeGenerationUtils.mavenModuleRoot(ConcurrencyRepairTool.class)
        .resolve(Paths.get("output"));
    // Names of source java programs to be modified by the application.
    // All must be inside the src/main/sources folder.
    private String[] sources;
    // SourceRoot is a tool that read and writes Java files from packages on a certain root directory.
    // In this case the root directory is found by taking the root from the current Maven module,
    // with src/main/sources appended.
    private final SourceRoot sourceRoot;
    private final int popSize; // Population size (per improved program)
    private final int fitnessEval; // Number of fitness evaluations (per improved program)
    private final int offspringCnt; // Number of new offsprings per generation (per program)
    private final int mutationCnt; // Number of mutations performed per new offspring
    private final int testCnt; // Number of tests to be performed for a fitness evaluation
    private final int timeout; // Timeout of a test
    private final int k; // Number of individuals to be randomly sampled for tournament selection.

    ReparationTool(int popSize, int fitnessEval, float offsprings, int mutationCnt, int poolSize, int testCnt, int timeout, float k) {
        Path sourceRoot = CodeGenerationUtils.mavenModuleRoot(ConcurrencyRepairTool.class)
            .resolve("src/main/sources");
        try {
            this.sources = Files.list(sourceRoot).filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString()).toArray(String[]::new);
        } catch(IOException e) {
            e.getCause();
        }
        this.pool = Executors.newFixedThreadPool(poolSize);
        this.sourceRoot = new SourceRoot(sourceRoot);
        this.popSize = (popSize < fitnessEval)? popSize: fitnessEval;
        this.fitnessEval = fitnessEval;
        int i = (int)(offsprings * popSize);
        this.offspringCnt = (i > 0)? i: 1; // At least one offspring per generation
        this.mutationCnt = mutationCnt;
        this.testCnt = testCnt;
        this.timeout = timeout;
        i = (int)(k * popSize);
        this.k = (i > 1)? i: 2; // At least two individuals used in tournament selection.
    }

    public void repairAll()
    {
        // Repair all sources
        for(String src : sources)
            repair(src);
    }

    private void repair(String src)
    {
        CompilationUnit cu = this.sourceRoot.parse("", src);
        Log.info("Repairing: " + src);

        Population pop = new Population(popSize, fitnessEval, offspringCnt, mutationCnt, testCnt, timeout, k, src, pool, cu);
        pop.evolve();
        // Store the best solution in the /output directory
        cu = pop.bestSolution().cu;
        cu.setStorage(this.output.resolve(Paths.get(src)));
        cu.getStorage().get().save(this.sourceRoot.getPrinter());
    }

    public void finish()
    {
        pool.shutdownNow();
    }
}

class Population
{
    // List of pending fitness evaluations
    private final List<FitnessEvaluator> tasks = new ArrayList<>();
    private final int size; // Population size
    private int fitnessEval; // Remaining fitness evaluations
    private final int offspringCnt; // Max. number of new offsprings per generation
    private final int mutationCnt; // Number of mutations performed per new offspring
    private final Solution solutions[]; // Sorted array of solutions (by fitness, descending order)
    private final ExecutorService pool; // Threadpool used for fitness evaluations
    private final int testCnt; // Number of tests to be performed for a fitness evaluation
    private final int timeout; // Timeout of a test
    private final int k; // Number of individuals to be randomly sampled for tournament selection.
    private final String src; // Name of source file
    private int solutionID = 0; // Identifies a solution when evaluating its fitness

    Population(int size, int fitnessEval, int offspringCnt, int mutationCnt, int testCnt, int timeout, int k, String src, ExecutorService pool, CompilationUnit cu)
    {
        this.size = size;
        this.fitnessEval = fitnessEval - size;
        this.offspringCnt = offspringCnt;
        this.mutationCnt = mutationCnt;
        this.testCnt = testCnt;
        this.timeout = timeout;
        this.k = k;
        this.src = src;
        this.pool = pool;
        solutions = new Solution[size];
        // Generate the initial population
        for (int i = 0; i < size; i++)
            solutions[i] = new Solution(cu.clone());
        evaluateSolutions();
        Arrays.sort(solutions, Population::compareSolutions);
        //printSolutions(); // Testing purposes
    }

    // Evolve until all fitness evaluations have been performed
    public void evolve()
    {
        while (fitnessEval > 0)
            fitnessEval -= nextGeneration();
    }

    // Updates the population through the creation of M new solutions (offsprings) that replace 
    // the M-worst ones of the current generation. Where M is obtained by multiplying the given 
    // programs arguments that correspond to the population size and the percentage of the
    // population to be replaced on each generation (see README file).  
    private int nextGeneration()
    {
        // Set the number of offsprings to be generated (Constrained by fitness evaluation limit)
        int offspringCnt = (this.offspringCnt < fitnessEval)? this.offspringCnt: fitnessEval;
        // Generate the offsprings and update the population
        for (int i = size - offspringCnt; i < size; i++) {
            int parents[] = getParents();
            solutions[i] = new Solution(solutions[parents[0]].cu, solutions[parents[1]].cu);
        }
        evaluateSolutions();
        Arrays.sort(solutions, Population::compareSolutions); /////////////////////////////////////////////////////////Insert in order instead?
        return offspringCnt;
    }

    // Chooses two parents (their index inside the solutions list) so that a new
    // solution constructor can use them later on (see nextGeneration()).
    // The parents are chosen through tournament selection by sampling K random
    // solutions out of the current generation.
    private int[] getParents()
    {
        int parents[] = new int[2];
        List<Integer> indexes = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            indexes.add(i);
        Collections.shuffle(indexes);
        // Select the 2 -out of K- best solutions
        if (indexes.get(0) < indexes.get(1)) {
            parents[0] = indexes.get(0);
            parents[1] = indexes.get(1);
        } else {
            parents[0] = indexes.get(1);
            parents[1] = indexes.get(0);
        }
        int idx;
        for (int i = 2; i < k; i++) {
            idx = indexes.get(i);
            if (idx < parents[0]) {
                parents[1] = parents[0];
                parents[0] = idx;
            } else if (idx < parents[1])
                parents[1] = idx;
        }
        return parents;
    }

    // Return the best solution found so far
    public Solution bestSolution()
    {
        return solutions[0];
    }

    // Sets the fitness for all solutions that have not been evaluated
    private void evaluateSolutions()
    {
        try {
            pool.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tasks.clear();
    }

    private void printSolutions() // Testing purposes
    {
        for(int i = 0; i < solutions.length; i++)
            Log.info("Solution: " + i + ", fitness: " + solutions[i].fitness);
    }

    // Comparator used for keeping the population array sorted
    private static int compareSolutions(Solution a, Solution b)
    {
        float diff = b.fitness - a.fitness;
        return (diff > 0)? 1: (diff < 0)? -1: 0;
    }

    public class Solution
    {
        public final CompilationUnit cu;
        public float fitness;
    
        // 'Random' solution generator
        Solution(CompilationUnit cu) //////////////////////////////////////////////////////////////////////////////////NOT FINISHED
        {
            //Log.info("Creating 'random' solution");
            // TODO: Initialize
            
            this.cu = cu; // Copy of the original (testing only)
    
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////SAMPLE JAVAPARSER CODE (TESTING PURPOSES ONLY)
            /*
            cu.accept(new ModifierVisitor<Void>() {
                 // For every if-statement, see if it has a comparison using "!=".
                 // Change it to "==" and switch the "then" and "else" statements around.
                @Override
                public Visitable visit(IfStmt n, Void arg) {
                    // Figure out what to get and what to cast simply by looking at the AST in a debugger! 
                    n.getCondition().ifBinaryExpr(binaryExpr -> {
                        if (binaryExpr.getOperator() == BinaryExpr.Operator.NOT_EQUALS && n.getElseStmt().isPresent()) {
                            // It's a good idea to clone nodes that you move around.
                            //    JavaParser (or you) might get confused about who their parent is!
                            //
                            Statement thenStmt = n.getThenStmt().clone();
                            Statement elseStmt = n.getElseStmt().get().clone();
                            n.setThenStmt(elseStmt);
                            n.setElseStmt(thenStmt);
                            binaryExpr.setOperator(BinaryExpr.Operator.EQUALS);
                        }
                    });
                    return super.visit(n, arg);
                }
            }, null);
            */
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
            // Evaluate
            getFitness();
        }
    
        // Creates a solution based on two given parents (i.e. their compilation units)
        Solution(CompilationUnit p1, CompilationUnit p2) ///////////////////////NOT FINISHED
        {
            //Log.info("Creating offspring solution");
            // TODO: Inherit
            cu = p1.clone();
    
            // TODO: Mutate
    
            // Evaluate
            getFitness();
        }
    
        // Adds the current solution to the tasks list so that its fitness can be
        // evaluated later on by the population object
        private void getFitness()
        {
            tasks.add(new FitnessEvaluator(this, solutionID++));
        }
    }

    // Callable object used to evaluate and set the fitness of a solution
    private class FitnessEvaluator implements Callable<Void>
    {
        private final Solution sol; // Solution to be evaluated
        private final int solID; // Identifies the solution

        FitnessEvaluator(Solution sol, int solID)
        {
            this.sol = sol;
            this.solID = solID;
        }

        public Void call()
        {
            Path sourcePath = CodeGenerationUtils.mavenModuleRoot(ConcurrencyRepairTool.class)
                .resolve(Paths.get("temp/" + solID + src));
            // Store source code in a temporary folder (/temp)
            sol.cu.setStorage(sourcePath);
            sol.cu.getStorage().get().save(new PrettyPrinter()::print);
            // Test and set fitness
            float successCnt = 0; // Amount of times the solution has passed a test
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("java", sourcePath.toString());
            Process p;
            for (int i = 0; i < testCnt; i++) {
                try {
                    p = processBuilder.start();
                    if (p.waitFor(timeout, TimeUnit.SECONDS) && p.exitValue() == 0) successCnt++;
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
            sol.fitness = successCnt / testCnt; ///////////////////////////////////////////////////////////////////////Final???
            // Clean up
            try {
                Files.delete(sourcePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}