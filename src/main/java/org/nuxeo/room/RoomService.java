package org.nuxeo.room;

import java.io.File;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface RoomService {

    DocumentModel createRoom(String name, int branchingFactor, int maxItems, CoreSession session);

    DocumentModel moveRoom(String oldName, String newname, CoreSession session);

    DocumentModel renameRoom(String oldName, String newname, CoreSession session);

    DocumentModel updateRoomACL(String name, String principal, String permission, CoreSession session);

    DocumentModel reindexRoom(String name, CoreSession session) throws Exception;

    File exportRoom(String name, CoreSession session) throws Exception;

    String exportRoomStructure(String name, CoreSession session) throws Exception;

    DocumentModel getRoom(String name, CoreSession session);
}
