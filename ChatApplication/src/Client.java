
import java.net.*;
import java.io.*;
import java.util.*;

public class Client  {

	private ObjectInputStream inputStream;		// to read from the socket
	private ObjectOutputStream outputStream;		// to write on the socket
	private Socket socket;

	private String username, server;
	private int port;

	public Client(int p, String u) {
		port = p;
		username = u;
		server = "127.0.0.1";
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
			System.out.println("Error connecting to server:" + e);
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
		
		if(msg.getOperation()==false){
			try {
				outputStream.writeObject(msg);
				outputStream.flush();
			} catch(IOException e) {
				System.out.println("Exception writing to server: " + e);
			}
		} else {
			FileInputStream fileStream = null;
		    BufferedInputStream bufferedStream = null;
		    
		    String fileName = msg.getMessage();
		    File toSend = new File(fileName);	//contains filename
	        msg.fileBytes  = new byte [(int)toSend.length()];

	        try {
				fileStream = new FileInputStream(toSend);
				bufferedStream = new BufferedInputStream(fileStream);
				bufferedStream.read(msg.fileBytes, 0, msg.fileBytes.length);
				outputStream.writeObject(msg);
				outputStream.flush();
			} catch (Exception e) {
				System.out.println("Error in sending file contents" + e);
			}
		}
	}
	
	public static void main(String[] args) throws InterruptedException {

		int portNumber = Integer.parseInt(args[1]);
		String userName = args[0];

		Client client = new Client(portNumber, userName);
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
				client.sendMessage(new ChatMessage(ChatMessage.Logout, userName, "", false));
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
						client.sendMessage(new ChatMessage(choice, username, message, false));
					}
					else if(choice2==2){
						System.out.println("Enter filepath: ");
						String filePath = scan.nextLine();
						client.sendMessage(new ChatMessage(choice, username, filePath, true));
					}
					else{
						System.out.println("Invalid Entry..\n");
						break;
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
		
		public final static int FILE_SIZE = 6022386; // file size temporary hard coded
		
		public void run() {
			FileOutputStream outputStream = null;
		    BufferedOutputStream bufferedStream = null;

			while(true) {
				try {
					ChatMessage msg = (ChatMessage) inputStream.readObject();
					if(!msg.getOperation()) {	//if not file operation
						System.out.println(msg.getMessage());
					} else {					// write file						
					    outputStream = new FileOutputStream(msg.getMessage());	  //has file name
					    bufferedStream = new BufferedOutputStream(outputStream);					    
					    bufferedStream.write(msg.fileBytes, 0 , msg.fileBytes.length);						
					    bufferedStream.flush();
					}
				} catch(Exception e) {
					System.out.println("Successfully Logged Out" +  e);
					break;
				} finally {
					if (outputStream != null) {
						try {
							outputStream.close();
						} catch (IOException e) {
							System.out.println("Error closing output stream" + e);
						}
					}
					if (bufferedStream != null){
						try {
							bufferedStream.close();
						} catch (IOException e) {
							System.out.println("Error closing buffered stream" + e);
						}
					}
				}
			}
		}
	}
}

