package cs454;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////TEMPORARY CODE(?)
/*
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
*/
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.SourceRoot;

import java.nio.file.Paths;

/**
 * Main Class
*/
public class ConcurrencyRepairTool
{
    public static void main( String[] args )
    {
        // Names of source java programs to be modified by the application.
        // All must be inside the src/main/sources folder.
        String[] sources = {"sample.java", "sample(copy).java"};/////////////////////////////////////////////////////////////////MAY CHANGE

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
        for(String src : sources) {
			repTool.repair(src);
        }
        
        // Finish
        repTool.saveAll();
    }
}

class ReparationTool
{
    // SourceRoot is a tool that read and writes Java files from packages on a certain root directory.
    // In this case the root directory is found by taking the root from the current Maven module,
    // with src/main/sources appended.
    private SourceRoot sourceRoot;
    private int popSize; // Population size (per improved program)
    private int fitnessEval; // Number of fitness evaluations (per improved program)
    private int offspringCnt; // Number of new offsprings per generation (per program)
    private int mutationCnt; // Number of mutations performed per new offspring

    ReparationTool(int popSize, int fitnessEval, float offsprings, int mutationCnt) {
        this.sourceRoot = new SourceRoot(CodeGenerationUtils.mavenModuleRoot(ConcurrencyRepairTool.class).resolve("src/main/sources"));
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

        // Generate the initial population
        int popSize = (this.popSize < this.fitnessEval)? this.popSize: this.fitnessEval; // Constrained by fitness evaluation limit
        Solution population[] = new Solution[popSize]; 
        for (int i = 0; i < popSize; i++)
            population[i] = new Solution(cu.clone());
        // Continue until all fitness evaluations are performed
        this.fitnessEval -= popSize;
        while (this.fitnessEval > 0)
            this.fitnessEval -= this.nextGeneration();
    }

    private int nextGeneration()
    {
        // Set the number of offsprings to be generated (Constrained by fitness evaluation limit)
        int offsprings = (this.offspringCnt < this.fitnessEval)? this.offspringCnt: this.fitnessEval;
        // Generate the offsprings and update the population
        for (int i = 0; i < offsprings; i++)
            Log.info("Next Generation");///////////////////////////////////////////////////////////////////////////////////////NOT FINISHED
        return offsprings;
        
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////SAMPLE JAVAPARSER CODE
        /*
        cu.accept(new ModifierVisitor<Void>() {
             * For every if-statement, see if it has a comparison using "!=".
             * Change it to "==" and switch the "then" and "else" statements around.
            @Override
            public Visitable visit(IfStmt n, Void arg) {
                // Figure out what to get and what to cast simply by looking at the AST in a debugger! 
                n.getCondition().ifBinaryExpr(binaryExpr -> {
                    if (binaryExpr.getOperator() == BinaryExpr.Operator.NOT_EQUALS && n.getElseStmt().isPresent()) {
                        * It's a good idea to clone nodes that you move around.
                            JavaParser (or you) might get confused about who their parent is!
                        *
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
    }

    // This saves all the files we just repaired to an output directory. 
    public void saveAll() //////////////////////////////////////////////////////////////////////////ISSUES: May not print the best solution
    {

        this.sourceRoot.saveAll(
            // The path of the Maven module/project which contains the ConcurrencyRepairTool class.
            CodeGenerationUtils.mavenModuleRoot(ConcurrencyRepairTool.class)
                // appended with a path to "output"
                .resolve(Paths.get("output")));
    }
}

class Solution
{
    private CompilationUnit cu;
    public int fitness;

    Solution(CompilationUnit cu) 
    {
        Log.info("Creating solution");/////////////////////////////////////////////////////////////////////////////////////////NOT FINISHED
        this.cu = cu; // Copy of the original
        // 

        // Evaluate
        this.getFitness();
    }

    private void getFitness()
    {
        Log.info("Evaluating solution");///////////////////////////////////////////////////////////////////////////////////////NOT FINISHED
    }
}
