package plugin.serialization

import org.junit.*
import org.junit.Assert.assertNotNull

class SerializationTest {
  @Test
  fun dataShouldHaveASerializerMethod() {

    assertNotNull(Data.serializer())
  }
}
