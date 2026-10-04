import com.sun.source.util.JavacTask;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Parses (does not compile) every .java file under the given directory and reports syntax errors. */
public class ParseCheck {
    public static void main(String[] args) throws Exception {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(Paths.get(args[0]))) {
            files = stream.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnostics,
            List.of("--release", "25", "-proc:none"), null, fileManager.getJavaFileObjectsFromPaths(files));
        task.parse();
        int errors = 0;
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                errors++;
            }
            System.out.println(d.getKind() + " " + d.getSource().getName() + ":" + d.getLineNumber() + " " + d.getMessage(null));
        }
        System.out.println("parsed " + files.size() + " files, " + errors + " syntax error(s)");
        System.exit(errors == 0 ? 0 : 1);
    }
}
