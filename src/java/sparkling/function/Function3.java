package sparkling.function;

import clojure.lang.IFn;

public class Function3 extends sparkling.serialization.AbstractSerializableWrappedIFn implements org.apache.spark.api.java.function.Function3 {
    public Function3(IFn func) {
        super(func);
    }

    public Object call(Object v1, Object v2, Object v3) throws Exception {
    return f.invoke(v1, v2, v3);
  }
}
