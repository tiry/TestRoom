

## About

This is just a WebEngine Module that exposed a REST endpoint for :

 - creating room 
    - a workspace with a subtree of 5000 docs
    - documents are generated randomly 
 - rename room
 - move the room
 - change acl on the room
 - export the room

This modules also gives some examples on how to extend the REST API and Automation.

## Custom API

The exposed API is very simple :

### Create a Room

    GET/POST http://127.0.0.1:8080/nuxeo/site/rooms/new/{newRoomName}?branches={nbThreads}&max={size}

By default :

 - nbThreads = 5;
 - max= 5 000;

Output is text :

    GET http://127.0.0.1:8080/nuxeo/site/rooms/new/yahu

    Room yahu created (5390 items)
    Exec time :30948 ms
    Throughput :174.1631123174357 object/s

### Rename 

Rename a room (actually since the path is not stored, this is a free operation).

    GET/POST http://127.0.0.1:8080/nuxeo/site/rooms/rename/{oldName}/{newName}

Output is text :

    GET http://127.0.0.1:8080/nuxeo/site/rooms/rename/yahu/youhou

    Room yahu renamed to youhou
    Exec time :18 ms
    Throughput :299444.44444444444 object/s

### Index

Runs complete ES indexing of the whole tree and wait for completion.

    GET/POST http://127.0.0.1:8080/nuxeo/site/rooms/index/{romName}

Output is text :

    GET http://127.0.0.1:8080/nuxeo/site/rooms/index/youhou

    Room youhou reindexed
    Exec time :24392 ms
    Throughput :220.9740898655297 object/s

### Update ACL

Adds a new ACL on the room, triggering the full tree security propagation.

    GET/POST http://127.0.0.1:8080/nuxeo/site/rooms/acl/{roomName}

Output is text :

    http://127.0.0.1:8080/nuxeo/site/rooms/acl/youhou

    Updated ACL on Room youhou
    Exec time :2661 ms
    Throughput :2025.554302893649 object/s

### Export room

Run XML export of the room full tree (meta-data + blobs) in a zip archive.

    GET http://127.0.0.1:8080/nuxeo/site/rooms/export/{roomName}

Output is text :

    GET http://127.0.0.1:8080/nuxeo/site/rooms/export/youhou

    Room youhou exported as /opt/tiry/devs/runEnv/nuxeo-cap-7.3-I20150402_0121-tomcat/tmp/room-io-archive8334444688081952714zip 34359KB
    Exec time :6944 ms


### Export room structure

Run XML export of the room full tree (meta-data only) and return an XML stream

    GET http://127.0.0.1:8080/nuxeo/site/rooms/exportStructure/{roomName}

### Move Room

Create a new room and move the old one under it : this wait the while hierarchy has to be updated.

    GET/POST http://127.0.0.1:8080/nuxeo/site/rooms/move/{oldName}/{newName}

Output is text :

    GET http://127.0.0.1:8080/nuxeo/site/rooms/move/youhou/yh

    Room youhou moved to yh
    Exec time :3695 ms
    Throughput :1458.7280108254397 object/s

## Dependencies

This module uses `Nuxeo-platform-importer` to generate the random room tree.

## Using and Extending REST API

The standard Nuxeo Rest API can be used to manipulate content :

     GET http://127.0.0.1:8080/nuxeo/api/v1/id/d75cae39-4a14-43e2-b88a-597c21ba2988

     GET http://127.0.0.1:8080/nuxeo/api/v1/path/youpla

### Retrieving extract data

This module also contribute a Content Enricher so that when retrieveing a room, you can also retrieve the full room structure :

     GET http://127.0.0.1:8080/nuxeo/api/v1/path/youpla
     X-NXenrichers.document roomStructure


     {
      "entity-type": "document",
      "repository": "default",
      "uid": "d75cae39-4a14-43e2-b88a-597c21ba2988",
      "path": "/youpla",
      "type": "Workspace",
      "state": "project",
      "parentRef": "4972d5ea-fdf3-4f41-bcf4-fabec54faa11",
      "isCheckedOut": true,
      "changeToken": "1431965649708",
      "title": "yop",
      "lastModified": "2015-05-18T16:14:09.70Z",
      "facets": [
        "Folderish",
        "SuperSpace"
      ],
      "contextParameters": {
        "roomStructure": {
            "size": "5381",
            "children": [
                {
                    "uid": "aee86940-7722-4e0c-8304-f4f857ae344f",
                    "name": "file-0-0",
                    "type": "File"
                },
                ...
            ],
            "execTime" : "497ms 
        }
      }
     }

The associated class is [RoomTreeEnricher.java](/blob/master/src/main/java/org/nuxeo/room/apiextension/RoomTreeEnricher.java).

### new Operation 

This modules contributes a new `Room.Export` Operation:

Automatically generated documentation is available at : /nuxeo/site/automation/doc?id=Room.Export

The associated class is [RoomOperation.java](/blob/master/src/main/java/org/nuxeo/room/apiextension/RoomOperation.java).

You can combine this operation with the REST API :

    POST http://127.0.0.1:8080/nuxeo/api/v1/path/yopy/@op/Room.Export
    Content-Type application/json+nxrequest

    {}

### new Business Adapter

This modules contribute a (pretty dummy) Document Adapter called [Room](/blob/master/src/main/java/org/nuxeo/room/adapter/Room.java).

This adapter could be used via the REST API :

    GET http://127.0.0.1:8080/nuxeo/api/v1/path/yopy/@bo/Room


