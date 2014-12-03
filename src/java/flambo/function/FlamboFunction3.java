package flambo.function;

import clojure.lang.AFunction;
import flambo.kryo.AbstractSerializableWrappedAFunction;
import org.apache.spark.api.java.function.Function3;

public class FlamboFunction3 extends AbstractSerializableWrappedAFunction implements Function3 {
    public FlamboFunction3(AFunction func) {
        super(func);
    }

    public Object call(Object v1, Object v2, Object v3) throws Exception {
    return f.invoke(v1, v2, v3);
  }
}
