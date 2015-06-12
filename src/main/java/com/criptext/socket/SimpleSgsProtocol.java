package com.criptext.socket;

/**
 * SGS Protocol constants.
 * <p>
 * A protocol message is constructed as follows:
 * <ul>
 * <li> (unsigned short) payload length, not including this field
 * <li> (byte) operation code
 * <li> optional content, depending on the operation code.
 * </ul>
 * <p>
 * A {@code ByteArray} is encoded as follows:
 * <ul>
 * <li> (byte[]) the bytes in the array
 * </ul>
 * <b>Note:</b> Messages may need to include explicit array length fields
 * if they include more than one ByteArray.
 * <p>
 * A {@code String} is encoded as follows:
 * <ul>
 * <li> (unsigned short) number of bytes of modified UTF-8 encoded String
 * <li> (byte[]) String encoded in modified UTF-8 as described
 * in {@link java.io.DataInput}
 * </ul>
 */
public interface SimpleSgsProtocol {
    
    /**
     * The maximum length of a protocol message:
     * {@value #MAX_MESSAGE_LENGTH} bytes.
     */
    final int MAX_MESSAGE_LENGTH = 65535;

    /**
     * The maximum payload length:
     * {@value #MAX_PAYLOAD_LENGTH} bytes.
     */
    final int MAX_PAYLOAD_LENGTH = 65532;

    /** The version number, currently {@code 0x04}. */
    final byte VERSION = 0x05;

    /**
     * Login request from a client to a server.
     * <br>
     * Opcode: {@code 0x10}
     * <br>
     * Payload:
     * <ul>
     * <li>(byte)   protocol version
     * <li>(String) name
     * <li>(String) password
     * </ul>
     */
    final byte LOGIN_REQUEST = 0x10;

    /**
     * Login success.  Server response to a client's {@link #LOGIN_REQUEST}.
     * <br>
     * Opcode: {@code 0x11}
     * <br>
     * Payload:
     * <ul>
     * <li> (ByteArray) reconnectionKey
     * </ul>
     */
    final byte LOGIN_SUCCESS = 0x11;

    /**
     * Login failure.  Server response to a client's {@link #LOGIN_REQUEST}.
     * <br>
     * Opcode: {@code 0x12}
     * <br>
     * Payload:
     * <ul>
     * <li> (String) reason
     * </ul>
     */
    final byte LOGIN_FAILURE = 0x12;

    /**
     * Login redirect.  Server response to a client's {@link #LOGIN_REQUEST}.
     * <br>
     * Opcode: {@code 0x13}
     * <br>
     * Payload:
     * <ul>
     * <li> (String) hostname
     * <li> (int) port
     * </ul>
     */
    final byte LOGIN_REDIRECT = 0x13;

    /**
     * Reconnection request.  Client requesting reconnect to a server.
     * <br>
     * Opcode: {@code 0x20}
     * <br>
     * Payload:
     * <ul>
     * <li> (byte)      protocol version
     * <li> (ByteArray) reconnectionKey
     * </ul>
     */
    final byte RECONNECT_REQUEST = 0x20;

    /**
     * Reconnect success.  Server response to a client's
     * {@link #RECONNECT_REQUEST}.
     * <br>
     * Opcode: {@code 0x21}
     * <br>
     * Payload:
     * <ul>
     * <li> (ByteArray) reconnectionKey
     * </ul>
     */
    final byte RECONNECT_SUCCESS = 0x21;

    /**
     * Reconnect failure.  Server response to a client's
     * {@link #RECONNECT_REQUEST}.
     * <br>
     * Opcode: {@code 0x22}
     * <br>
     * Payload:
     * <ul>
     * <li> (String) reason
     * </ul>
     */
    final byte RECONNECT_FAILURE = 0x22;

    /**
     * Session message.  May be sent by the client or the server.
     * Maximum length is {@value #MAX_PAYLOAD_LENGTH} bytes.
     * Larger messages require fragmentation and reassembly above
     * this protocol layer.
     * <br>
     * Opcode: {@code 0x30}
     * <br>
     * Payload:
     * <ul>
     * <li> (ByteArray) message
     * </ul>
     */
    final byte SESSION_MESSAGE = 0x30;


    /**
     * Logout request from a client to a server.
     * <br>
     * Opcode: {@code 0x40}
     * <br>
     * No payload.
     */
    final byte LOGOUT_REQUEST = 0x40;

    /**
     * Logout success.  Server response to a client's {@link #LOGOUT_REQUEST}.
     * <br>
     * Opcode: {@code 0x41}
     * <br>
     * No payload.
     */
    final byte LOGOUT_SUCCESS = 0x41;


    /**
     * Channel join.  Server notifying a client that it has joined a channel.
     * <br>
     * Opcode: {@code 0x50}
     * <br>
     * Payload:
     * <ul>
     * <li> (String) channel name
     * <li> (ByteArray) channel ID
     * </ul>
     */
    final byte CHANNEL_JOIN = 0x50;

    /**
     * Channel leave.  Server notifying a client that it has left a channel.
     * <br>
     * Opcode: {@code 0x51}
     * <br>
     * Payload:
     * <ul>
     * <li> (ByteArray) channel ID
     * </ul>
     */
    final byte CHANNEL_LEAVE = 0x51;
    
    /**
     * Channel message.  May be sent by the client or the server.
     * Maximum length is {@value #MAX_PAYLOAD_LENGTH} bytes.
     * Larger messages require fragmentation and reassembly above
     * this protocol layer.
     * <br>
     * Opcode: {@code 0x52}
     * <br>
     * Payload:
     * <ul>
     * <li> (unsigned short) channel ID size
     * <li> (ByteArray) channel ID
     * <li> (ByteArray) message
     * </ul>
     */
    final byte CHANNEL_MESSAGE = 0x52;
}
