package sparkling.serialization;

import com.esotericsoftware.kryo.Kryo;
import org.apache.spark.serializer.KryoRegistrator;
import carbonite.JavaBridge;
import org.objenesis.strategy.StdInstantiatorStrategy;
import scala.Tuple2;
import com.twitter.chill.Tuple2Serializer;

public class BaseRegistrator implements KryoRegistrator {

    protected void register(Kryo kryo) {
    }

    @Override
    public final void registerClasses(Kryo kryo) {
        try {
            JavaBridge.enhanceRegistry(kryo);
            kryo.register(Tuple2.class, new Tuple2Serializer());
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            register(kryo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register kryo!", e);
        }
    }
}
