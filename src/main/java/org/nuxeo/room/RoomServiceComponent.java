package org.nuxeo.room;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentTreeWriter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class RoomServiceComponent extends DefaultComponent implements RoomService {

    protected static final Log log = LogFactory.getLog(RoomServiceComponent.class);

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
    public DocumentModel createRoom(String name, int branchingFactor, int maxItems, int batchSize, CoreSession session) {

        getLogger().info("Init Random text generator");
        SourceNode source = RandomTextSourceNode.init(maxItems, 10, true);
        getLogger().info("Random text generator initialized");

        DocumentModel room = session.createDocumentModel("/", name, "Workspace");
        room.setPropertyValue("dc:title", name);
        room = session.createDocument(room);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();

        TransactionHelper.startTransaction(3000);

        ImporterRunnerConfiguration configuration = new ImporterRunnerConfiguration.Builder(source,
                room.getPathAsString(), getLogger()).skipRootContainerCreation(true).batchSize(batchSize).nbThreads(
                branchingFactor).build();
        GenericMultiThreadedImporter runner = new GenericMultiThreadedImporter(configuration);
        ImporterFilter filter = new EventServiceConfiguratorFilter(true, true, false, true) {

            @Override
            public void handleBeforeImport() {
                super.handleBeforeImport();
                EventServiceAdmin eventAdmin = Framework.getLocalService(EventServiceAdmin.class);
                eventAdmin.setListenerEnabledFlag("elasticSearchInlineListener", false);
            }

            @Override
            public void handleAfterImport(Exception e) {
                super.handleAfterImport(e);
                EventServiceAdmin eventAdmin = Framework.getLocalService(EventServiceAdmin.class);
                eventAdmin.setListenerEnabledFlag("elasticSearchInlineListener", true);
            }
        };
        runner.addFilter(filter);

        Thread importer = new Thread(runner);
        importer.run();

        room.setPropertyValue("dc:description", "" + GenericMultiThreadedImporter.getCreatedDocsCounter());
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

        DocumentModel room = getRoom(name, session);

        ACP acp = room.getACP();
        ACL acl = new ACLImpl("test ACL" + System.currentTimeMillis());
        acl.add(new ACE(principal, permission, true));
        acp.addACL(acl);
        session.setACP(room.getRef(), acp, true);

        return session.getDocument(room.getRef());
    }

    @Override
    public DocumentModel reindexRoom(String name, CoreSession session) throws Exception {

        DocumentModel room = getRoom(name, session);

        IndexingCommand cmd = new IndexingCommand(room, IndexingCommand.Type.UPDATE_SECURITY, false, true);
        List<IndexingCommand> cmds = new ArrayList<IndexingCommand>();
        cmds.add(cmd);

        ElasticSearchIndexing esi = Framework.getService(ElasticSearchIndexing.class);

        esi.runIndexingWorker(cmds);

        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);

        esa.prepareWaitForIndexing().get(30, TimeUnit.SECONDS); // wait for indexing
        esa.refresh(); // explicit refresh

        return room;
    }

    @Override
    public File exportRoom(String name, CoreSession session) throws Exception {

        DocumentModel room = getRoom(name, session);

        File archive = File.createTempFile("room-io-archive", "zip");

        DocumentReader reader = new DocumentTreeReader(session, room);
        DocumentWriter writer = new NuxeoArchiveWriter(archive);

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        return archive;
    }

    @Override
    public DocumentModel getRoom(String name, CoreSession session) {
        PathRef roomRef = new PathRef("/" + name);
        return session.getDocument(roomRef);
    }

    @Override
    public String exportRoomStructure(String name, CoreSession session) throws Exception {

        DocumentModel room = getRoom(name, session);

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

    @Override
    public void randomUpdates(String name, CoreSession session, int nbThreads, int nbUpdates) throws Exception {

        final String repoName = session.getRepositoryName();

        List<Thread> workers = new ArrayList<Thread>();

        for (int i = 0; i < nbThreads; i++) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    new UnrestrictedSessionRunner(repoName) {
                        @Override
                        public void run() {
                            TransactionHelper.startTransaction();
                            runRandomUpdates(name, session, nbUpdates / nbThreads, workers.size());
                            session.save();
                            TransactionHelper.commitOrRollbackTransaction();
                        }
                    }.runUnrestricted();
                }
            });

            t.start();
            workers.add(t);
        }

        boolean completed = false;

        while (!completed) {
            completed = true;
            for (Thread worker : workers) {
                if (worker.isAlive()) {
                    completed = false;
                    break;
                }
            }
            Thread.sleep(100);
        }
    }

    protected void runRandomUpdates(String name, CoreSession session, int nbUpdates, int threadnb) {

        Random rnd = new Random(System.currentTimeMillis() + threadnb * 100);

        DocumentModel room = getRoom(name, session);

        IterableQueryResult res = session.queryAndFetch(
                "select ecm:uuid from Document where ecm:ancestorId ='" + room.getId() + "' order by dc:modified asc ",
                NXQL.NXQL);

        Iterator<Map<String, Serializable>> it = res.iterator();

        while (it.hasNext() && nbUpdates > 0) {
            Map<String, Serializable> data = it.next();
            String uuid = (String) data.get("ecm:uuid");

            if (rnd.nextInt(100) % 5 == 0) {
                DocumentModel doc = session.getDocument(new IdRef(uuid));
                doc.setPropertyValue("dc:description", "updated at " + System.currentTimeMillis());
                doc.putContextData(ScopeType.DEFAULT, "disableAutoIndexing", true);
                doc.putContextData(ScopeType.REQUEST, "disableAutoIndexing", true);
                session.saveDocument(doc);
                nbUpdates--;
            }
        }

        res.close();
        if (nbUpdates > 0) {
            runRandomUpdates(name, session, nbUpdates, threadnb);
        }
    }

    @Override
    public Map<String, Double> randomReadAndUpdates(String name, CoreSession session, int nbReads, int nbUpdates,
            int batchSize) throws Exception {

        final String repoName = session.getRepositoryName();

        List<Thread> workers = new ArrayList<Thread>();

        final List<Double> readSpeeds = new ArrayList<Double>();
        final List<Double> writeSpeeds = new ArrayList<Double>();

        for (int i = 0; i < nbUpdates; i++) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    new UnrestrictedSessionRunner(repoName) {
                        @Override
                        public void run() {
                            long t0 = System.currentTimeMillis();
                            TransactionHelper.startTransaction();
                            runRandomUpdates(name, session, batchSize, workers.size());
                            session.save();
                            TransactionHelper.commitOrRollbackTransaction();
                            Double speed = batchSize / ((System.currentTimeMillis() - t0) / 1000.0);
                            synchronized (writeSpeeds) {
                                writeSpeeds.add(speed);
                            }
                        }
                    }.runUnrestricted();
                }
            });

            t.start();
            workers.add(t);
        }
        for (int i = 0; i < nbReads; i++) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    new UnrestrictedSessionRunner(repoName) {
                        @Override
                        public void run() {
                            long t0 = System.currentTimeMillis();
                            TransactionHelper.startTransaction();
                            runRandomReads(name, session, batchSize, workers.size());
                            session.save();
                            TransactionHelper.commitOrRollbackTransaction();
                            Double speed = batchSize / ((System.currentTimeMillis() - t0) / 1000.0);
                            synchronized (readSpeeds) {
                                readSpeeds.add(speed);
                            }
                        }
                    }.runUnrestricted();
                }
            });

            t.start();
            workers.add(t);
        }

        boolean completed = false;

        while (!completed) {
            completed = true;
            for (Thread worker : workers) {
                if (worker.isAlive()) {
                    completed = false;
                    break;
                }
            }
            Thread.sleep(100);
        }

        Map<String, Double> result = new HashMap<String, Double>();

        Double readSpeed = 0.0;
        Double writeSpeed = 0.0;

        for (Double speed : readSpeeds) {
            readSpeed += speed;
        }
        readSpeed = readSpeed / (readSpeeds.size() + 0.0);

        for (Double speed : writeSpeeds) {
            writeSpeed += speed;
        }
        writeSpeed = writeSpeed / (writeSpeeds.size() + 0.0);

        result.put("read", readSpeed);
        result.put("write", writeSpeed);

        return result;
    }

    protected void runRandomReads(String name, CoreSession session, int nbUpdates, int threadnb) {

        DocumentModel room = getRoom(name, session);

        DocumentModelList docs = session.query("select * from Document where ecm:ancestorId ='" + room.getId()
                + "' order by dc:modified asc ", nbUpdates);

        for (DocumentModel doc : docs) {
            doc.getPropertyValue("dc:description");
            doc.getPropertyValue("common:size");
        }

    }

    protected long doHeavyReads(CoreSession session, int nbCycles) {

        Long nbReads = 0L;

        Random rnd = new Random(System.currentTimeMillis());

        List<String> uuids = new ArrayList<String>();

        for (int i = 0; i < nbCycles; i++) {
            String query = "SELECT * FROM Document WHERE ecm:name like '";
            query += "file-" + rnd.nextInt(10) + "-" + +rnd.nextInt(10) + "%";
            query += "' AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted'";
            query += " order by dc:modified asc ";
            DocumentModelList docs = session.query(query, 200);
            if (docs.size() > 0) {
                uuids.add(docs.get(0).getParentRef().toString());
                nbReads+=docs.size();
            }
        }

        for (String uuid : uuids) {
            DocumentModelList docs = session.query("select * from Document where ecm:ancestorId ='" + uuid
                    + "' order by dc:modified asc ", 1000);
            nbReads+=docs.size();
        }
        return nbReads;
    }


    @Override
    public Double heavyReads(CoreSession session, int nbThreads, int nbCycles) throws Exception {

        long t0 = System.currentTimeMillis();
        AtomicLong counter = new AtomicLong();

        final String repoName = session.getRepositoryName();

        List<Thread> workers = new ArrayList<Thread>();

        for (int i = 0; i < nbThreads; i++) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    new UnrestrictedSessionRunner(repoName) {
                        @Override
                        public void run() {
                            TransactionHelper.startTransaction();
                            counter.addAndGet(doHeavyReads(session, nbCycles));
                            TransactionHelper.commitOrRollbackTransaction();
                        }
                    }.runUnrestricted();
                }
            });

            t.start();
            workers.add(t);
        }

        boolean completed = false;

        while (!completed) {
            completed = true;
            for (Thread worker : workers) {
                if (worker.isAlive()) {
                    completed = false;
                    break;
                }
            }
            Thread.sleep(100);
        }

        return counter.get() / ((System.currentTimeMillis() -t0)/1000.0);
    }
}
