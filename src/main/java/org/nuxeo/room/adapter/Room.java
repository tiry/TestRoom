package org.nuxeo.room.adapter;

import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.core.api.DocumentModel;

public class Room extends BusinessAdapter {

    public Room() {
        super();
    }

    public Room(DocumentModel documentModel) {
        super(documentModel);
    }

    public String getName() {
        return getDocument().getTitle();
    }

    public int getSize() {
        String s =  (String) getDocument().getPropertyValue("dc:description");
        if (s==null || s.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(s);
    }
}
