package cs454;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////TEMPORARY CODE(?)

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import java.util.Random;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Main Class
*/
public class ConcurrencyRepairTool
{
    public static void main( String[] args )
    {
        if(args.length != 7) { ////////////////////////////////////////////////////////////////////////////////////////MAY CHANGE
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

        // JavaParser has a minimal logging class that normally logs nothing.
        // Let's ask it to write to standard out:
        Log.setAdapter(new Log.StandardOutStandardErrorAdapter());

        ReparationTool repTool = new ReparationTool(populationSize, fitnessEvaluations, offsprings, mutationCnt, poolSize, testCnt, timeout);
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
    private final String outputFile = CodeGenerationUtils.mavenModuleRoot(ConcurrencyRepairTool.class)
        .resolve(Paths.get("output")).toString();
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

    ReparationTool(int popSize, int fitnessEval, float offsprings, int mutationCnt, int poolSize, int testCnt, int timeout) {
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
        int offspringCnt = (int)(offsprings * popSize);
        this.offspringCnt = (offspringCnt > 0)? offspringCnt: 1; // At least one offspring per generation
        this.mutationCnt = mutationCnt;
        this.testCnt = testCnt;
        this.timeout = timeout;
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

        Population pop = new Population(popSize, fitnessEval, offspringCnt, mutationCnt, testCnt, timeout, pool, cu);
        pop.evolve();
        this.sourceRoot.add(this.outputFile, src, pop.bestSolution().cu);
    }

    // This saves all the files we just repaired to an output directory. 
    public void finish()
    {
        this.sourceRoot.saveAll();
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
    private final int testCnt;
    private final int timeout; // Timeout of a test

    Population(int size, int fitnessEval, int offspringCnt, int mutationCnt, int testCnt, int timeout, ExecutorService pool, CompilationUnit cu)
    {
        this.size = size;
        this.fitnessEval = fitnessEval - size;
        this.offspringCnt = offspringCnt;
        this.mutationCnt = mutationCnt;
        this.testCnt = testCnt;
        this.timeout = timeout;
        this.pool = pool;
        solutions = new Solution[size];
        // Generate the initial population
        for (int i = 0; i < size; i++)
            solutions[i] = new Solution(cu.clone());
        evaluateSolutions();
        Arrays.sort(solutions, Population::compareSolutions);
        printSolutions(); // Testing purposes
    }

    // Evolve until all fitness evaluations have been performed
    public void evolve()
    {
        while (fitnessEval > 0)
            fitnessEval -= nextGeneration();
    }

    private int nextGeneration() //////////////////////////////////////////////////////////////////////////////////////NOT FINISHED
    {
        // Set the number of offsprings to be generated (Constrained by fitness evaluation limit)
        int offspringCnt = (this.offspringCnt < fitnessEval)? this.offspringCnt: fitnessEval;
        // Generate the offsprings
        Solution offsprings[] = new Solution[offspringCnt];
        for (int i = 0; i < offspringCnt; i++) {
            int parents[] = getParents();
            offsprings[i] = new Solution(solutions[parents[0]].cu, solutions[parents[1]].cu);
        }
        evaluateSolutions();
        printSolutions(); // Testing purposes
        // TODO: Update the population


        return offspringCnt;
    }

    private int[] getParents() ////////////////////////////////////////////////////////////////////////////////////////NOT FINISHED
    {
        // TODO: Choose parents (their index inside the solutions list)
        //Log.info("Choosing parents");
        int parents[] = new int[2];
        parents[0] = 1;
        parents[1] = 2;


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
        Solution(CompilationUnit cu) ///////////////////////////////////////////NOT FINISHED
        {
            //Log.info("Creating 'random' solution");
            // TODO: Initialize
            
            this.cu = cu; // Copy of the original (testing only)
    
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////SAMPLE JAVAPARSER CODE (TESTING PURPOSES ONLY)
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
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
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
            tasks.add(new FitnessEvaluator(this));
        }
    }

    // Callable object used to evaluate and set the fitness of a solution
    private class FitnessEvaluator implements Callable<Void>
    {
        private final Solution sol; // Solution to be evaluated
        // Actual thread that runs a test. Returns 0 on success, 1 otherwise.
        private final Test evaluator = new Test();

        FitnessEvaluator(Solution sol)
        {
            this.sol = sol;
        }

        public Void call()
        {   
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Integer> future;
            int failCnt = 0;
            for (int i = 0; i < testCnt; i++) {
                future = executor.submit(evaluator);
                try {
                    failCnt += future.get(timeout, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    failCnt++; // Timeout test considered a failed one
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            executor.shutdownNow();
            sol.fitness = ((float)testCnt - failCnt) / testCnt; //////////////////////////////////////////////////////////////////TESTING PURPOSES ONLY
            return null;
        }

        // Class that tests a solution. Returns 0 on success and 1 otherwise.
        private class Test implements Callable<Integer>
        {
            Test() {}

            public Integer call()
            {
                // TODO: Run compilation unit and return
                Random rand = new Random();
                int i = rand.nextInt();
                return (i%2 != 0)? 1: 0;
            }
        }
    }
}
