package at.ac.tuwien.owltomysql;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.FileManager;

/**
 * Common helper for OWL management. 
 * 
 * @author Ekaputra
 *
 */
public class CommonHelper {
	
    /**
     * Read or create from URL.
     * 
     * @param URL link to TDB folder. 
     * @return dataset from TDB folder.
     */
	public static Dataset readFileTDB(String URL) {
	    return TDBFactory.createDataset(URL);
	}
    
	/**
	 * Read model from OWL file.
	 * 
	 * @param URL link to OWL file.
	 * @return Model representation of OWL file.
	 */
    public static Model readFile(String URL) {
        Model model = ModelFactory.createDefaultModel();
        try {
            InputStream in = FileManager.get().open(URL);
            if(in==null) throw new IllegalArgumentException("File: '"+URL+"' not found");
            model.read(in, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return model;
    }
	
    /**
     * Write TDB data into file. 
     * 
     * @param dataset TDB dataset.
     * @param URL link to file to be written.
     */
	public static void writeFileTDB(Dataset dataset, String URL) {
        dataset.begin(ReadWrite.READ) ;
        Model model = dataset.getDefaultModel();
		try {
			FileOutputStream fileOut = new FileOutputStream(URL+".owl");
			model.write(fileOut);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		    model.close();
	        dataset.end();
		}
	}
}
