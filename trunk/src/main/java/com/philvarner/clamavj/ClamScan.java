package com.philvarner.clamavj;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ClamScan {
    
    private static Log log = LogFactory.getLog(ClamScan.class);

    private static final int DEFAULT_CHUNK_SIZE = 2048;
    private static final byte[] INSTREAM = "zINSTREAM\0".getBytes();
    private static final byte[] PING = "zPING\0".getBytes();
    private static final byte[] STATS = "zSTATS\0".getBytes();
    //  IDSESSION, END
//    It is mandatory to prefix this command with n or z, and all commands inside IDSESSION must  be
//    prefixed.
//
//    Start/end  a  clamd  session. Within a session multiple SCAN, INSTREAM, FILDES, VERSION, STATS
//    commands can be sent on the same socket without opening new connections.  Replies  from  clamd
//    will  be  in  the form '<id>: <response>' where <id> is the request number (in ascii, starting
//    from 1) and <response> is the usual clamd reply.  The reply lines have same delimiter  as  the
//    corresponding  command had.  Clamd will process the commands asynchronously, and reply as soon
//    as it has finished processing.
//
//    Clamd requires clients to read all the replies it sent, before sending more commands  to  pre-vent prevent
//    vent  send()  deadlocks. The recommended way to implement a client that uses IDSESSION is with
//    non-blocking sockets, and  a  select()/poll()  loop:  whenever  send  would  block,  sleep  in
//    select/poll  until either you can write more data, or read more replies.  Note that using non-blocking nonblocking
//    blocking sockets without the select/poll loop and  alternating  recv()/send()  doesn't  comply
//    with clamd's requirements.
//
//    If  clamd detects that a client has deadlocked,  it will close the connection. Note that clamd
//    may close an IDSESSION connection too if you don't follow the protocol's requirements.


    private int timeout;
    private String host;
    private int port;

    public ClamScan(){}

    public ClamScan(String host, int port, int timeout){
        setHost(host);
        setPort(port);
        setTimeout(timeout);
    }

    /**
     *
     * The method to call if you already have the content to scan in-memory as a byte array.
     *
     * @param in the byte array to scan
     * @return
     * @throws IOException
     */
    public ScanResult scan(byte[] in) throws IOException {
        return scan(new ByteArrayInputStream(in));
    }

    /**
     *
     * The preferred method to call. This streams the contents of the InputStream to clamd, so
     * the entire content is not loaded into memory at the same time.
     * @param in the InputStream to read.  The stream is NOT closed by this method.
     * @return a ScanResult representing the server response
     * @throws IOException if anything goes awry other than setting the socket timeout or closing the socket
     * or the DataOutputStream wrapping the socket's OutputStream
     */
    public ScanResult scan(InputStream in) throws IOException {

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(getHost(), getPort()));

        try {
            socket.setSoTimeout(getTimeout());
        } catch (SocketException e) {
        	log.error("Could not set socket timeout to " + getTimeout() + "ms", e);
        }

        DataOutputStream dos = null;
        String response = "";
        try {
            dos = new DataOutputStream(socket.getOutputStream());
            dos.write(INSTREAM);

            InputStream sin = socket.getInputStream();

            int read;
            byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];

            // clamd has an explicit limit on the number of bytes which can be sent,
            // so we need to be sure not to exceed that.  So, we check for an error condition
            // with each write
            while ((read = in.read(buffer))> 0 && sin.available() == 0){
                dos.writeInt(read);
                dos.write(buffer, 0, read);
                dos.flush();            
            }

            dos.writeInt(0);
            dos.flush();
 
            read = sin.read(buffer);
            if (read > 0) response = new String(buffer, 0, read);

        } finally {
            if (dos != null) try { dos.close(); } catch (IOException e) { log.debug("exception closing DOS", e); }
            try { socket.close(); } catch (IOException e) { log.debug("exception closing socket", e); }
        }

        if (log.isDebugEnabled()) log.debug( "Response: " + response);

        return new ScanResult(response.trim());
	}

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Socket timeout in milliseconds
     * @param timeout socket timeout in milliseconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
