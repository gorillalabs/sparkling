package flambo.function;

import clojure.lang.AFunction;
import flambo.kryo.AbstractSerializableWrappedAFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

public class FlamboPairFlatMapFunction extends AbstractSerializableWrappedAFunction implements PairFlatMapFunction {
    public FlamboPairFlatMapFunction(AFunction func) {
        super(func);
    }

    @SuppressWarnings("unchecked")
  public Iterable<Tuple2<Object, Object>> call(Object v1) throws Exception {
    return (Iterable<Tuple2<Object, Object>>) f.invoke(v1);
  }
}
