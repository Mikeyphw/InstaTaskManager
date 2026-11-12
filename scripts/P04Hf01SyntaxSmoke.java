import com.sun.source.util.JavacTask;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Parse complete Android UI sources without needing Android SDK stubs. Hosted CI compiles fully. */
public final class P04Hf01SyntaxSmoke {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Need TaskPanel, P04GateTrace, P04TraceSummary paths");
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if (compiler==null) throw new IllegalStateException("JDK compiler unavailable");
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        try (StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,Locale.ROOT,null)) {
            Iterable<? extends JavaFileObject> files=manager.getJavaFileObjectsFromFiles(
                    Arrays.asList(new File(args[0]),new File(args[1]),new File(args[2])));
            JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,
                    Arrays.asList("-proc:none","--release","17"),null,files);
            int units=0;
            for (Object unused: task.parse()) units++;
            if (units!=3) throw new AssertionError("Unexpected Java syntax unit count");
            for (Diagnostic<? extends JavaFileObject> d: diagnostics.getDiagnostics()) {
                if (d.getKind()==Diagnostic.Kind.ERROR)
                    throw new AssertionError("Java syntax error line="+d.getLineNumber()+" code="+d.getCode());
            }
            System.out.println("ITM_P04_HF01_JAVA_UI_SYNTAX_PASS=3");
        }
    }
}
