package sparkling.scalaInterop;

import clojure.lang.IFn;
import sparkling.serialization.Utils;
import scala.Function1;
import scala.runtime.AbstractFunction1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ScalaFunction1 extends AbstractFunction1 implements Function1, Serializable {

  private IFn f;

  public ScalaFunction1() {}

  public ScalaFunction1(IFn func) {
    f = func;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    Utils.writeIFn(out, f);
  }
  
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    f = Utils.readIFn(in);
  }

    @Override
    public Object apply(Object o) {
        return f.invoke(o);
    }
}
