package flambo.function;

import clojure.lang.AFunction;
import flambo.kryo.AbstractSerializableWrappedAFunction;
import org.apache.spark.api.java.function.Function2;

public class FlamboFunction2 extends AbstractSerializableWrappedAFunction implements Function2 {
    public FlamboFunction2(AFunction func) {
        super(func);
    }

    public Object call(Object v1, Object v2) throws Exception {
    return f.invoke(v1, v2);
  }
}
