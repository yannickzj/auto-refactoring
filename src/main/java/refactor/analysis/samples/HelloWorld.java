package refactor.analysis.samples;

import java.util.Random;

public class HelloWorld {
    public static void main(String[] args) {
        int randomNum = new Random().nextInt();
        System.out.println("hello, world! " + randomNum);
    }
}
