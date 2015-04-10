package sparkling.hadoop;

import java.io.IOException;

import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

import org.apache.avro.Schema;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

/**
 * Created by cbetz on 16.09.14.
 */
public class ClojureAvroInputFormat<K, V>
        extends FileInputFormat<K, V> {
    public static final String REQUIRED_NAMESPACES = "gorillalabs.sparkling.avro.requiredNamespaces";
    private static class Vars {
        private static final String PARKOUR_CONF_NS = "parkour.conf";
        private static final Var configuration = RT.var(PARKOUR_CONF_NS, "configuration");
        private static final Var requireFn = RT.var("clojure.core", "require");

        private static void require(String ns) {
            requireFn.invoke(Symbol.intern(ns));
        }

        static {
            require(PARKOUR_CONF_NS);
            require("sparkling.rdd.hadoopAvro");
        }
    }

    @Override
    public RecordReader<K, V>
    createRecordReader(InputSplit split, TaskAttemptContext context)
            throws IOException, InterruptedException {

        // Get `Configuration` via `parkour.conf/ig` to avoid caring at compile-time
        // if `TaskAttempContext` is an interface or a class.
        Configuration conf = (Configuration) Vars.configuration.invoke(context);
        Schema ks = AvroJob.getInputKeySchema(conf);
        Schema vs = AvroJob.getInputValueSchema(conf);
        String[] requiredNamespaces = conf.getStrings(REQUIRED_NAMESPACES);
        if (requiredNamespaces!=null) {
        for (String ns : requiredNamespaces) {
            Vars.require(ns);
        }}
        return new ClojureAvroRecordReader<K, V>(ks, vs);
    }
}