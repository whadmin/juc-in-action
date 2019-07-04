package unsafe;

public class TestA {

    public static int b=2;
    private int a = 0;

    private int ACCESS_ALLOWED = 1;

    public boolean giveAccess() {
        return 40 == ACCESS_ALLOWED;
    }

    {
        a=2;
    }

    public TestA() {
        a = 1;
    }

    public int getA() {
        return a;
    }
}
