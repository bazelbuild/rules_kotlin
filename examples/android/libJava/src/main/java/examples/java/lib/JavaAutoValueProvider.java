package examples.java.lib;

public class JavaAutoValueProvider {

    public TestJavaValue getAutoValue() {
        return new AutoValue_TestJavaValue.Builder().setName("Auto Value Test").build();

    }
}