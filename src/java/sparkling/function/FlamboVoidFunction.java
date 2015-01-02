package sparkling.function;

import clojure.lang.AFunction;
import sparkling.kryo.AbstractSerializableWrappedAFunction;
import org.apache.spark.api.java.function.VoidFunction;

public class FlamboVoidFunction extends AbstractSerializableWrappedAFunction implements VoidFunction{
  
  @SuppressWarnings("unchecked")
  public void call(Object v1) throws Exception {
    f.invoke(v1);
  }

    public FlamboVoidFunction(AFunction func) {
        super(func);
    }
}
