package sparkling.serialization;

import clojure.lang.IFn;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static sparkling.serialization.Utils.readIFn;
import static sparkling.serialization.Utils.writeIFn;

/**
 * Created by cbetz on 03.12.14.
 */
public abstract class AbstractSerializableWrappedIFn implements Serializable {
    protected IFn f;

    public AbstractSerializableWrappedIFn() {
    }

    public AbstractSerializableWrappedIFn(IFn func) {
        f = func;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
            writeIFn(out, f);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        f = readIFn(in);
    }


}
