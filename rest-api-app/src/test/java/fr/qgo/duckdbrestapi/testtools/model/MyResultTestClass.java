package fr.qgo.duckdbrestapi.testtools.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MyResultTestClass {
    private String additionalField = "additionalField";
    private Integer id;
    private String str;
    private IJClass s;
    private Map<Integer, String> m;
    private List<Integer> l;
}
