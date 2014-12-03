package flambo.kryo;

import clojure.lang.AFunction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static flambo.kryo.Utils.readAFunction;
import static flambo.kryo.Utils.writeAFunction;

/**
 * Created by cbetz on 03.12.14.
 */
public abstract class AbstractSerializableWrappedAFunction implements Serializable {
    protected AFunction f;

    public AbstractSerializableWrappedAFunction() {
    }

    public AbstractSerializableWrappedAFunction(AFunction func) {
        f = func;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeAFunction(out, f);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        f = readAFunction(in);
    }


}
