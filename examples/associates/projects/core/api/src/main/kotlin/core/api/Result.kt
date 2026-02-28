package core.api

inline fun <V : Any?> Result<V>.flatMapError(transform: (Exception) -> Result<V>) = when (this) {
    is Result.Success -> Result.Success(value)
    is Result.Failure -> transform(error)
}

sealed class Result<out V : Any?> {

    open operator fun component1(): V? = null
    open operator fun component2(): Exception? = null

    inline fun <X> fold(success: (V) -> X, failure: (Exception) -> X): X = when (this) {
        is Success -> success(this.value)
        is Failure -> failure(this.error)
    }

    abstract fun get(): V

    class Success<out V : Any?>(val value: V) : Result<V>() {
        override fun component1(): V? = value

        override fun get(): V = value

        override fun toString() = "[Success: $value]"

        override fun hashCode(): Int = value.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Success<*> && value == other.value
        }
    }

    class Failure(val error: Exception) : Result<Nothing>() {
        override fun component2(): Exception? = error

        override fun get() = throw error

        fun getException(): Exception = error

        override fun toString() = "[Failure: $error]"

        override fun hashCode(): Int = error.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Failure && error == other.error
        }
    }

    companion object {
        // Factory methods
        fun error(ex: Exception) = Failure(ex)

        fun <V : Any?> success(v: V) = Success(v)

        fun <V : Any?> of(value: V?, fail: (() -> Exception) = { Exception() }): Result<V> =
                value?.let { success(it) } ?: error(fail())

        fun <V : Any?> of(f: () -> V): Result<V> = try {
            success(f())
        } catch (ex: Exception) {
            error(ex)
        }
    }
}
