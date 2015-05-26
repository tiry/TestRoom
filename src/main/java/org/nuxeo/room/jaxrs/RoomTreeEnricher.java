package org.nuxeo.room.jaxrs;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.query.sql.NXQL;

@Setup(mode = SINGLETON, priority = REFERENCE)
public class RoomTreeEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "roomStructure";

    public RoomTreeEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel room) throws IOException {
        jg.writeObjectFieldStart(NAME);
        long t0= System.currentTimeMillis();
        jg.writeStringField("size", (String) room.getPropertyValue("dc:description"));
        jg.writeArrayFieldStart("children");
        dumpDoc(room.getCoreSession(), jg, room.getId());
        jg.writeEndArray();
        jg.writeStringField("execTime", (System.currentTimeMillis()-t0)+"ms");
        jg.writeEndObject();
    }

    protected void dumpDoc(CoreSession session, JsonGenerator jg, String uid) throws IOException {
        IterableQueryResult result = session.queryAndFetch(
                "select ecm:uuid, ecm:name, ecm:primaryType from Document where ecm:parentId='" + uid + "'", NXQL.NXQL);
        Iterator<Map<String, Serializable>> it = result.iterator();
        while (it.hasNext()) {
            Map<String, Serializable> entry = it.next();
            jg.writeStartObject();
            jg.writeStringField("uid", (String) entry.get("ecm:uuid"));
            jg.writeStringField("name", (String) entry.get("ecm:name"));
            String docType = (String) entry.get("ecm:primaryType");
            jg.writeStringField("type", docType);
            if (!"File".equals(docType)) {
                jg.writeArrayFieldStart("children");
                dumpDoc(session, jg, (String) entry.get("ecm:uuid"));
                jg.writeEndArray();
            }
            jg.writeEndObject();
        }
        result.close();
    }

}
