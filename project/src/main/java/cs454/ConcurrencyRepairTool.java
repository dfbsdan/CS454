package cs454;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////TEMPORARY CODE(?)

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.SourceRoot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Main Class
*/
public class ConcurrencyRepairTool
{
    public static void main( String[] args )
    {
        if(args.length != 4) { //////////////////////////////////////////////////////////////////////////////////////////////////MAY CHANGE
            throw new IllegalArgumentException("Wrong amount of arguments.");
        }
        // Population size (per program repaired)
        int populationSize = Integer.parseInt(args[0]);
        if(populationSize <= 0) {
            throw new IllegalArgumentException("The population size has to be greater than 0.");
        }
        // Number of fitness evaluations (per program repaired)
        int fitnessEvaluations = Integer.parseInt(args[1]);
        if(fitnessEvaluations <= 0) {
            throw new IllegalArgumentException("The amount of fitness evaluations has to be greater than 0.");
        }
        // Percentage of offsprings per generation (per program repaired)
        float offsprings = Float.parseFloat(args[2]);
        if(offsprings <= 0 || offsprings > 1) {
            throw new IllegalArgumentException("Incorrect offspring percentage.");
        }
        int mutationCnt = Integer.parseInt(args[3]);
        if(mutationCnt < 0) {
            throw new IllegalArgumentException("The amount of mutations has to be greater or equal to 0.");
        }

        // JavaParser has a minimal logging class that normally logs nothing.
        // Let's ask it to write to standard out:
        Log.setAdapter(new Log.StandardOutStandardErrorAdapter());

        ReparationTool repTool = new ReparationTool(populationSize, fitnessEvaluations, offsprings, mutationCnt);

        // Repair all sources
        for(String src : repTool.sources)
            repTool.repair(src);

        // Finish
        repTool.saveAll();
    }
}

class ReparationTool
{
    // The path of the Maven module/project which contains the ConcurrencyRepairTool class.
    // appended with a path to "output".
    private final String outputFile = CodeGenerationUtils.mavenModuleRoot(ConcurrencyRepairTool.class)
        .resolve(Paths.get("output")).toString();
    // Names of source java programs to be modified by the application.
    // All must be inside the src/main/sources folder.
    public String[] sources;
    // SourceRoot is a tool that read and writes Java files from packages on a certain root directory.
    // In this case the root directory is found by taking the root from the current Maven module,
    // with src/main/sources appended.
    private SourceRoot sourceRoot;
    private int popSize; // Population size (per improved program)
    private int fitnessEval; // Number of fitness evaluations (per improved program)
    private int offspringCnt; // Number of new offsprings per generation (per program)
    private int mutationCnt; // Number of mutations performed per new offspring

    ReparationTool(int popSize, int fitnessEval, float offsprings, int mutationCnt) {
        Path sourceRoot = CodeGenerationUtils.mavenModuleRoot(ConcurrencyRepairTool.class)
            .resolve("src/main/sources");
        try {
            this.sources = Files.list(sourceRoot).filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString()).toArray(String[]::new);
        } catch(IOException e) {
            e.getCause();
        }
        this.sourceRoot = new SourceRoot(sourceRoot);
        this.popSize = popSize;
        this.fitnessEval = fitnessEval;
        this.offspringCnt = (int)(offsprings * popSize);
        if (this.offspringCnt == 0)
            this.offspringCnt = 1; // At least one offspring per generation
        this.mutationCnt = mutationCnt;
    }

    public void repair(String src)
    {
        CompilationUnit cu = this.sourceRoot.parse("", src);
        Log.info("Repairing: " + src);

        // Create the population (size constrained by fitness evaluation limit)
        int popSize = (this.popSize < this.fitnessEval)? this.popSize: this.fitnessEval;
        Solution population[] = new Solution[popSize]; // Sorted array of solutions (by fitness)
        // Generate the initial population
        for (int i = 0; i < popSize; i++)
            population[i] = new Solution(cu.clone());
        Arrays.sort(population, Solution::compare);
        int fitnessEval = this.fitnessEval - popSize;
        // Continue until all fitness evaluations are performed
        while (fitnessEval > 0)
            fitnessEval -= this.nextGeneration(population);
        // Save the best solution
        this.sourceRoot.add(this.outputFile, src, population[0].cu);
    }

    private int nextGeneration(Solution population[]) /////////////////////////////////////////////////////////////////////////NOT FINISHED
    {
        // Set the number of offsprings to be generated (Constrained by fitness evaluation limit)
        int offspringCnt = (this.offspringCnt < this.fitnessEval)? this.offspringCnt: this.fitnessEval;
        // Generate the offsprings
        Solution offsprings[] = new Solution[offspringCnt];
        for (int i = 0; i < offspringCnt; i++) {
            int parents[] = this.getParents();
            offsprings[i] = new Solution(population, parents[0], parents[1]);
        }
        // Update the population

        return offspringCnt;
    }

    private int[] getParents() ////////////////////////////////////////////////////////////////////////////////////////////////NOT FINISHED
    {
        int parents[] = new int[2]; // Parent indexes inside the population
        //Log.info("Choosing parents");

        return parents;
    }

    // This saves all the files we just repaired to an output directory. 
    public void saveAll()
    {
        this.sourceRoot.saveAll();
    }
}

class Solution
{
    public CompilationUnit cu;
    public int fitness;

    // 'Random' solution generator
    Solution(CompilationUnit cu) //////////////////////////////////////////////////////////////////////////////////////////////NOT FINISHED
    {
        //Log.info("Creating 'random' solution");
        this.cu = cu; // Copy of the original
        // Initialize
        
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////SAMPLE JAVAPARSER CODE
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
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Evaluate
        this.getFitness();
    }

    // Creates a solution based on two given parents (i.e. their indexes inside
    // the population array)
    Solution(Solution population[], int p1, int p2) ///////////////////////////////////////////////////////////////////////////NOT FINISHED
    {
        //Log.info("Creating offspring solution");
        // Inherit

        // Mutate

        // Evaluate
        this.getFitness();
    }

    private void getFitness()//////////////////////////////////////////////////////////////////////////////////////////////////NOT FINISHED
    {
        //Log.info("Evaluating solution");
    }

    // Comparator used for keeping the population array sorted
    public static int compare(Solution a, Solution b)
    {
        return b.fitness - a.fitness;
    }
}
