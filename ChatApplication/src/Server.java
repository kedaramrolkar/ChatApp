import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Server {

	private static int uniqueId = 1;
	private ArrayList<ClientThread> clientList;
	private int port;

	public Server(int p) {
		port = p;
		clientList = new ArrayList<ClientThread>();
	}

	public void start() {

		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			System.out.println("Could not create server socket");
		}

		try {
			System.out.println("Waiting on port " + port);
			while (true) {
				Socket socket = serverSocket.accept();
				ClientThread t = new ClientThread(socket);
				clientList.add(t);
				t.start();
			}
		} catch (Exception e) {
			System.out.println("Error in accepting connection " + e);
		} finally {
			try {
				serverSocket.close();
			} catch (Exception e) {
				System.out.println("Error in closing server " + e);
			}

			for (int i = 0; i < clientList.size(); ++i) {
				ClientThread tc = clientList.get(i);
				try {
					tc.inputStream.close();
					tc.outputStream.close();
					tc.socket.close();
				} catch (IOException e) {
					System.out.println("Error in closing clients " + e);
				}
			}
		}
	}

	private synchronized void broadcast(ChatMessage message) {
		for (int i = clientList.size() - 1; i >= 0; i--) {
			ClientThread ct = clientList.get(i);
			ct.writeMsg(message);
		}
		System.out.println("Broadcast Message sent");
	}
	
	private synchronized void broadcastFile(ChatMessage message) {

		final String location = 
        		"C:\\Users\\Hamza Karachiwala\\Documents\\Fall 16\\Networks\\Project\\";

		String filePath = message.getMessage();
		Path p = Paths.get(filePath);
		String fileName = p.getFileName().toString();
		
		for (int i = clientList.size() - 1; i >= 0; i--) {
			ClientThread ct = clientList.get(i);
			String clientName = ct.username;
			String fileToWrite = location + "\\" + clientName + "\\" + fileName;
			message.setMessage(fileToWrite);
			ct.writeFile(message);
		}
		System.out.println("File broadcasted");
	}

	private synchronized void unicast(String receiverUsername, ChatMessage message) {
		
		for (int i = clientList.size() - 1; i >= 0; i--) {			
			ClientThread ct = clientList.get(i);
			if(ct.username.equalsIgnoreCase(receiverUsername)){
				ct.writeMsg(message);
			}
		}
		System.out.println("Unicast Message sent");
	}
	
	private synchronized void unicastFile(String receiverUsername, ChatMessage message) {

		final String location = 
        		"C:\\Users\\Hamza Karachiwala\\Documents\\Fall 16\\Networks\\Project\\";

		String filePath = message.getMessage();
		Path p = Paths.get(filePath);
		String fileName = p.getFileName().toString();
		
		for (int i = clientList.size() - 1; i >= 0; i--) {
			
			ClientThread ct = clientList.get(i);
			if(ct.username.equalsIgnoreCase(receiverUsername)){
				String clientName = ct.username;
				String fileToWrite = location + "\\" + clientName + "\\" + fileName;
				message.setMessage(fileToWrite);
				ct.writeFile(message);
			}
		}
		System.out.println("File sent to recipient");
	}

	
	private synchronized void blockcast(String blockerUsername, ChatMessage message) {
		for (int i = clientList.size() - 1; i >= 0; i--) {
			ClientThread ct = clientList.get(i);
			if(!ct.username.equalsIgnoreCase(blockerUsername)){
				ct.writeMsg(message);
			}
		}
		System.out.println("Blockcast Message sent");
	}

	synchronized void remove(String removerUsername) {
		ClientThread remover = null;
		for (int i=0; i < clientList.size(); ++i) {
			ClientThread ct = clientList.get(i);
			if (ct.username.equalsIgnoreCase(removerUsername)) {
				remover = ct; 
				break;
			}
		}
		clientList.remove(remover);
		System.out.println(remover.username + " disconnected");
	}

	public static void main(String[] args) {
		int portNumber = Integer.parseInt(args[0]);
		Server server = new Server(portNumber);
		server.start();
	}

	class ClientThread extends Thread {

		Socket socket;
		ObjectInputStream inputStream;
		ObjectOutputStream outputStream;
		int id;
		String username;
		ChatMessage cm;

		ClientThread(Socket socket) {
			id = ++uniqueId;
			this.socket = socket;
			try {
				outputStream = new ObjectOutputStream(socket.getOutputStream());
				inputStream = new ObjectInputStream(socket.getInputStream());
				username = (String) inputStream.readObject();
				System.out.println(username + " connected.");
			} catch (IOException e) {
				System.out
						.println("Exception creating new Input/output Streams: "
								+ e);
				return;
			} catch (Exception e) {
				System.out.println("Exception in Client thread " + e);
				return;
			}
		}

		public void run() {
			boolean continueProcess = true;
			while (continueProcess) {
				try {
					cm = (ChatMessage) inputStream.readObject();
				} catch (Exception e) {
					System.out.println("Error in Stream " + e);
					break;
				}
				
				boolean isFileOp = cm.getOperation();
				String message=null;
				String newMessage = null;
				if(!isFileOp) {
					message = cm.getMessage();
					newMessage = "@" + username + ": " + message;
				}
				
				switch (cm.getType()) {
					case ChatMessage.Broadcast:
						if(!isFileOp) {
							broadcast(new ChatMessage(ChatMessage.Broadcast, cm.getUserName(), newMessage, cm.getOperation()));
						} else {
							broadcastFile(cm);
						}
					break;

					case ChatMessage.Unicast:
						String receiver = cm.getUserName();
						if(!isFileOp) {
							unicast(receiver, new ChatMessage(ChatMessage.Unicast, cm.getUserName(), newMessage, cm.getOperation()));
						} else {
							unicastFile(receiver, cm);
						}
					break;
					
					case ChatMessage.Blockcast:
						String blockerUsername = cm.getUserName();
						blockcast(blockerUsername, new ChatMessage(ChatMessage.Blockcast, cm.getUserName(), newMessage, cm.getOperation()));
					break;
					
					case ChatMessage.Logout:
						continueProcess = false;
					break;
				}
			}
			remove(username);
			close();
		}

		private boolean writeMsg(ChatMessage msg) {

			if (!socket.isConnected()) {
				close();
				return false;
			}

			try {
				outputStream.writeObject(msg);
				outputStream.flush();
			} catch (Exception e) {
				System.out.println("Error sending message to " + username);
				System.out.println(e.toString());
			}
			return true;
		}

		private boolean writeFile(ChatMessage message) {

			if (!socket.isConnected()) {
				close();
				return false;
			}

			try {
				outputStream.writeObject(message);
				outputStream.flush();
			} catch (Exception e) {
				System.out.println("Error sending file to " + username);
				System.out.println(e.toString());
			}
			return true;
		}

		private void close() {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
				if (inputStream != null) {
					inputStream.close();
				}
				if (socket != null) {
					socket.close();
				}
			} catch (Exception e) {
				System.out.println("Error in closing resources " + e);
			}
		}
	}
}
