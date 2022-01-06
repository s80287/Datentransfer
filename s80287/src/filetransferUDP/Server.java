// s80287 Duy Tien Nguyen

package filetransferUDP;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 * The server tries to download files from a client.
 *
 */

public class Server extends FileTransfer {

	static final String ARGUMENT_MESSAGE = "Arguments: <port number> [<packet loss rate> <average packet delay>] [--debug]";

	/**
	 * Executes the server.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public Server(String[] args) {
		parseArguments(args);
		displayArguments();

		initializeDownload();

		while (true) {
			receivePacket();
			proccessPacket();
		}

	}

	/**
	 * Sets the Sockets up.
	 */
	private void initializeDownload() {
		try {
			dataSocket = new DatagramSocket(port);
			dataSocket.setSoTimeout(timeout);
			printMessage("Socket opened.", 1);
		} catch (SocketException e) {
			exitApp("Could not open Socket.", -1);
		}
	}

	/**
	 * Tries to receive a packet. If the receiving process times out and a session was active, delete the session.
	 */
	private void receivePacket() {
		dataPacket = new DatagramPacket(new byte[PACKETLENGTH], PACKETLENGTH);
		packetData = null;

		for (int tryNumber = 1; tryNumber <= MAX_TRIES; tryNumber++) {
			try {
				dataSocket.receive(dataPacket);
				packetData = new byte[dataPacket.getLength()];
				packetData = Arrays.copyOfRange(dataPacket.getData(), 0, packetData.length);
				return;
			} catch (Exception e) {
				if (activeSession != 0) {
					if (tryNumber == MAX_TRIES) {
						printMessage("Connection timed out, waiting for new connection.", 0);
						resetSession();
					} else {
//						printMessage("Resending ACK: "+bytePacketNumber,1);
						sendACK(bytePacketNumber);
					}
				}
				// printMessage("No packet received "+activeSession+" ("+tryNumber+").",1);
				if (activeSession == 0) {
					tryNumber = 0;
				}
				continue;
			}
		}
	}

	/**
	 * Processes the received packet. The packet is either a first packet or a data packet. <br>
	 * If everything went o.k., an ACK is sent. <br>
	 * If the data packet has the wrong packet number, an ACK with the expected packet number is sent.
	 */
	private void proccessPacket() {
		byte[] tempSessionNumber = new byte[2];
		byte tempPacketNumber = (byte) 2;
		if (activeSession == 0) {
			byte[] tempStartIdentifier = new byte[5];
			ByteBuffer buffer = ByteBuffer.wrap(packetData);
			buffer.get(tempSessionNumber);
			tempPacketNumber = buffer.get();
			buffer.get(tempStartIdentifier);
			if (isCorrectStartIdentifier(tempStartIdentifier) && isCorrectPacketNumber(bytePacketNumber)) {
				getFirstPacketContents(packetData);
				if (isCorrectCRC(Arrays.copyOf(packetData, packetData.length - 4), byteFirstPacketCRC)) {
					getFirstPacketContents(packetData);
					setPort(dataPacket.getPort());
					setConnectionIP(dataPacket.getAddress());
					sendACK(bytePacketNumber);
//					printMessage("Sending first ACK: "+bytePacketNumber,1);		
					toggleBytePacketNumber();
					if (debug) {
						printSessionData();
					}
				} else {
					resetSession();
				}
			}
		} else {
			int dataLength = 0;
			boolean isLastPacket = getByteFileLengthLong() - bytesProcessed < packetData.length - 3;
			if (isLastPacket) {
				dataLength = (int) (getByteFileLengthLong() - bytesProcessed);
			} else {
				dataLength = packetData.length - 3;
			}
			byte[] tempFileBytes = new byte[dataLength];
			ByteBuffer buffer = ByteBuffer.wrap(packetData);
			buffer.get(tempSessionNumber);
			tempPacketNumber = buffer.get();
			if (!isCorrectPacketNumber(tempPacketNumber)) {
				return;
			}
			buffer.get(tempFileBytes);
			if (isLastPacket) {
				buffer.get(byteLastPacketCRC);
			}
			ByteBuffer procBuffer;
			procBuffer = ByteBuffer.allocate((int) bytesProcessed + dataLength);
			if (processedBytes != null) {
				procBuffer.put(processedBytes);
			}
			procBuffer.put(tempFileBytes);
			processedBytes = procBuffer.array();
			bytesProcessed = processedBytes.length;
			if (isCorrectSessionNumber(tempSessionNumber) && isCorrectPacketNumber(tempPacketNumber)) {
//				printMessage("Packet received: "+(short)tempPacketNumber, 1);
				if (isLastPacket) {
					if (isCorrectCRC(processedBytes, byteLastPacketCRC)) {
						String savePath = getAppLocation() + getNewFileName(getByteFileNameString());
						try {
							FileOutputStream stream = new FileOutputStream(savePath);
							stream.write(processedBytes);
							stream.close();
							sendACK(bytePacketNumber);
							printMessage("File successfully written: " + savePath, 0);
						} catch (Exception e) {
							printMessage("Could not write file: " + savePath, 2);
						}
					} else {
						printMessage("CRC of file is not correct.", 2);
					}
					resetSession();
					return;
				}
				sendACK(bytePacketNumber);
//				printMessage("Sending ACK: "+bytePacketNumber,1);
			} else {
				printMessage("The packet doesn't have a valid session and/or packet number: " + getByteSessionNumberShort(tempSessionNumber) + ", " + (short) tempPacketNumber, 1);
			}
			toggleBytePacketNumber();
		}
	}

