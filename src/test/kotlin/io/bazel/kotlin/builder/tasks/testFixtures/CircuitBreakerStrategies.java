package something;

public class CircuitBreakerStrategies {
    public static CircuitBreakerStrategy alwaysClosed() {
        return AlwaysClosedStrategy.INSTANCE;
    }
}

