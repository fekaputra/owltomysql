package at.ac.tuwien.owltomysql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainClass {

    public static void main(String[] args) {
        String owl = "data/gate_control";
        OwlToMySQL converter = new OwlToMySQL(owl);
        converter.initWithOwlFile(owl + ".owl");
        Map<String, TableWrapper> scripts = converter.transform();
        List<TableWrapper> listScripts = new ArrayList<TableWrapper>(scripts.values());
        String s = converter.reorderScripts(listScripts);

        System.out.println(s);
    }
}
