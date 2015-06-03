package org.nuxeo.room.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.room.RoomService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;


@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({"org.nuxeo.ecm.platform.importer.core"})
@LocalDeploy({"org.nuxeo.room.sample"})
public class TestRoomService {

    @Inject
    CoreSession session;

    @Test
    public void checkServiceDeployed() throws Exception {
        RoomService rs = Framework.getService(RoomService.class);
        Assert.assertNotNull(rs);
    }

    @Test
    public void shouldCreateRoom() {
        RoomService rs = Framework.getService(RoomService.class);
        DocumentModel room = rs.createRoom("toto", 1, 100, session);
        Assert.assertNotNull(room);
    }
}
