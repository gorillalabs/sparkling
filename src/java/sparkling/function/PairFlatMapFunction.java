package sparkling.function;

import clojure.lang.IFn;
import scala.Tuple2;

public class PairFlatMapFunction extends sparkling.serialization.AbstractSerializableWrappedIFn implements org.apache.spark.api.java.function.PairFlatMapFunction {
    public PairFlatMapFunction(IFn func) {
        super(func);
    }

    @SuppressWarnings("unchecked")
  public Iterable<Tuple2<Object, Object>> call(Object v1) throws Exception {
    return (Iterable<Tuple2<Object, Object>>) f.invoke(v1);
  }
}
