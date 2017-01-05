package sparkling.function;
import java.util.Iterator;
import java.lang.Object;
import java.util.Collection;



import clojure.lang.IFn;
import scala.Tuple2;

public class PairFlatMapFunction extends sparkling.serialization.AbstractSerializableWrappedIFn implements org.apache.spark.api.java.function.PairFlatMapFunction {
    public PairFlatMapFunction(IFn func) {
        super(func);
    }

  @SuppressWarnings("unchecked")
  public Iterator<Tuple2<Object, Object>> call(Object v1) throws Exception {
      return (Iterator<Tuple2<Object, Object>>) ((Collection) f.invoke(v1)).iterator();
  }
}
