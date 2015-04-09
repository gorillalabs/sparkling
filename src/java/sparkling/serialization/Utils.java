package sparkling.serialization;

import java.lang.ClassNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    final static Logger logger = LoggerFactory.getLogger(Utils.class);

    static final Var require = RT.var("clojure.core", "require");
    static final Var symbol = RT.var("clojure.core", "symbol");

    private Utils() {
    }

    public static void requireNamespace(String namespace) {
        try {
            require.invoke(symbol.invoke(namespace));
        } catch (Exception e) {
            logger.warn ("Error deserializing function (require " + namespace +")" ,e);
        }
    }

    public static void writeIFn(ObjectOutputStream out, IFn f) throws IOException {
        try {
            logger.debug("Serializing " + f );
            out.writeObject(f.getClass().getName());
            out.writeObject(f);
        } catch (Exception e) {
            logger.warn("Error serializing object",e);
        }
    }

    public static IFn readIFn(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String clazz = "", namespace = "";
        try {
            clazz = (String) in.readObject();
            namespace = clazz.split("\\$")[0];

            requireNamespace(namespace);

            IFn f = (IFn) in.readObject();
            logger.debug("Deserializing " + f );
            return f;
        } catch (Exception e) {
            logger.warn("Error deserializing object (clazz: " + clazz + ", namespace: " + namespace + ")" ,e);
        }
        return null;
    }
}
