package at.ac.tuwien.owltomysql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Main Class for Owl to MySQL conversion.
 * 
 * @author Ekaputra
 * 
 */
public class OwlToMySQL {
    /**
     * Dataset coming from TDB storage.
     */
    private final Dataset dataset;

    /**
     * Class constructor, taking TDB storage.
     */
    public OwlToMySQL(String owlString) {
        dataset = CommonHelper.readFileTDB(owlString);
    }

    /**
     * If needed, we could initiate OWL from file.
     * 
     * @param OwlFile
     */
    public void initWithOwlFile(String OwlFile) {
        dataset.begin(ReadWrite.WRITE);
        Model model = dataset.getDefaultModel();

        Model inputModel = CommonHelper.readFile(OwlFile);
        model.add(inputModel);
        model.setNsPrefixes(inputModel.getNsPrefixMap());
        model.close();

        dataset.commit();
        dataset.end();
    }

    /**
     * The transformation from OWL to MySQL DB Script . LIMITATION: Currently
     * treat all DatatypeProperty as String.
     * 
     * @return List of TableWrapper (Table Script Creator)
     */
    public List<TableWrapper> transform() {
        List<TableWrapper> tableScripts = new ArrayList<TableWrapper>();

        dataset.begin(ReadWrite.READ);

        Model m = dataset.getDefaultModel();
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, m);

        ExtendedIterator<OntClass> listClasses = model.listClasses();
        while (listClasses.hasNext()) {
            StringBuilder tableScript = new StringBuilder();
            tableScript.append("CREATE TABLE ");
            List<String> fk = new ArrayList<String>();

            OntClass cls = listClasses.next();
            if (cls.isAnon())
                continue;
            String cls_id = model.getNsURIPrefix(cls.getNameSpace()) + "_" + cls.getLocalName();
            ExtendedIterator<OntProperty> listProps = cls.listDeclaredProperties();
            tableScript.append(cls_id);
            tableScript.append("( ");
            tableScript.append(System.getProperty("line.separator"));
            tableScript.append(cls_id + "_id").append(" VARCHAR(80) NOT NULL, ");
            tableScript.append(System.getProperty("line.separator"));

            while (listProps.hasNext()) {
                OntProperty prop = listProps.next();
                if (prop.isAnon())
                    continue;
                String prop_id = model.getNsURIPrefix(prop.getNameSpace()) + "_" + prop.getLocalName();
                tableScript.append(prop_id).append(" ").append(" VARCHAR(80), ");
                tableScript.append(System.getProperty("line.separator"));

                if (prop.isObjectProperty()) {
                    // add foreign key
                    OntResource range = prop.getRange();
                    if (range.isAnon())
                        continue;
                    String range_id = model.getNsURIPrefix(range.getNameSpace()) + "_" + range.getLocalName();
                    tableScript.append("FOREIGN KEY ").append("(" + prop_id + ")").append(" REFERENCES ")
                            .append(range_id).append("(" + range_id + "_id), ");
                    tableScript.append(System.getProperty("line.separator"));
                    fk.add(range_id);
                }
            }

            tableScript.append("PRIMARY KEY (").append(cls_id + "_id").append(") ");
            tableScript.append(System.getProperty("line.separator"));

            tableScript.append(" ); ");
            tableScript.append(System.getProperty("line.separator"));
            tableScript.append(System.getProperty("line.separator"));

            TableWrapper tw = new TableWrapper(cls_id, tableScript.toString(), fk);
            tableScripts.add(tw);
        }

        dataset.end();

        return tableScripts;
    }

    /**
     * Reorder the scripts so they could be run in one long script.
     * 
     * @param scripts TableWrapper list to be reordered.
     * @return String reordered representation of all MySQL creator scripts.
     */
    public String reorderScripts(List<TableWrapper> scripts) {
        Collections.sort(scripts, new ForeignKeyNums());
        return finalizeScripts(scripts);
    }

    /**
     * Convert TableWrapper list to String.
     * 
     * @param scripts TableWrapper list to be converted into string
     * @return
     */
    public String finalizeScripts(List<TableWrapper> scripts) {
        StringBuilder sb = new StringBuilder();

        Iterator<TableWrapper> iter = scripts.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next().getTableScript());
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        String owl = "data/gate_control";
        OwlToMySQL converter = new OwlToMySQL(owl);
        converter.initWithOwlFile(owl + ".owl");
        List<TableWrapper> scripts = converter.transform();
        String s = converter.reorderScripts(scripts);

        System.out.println(s);
    }
}
