Simpella
========

A stripped dwn version of GNUTella for a Netoworks Course project

Task breakdown

Phase 1:

Iteration 0:
Setup repo -- Done

Iteration 1:
Implement open 
* Implement a TCP server listening on a particular port
* open ip:port
* Use just IP for now. Establish a TCP connection
* Send Simpella header - SIMPELLA/0.6 \n\r
* Server to respond with 200 after accepting connection. 503 if there are 3 connections already. 
Maintain the connections n a data structure.

Iteration 2:
Implement Ping
* Implement the 23 byte header
* Initiate Ping message to all connected neighbours

Iteration 3:
Forward Ping message
* Implement routing table containing the last 160 ping messages and their origins
* Use array for simplicity
* Forward the ping messaage to the nighbours if not already done.

Iteration 4:
Implement Pong
* Implment pong response message
* Send pong to the sender of ping

Iteration 5:
Forward Pong
* Implement routing table based forwarding of pong messages
* Original sender of ping should stop the flooding process of pong
* The data got through pong should be stored in a data structure

Iteration 6:
Implement GET command for HTTP
* Create GET headers in client
* Create server for file downloads
* Server should repond for GET message
* Appropriate error code should be sent back
* Use predefined IP and port for testing.
* Use predefined file index and file name or testing

Iteration 7:
Implement query
* Form header at client and send
* At the server, process the header to find if it is a query message

Iteration 8:
Implement the file list and the query hit
* Scan the files shared and store in a data structure
* Use predefined directory to scan
* Form the query hit message and repond

Iteration 9:
Implement share and scan commands
* Maitain the directory that is shared and the files present in it in a table
* The directory should be read from CLI
* Table should be updated on the scan command

Iteration 10:
Implement the find command
* file command shall be wrapper around the query message.
* Response to query, query-hit shall be sent. Query-hit packet should be parsed and displayed to the user

End of Phase 1

Phase 2:
Monitor, Info, other parts left out in Phase 1

Phase 3: 
Clean up, error handling, bug fixing