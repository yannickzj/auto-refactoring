package refactor.analysis;

import java.util.function.Function;
import java.util.function.Supplier;

public class TestLambda <T> {

    private Class<T> myClass;

    public TestLambda(Class<T> cls){
        myClass = cls;
    }

    public int runTemplate(boolean flag, Supplier<Integer> add, Supplier<Integer> add2) {
        //final int y = 5;
        //System.out.println(add.get());
        if (flag) return add.get();
        else return add2.get();
    }

    public int runTemplate1(boolean flag, CustomInterface<String, Integer> fun) {
        //final int y = 5;
        //System.out.println(add.get());
        if (flag) return fun.apply("hello");
        else return fun.apply("world");
    }

    public AnonInner getAnonInner(final int x) {
        final int y = 5;
        return new AnonInner() {
            public int add() {
                return x + y;
            }
        };
    }

    public void testMain(boolean flag) throws Exception {
        //myClass.getDeclaredConstructor(Integer.TYPE, Long.TYPE).newInstance(10);
        int y = 10;
        int z = 5;
        int result = runTemplate(
                flag,
                () -> y + 1,
                () -> y + z + 2
        );
        System.out.println(result);
    }

}

