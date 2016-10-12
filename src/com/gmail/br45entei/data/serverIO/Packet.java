package com.gmail.br45entei.data.serverIO;

import static com.gmail.br45entei.data.serverIO.PacketIDs.BooleanID;
import static com.gmail.br45entei.data.serverIO.PacketIDs.ByteID;
import static com.gmail.br45entei.data.serverIO.PacketIDs.CharID;
import static com.gmail.br45entei.data.serverIO.PacketIDs.DISABLE_COMPRESSION;
import static com.gmail.br45entei.data.serverIO.PacketIDs.DoubleID;
import static com.gmail.br45entei.data.serverIO.PacketIDs.ENABLE_COMPRESSION;
import static com.gmail.br45entei.data.serverIO.PacketIDs.FloatID;
import static com.gmail.br45entei.data.serverIO.PacketIDs.IntID;
import static com.gmail.br45entei.data.serverIO.PacketIDs.LongID;
import static com.gmail.br45entei.data.serverIO.PacketIDs.ShortID;
import static com.gmail.br45entei.data.serverIO.PacketIDs.StringID;
import static com.gmail.br45entei.data.serverIO.PacketIDs.getByID;

import com.gmail.br45entei.data.DisposableByteArrayInputStream;
import com.gmail.br45entei.data.DisposableByteArrayOutputStream;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.StringUtil;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public final class Packet {
	
	private static volatile boolean	enableStrCompression	= true;
	private static final int		compressionRatio		= 244;
	
	protected static final int		_696190					= 0x0a9f7e;	//lol
	protected static final int		_255					= 0xFF;
	
	private static final int		maxSize					= 32768;
	
	public static final Packet readNextPacket(InputStream in) throws IOException, PacketReadException {
		int readID = in.read();
		if(readID >= 0 && readID < 128) {
			PacketIDs id = getByID(readID);
			if(id == null) {
				throw new PacketReadException("Unknown packet id: " + readID);
			}
			if(id == ENABLE_COMPRESSION || id == DISABLE_COMPRESSION) {
				enableStrCompression = id == ENABLE_COMPRESSION;
				return readNextPacket(in);
			}
			final int length;
			if(id.getLength() == -1) {
				length = readInt(in);
				if(length == -1) {
					throw new IOException("Error reading packet: End of stream reached");
				}
				if(length > Packet.maxSize || length < 0) {
					throw new PacketReadException("Packet size out of bounds(must be >= 0 and <= " + Packet.maxSize + "): " + length);
				}
			} else {
				length = id.getLength();
			}
			return new Packet(id, length, in);
		}
		return null;
	}
	
	public static final void setEnableCompression(boolean enable, OutputStream out) throws IOException {
		enableStrCompression = enable;
		if(out != null) {
			writePacket(new Packet((enableStrCompression ? ENABLE_COMPRESSION : DISABLE_COMPRESSION), new byte[0]), out);
		}
	}
	
	@SuppressWarnings("resource")
	public static final void writePacket(Packet packet, OutputStream out) throws IOException {
		if(packet == null || out == null) {
			return;
		}
		if(packet.isCompressed && !enableStrCompression) {
			setEnableCompression(true, out);
		}
		packet.id.write(out);//send the packet id first, telling the other end what to expect
		final int len = packet.data.length;
		if(packet.id.getLength() == -1) {//Where -1 means the packet has a volatile length. If the packet id has a fixed length, there's no need to send the length over, since the other side should also know what the length of the packet id is.
			new DataOutputStream(out).writeInt(len);
		}
		out.write(packet.data, 0, len);//send the data
		out.flush();//doo eet. doo eet nao. naoooo
	}
	
	private final PacketIDs	id;
	private final byte[]	data;
	
	protected Packet(PacketIDs id, int len, InputStream in) throws IOException, PacketReadException {
		this.id = id;
		if(len > maxSize) {
			throw new IllegalArgumentException("Max packet size is: " + maxSize);
		}
		final byte[] data = new byte[maxSize];
		int read = in.read(data, 0, len);
		this.data = new byte[read];
		System.arraycopy(data, 0, this.data, 0, read);
		this.isCompressed = (this.id == StringID && enableStrCompression && this.data.length >= compressionRatio);
		if(this.getValue() == null) {
			throw new PacketReadException("Malformed Packet('" + this.getID().toString() + "') data; packet.getValue() returns null!");
		}
	}
	
	public Packet(PacketIDs id, byte[] data) {
		this.id = id;
		this.data = data;
	}
	
	private volatile boolean isCompressed = false;
	
	public Packet(String text) {
		this.id = StringID;
		byte[] data = text.getBytes(StandardCharsets.UTF_8);
		if(enableStrCompression && data.length >= compressionRatio) {
			data = StringUtil.compressString(text, StandardCharsets.UTF_8);
			this.isCompressed = true;
		}
		this.data = data;
	}
	
	public Packet(byte b) {
		this.id = ByteID;
		this.data = new byte[] {b};
	}
	
	public Packet(boolean b) {
		this.id = BooleanID;
		this.data = new byte[] {(byte) (b ? 1 : 0)};
	}
	
	public Packet(short s) {
		this.id = ShortID;
		this.data = new byte[] {(byte) s, (byte) (s >> 8)};
	}
	
	public Packet(char c) {
		this.id = CharID;
		this.data = new byte[] {(byte) c, (byte) (c >> 8)};
	}
	
	public Packet(int i) {
		this.id = IntID;
		this.data = new byte[] {(byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) (i >>> 0)};
	}
	
	public Packet(long l) {
		this.id = LongID;
		this.data = longToBytes(l);
	}
	
	public Packet(float f) {
		this.id = FloatID;
		final int i = Float.floatToIntBits(f);
		this.data = new byte[] {(byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) (i >>> 0)};
	}
	
	public Packet(double d) {
		this.id = DoubleID;
		final long l = Double.doubleToLongBits(d);
		this.data = longToBytes(l);
	}
	
	public final PacketIDs getID() {
		return this.id;
	}
	
	/** @return A string from this packet's data and the UTF-8 charset */
	public final String getText() {
		if(this.isCompressed) {
			String check = StringUtil.decompressString(this.data);
			if(check != null) {
				return check;
			}
		}
		return new String(this.data, StandardCharsets.UTF_8);
	}
	
	/** @return A copy of this packet's data */
	public final byte[] getData() {
		byte[] rtrn = new byte[this.data.length];
		System.arraycopy(this.data, 0, rtrn, 0, this.data.length);
		return rtrn;
	}
	
	public final Object getValue() {
		try {
			switch(this.id) {
			case BooleanID:
				return Boolean.valueOf(this.data[0] == 1);
			case ByteID:
				return Byte.valueOf(this.data[0]);
			case CharID:
				return Character.valueOf((char) ((this.data[0] << 0) + (this.data[1] << 8)));
			case DoubleID:
				return Double.valueOf(Double.longBitsToDouble(bytesToLong(this.data)));
			case FloatID:
				return Float.valueOf(Float.intBitsToFloat(((this.data[0] & 255) << 24) + ((this.data[1] & 255) << 16) + ((this.data[2] & 255) << 8) + ((this.data[3] & 255) << 0)));
			case IntID:
				return Integer.valueOf(((this.data[0] & 255) << 24) + ((this.data[1] & 255) << 16) + ((this.data[2] & 255) << 8) + ((this.data[3] & 255) << 0));
			case LongID:
				return Long.valueOf(((long) this.data[0] << 56) + //
						((long) (this.data[1] & 255) << 48) + //
						((long) (this.data[2] & 255) << 40) + //
						((long) (this.data[3] & 255) << 32) + //
						((long) (this.data[4] & 255) << 24) + //
						((this.data[5] & 255) << 16) + //
						((this.data[6] & 255) << 8) + //
						((this.data[7] & 255) << 0));
			case ShortID:
				return Short.valueOf((short) ((this.data[0] << 0) + (this.data[1] << 8)));
			case StringID:
			case Client_Hello:
			case Close:
			case ResendLastPacket:
			case DISABLE_COMPRESSION:
				return Boolean.valueOf(!enableStrCompression);
			case ENABLE_COMPRESSION:
				return Boolean.valueOf(enableStrCompression);
			default:
				return this.getText();
			}
		} catch(Throwable ignored) {
			return null;
		}
	}
	
	public final int getLength() {
		return this.data != null ? this.data.length : -1;
	}
	
	//=============================================
	
	@Override
	public final String toString() {
		return this.id.toString() + " " + this.data.length + (this.isCompressed ? "[Compressed]" : "") + " '" + this.getValue() + "'";
	}
	
	//=============================================
	
	private static final int readInt(InputStream in) throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if((ch1 | ch2 | ch3 | ch4) < 0) {
			return -1;//throw new EOFException();
		}
		return((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}
	
	@SuppressWarnings("unused")
	private static final void writeInt(OutputStream out, int i) throws IOException {
		out.write((i >>> 24) & 0xFF);
		out.write((i >>> 16) & 0xFF);
		out.write((i >>> 8) & 0xFF);
		out.write((i >>> 0) & 0xFF);
	}
	
	private static final byte[] longToBytes(long l) {
		byte[] result = new byte[8];
		for(int i = 7; i >= 0; i--) {
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}
	
	private static final long bytesToLong(byte[] data) {
		/*long result = 0;
		for(int i = 0; i < 8; i++) {
			result <<= 8;
			result |= (data[i] & 0xFF);
		}
		return result;*/
		return ((long) data[0] << 56) + //
				((long) (data[1] & 255) << 48) + //
				((long) (data[2] & 255) << 40) + //
				((long) (data[3] & 255) << 32) + //
				((long) (data[4] & 255) << 24) + //
				((data[5] & 255) << 16) + //
				((data[6] & 255) << 8) + //
				((data[7] & 255) << 0);
	}
	
	public static final void main(String[] args) {
		DisposableByteArrayOutputStream out = new DisposableByteArrayOutputStream();
		final Packet[] packets = new Packet[] {//
				new Packet(true), //
				new Packet((byte) 26), //
				new Packet('f'), //
				new Packet(67.25546D), //Converted to a long(by accident) this equates to 4634433099196161002 ...
				new Packet(1.25F), //
				new Packet(-42), //
				new Packet(147260003294L), //
				new Packet((short) 15), //
				new Packet("Hello, world! I'm a packet's data! ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz!@#$%^&*()`~-_=+[{]}\\|;:'\",<.>/?\t\r\n\br The quick brown fox jumps over the lazy dog. I like pie. This is filler text. We're testing compression! Can we keep filling it up? Let's keep going! Weeeeeeeeeeeeeeee!~"), //
				new Packet(PacketIDs.ResendLastPacket, "I didn't quite catch that.".getBytes(StandardCharsets.UTF_8)), //
				new Packet(PacketIDs.Client_Hello, "Hai.".getBytes(StandardCharsets.UTF_8)), new Packet(PacketIDs.Close, "Bai.".getBytes(StandardCharsets.UTF_8))//
		};
		final long startTime = System.currentTimeMillis();
		for(Packet packet : packets) {
			long curTime = System.currentTimeMillis();
			long totalElapsedTime = curTime - startTime;
			System.out.println("=====\r\nWriting packet: \"" + packet.toString() + "\"; Total Elapsed Time: " + (totalElapsedTime / 1000.00D));
			try {
				writePacket(packet, out);
			} catch(IOException e) {
				System.out.println("Writing packet failed: " + Functions.throwableToStr(e));
			}
			System.out.println("\r\nCurrent elapsed time: " + ((System.currentTimeMillis() - curTime) / 1000.00D));
		}
		DisposableByteArrayInputStream in = new DisposableByteArrayInputStream(out.toByteArray());
		ArrayList<Packet> readPackets = new ArrayList<>();
		while(in.available() > 0) {
			try {
				readPackets.add(readNextPacket(in));
			} catch(Throwable e) {
				System.out.println("Reading packet failed: " + Functions.throwableToStr(e));
			}
		}
		out.close();
		in.close();
		Packet[] packets2 = readPackets.toArray(new Packet[readPackets.size()]);
		if(packets2.length != packets.length) {
			final int numLost = (packets.length - packets2.length);
			System.out.println("We lost " + numLost + " packet" + (numLost == 1 ? "" : "s") + "!\r\nWe received:");
			for(Packet packet : packets2) {
				System.out.println("===== " + packet.toString());
			}
		} else {
			for(int i = 0; i < packets.length; i++) {
				System.out.println("=====\r\nOriginal: \"" + packets[i] + "\";\r\nNew: \"" + packets2[i] + "\";");
			}
		}
	}
	
}
