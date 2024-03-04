package something;

public interface MyErrorReporter {
  public void report(Throwable e, boolean fatal);
}
