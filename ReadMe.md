

## About

This is just a WebEngine Module that exposed a REST endpoint for :

 - creating room 
    - a workspace with a subtree of 5000 docs
    - documents are generated randomly 
 - rename room
 - move the room
 - change acl on the room
 - export the room

## API

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


### Move Room

Create a new room and move the old one under it : this wait the while hierarchy has to be updated.

    GET/POST http://127.0.0.1:8080/nuxeo/site/rooms/move/{oldName}/{newName}

Output is text :

    GET http://127.0.0.1:8080/nuxeo/site/rooms/move/youhou/yh

    Room youhou moved to yh
    Exec time :3695 ms
    Throughput :1458.7280108254397 object/s

