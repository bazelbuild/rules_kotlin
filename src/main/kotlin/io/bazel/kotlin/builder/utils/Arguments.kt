package io.bazel.kotlin.builder.utils

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Arguments derived from an array of strings.
 *
 * Usage:
 *   class Command(a:Arguments) {
 *     val aString by a.flag(name="string", description = "an interesting string") {
 *       first()
 *     }
 *   }
 *
 *
 *   Arguments(*args)
 *     .parseInto(::Command)
 *     .onError { exitCode ->
 *       print(help())
 *     }
 *     .execute()
 *
 */
class Arguments(
  private val arguments: List<String>,
  private val allowUnused: Boolean = false,
  private val fs: FileSystem = FileSystems.getDefault()
) {
  constructor(vararg arguments: String) : this(arguments.toList())

  private val options = mutableMapOf<String, FlagReadOnlyProperty<*>>()

  /** Custom provides a fluid interface for declaring multiple value flag */
  interface Custom {
    fun <T : Any> flag(
      name: String,
      description: String,
      default: T,
      required: Boolean = false,
      convert: ListIterator<String>.(T) -> T
    ): ReadOnlyProperty<Any?, T>

    fun <T : Any> flag(
      name: String,
      description: String,
      required: Boolean = false,
      convert: ListIterator<String>.(T?) -> T?
    ): ReadOnlyProperty<Any?, T?>
  }

  /**
   * Result of parsing Arguments.
   */
  inner class ParseResult<T>(
    val errs: Collection<String>,
    val cmd: T,
    val remaining: List<String>
  ) {
    private infix fun String.ln(n: Any): String {
      return this + (if (isNotEmpty()) "\n" else "") + n.toString()
    }

    /**
     * help provides a test representation of the valid arguments.
     */
    fun help(): String {
      return "Flags:" ln options.entries.joinToString("\n") { (n, o) -> "  --$n: ${o.description}" }
    }

    fun ifError(handle: ParseResult<T>.() -> Unit): ParseResult<T> {
      if (errs.isNotEmpty()) {
        handle()
      }
      return this
    }

    inline fun then(
      onError: ParseResult<T>.() -> Unit = {},
      enact: T.(ParseResult<T>) -> Unit
    ): T? =
      if (errs.isNotEmpty()) {
        onError().let { null }
      } else {
        cmd.enact(this).run { if (errs.isEmpty()) cmd else null }
      }
  }

  private interface FlagReadOnlyProperty<T> : ReadOnlyProperty<Any?, T> {
    fun parse(args: ListIterator<String>)
    val isSatisfied: Boolean
    val description: String
  }

  /** FlagReadOnlyProperty converts and stores parsed flag value(s) */
  private class NonNullFlagReadOnlyProperty<T : Any>(
    private val convert: ListIterator<String>.(T) -> T,
    var value: T,
    val required: Boolean,
    override val description: String,
  ) : FlagReadOnlyProperty<T> {

    private var mustParse: Boolean = !required

    override fun parse(args: ListIterator<String>) {
      value = args.convert(value)
      mustParse = true
    }

    override val isSatisfied: Boolean get() = mustParse

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
      return value
    }
  }

  /** FlagReadOnlyProperty converts and stores parsed flag value(s) */
  private class NullFlagReadOnlyProperty<T : Any>(
    private val convert: ListIterator<String>.(T?) -> T?,
    required: Boolean,
    override val description: String,
    var value: T? = null
  ) : FlagReadOnlyProperty<T?> {

    private var mustParse: Boolean = !required

    override fun parse(args: ListIterator<String>) {
      value = args.convert(value)
      mustParse = true
    }

    override val isSatisfied: Boolean get() = mustParse

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
      return value
    }
  }

  /**
   * custom indicates that argument conversion will consume any number of values.
   *
   * Usage:
   *
   *  class Complicated(a:Arguments) {
   *    val tokenList by a.custom.flag("a", "list of tokens", emptyList<String>()) {
   *
   *    }
   *  }
   */
  val custom: Custom
    get() = object : Custom {

      override fun <T : Any> flag(
        name: String,
        description: String,
        default: T,
        required: Boolean,
        convert: ListIterator<String>.(T) -> T
      ): ReadOnlyProperty<Any?, T> {
        return NonNullFlagReadOnlyProperty(
          convert = convert,
          description = description,
          required = required,
          value = default,
        ).also { options[name] = it }
      }

      override fun <T : Any> flag(
        name: String,
        description: String,
        required: Boolean,
        convert: ListIterator<String>.(T?) -> T?
      ): ReadOnlyProperty<Any?, T?> {
        return NullFlagReadOnlyProperty(
          convert = convert,
          description = description,
          required = required,
        ).also { options[name] = it }
      }
    }

  /**
   * flag defines a command line attribute of "--$name" populated using convert.
   *
   * @param name is used to derive the command line flag
   * @param description of the flag usage
   * @param default value for the flag
   * @param required flag must be set
   * @param convert a List<String> into expected value. Any exception is treated as a failed conversion.
   *
   * @return ReadOnlyProperty property for the value.
   */
  fun <T : Any> flag(
    name: String,
    description: String,
    default: T,
    required: Boolean = false,
    convert: String.(T) -> T
  ): ReadOnlyProperty<Any?, T> {
    return NonNullFlagReadOnlyProperty(
      convert = { last: T -> nextFor(name).convert(last) },
      value = default,
      description = description,
      required = required
    ).also { options[name] = it }
  }

  /**
   * flag defines a command line attribute of "--$name" populated using convert.
   *
   * @param name is used to derive the command line flag
   * @param description of the flag usage
   * @param required flag must be set
   * @param convert a List<String> into expected value. Any exception is treated as a failed conversion.
   *
   * @return ReadOnlyProperty property for the value.
   */
  fun <T : Any> flag(
    name: String,
    description: String,
    required: Boolean = false,
    convert: String.(T?) -> T
  ): ReadOnlyProperty<Any?, T?> {
    return NullFlagReadOnlyProperty(
      convert = { last: T? -> nextFor(name).convert(last) },
      description = description,
      required = required,
    ).also { options[name] = it }
  }

  fun flag(
    name: String,
    description: String,
    required: Boolean=false,
    default: String = ""
  ) = flag(name = name, description = description, required = required, default = default) {
    toString()
  }

  private fun ListIterator<String>.nextFor(name: String) = when {
    hasNext() -> next()
    else -> error("Expected argument for $name")
  }


  /**
   * Argument derived from a string token.
   */
  private data class Argument(
    val key: String,
    val values: MutableList<String> = mutableListOf(),
    val flag: String? = if (key.startsWith("--")) key.substring(2) else null,
    val end: Boolean = key == "--",
    val file: Boolean = key.startsWith("@")
  )

  private fun convertToFlag(arg: Argument, args: ListIterator<String>): String? {
    return options[arg.flag]
      ?.run {
        try {
          parse(args)
          return null
        } catch (e: Throwable) {
          return "Failed to parse ${arg.flag}: $e"
        }
      }
      ?: if (allowUnused) null else "unexpected argument: ${arg.flag}"
  }

  /**
   * parseInto a newCommand and return a Result the parse.
   */
  fun <COMMAND> parseInto(newCommand: (Arguments) -> COMMAND): ParseResult<COMMAND> {
    val cmd = newCommand(this)
    val tokens = arguments.asSequence().flatMap { t ->
      if (t.startsWith("@")) {
        Files.readAllLines(fs.getPath(t.drop(1))).asSequence()
      } else {
        sequenceOf(t)
      }
    }.toList().listIterator()
    val (remainder, errs) = parse(tokens)
    return ParseResult(errs, cmd, remainder)
  }

  private fun parse(tokens: ListIterator<String>): Pair<List<String>, Set<String>> {
    val errs = mutableSetOf<String>()
    while (tokens.hasNext()) {
      val arg = Argument(tokens.next())
      if (arg.end) {
        break
      }
      when {
        arg.end -> return tokens.asSequence().toList() to errs
        arg.flag != null -> {
          convertToFlag(arg, ConditionalListIterator(tokens) {
            it.startsWith("--")
          })?.let(errs::add)
        }
        else -> {
          errs.add("unexpected argument $arg")
        }
      }
    }
    options
      .filterNot { (_, flag) -> flag.isSatisfied }
      .forEach { (name, _) ->
        errs.add("--$name is required")
      }
    return emptyList<String>() to errs
  }

  /** ConditionalListIterator reads arguments until the condition is satisfied. */
  private class ConditionalListIterator(
    private val args: ListIterator<String>,
    private val done: (String) -> Boolean
  ) : ListIterator<String> by args {
    override fun hasNext(): Boolean {
      val hasNext = args.hasNext()
      if (hasNext && done(args.next().also { args.previous() })) {
        return false
      }
      return hasNext
    }
  }
}
