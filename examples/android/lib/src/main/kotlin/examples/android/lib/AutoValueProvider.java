package examples.android.lib;

public class AutoValueProvider {

	public TestJavaValue getAutoValue() {
		return new AutoValue_TestJavaValue.Builder().setName("Auto Value Test").build();

	}
}
