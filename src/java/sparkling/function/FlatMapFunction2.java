package sparkling.function;

import clojure.lang.IFn;

public class FlatMapFunction2 extends sparkling.serialization.AbstractSerializableWrappedIFn implements org.apache.spark.api.java.function.FlatMapFunction2 {
    public FlatMapFunction2(IFn func) {
        super(func);
    }

    @SuppressWarnings("unchecked")
  public Iterable<Object> call(Object v1, Object v2) throws Exception {
    return (Iterable<Object>) f.invoke(v1, v2);
  }
  
}
