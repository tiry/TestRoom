package org.nuxeo.room.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.room.RoomService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;

@RunWith(FeaturesRunner.class)
@Features(RestServerFeature.class)
@Jetty(port = 18090)
@Deploy({ "org.nuxeo.ecm.platform.importer.core", "org.nuxeo.room.sample" })
@LocalDeploy({ "org.nuxeo.room.sample" })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestRestAPI extends BaseTest {

    @Inject
    CoreSession session;

    @Test
    public void checkRoomAccess() throws Exception {

        RoomService rs = Framework.getService(RoomService.class);
        // first create a room using Java API
        DocumentModel room = rs.createRoom("toto", 1, 100, 20, session);
        Assert.assertNotNull(room);

        // Simple REST API access
        ClientResponse response = getResponse(RequestType.GET, "id/" + room.getId());

        // Check status
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        assertEquals("document", node.get("entity-type").getValueAsText());

        // use Adapter
        response = getResponse(RequestType.GET, "id/" + room.getId() + "/@bo/Room");

        // Check status
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("Room", node.get("entity-type").getValueAsText());

        // Call Operation piped on REST API
        Map<String, String> headers = new HashMap<String, String>();
        // headers.put("Content-Type", "application/json+nxrequest");

        response = getResponse(RequestType.POSTREQUEST, "id/" + room.getId() + "/@op/Room.Export", "{}", headers);

        // Check status
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String xml = IOUtils.toString(response.getEntityInputStream());

        assertTrue(xml.startsWith("<documents>"));

    }

}
