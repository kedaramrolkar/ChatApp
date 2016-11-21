
import java.net.*;
import java.io.*;
import java.util.*;

public class Client  {

	private ObjectInputStream inputStream;		// to read from the socket
	private ObjectOutputStream outputStream;		// to write on the socket
	private Socket socket;

	private String username;
	private int port;

	public Client(int p, String u) {
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
			socket = new Socket(port);
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
	
	public static void main(String[] args) throws InterruptedException {

		int portNumber = Integer.parseInt(args[1]);
		String serverAddress = args[2];
		String userName = args[0];

		Client client = new Client(serverAddress, portNumber, userName);
		if(!client.start()){
			return;			
		}
		
		Scanner scan = new Scanner(System.in);
		boolean continueProcess = true;
		
		while(continueProcess) {
			System.out.println("Enter Action (1.Broadcast 2.Unicast 3.Blockcast 4.Logout): ");			
			int choice=0;
			try{
				choice = Integer.parseInt(scan.nextLine());
			}
			catch(Exception ex){
			}
			
			if(choice==4){
				client.sendMessage(new ChatMessage(ChatMessage.Logout, userName, ""));
				continueProcess = false;
			}
			else if(choice==1 || choice==2 || choice==3){
				String username = null;
				
				if(choice==2 || choice==3){
					System.out.println("Enter the "+(choice==2?"receiver":"blocked")+" Username: ");
					username = scan.nextLine();
				}
				
				while(true){
					System.out.println("Enter Type of Message (1.Text 2.File): ");
					int choice2 = 0;
					try{
						choice2 = Integer.parseInt(scan.nextLine());
					}
					catch(Exception ex){
					}
					
					if(choice2==1){
						System.out.print("Enter Text: ");
						String message = scan.nextLine();
						client.sendMessage(new ChatMessage(choice, username, message));
						break;
					}
					else if(choice2==2){
						System.out.println("Enter filepath: ");
						String filePath = scan.nextLine();
						System.out.println("File Copied");
					}
					else{
						System.out.println("Invalid Entry..\n");
					}
				}
			}
			else{
				System.out.println("Invalid Entry. Please enter a valid number\n");
			}
		}
		scan.close();
		client.disconnect();
	}

	class ListenFromServer extends Thread {

		public void run() {
			while(true) {
				try {
					String msg = (String) inputStream.readObject();
					System.out.println(msg);
				} catch(Exception e) {
					System.out.println("Successfully Loggedout");
					break;
				}
			}
		}
	}
}

