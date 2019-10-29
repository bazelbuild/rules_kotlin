package app

import com.expedia.graphql.SchemaGeneratorConfig
import com.expedia.graphql.TopLevelObject
import com.expedia.graphql.toSchema
import graphql.GraphQL

data class Foo(val name: String, val age: Int)

class Query {
  fun foo(bar: String?) = Foo("$bar!", 42)
}

class MyApp {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val schema = toSchema(queries = listOf(TopLevelObject(Query())),
          config = SchemaGeneratorConfig(listOf("app")))

      val graphql = GraphQL.newGraphQL(schema).build()

      val result = graphql.execute("""{ foo(bar: "baz") { name, age } }""").toSpecification()

      println(result)
    }
  }
}
