package sparkling.function;

import clojure.lang.IFn;

public class Function extends sparkling.serialization.AbstractSerializableWrappedIFn implements org.apache.spark.api.java.function.Function, org.apache.spark.sql.api.java.UDF1 {
    public Function(IFn func) {
        super(func);
    }

    public Object call(Object v1) throws Exception {
        return f.invoke(v1);
    }
}
