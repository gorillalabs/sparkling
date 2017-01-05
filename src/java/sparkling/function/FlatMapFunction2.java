package sparkling.function;
import java.util.Iterator;
import java.lang.Object;
import java.util.Collection;

import clojure.lang.IFn;

public class FlatMapFunction2 extends sparkling.serialization.AbstractSerializableWrappedIFn implements org.apache.spark.api.java.function.FlatMapFunction2 {
    public FlatMapFunction2(IFn func) {
        super(func);
    }

  @SuppressWarnings("unchecked")
  public java.util.Iterator call(Object v1, Object v2) throws Exception {
      return (java.util.Iterator) ((Collection) f.invoke(v1, v2)).iterator();
  }
}
