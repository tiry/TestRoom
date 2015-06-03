package org.nuxeo.room.apiextension;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.room.RoomService;
import org.nuxeo.runtime.api.Framework;

@Operation(id = RoomOperation.ID, category = Constants.CAT_BUSINESS, label = "Export Room", description = "Sample Operation that exports a room")
public class RoomOperation {

    public static final String ID = "Room.Export";

    @Context
    protected CoreSession session;

    @Param(name = "structureOnly", required = false)
    protected Boolean structureOnly = true;

    @OperationMethod
    public Blob run(DocumentModel doc) throws Exception {

        RoomService rm = Framework.getService(RoomService.class);

        Blob result = null;
        if (structureOnly) {
            result= new StringBlob(rm.exportRoomStructure(doc.getName(), session));
            result.setFilename(doc.getName() + ".xml");
        } else {
            result = new FileBlob(rm.exportRoom(doc.getName(), session));
            result.setFilename(doc.getName() + ".zip");
        }

        return result;
    }
}
