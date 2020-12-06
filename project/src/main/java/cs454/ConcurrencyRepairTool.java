package cs454;

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////TEMPORARY CODE(?)
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.SourceRoot;

import java.nio.file.Paths;
import java.util.List;

/**
 * Main Class
*/
public class ConcurrencyRepairTool
{
    public static void main( String[] args )
    {
        // Names of source java programs to be modified by the application.
        // All must be inside the src/main/sources folder.
        String[] sources = {"sample.java", "sample(copy).java"};

        // JavaParser has a minimal logging class that normally logs nothing.
        // Let's ask it to write to standard out:
        Log.setAdapter(new Log.StandardOutStandardErrorAdapter());

        ReparationTool repTool = new ReparationTool();

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
    private SourceRoot sourceRoot;

    {
        // SourceRoot is a tool that read and writes Java files from packages on a certain root directory.
        // In this case the root directory is found by taking the root from the current Maven module,
        // with src/main/sources appended.
        this.sourceRoot = new SourceRoot(CodeGenerationUtils.mavenModuleRoot(ConcurrencyRepairTool.class).resolve("src/main/sources"));
    }

    ReparationTool() {}

    public void repair (String src)
    {
        CompilationUnit cu = this.sourceRoot.parse("", src);

        Log.info("Repairing: " + src);

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////TEMPORARY CODE
        cu.accept(new ModifierVisitor<Void>() {
            /**
             * For every if-statement, see if it has a comparison using "!=".
             * Change it to "==" and switch the "then" and "else" statements around.
             */
            @Override
            public Visitable visit(IfStmt n, Void arg) {
                // Figure out what to get and what to cast simply by looking at the AST in a debugger! 
                n.getCondition().ifBinaryExpr(binaryExpr -> {
                    if (binaryExpr.getOperator() == BinaryExpr.Operator.NOT_EQUALS && n.getElseStmt().isPresent()) {
                        /* It's a good idea to clone nodes that you move around.
                            JavaParser (or you) might get confused about who their parent is!
                        */
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
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    // This saves all the files we just repaired to an output directory. 
    public void saveAll()
    {
        this.sourceRoot.saveAll(
            // The path of the Maven module/project which contains the ConcurrencyRepairTool class.
            CodeGenerationUtils.mavenModuleRoot(ConcurrencyRepairTool.class)
                // appended with a path to "output"
                .resolve(Paths.get("output")));
    }
}
