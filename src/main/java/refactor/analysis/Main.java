package refactor.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StreamProvider;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.DotPrinter;
import com.github.javaparser.printer.JsonPrinter;
import com.github.javaparser.printer.XmlPrinter;
import com.github.javaparser.printer.YamlPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;
import org.slf4j.LoggerFactory;
//import refactor.analysis.transform.RefactorTransformer;
import refactor.analysis.transform.RefactorTransformer;
import soot.PackManager;
//import soot.PhaseOptions;
import soot.Transform;
import soot.options.Options;
import org.slf4j.Logger;

import static com.github.javaparser.ast.type.PrimitiveType.intType;

public class Main {
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            System.out.println(arg);
        }

        // use java8 as soot cp
        //String sootCP = "inputs/:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/jce.jar:lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar";
        //String sootCP = "inputs/:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar:lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar";

        // use java7 as soot cp
        //String sootCP = "inputs/:/Library/Java/JavaVirtualMachines/zulu1.7.0_171.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/zulu1.7.0_171.jdk/Contents/Home/jre/lib/jce.jar:lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar";

        //String[] sootArgs = new String[]{"-cp", sootCP, "-p", "jb", "use-original-names:true", "-f", "jimple", "-w", "-process-dir", "inputs/"};
        //PackManager.v().getPack("wjtp").add(new Transform("wjtp.rf", new RefactorTransformer()));
        //PackManager.DEBUG = true;

        //soot.Main.main(sootArgs);

        //TestLambda testLambda = new TestLambda(Integer.class);
        //testLambda.testMain(true);



        //String project = "commons-io";
        //SourceRoot sourceRoot = new SourceRoot(CodeGenerationUtils.mavenModuleRoot(Main.class).resolve("inputs/" + project));
        File file1 = new File("inputs/commons-io/org/apache/commons/io/input/NullInputStreamTestOR.java");
        File file2 = new File("inputs/commons-io/org/apache/commons/io/input/NullReaderTestOR.java");

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JarTypeSolver(new File("lib/junit-4.12.jar")));
        /*
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/test/resources/javaparser_src/proper_source")));
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/test/resources/javaparser_src/generated")));
        */

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
        JavaParser parser = new JavaParser(parserConfiguration);

        CompilationUnit cu1 = parser.parse(
                ParseStart.COMPILATION_UNIT,
                new StreamProvider(new FileInputStream(file1))).getResult().get();
        CompilationUnit cu2 = parser.parse(
                ParseStart.COMPILATION_UNIT,
                new StreamProvider(new FileInputStream(file2))).getResult().get();

        //CompilationUnit compilationUnit = JavaParser.parse("class A { }");
        //CompilationUnit cu1 = sourceRoot.parse("org.apache.commons.io.input", "NullInputStreamTestOR.java");
        //CompilationUnit cu2 = sourceRoot.parse("org.apache.commons.io.input", "NullReaderTestOR.java");
        cu1.accept(new MethodVisitor(), null);

        // visit and change the methods names and parameters
        //cu1.accept(new MethodChangerVisitor(), null);
        //changeMethods(cu1);

        // prints the changed compilation unit
        //System.out.println(cu1);

        //Optional<ClassOrInterfaceDeclaration> classA = compilationUnit.getClassByName("A");
        System.out.println(cu1.toString());
        //System.out.println(cu2.toString());

    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class MethodVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodDeclaration n, Void arg) {
            /* here you can access the attributes of the method.
             this method will be called for all methods in this
             CompilationUnit, including inner class methods */
            System.out.println(n.getName());
            super.visit(n, arg);
        }
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class MethodChangerVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodDeclaration n, Void arg) {
            // change the name of the method to upper case
            n.setName(n.getNameAsString().toUpperCase());

            // add a new parameter to the method
            n.addParameter("int", "value");
        }
    }

    private static void changeMethods(CompilationUnit cu) {
        // Go through all the types in the file
        NodeList<TypeDeclaration<?>> types = cu.getTypes();
        for (TypeDeclaration<?> type : types) {
            System.out.println(type.getName());
            // Go through all fields, methods, etc. in this type
            NodeList<BodyDeclaration<?>> members = type.getMembers();
            for (BodyDeclaration<?> member : members) {
                if (member instanceof MethodDeclaration) {

                    MethodDeclaration method = (MethodDeclaration) member;
                    //System.out.println(method.resolve().getQualifiedSignature());
                    //System.out.println(method.getName());

                    BlockStmt body = method.getBody().get();
                    //System.out.println(new DotPrinter(true).output(body));
                    //System.out.println(new JsonPrinter(true).output(body));
                    //System.out.println(new YamlPrinter(true).output(body));
                    //System.out.println(new XmlPrinter(true).output(body));
                    for (Statement stmt: body.getStatements()) {
                        if (stmt.isExpressionStmt()) {
                            ExpressionStmt expr = stmt.asExpressionStmt();
                            System.out.println(expr.getExpression().calculateResolvedType().describe());
                        }
                        //System.out.println(stmt.isAssertStmt());
                        //System.out.println(stmt.getMetaModel().getTypeName());
                    }
                    //changeMethod(method);
                }
            }
        }
    }

    private static void changeMethod(MethodDeclaration n) {
        // change the name of the method to upper case
        n.setName(n.getNameAsString().toUpperCase());

        // create the new parameter
        n.addParameter(intType(), "value");
    }
}
