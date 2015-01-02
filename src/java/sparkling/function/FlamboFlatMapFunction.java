package sparkling.function;

import clojure.lang.AFunction;
import sparkling.kryo.AbstractSerializableWrappedAFunction;
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
