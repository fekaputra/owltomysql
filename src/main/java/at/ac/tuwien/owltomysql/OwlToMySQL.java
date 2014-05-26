package at.ac.tuwien.owltomysql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    public Map<String, TableWrapper> transform() {
        Map<String, TableWrapper> tableScripts = new HashMap<String, TableWrapper>();

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

                if (prop.isFunctionalProperty() || cls.getCardinality(prop) == 1) {
                    String prop_id = model.getNsURIPrefix(prop.getNameSpace()) + "_" + prop.getLocalName();
                    tableScript.append(prop_id).append(" ").append(" VARCHAR(80), ");
                    tableScript.append(System.getProperty("line.separator"));

                    if (prop.isObjectProperty()) {
                        processObjectProperty(prop, model, tableScript, fk);
                    }
                } else {
                    TableWrapper tws = handleNonFuncProperty(m, cls, prop);
                    if (!tableScripts.containsKey(tws.getTableName().toLowerCase()))
                        tableScripts.put(tws.getTableName().toLowerCase(), tws);
                }
            }

            tableScript.append("PRIMARY KEY (").append(cls_id + "_id").append(") ");
            tableScript.append(System.getProperty("line.separator"));

            tableScript.append(" ); ");
            tableScript.append(System.getProperty("line.separator"));
            tableScript.append(System.getProperty("line.separator"));

            if (!tableScripts.containsKey(cls_id.toLowerCase())) {
                TableWrapper tw = new TableWrapper(cls_id, tableScript.toString(), fk);
                tableScripts.put(cls_id.toLowerCase(), tw);
            }
        }

        dataset.end();

        return tableScripts;
    }

    private TableWrapper handleNonFuncProperty(Model model, OntClass cls, OntProperty prop) {
        List<String> fk = new ArrayList<String>();

        StringBuilder tableScript = new StringBuilder();
        tableScript.append("CREATE TABLE ");
        String tableName = model.getNsURIPrefix(cls.getNameSpace()) + "_" + cls.getLocalName() + "__"
                + prop.getLocalName();
        String domainTable = cls.getLocalName();

        tableScript.append(tableName);
        tableScript.append("( ");
        tableScript.append(System.getProperty("line.separator"));
        tableScript.append(tableName + "_id").append(" INT(11) NOT NULL, ");
        tableScript.append(System.getProperty("line.separator"));
        tableScript.append(domainTable + "id").append(" VARCHAR(80) NOT NULL, ");

        String prop_id = model.getNsURIPrefix(prop.getNameSpace()) + "_" + prop.getLocalName();
        tableScript.append(prop_id).append(" ").append(" VARCHAR(80), ");
        tableScript.append(System.getProperty("line.separator"));

        if (prop.isObjectProperty()) {
            processObjectProperty(prop, model, tableScript, fk);
        }

        tableScript.append("PRIMARY KEY (").append(tableName + "_id").append(") ");
        tableScript.append(System.getProperty("line.separator"));
        tableScript.append(" ); ");
        tableScript.append(System.getProperty("line.separator"));
        tableScript.append(System.getProperty("line.separator"));

        TableWrapper wrapper = new TableWrapper(tableName, tableScript.toString(), fk);

        return wrapper;
    }

    public void processObjectProperty(OntProperty prop, Model model, StringBuilder tableScript, List<String> fk) {
        String prop_id = model.getNsURIPrefix(prop.getNameSpace()) + "_" + prop.getLocalName();
        ExtendedIterator<? extends OntResource> iter = prop.listRange();
        while (iter.hasNext()) {
            OntResource range = iter.next();
            if (!range.isAnon()) {
                String range_id = model.getNsURIPrefix(range.getNameSpace()) + "_" + range.getLocalName();
                tableScript.append("FOREIGN KEY ").append("(" + prop_id + ")").append(" REFERENCES ").append(range_id)
                        .append("(" + range_id + "_id), ");
                tableScript.append(System.getProperty("line.separator"));
                fk.add(range_id);
            }
        }
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
}
