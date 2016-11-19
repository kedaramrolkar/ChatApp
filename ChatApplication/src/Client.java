
import java.net.*;
import java.io.*;
import java.util.*;

public class Client  {

	private ObjectInputStream inputStream;		// to read from the socket
	private ObjectOutputStream outputStream;		// to write on the socket
	private Socket socket;

	private String server, username;
	private int port;

	public Client(String s, int p, String u) {
		server = s;
		port = p;
		username = u;		
	}
	
	private void disconnect() {
		try { 
			if(inputStream != null) {
				inputStream.close();
			}
		}
		catch(Exception e) {
			System.out.println("Error in closing input");
		}
		try {
			if(outputStream != null) {
				outputStream.close();
			}
		}
		catch(Exception e) {
			System.out.println("Error in closing output");			
		}
        try{
			if(socket != null) {
				socket.close();
			}
		}
		catch(Exception e) {
			System.out.println("Error in closing socket");			
		}		
	}

	
	public boolean start() {

		try {
			socket = new Socket(server, port);
		} catch(Exception e) {
			System.out.println("Error connectiong to server:" + e);
			return false;
		}
		
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		System.out.println(msg);
	
		try {
			inputStream  = new ObjectInputStream(socket.getInputStream());
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			outputStream.flush();
		} catch (IOException eIO) {
			System.out.println("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		//listen
		new ListenFromServer().start();
		
		try {
			outputStream.writeObject(username);
			outputStream.flush();
		} catch (IOException e) {
			System.out.println("Login exception : " + e);
			disconnect();
			return false;
		}
		return true;
	}

	void sendMessage(ChatMessage msg) {
		try {
			outputStream.writeObject(msg);
			outputStream.flush();
		} catch(IOException e) {
			System.out.println("Exception writing to server: " + e);
		}
	}
	
	/*
	 * java Client username portNumber serverAddress
	 */
	public static void main(String[] args) {

		int portNumber = Integer.parseInt(args[1]);
		String serverAddress = args[2];
		String userName = args[0];

		Client client = new Client(serverAddress, portNumber, userName);
		if(!client.start()){
			return;			
		}
		
		Scanner scan = new Scanner(System.in);

		while(true) {

			System.out.println("Enter the message");
			String message = scan.nextLine();
			System.out.println("Option - \n1)Broadcast \n2)Unicast");			
			
			int choice = Integer.parseInt(scan.nextLine());
			
			switch(choice) {
				case 1:
					client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, message));
					break;
				
				case 2:					
					System.out.println("Enter the receiver username");
					String receiver = scan.nextLine();									
					break;
				//	if(msg.equalsIgnoreCase("LOGOUT")) {
				//		client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
				//  }
				
				case 3 : 
					scan.close();
					client.disconnect();	
				break;
			}
		}
	}

	class ListenFromServer extends Thread {

		public void run() {
			while(true) {
				try {
					String msg = (String) inputStream.readObject();
					System.out.println(msg);
				} catch(Exception e) {
					System.out.println("Error while listening " + e);
					break;
				}
			}
		}
	}
}

