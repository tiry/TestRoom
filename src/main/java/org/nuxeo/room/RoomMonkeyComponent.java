package org.nuxeo.room;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentTreeWriter;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.core.IndexingMonitor;
import org.nuxeo.elasticsearch.work.IndexingWorker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class RoomMonkeyComponent extends DefaultComponent implements RoomMonkey {

    protected static final Log log = LogFactory.getLog(RoomMonkeyComponent.class);

    protected static ImporterLogger inporterLogger;

    public ImporterLogger getLogger() {
        if (inporterLogger == null) {
            inporterLogger = new BufferredLogger(getJavaLogger());
        }
        return inporterLogger;
    }

    protected Log getJavaLogger() {
        return log;
    }

    @Override
    public DocumentModel createRoom(String name, int branchingFactor, int maxItems, CoreSession session) {

        getLogger().info("Init Random text generator");
        SourceNode source = RandomTextSourceNode.init(maxItems, 10, true);
        getLogger().info("Random text generator initialized");

        DocumentModel room = session.createDocumentModel("/", name, "Workspace");
        room.setPropertyValue("dc:title", name);
        room = session.createDocument(room);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();

        TransactionHelper.startTransaction();

        ImporterRunnerConfiguration configuration = new ImporterRunnerConfiguration.Builder(source,
                room.getPathAsString(), getLogger()).skipRootContainerCreation(true).batchSize(20).nbThreads(
                branchingFactor).build();
        GenericMultiThreadedImporter runner = new GenericMultiThreadedImporter(configuration);
        ImporterFilter filter = new EventServiceConfiguratorFilter(true, true, false, true);
        runner.addFilter(filter);

        Thread importer = new Thread(runner);
        importer.run();

        room.setPropertyValue("dc:description", ""+GenericMultiThreadedImporter.getCreatedDocsCounter());
        return session.saveDocument(room);
    }

    @Override
    public DocumentModel moveRoom(String oldName, String newname, CoreSession session) {

        DocumentModel newRoom = session.createDocumentModel("/", newname, "Workspace");
        newRoom.setPropertyValue("dc:title", newname);
        newRoom = session.createDocument(newRoom);
        session.save();


        PathRef roomRef = new PathRef("/" + oldName);
        return session.move(roomRef, newRoom.getRef(), newname);
    }


    @Override
    public DocumentModel renameRoom(String oldName, String newname, CoreSession session) {
        PathRef roomRef = new PathRef("/" + oldName);

        return session.move(roomRef, null, newname);
    }

    @Override
    public DocumentModel updateRoomACL(String name, String principal, String permission, CoreSession session) {

        PathRef roomRef = new PathRef("/" + name);
        DocumentModel room = session.getDocument(roomRef);

        ACP acp = room.getACP();
        ACL acl = new ACLImpl("test ACL" + System.currentTimeMillis());
        acl.add(new ACE(principal, permission, true));
        acp.addACL(acl);
        session.setACP(roomRef, acp, true);

        return session.getDocument(roomRef);
    }

    @Override
    public DocumentModel reindexRoom(String name, CoreSession session) throws Exception {

        PathRef roomRef = new PathRef("/" + name);
        DocumentModel room = session.getDocument(roomRef);

        IndexingCommand cmd = new IndexingCommand(room, IndexingCommand.Type.UPDATE_SECURITY, false, true);
        List<IndexingCommand> cmds = new ArrayList<IndexingCommand>();
        cmds.add(cmd);

        IndexingMonitor indexingMonitor = new IndexingMonitor();
        WorkManager wm = Framework.getService(WorkManager.class);
        IndexingWorker idxWork = new IndexingWorker(indexingMonitor, session.getRepositoryName(), cmds);
        wm.schedule(idxWork, false);

        String wid = idxWork.getId();

        indexingMonitor.waitForWorkerToComplete();
        while (!wm.getWorkState(wid).equals(Work.State.COMPLETED)) {
            Thread.sleep(200);
        }

        return room;
    }

    @Override
    public File exportRoom(String name, CoreSession session) throws Exception {

        PathRef roomRef = new PathRef("/" + name);
        DocumentModel room = session.getDocument(roomRef);

        File archive = File.createTempFile("room-io-archive", "zip");

        DocumentReader reader = new DocumentTreeReader(session, room);
        DocumentWriter writer = new NuxeoArchiveWriter(archive);

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();


        return archive;
    }


    public String exportRoomStructure(String name, CoreSession session) throws Exception {

        PathRef roomRef = new PathRef("/" + name);
        DocumentModel room = session.getDocument(roomRef);

        File xml = File.createTempFile("room-io-tree", "xml");

        DocumentReader reader = new DocumentTreeReader(session, room);
        DocumentWriter writer = new XMLDocumentTreeWriter(xml);

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();


        try {
            return FileUtils.readFileToString(xml);
        } finally {
            xml.delete();
        }
    }





}
