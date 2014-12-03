package flambo.function;

import clojure.lang.AFunction;
import flambo.kryo.AbstractSerializableWrappedAFunction;
import org.apache.spark.api.java.function.Function;

public class FlamboFunction extends AbstractSerializableWrappedAFunction implements Function {
    public FlamboFunction(AFunction func) {
        super(func);
    }

    public Object call(Object v1) throws Exception {
        return f.invoke(v1);
    }
}