	/**
	 * Sends an acknowledge packet to the client. If command line arguments are set, it may delay or lose the packet.
	 * 
	 * @param packetNumber
	 *            the packet number which should be acknowledged.
	 */
	private void sendACK(byte packetNumber) {

		try {
			if (packetDelay > 0) {
				int sleepTime = (int) (new Random().nextInt(packetDelay * 2));
//				printMessage("Sleep " + sleepTime + "ms", 1);
				Thread.sleep(sleepTime);
			}
		} catch (InterruptedException e1) {
			printMessage("Could not wait...", 2);
		}

		if (new Random().nextInt(100) < packetLossRate * 100) {
//			printMessage("Lose packet.", 1);
			return;
		}

		ByteBuffer buffer = ByteBuffer.allocate(3);
		buffer.put(byteSessionNumber);
		buffer.put(packetNumber);
		// printMessage("ACK Session Number: "+getByteSessionNumberShort(),1);
		// printMessage("ACK Packet Number: "+ (short)packetNumber,1);
		dataPacket = new DatagramPacket(buffer.array(), 3, connectionIP, port);
		try {
			dataSocket.send(dataPacket);
			// printMessage("ACK send.", 1);
		} catch (IOException e) {
			printMessage("ACK not send", 1);
		}
	}

	/**
	 * Displays the server arguments.
	 */
	private void displayArguments() {
		String printableArguments = "Waiting on port:\t" + port;
		String printableLossRateDelayValues = getPrintableLossRateDelayValues();
		String printableDebugStatus = getPrintableDebugStatus();
		if (!printableLossRateDelayValues.isEmpty())
			printableArguments += "\n" + printableLossRateDelayValues;
		if (!printableDebugStatus.isEmpty())
			printableArguments += "\n" + printableDebugStatus;
		out.println(printableArguments);
	}

	/**
	 * Parses the command line arguments.
	 * 
	 * @param args
	 *            command line arguments.
	 */
	private void parseArguments(String[] args) {
		if (args.length < 2) {
			out.println(ARGUMENT_MESSAGE);
			exitApp("Error while parsing arguments: Too few Arguments", 1);
		}
		/*
		// Assign Port
		setPort(Integer.parseInt(args[0]));
		if (args.length > 1) {
			// Check for debug flag (only even argument-length)
			if (args.length % 2 == 0 && (args[1].contains("--debug") || (args.length > 3 && args[3].contains("--debug")))) {
				setDebug(true);
			} else {
				err.println(ARGUMENT_MESSAGE);
				exitApp("Error while parsing arguments: Searched for --debug, but was not found!", 1);
			}
			// Assign optional Parameters
			if (args.length > 2) {
				setPacketLossRate(Float.parseFloat(args[1]));
				setPacketDelay(Integer.parseInt(args[2]));
			}
		}
		*/
		
		// Assign Port
		setPort(Integer.parseInt(args[1]));
		if (args.length > 2) {
			// Check for debug flag (only uneven argument-length)
			if (args.length % 2 == 1 && (args[2].contains("--debug") || (args.length > 4 && args[4].contains("--debug")))) {
				setDebug(true);
			} 
			// Assign optional Parameters
			else if (args.length == 4) {
				setPacketLossRate(Float.parseFloat(args[2]));
				setPacketDelay(Integer.parseInt(args[3]));
			}
			else {
				err.println(ARGUMENT_MESSAGE);
				exitApp("Error while parsing arguments: Searched for --debug, but was not found!", 1);
			}
		}
	}

	/**
	 * Calls Server(args) and thereby starts the server.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {
		new Server(args);
	}

}
