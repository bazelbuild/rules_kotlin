package coffee;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class CoffeeAppJavaModel {

    abstract CoffeeAppModel coffeeAppModel();

    Builder builder() {
        return new AutoValue_CoffeeAppJavaModel.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder setCoffeeAppModel(CoffeeAppModel coffeeAppModel);

        abstract CoffeeAppJavaModel build();
    }
}
