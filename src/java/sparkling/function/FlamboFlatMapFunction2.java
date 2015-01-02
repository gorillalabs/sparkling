package sparkling.function;

import clojure.lang.AFunction;
import sparkling.kryo.AbstractSerializableWrappedAFunction;
import org.apache.spark.api.java.function.FlatMapFunction2;

public class FlamboFlatMapFunction2 extends AbstractSerializableWrappedAFunction implements FlatMapFunction2 {
    public FlamboFlatMapFunction2(AFunction func) {
        super(func);
    }

    @SuppressWarnings("unchecked")
  public Iterable<Object> call(Object v1, Object v2) throws Exception {
    return (Iterable<Object>) f.invoke(v1, v2);
  }
  
}
