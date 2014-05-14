package at.ac.tuwien.owltomysql;

import java.util.List;

/**
 * Container class for Table Creator Scripts. 
 * 
 * @author Ekaputra
 *
 */
public class TableWrapper {
    private String tableName;
    private String tableScript;
    private List <String> foreignKeys;
    
    public TableWrapper(String tableName, String tableScript, List<String> foreignKeys) {
        this.tableName = tableName;
        this.tableScript = tableScript;
        this.foreignKeys = foreignKeys;
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
