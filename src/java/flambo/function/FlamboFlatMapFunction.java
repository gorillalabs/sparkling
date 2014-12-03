package flambo.function;

import clojure.lang.AFunction;
import flambo.kryo.AbstractSerializableWrappedAFunction;
import org.apache.spark.api.java.function.FlatMapFunction;

public class FlamboFlatMapFunction extends AbstractSerializableWrappedAFunction implements FlatMapFunction {

    public FlamboFlatMapFunction(AFunction func) {
        super(func);
    }

    @SuppressWarnings("unchecked")
  public Iterable<Object> call(Object v1) throws Exception {
    return (Iterable<Object>) f.invoke(v1);
  }
  
}
