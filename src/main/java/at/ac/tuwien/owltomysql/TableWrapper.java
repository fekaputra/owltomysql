package at.ac.tuwien.owltomysql;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Container class for Table Creator Scripts.
 * 
 * @author Ekaputra
 * 
 */
public class TableWrapper {
    private String tableName;
    private String tableScript;
    private List<String> foreignKeys;

    public TableWrapper() {
        foreignKeys = new ArrayList<String>();
    }

    public TableWrapper(String tableName, String tableScript, List<String> foreignKeys) {
        this.tableName = tableName;
        this.tableScript = tableScript;
        this.foreignKeys = foreignKeys;
    }

    public TableWrapper(String cls_id, String prop_id, OntResource range) {
        this.tableName = cls_id + "_" + prop_id;
        StringBuilder sb = new StringBuilder();
        if (range.equals(XSD.xstring)) {
            sb.append(" CREATE TABLE ").append(tableName).append(" (");
            sb.append("  ");
            sb.append(" ) ");
        } else {

        }
        // TODO: add script + foreign keys
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableScript() {
        return tableScript;
    }

    public void setTableScript(String tableScript) {
        this.tableScript = tableScript;
    }

    public List<String> getForeignKeys() {
        return foreignKeys;
    }

    public void setForeignKeys(List<String> foreignKeys) {
        this.foreignKeys = foreignKeys;
    }

}
