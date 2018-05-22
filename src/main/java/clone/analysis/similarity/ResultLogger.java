package clone.analysis.similarity;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Stack;

import soot.G;

public class ResultLogger {
    public static boolean CONSOLE = false;
    public static boolean FILE_LOG = true;
    static Stack<BufferedWriter> fouts = new Stack<BufferedWriter>();

    public static void createLog(String s) {
        G.v().out.println("\nLogging to file: " + s);
        if (FILE_LOG) {
            try {
                fouts.push(new BufferedWriter(new FileWriter(s)));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void closeLog() {
        if (FILE_LOG) {
            try {
                fouts.peek().flush();
                fouts.peek().close();
                fouts.pop();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void log(Object o) {
        if (CONSOLE)
            System.out.println(o);
        if (FILE_LOG) {
            try {
                fouts.peek().write(o.toString() + '\n');
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
