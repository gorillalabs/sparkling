package sparkling.serialization;

import java.lang.ClassNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Var;

public class Utils {
  
  static final Var require = RT.var("clojure.core", "require");
  static final Var symbol = RT.var("clojure.core", "symbol");
  
  private Utils() {}
  
  public static void requireNamespace(String namespace) {
    require.invoke(symbol.invoke(namespace));
  }
  
  public static void writeIFn(ObjectOutputStream out, IFn f) throws IOException {
    out.writeObject(f.getClass().getName());
    out.writeObject(f);
  }
  
  public static IFn readIFn(ObjectInputStream in) throws IOException, ClassNotFoundException {
    String clazz = (String) in.readObject();
    String namespace = clazz.split("\\$")[0];
    
    requireNamespace(namespace);
    
    return (IFn) in.readObject();
  }
}
