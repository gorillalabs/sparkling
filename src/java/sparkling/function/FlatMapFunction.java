package sparkling.function;

import clojure.lang.IFn;

public class FlatMapFunction extends sparkling.serialization.AbstractSerializableWrappedIFn implements org.apache.spark.api.java.function.FlatMapFunction {

    public FlatMapFunction(IFn func) {
        super(func);
    }

    @SuppressWarnings("unchecked")
  public Iterable<Object> call(Object v1) throws Exception {
    return (Iterable<Object>) f.invoke(v1);
  }
  
}
