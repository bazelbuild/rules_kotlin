package something;


public class MyHttpClientBuilder {
    public MyHttpClientBuilder name(String name) {
        return this;
    }

    public MyHttpClientBuilder circuitBreakerStrategy(CircuitBreakerStrategy strategy) {
        return this;
    }

    public something.MyHttpClient build() {
        return new MyHttpClient();
    }
}
