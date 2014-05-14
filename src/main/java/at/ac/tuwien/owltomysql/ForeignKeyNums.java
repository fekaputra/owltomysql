package at.ac.tuwien.owltomysql;

import java.util.Comparator;

/**
 * Comparator for TableWrapper. 
 * 
 * @author Ekaputra
 *
 */
public class ForeignKeyNums implements Comparator<TableWrapper> {

    public int compare(TableWrapper o2, TableWrapper o1) {
        if(o1.getForeignKeys().size() == 0) {
            if(o2.getForeignKeys().size() == 0) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if(o2.getForeignKeys().size() == 0) {
                return -1;
            } else {
                if(o1.getForeignKeys().contains(o2.getTableName())) {
                    return -1;
                } else if(o2.getForeignKeys().contains(o1.getTableName())) {
                    return 1;
                } else {
                    return 0;
                }
            }
        } 
    }
}
