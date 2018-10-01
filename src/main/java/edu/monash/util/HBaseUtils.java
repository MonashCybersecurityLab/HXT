package edu.monash.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;

import java.io.IOException;

/**
 * Test the basic functions of HBase(connect, create, disable and drop).
 * </br>
 * In 0.2 version, I add some function to do query and check operations
 * on HBase.
 *
 * @author Shangqi
 * @version 0.2
 */
public class HBaseUtils {

    public static final String TSET_TABLE = "TSET";
    public static final String TSET_CF = "t";
    public static final String HVE_TABLE = "HVE";
    public static final String HVE_CF = "h";

    private static void createOrOverwrite(Admin admin, TableDescriptor table)
            throws IOException {
        if(admin.tableExists(table.getTableName())) {
            admin.disableTable(table.getTableName());
            admin.deleteTable(table.getTableName());
            System.out.println("Delete the existing table.");
        }
        admin.createTable(table);
    }

    public static void createSchemaTables(Configuration conf, String tableName, String cfName)
            throws IOException {
        try (Connection connection = ConnectionFactory.createConnection(conf);
             Admin admin = connection.getAdmin()) {
            TableDescriptor table = TableDescriptorBuilder
                    .newBuilder(TableName.valueOf(tableName))
                    .setColumnFamily(ColumnFamilyDescriptorBuilder
                            .newBuilder(cfName.getBytes())
                            .build())
                    .build();

            System.out.println("Create table.");
            createOrOverwrite(admin, table);
            System.out.println("Done.");
        }
    }

    public static boolean isAvailable(Admin admin, TableName name)
            throws IOException {
        return admin.tableExists(name);
    }

    public static void main(String[] args)
            throws IOException {
        Configuration conf = HBaseConfiguration.create();

        //Add any necessary configuration files (hbase-site.xml, core-site.xml)
        conf.addResource(new Path(System.getenv("HBASE_CONF_DIR"), "hbase-site.xml"));
        conf.addResource(new Path(System.getenv("HADOOP_CONF_DIR"), "core-site.xml"));
        createSchemaTables(conf, HBaseUtils.TSET_TABLE, HBaseUtils.TSET_CF);
    }
}
