package org.nuxeo.room.jaxrs;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.DocumentRoot;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.room.RoomService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

@WebObject(type = "rooms")
@Produces("text/html;charset=UTF-8")
@Path("/rooms")
public class Api extends ModuleRoot {

    protected static class Result {

        long t0;

        String message;

        Integer nbEntries;

        Result() {
            t0 = System.currentTimeMillis();
        }

        @Override
        public String toString() {

            StringBuffer sb = new StringBuffer();
            long t1 = System.currentTimeMillis();

            if (message != null) {
                sb.append(message);
                sb.append("\n");
            }

            sb.append("Exec time :");
            sb.append((t1 - t0) + " ms");

            if (nbEntries != null) {
                sb.append("\nnb entries :" + nbEntries);
                sb.append("\nThroughput :");
                sb.append(((1000 * (nbEntries + 0.0) / (t1 - t0))) + " object/s");
            }

            return sb.toString();
        }

    }

    protected Integer getRoomSize(DocumentModel room) {
        return Integer.parseInt(room.getPropertyValue("dc:description").toString());
    }


    @Path("new/{name}")
    @GET
    @Produces("text/plain")
    public String create(@PathParam(value = "name") String name, @QueryParam("branches") Integer branchingFactor,
            @QueryParam("max") Integer maxItems) {
        return doCreate(name, branchingFactor, maxItems);
    }

    @Path("new/{name}")
    @POST
    @Produces("text/plain")
    public String doCreate(@PathParam(value = "name") String name, @QueryParam("branches") Integer branchingFactor,
            @QueryParam("max") Integer maxItems) {

        if (branchingFactor == null) {
            branchingFactor = 5;
        }
        if (maxItems == null) {
            maxItems = 5000;
        }

        Result res = new Result();

        RoomService rm = Framework.getService(RoomService.class);
        DocumentModel room = rm.createRoom(name, branchingFactor, maxItems,
                WebEngine.getActiveContext().getCoreSession());

        res.message = "Room " + room.getName() + " created";
        res.nbEntries = getRoomSize(room);

        return res.toString();
    }

    @Path("rename/{oldName}/{newName}")
    @GET
    @Produces("text/plain")
    public String rename(@PathParam("oldName") String oldName, @PathParam("newName") String newName) {
        return doRename(oldName, newName);
    }


    @Path("rename/{oldName}/{newName}")
    @POST
    @Produces("text/plain")
    public String doRename(@PathParam("oldName") String oldName, @PathParam("newName") String newName) {

        Result res = new Result();

        RoomService rm = Framework.getService(RoomService.class);
        DocumentModel room = rm.renameRoom(oldName, newName, WebEngine.getActiveContext().getCoreSession());

        res.message = "Room " + oldName + " renamed to " + room.getName();

        // force commit
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        res.nbEntries = getRoomSize(room);

        return res.toString();
    }



    @Path("move/{oldName}/{newName}")
    @GET
    @Produces("text/plain")
    public String move(@PathParam("oldName") String oldName, @PathParam("newName") String newName) {
        return doMove(oldName, newName);
    }

    @Path("move/{oldName}/{newName}")
    @POST
    @Produces("text/plain")
    public String doMove(@PathParam("oldName") String oldName, @PathParam("newName") String newName) {

        Result res = new Result();

        RoomService rm = Framework.getService(RoomService.class);
        DocumentModel room = rm.moveRoom(oldName, newName, WebEngine.getActiveContext().getCoreSession());

        res.message = "Room " + oldName + " moved to " + room.getName();

        // force commit
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        res.nbEntries = getRoomSize(room);

        return res.toString();
    }

    @Path("index/{name}")
    @GET
    @Produces("text/plain")
    public String index(@PathParam(value = "name") String name) throws Exception {
        return doIndex(name);
    }

    @Path("index/{name}")
    @POST
    @Produces("text/plain")
    public String doIndex(@PathParam(value = "name") String name) throws Exception {

        Result res = new Result();

        RoomService rm = Framework.getService(RoomService.class);
        DocumentModel room = rm.reindexRoom(name, WebEngine.getActiveContext().getCoreSession());

        res.message = "Room " + room.getName() + " reindexed";

        res.nbEntries = getRoomSize(room);

        return res.toString();
    }

    @Path("acl/{name}")
    @GET
    @Produces("text/plain")
    public String acl(@PathParam("name") String name, @QueryParam("principal") String principal,
            @QueryParam("permission") String permission) {
        return doAcl(name, principal, permission);
    }

    @POST
    @Path("acl/{name}")
    @Produces("text/plain")
    public String doAcl(@PathParam("name") String name, @QueryParam("principal") String principal,
            @QueryParam("permission") String permission) {

        if (principal == null) {
            principal = "Bob" + System.currentTimeMillis();
        }
        if (permission == null) {
            permission = "Read";
        }

        Result res = new Result();

        RoomService rm = Framework.getService(RoomService.class);
        DocumentModel room = rm.updateRoomACL(name, principal, permission,
                WebEngine.getActiveContext().getCoreSession());

        res.message = "Updated ACL on Room " + room.getName();

        // force commit
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        res.nbEntries = getRoomSize(room);

        return res.toString();
    }


    @GET
    @Path("export/{name}")
    @Produces("text/plain")
    public String export(@PathParam(value = "name") String name) throws Exception {
        return doExport(name);
    }


    @POST
    @Path("export/{name}")
    @Produces("text/plain")
    public String doExport(@PathParam(value = "name") String name) throws Exception {

        Result res = new Result();

        RoomService rm = Framework.getService(RoomService.class);
        File zip = rm.exportRoom(name, WebEngine.getActiveContext().getCoreSession());

        res.message = "Room " + name + " exported as " + zip.getAbsolutePath() + (zip.length()/1024) + "KB";

        return res.toString();
    }


    @GET
    @Path("exportStructure/{name}")
    @Produces("text/plain")
    public String exportStructure(@PathParam(value = "name") String name) throws Exception {
        return doExportStructure(name);
    }


    @POST
    @Path("exportStructure/{name}")
    @Produces("text/plain")
    public String doExportStructure(@PathParam(value = "name") String name) throws Exception {

        Result res = new Result();

        RoomService rm = Framework.getService(RoomService.class);
        String xml = rm.exportRoomStructure(name, WebEngine.getActiveContext().getCoreSession());

        res.message = xml;

        return res.toString();
    }

    @Path("browse")
    public Object getRepository() {
        return new DocumentRoot(WebEngine.getActiveContext(), "/");
    }

}
