package flambo.function;

import java.lang.ClassNotFoundException;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import clojure.lang.AFunction;
import clojure.lang.Indexed;

import flambo.function.Utils;

import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;

public class FlamboPairFunction implements PairFunction, Serializable {
  
  private AFunction f;
  
  public FlamboPairFunction() {}
  
  public FlamboPairFunction(AFunction func) {
    f = func;
  }

  @SuppressWarnings("unchecked")
  public Tuple2<Object, Object> call(Object v1) throws Exception {
    return (Tuple2<Object, Object>) f.invoke(v1);
  }
  
  private void writeObject(ObjectOutputStream out) throws IOException {
    Utils.writeAFunction(out, f);
  }
  
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    f = Utils.readAFunction(in);
  }
}
