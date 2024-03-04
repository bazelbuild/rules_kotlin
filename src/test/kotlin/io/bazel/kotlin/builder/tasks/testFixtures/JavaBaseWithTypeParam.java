package something;

public class JavaBaseWithTypeParam<T> {
  public T passthru(T t) {
    return t;
  }
  protected Client1 client1 = new Client1();
  protected TestClient2 client2 = new TestClient2();
}
