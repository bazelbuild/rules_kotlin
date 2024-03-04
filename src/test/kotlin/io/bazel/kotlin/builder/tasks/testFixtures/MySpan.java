package something;

public class MySpan implements Span, MyErrorReporter {
  @Override
  public void report(Throwable e, boolean fatal) {
    System.out.println("Error: " + e);
  }
}
