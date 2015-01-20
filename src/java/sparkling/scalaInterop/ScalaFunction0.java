package sparkling.scalaInterop;

import clojure.lang.IFn;
import sparkling.serialization.Utils;
import scala.Function0;
import scala.runtime.AbstractFunction0;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ScalaFunction0 extends AbstractFunction0 implements Function0, Serializable {

  private IFn f;

  public ScalaFunction0() {}

  public ScalaFunction0(IFn func) {
    f = func;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    Utils.writeIFn(out, f);
  }
  
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    f = Utils.readIFn(in);
  }

    @Override
    public Object apply() {
        return f.invoke();
    }
}
