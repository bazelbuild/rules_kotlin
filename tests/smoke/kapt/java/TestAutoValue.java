package tests.smoke.kapt.java;


import com.google.auto.value.AutoValue;

@AutoValue
public abstract class TestAutoValue {
    abstract String name();


    static Builder builder() {
        return new AutoValue_TestAutoValue.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder setName(String name);
        abstract TestAutoValue build();
    }

}
