package examples.java.lib;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class TestJavaValue {
    abstract String name();

    Builder builder() {
        return new AutoValue_TestJavaValue.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder setName(String name);

        abstract TestJavaValue build();
    }
}