package fr.qgo.duckdbrestapi.testtools.service;

import fr.qgo.duckdbrestapi.service.ResultSetConvertor;
import fr.qgo.duckdbrestapi.service.defaultimpl.rs.ObjectResultSetConvertor;
import fr.qgo.duckdbrestapi.testtools.model.MyResultTestClass;
import org.springframework.stereotype.Service;
import org.sql2o.Query;
import org.sql2o.ResultSetIterable;

import java.sql.SQLException;
import java.util.Map;

@Service
public class MyResultSetConvertor extends ObjectResultSetConvertor<MyResultTestClass> {

    public MyResultSetConvertor() {
        super(MyResultTestClass.class, true, false, null, true);
    }
}
