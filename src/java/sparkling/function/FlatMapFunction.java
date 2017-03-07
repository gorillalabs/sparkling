package sparkling.function;
import java.util.Iterator;
import java.lang.Object;
import java.util.Collection;

import clojure.lang.IFn;

public class FlatMapFunction extends sparkling.serialization.AbstractSerializableWrappedIFn implements org.apache.spark.api.java.function.FlatMapFunction {

    public FlatMapFunction(IFn func) {
        super(func);
    }

  @SuppressWarnings("unchecked")
  public java.util.Iterator<java.lang.Object> call(Object v1) throws Exception {
      return (java.util.Iterator<java.lang.Object>) ((Collection) f.invoke(v1)).iterator();
  }

}
