import java.io.IOException;
import java.util.List;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

public class delete {

	private static final String TABLE_NAME="TEST";
	private static final String CF="CF";
	private static final byte[] CF_BYTES=Bytes.toBytes(CF);
	private static final byte[] COL_BYTES=Bytes.toBytes("COL");
	private static final byte[] ROW_BYTES=Bytes.toBytes("A");
	private static final byte[] VAL_BYTES=Bytes.toBytes("X");
	
	public static void main(String[] args) throws IOException, InterruptedException {
		createTable();
		
		// FAMILY DELETE MARKERS ARE NOT REMOVED WITH MAJOR COMPACTION. 
		// THEY KEEP ON INCREASING WITH EACH PUT>DELETE>MAJOR_COMPACT
		for (int i=0; i<10; i++) {
			putDeleteCompact();
		}
	}
	
	private static void putDeleteCompact() throws IOException, InterruptedException {
		HTable ht = new HTable(getConfiguration(), TABLE_NAME);
		
		// PUT
		Put p = new Put(ROW_BYTES);
		p.add(CF_BYTES, COL_BYTES, VAL_BYTES);
		ht.put(p);
		ht.flushCommits();
		
		// DELETE
		Delete d = new Delete(ROW_BYTES);
		ht.delete(d);
		ht.flushCommits();
		
		// MAJOR_COMPACT
		HBaseAdmin admin = new HBaseAdmin(getConfiguration());
		admin.majorCompact(TABLE_NAME);
		admin.close();
		Thread.sleep(2000);
		
		// COUNT FOR FAMILY DELETE MARKERS
		Scan s = new Scan();
		s.setRaw(true);
		int count = 0;
		ResultScanner scanner = ht.getScanner(s);
		for (Result r : scanner) {
			List<KeyValue> kvs = r.getColumn(CF_BYTES, null);
			for (KeyValue kv : kvs) {
				count++;
			}
		}
		System.out.println("DELETE FAMILY MARKER COUNT:" + count);
		ht.close();
	}
	
	private static void createTable() throws IOException {
		HBaseAdmin hbaseAdmin = new HBaseAdmin(getConfiguration());
		HTableDescriptor htable = new HTableDescriptor(TABLE_NAME); 
		HColumnDescriptor hcolumn = new HColumnDescriptor(CF);
		hcolumn.setValue("VERSIONS", "1");
		hcolumn.setValue("KEEP_DELETED_CELLS", "true");
		htable.addFamily(hcolumn);
		hbaseAdmin.createTable(htable);
		hbaseAdmin.close();
	}
	
	public static Configuration getConfiguration() {
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", "localhost");
		return config;
	}
}
