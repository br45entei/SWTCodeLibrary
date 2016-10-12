package com.gmail.br45entei.data.serverIO;

import java.io.IOException;
import java.io.OutputStream;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public enum PacketIDs {
	/** The packet id sent when a server or client wants to close the
	 * connection */
	Close(0x0, -1),
	
	/** The first packet sent by incoming clients */
	Client_Hello(0x01, -1),
	
	/** The String packet id */
	StringID(0x02, -1),
	/** The boolean packet id */
	BooleanID(0x03, 1),
	/** The byte packet id */
	ByteID(0x04, 1),
	/** The short packet id */
	ShortID(0x05, 2),
	/** The char packet id */
	CharID(0x06, 2),
	/** The float packet id */
	FloatID(0x07, 4),
	/** The integer packet id */
	IntID(0x08, 4),
	/** The double packet id */
	DoubleID(0x09, 8),
	/** The long packet id */
	LongID(0x0a, 8),
	
	//==================
	
	/** Packet used to tell the other end to enable [string] compression. */
	ENABLE_COMPRESSION(0x7d, 0),
	/** Packet used to tell the other end to disable [string] compression. */
	DISABLE_COMPRESSION(0x7e, 0),
	/** The packet id sent when a server or client receives a corrupted packet
	 * and wants the partner try again(can be used up to three times in a
	 * row) */
	ResendLastPacket(0x7f, -1);
	
	private final byte	id;
	private final int	packetLength;
	
	private PacketIDs(int id, int packetLength) {
		this.id = (byte) id;
		this.packetLength = packetLength;
	}
	
	public static final PacketIDs getByID(int id) {
		for(PacketIDs ID : values()) {
			if(ID.equals(id)) {
				return ID;
			}
		}
		return null;
	}
	
	public static final void main(String[] args) {
		System.out.println("test:");
		for(PacketIDs id : values()) {
			System.out.println(id.toString() + " " + id.getID());
		}
	}
	
	@Override
	public final String toString() {
		return (this.name().endsWith("ID") ? this.name().substring(0, this.name().length() - 2) : this.name()) + "(ID:" + this.id + (this.packetLength == -1 ? "" : ";Length:" + this.packetLength) + ")";
	}
	
	public final int getID() {
		return this.id & 0x7F;//byte & 0x7F - Grabs 7 bits of data from the byte.
	}
	
	public final int getLength() {
		return this.packetLength;
	}
	
	public final boolean equals(int id) {
		return this.getID() == id;
	}
	
	public final void write(OutputStream out) throws IOException {
		out.write(new byte[] {this.id});
	}
	
}
