import com.sun.source.tree.BindingPatternTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Finds a local variable, parameter, lambda parameter or pattern binding that redeclares a local still in scope
 * ("variable x is already defined"). javac stays silent about those when the variable's type cannot be resolved,
 * which without the Minecraft jars is nearly every type, so this checks names on the syntax tree instead.
 */
public class DuplicateLocals {
    private static int problems;

    public static void main(String[] args) throws Exception {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(Paths.get(args[0]))) {
            files = stream.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, d -> {}, List.of("--release", "25", "-proc:none"),
            null, fileManager.getJavaFileObjectsFromPaths(files));
        SourcePositions positions = Trees.instance(task).getSourcePositions();
        for (CompilationUnitTree unit : task.parse()) {
            new Scanner(unit, positions).scan(unit, null);
        }
        System.out.println("checked locals in " + files.size() + " files, " + problems + " duplicate(s)");
        System.exit(problems == 0 ? 0 : 1);
    }

    private static final class Scanner extends TreeScanner<Void, Void> {
        private final CompilationUnitTree unit;
        private final SourcePositions positions;
        /** One stack of block scopes per class body being visited (a class body starts with no locals in scope). */
        private final Deque<Deque<Set<String>>> frames = new ArrayDeque<>();

        Scanner(CompilationUnitTree unit, SourcePositions positions) {
            this.unit = unit;
            this.positions = positions;
            frames.push(new ArrayDeque<>());
        }

        private Deque<Set<String>> scopes() {
            return frames.peek();
        }

        private void push() {
            scopes().push(new HashSet<>());
        }

        private void pop() {
            scopes().pop();
        }

        private boolean inScope(String name) {
            for (Set<String> scope : scopes()) {
                if (scope.contains(name)) {
                    return true;
                }
            }
            return false;
        }

        private void check(Tree at, String name) {
            if (!name.equals("_") && inScope(name)) {
                problems++;
                long line = unit.getLineMap().getLineNumber(positions.getStartPosition(unit, at));
                System.out.println("ERROR " + unit.getSourceFile().getName() + ":" + line + " variable " + name
                    + " is already defined in an enclosing scope");
            }
        }

        private void declare(VariableTree variable) {
            String name = variable.getName().toString();
            check(variable, name);
            scopes().peek().add(name);
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            frames.push(new ArrayDeque<>());
            try {
                return super.visitClass(node, unused);
            } finally {
                frames.pop();
            }
        }

        @Override
        public Void visitMethod(MethodTree node, Void unused) {
            push();
            try {
                for (VariableTree parameter : node.getParameters()) {
                    declare(parameter);
                }
                scan(node.getBody(), unused);
                return null;
            } finally {
                pop();
            }
        }

        @Override
        public Void visitVariable(VariableTree node, Void unused) {
            if (scopes().isEmpty()) {
                return super.visitVariable(node, unused); // a field or enum constant, not a local
            }
            declare(node);
            scan(node.getInitializer(), unused);
            return null;
        }

        @Override
        public Void visitBlock(BlockTree node, Void unused) {
            push();
            try {
                return super.visitBlock(node, unused);
            } finally {
                pop();
            }
        }

        @Override
        public Void visitForLoop(ForLoopTree node, Void unused) {
            push();
            try {
                return super.visitForLoop(node, unused);
            } finally {
                pop();
            }
        }

        @Override
        public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
            scan(node.getExpression(), unused);
            push();
            try {
                declare(node.getVariable());
                scan(node.getStatement(), unused);
                return null;
            } finally {
                pop();
            }
        }

        @Override
        public Void visitCatch(CatchTree node, Void unused) {
            push();
            try {
                declare(node.getParameter());
                scan(node.getBlock(), unused);
                return null;
            } finally {
                pop();
            }
        }

        @Override
        public Void visitTry(TryTree node, Void unused) {
            push();
            try {
                scan(node.getResources(), unused);
                scan(node.getBlock(), unused);
            } finally {
                pop();
            }
            scan(node.getCatches(), unused);
            scan(node.getFinallyBlock(), unused);
            return null;
        }

        @Override
        public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
            push();
            try {
                for (VariableTree parameter : node.getParameters()) {
                    declare(parameter);
                }
                scan(node.getBody(), unused);
                return null;
            } finally {
                pop();
            }
        }

        @Override
        public Void visitSwitch(SwitchTree node, Void unused) {
            push();
            try {
                return super.visitSwitch(node, unused);
            } finally {
                pop();
            }
        }

        @Override
        public Void visitSwitchExpression(SwitchExpressionTree node, Void unused) {
            push();
            try {
                return super.visitSwitchExpression(node, unused);
            } finally {
                pop();
            }
        }

        @Override
        public Void visitCase(CaseTree node, Void unused) {
            if (node.getCaseKind() != CaseTree.CaseKind.RULE) {
                return super.visitCase(node, unused); // colon-style cases share the switch block's scope
            }
            push();
            try {
                return super.visitCase(node, unused);
            } finally {
                pop();
            }
        }

        @Override
        public Void visitBindingPattern(BindingPatternTree node, Void unused) {
            // A binding may not shadow a local, but its own scope is flow-dependent, so it is checked, not declared.
            check(node, node.getVariable().getName().toString());
            return null;
        }
    }
}
