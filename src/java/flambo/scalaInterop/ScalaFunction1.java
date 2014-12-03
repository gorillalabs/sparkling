package flambo.scalaInterop;

import clojure.lang.AFunction;
import flambo.kryo.Utils;
import scala.Function1;
import scala.runtime.AbstractFunction1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ScalaFunction1 extends AbstractFunction1 implements Function1, Serializable {

  private AFunction f;

  public ScalaFunction1() {}

  public ScalaFunction1(AFunction func) {
    f = func;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    Utils.writeAFunction(out, f);
  }
  
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    f = Utils.readAFunction(in);
  }

    @Override
    public Object apply(Object o) {
        return f.invoke(o);
    }
}
