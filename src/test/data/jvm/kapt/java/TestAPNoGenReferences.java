/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests.smoke.kapt.java;

import com.google.auto.value.AutoValue;

// unlike TestAutoValue, this class shouldn't fail compilation. Used to test that java annotation processing is disabled unless plugins are present.
@AutoValue
public abstract class TestAPNoGenReferences {
    abstract String name();

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder setName(String name);

        abstract TestAPNoGenReferences build();
    }
}
