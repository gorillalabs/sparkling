package flambo.scalaInterop;

import clojure.lang.AFunction;
import flambo.function.Utils;
import scala.Function0;
import scala.runtime.AbstractFunction0;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ScalaFunction0 extends AbstractFunction0 implements Function0, Serializable {

  private AFunction f;

  public ScalaFunction0() {}

  public ScalaFunction0(AFunction func) {
    f = func;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    Utils.writeAFunction(out, f);
  }
  
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    f = Utils.readAFunction(in);
  }

    @Override
    public Object apply() {
        return f.invoke();
    }
}
