import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
			while (true) {
				System.out.println("Waiting on port " + port);
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

	private synchronized void broadcast(String message) {

		System.out.println(message);
		for (int i = clientList.size() - 1; i >= 0; i--) {
			ClientThread ct = clientList.get(i);
			ct.writeMsg(message);
		}
	}

	synchronized void remove(int id) {
		for (int i = 0; i < clientList.size(); ++i) {
			ClientThread ct = clientList.get(i);
			if (ct.id == id) {
				clientList.remove(i);
				return;
			}
		}
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
				System.out.println(username + " just connected.");
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

			while (true) {
				try {
					cm = (ChatMessage) inputStream.readObject();
				} catch (Exception e) {
					System.out.println("Error in Stream " + e);
					break;
				}

				String message = cm.getMessage();

				switch (cm.getType()) {

					case ChatMessage.MESSAGE:
						broadcast(username + ": " + message);
					break;

					case ChatMessage.LOGOUT:
						System.out.println(username
							+ " disconnected with a LOGOUT message.");
					break;
				}
			}
			close();
			remove(id);
		}

		private boolean writeMsg(String msg) {

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
