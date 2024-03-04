package something;

public class AlwaysClosedStrategy implements CircuitBreakerStrategy {
    public static final AlwaysClosedStrategy INSTANCE = new AlwaysClosedStrategy();
}
