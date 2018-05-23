package refactor.analysis;

import org.slf4j.LoggerFactory;
//import refactor.analysis.transform.RefactorTransformer;
import refactor.analysis.transform.RefactorTransformer;
import soot.PackManager;
//import soot.PhaseOptions;
import soot.Transform;
import soot.options.Options;
import org.slf4j.Logger;

public class Main {
    public static void main(String[] args) {
        for (String arg : args) {
            System.out.println(arg);
        }

        // use java8 as soot cp
        //String sootCP = "inputs/:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/jce.jar:lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar";
        String sootCP = "inputs/:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar:lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar";

        // use java7 as soot cp
        //String sootCP = "inputs/:/Library/Java/JavaVirtualMachines/zulu1.7.0_171.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/zulu1.7.0_171.jdk/Contents/Home/jre/lib/jce.jar:lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar";

        String[] sootArgs = new String[]{"-cp", sootCP, "-p", "jb", "use-original-names:true", "-f", "jimple", "-w", "-process-dir", "inputs/"};
        //PackManager.v().getPack("wjtp").add(new Transform("wjtp.rf", new RefactorTransformer()));
        //PackManager.DEBUG = true;

        soot.Main.main(sootArgs);
    }
}
