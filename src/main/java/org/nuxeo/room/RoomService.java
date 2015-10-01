package org.nuxeo.room;

import java.io.File;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface RoomService {

    DocumentModel createRoom(String name, int branchingFactor, int maxItems, int batchSize, CoreSession session);

    DocumentModel moveRoom(String oldName, String newname, CoreSession session);

    DocumentModel renameRoom(String oldName, String newname, CoreSession session);

    DocumentModel updateRoomACL(String name, String principal, String permission, CoreSession session);

    DocumentModel reindexRoom(String name, CoreSession session) throws Exception;

    File exportRoom(String name, CoreSession session) throws Exception;

    String exportRoomStructure(String name, CoreSession session) throws Exception;

    DocumentModel getRoom(String name, CoreSession session);

    void randomUpdates(String name, CoreSession session, int nbThreads, int nbUpdates) throws Exception;

    Map<String, Double> randomReadAndUpdates(String name, CoreSession session, int nbReads, int nbUpdates, int batchSize)
            throws Exception;

    Double heavyReads(CoreSession session, int nbThreads, int nbCycles) throws Exception;

}
