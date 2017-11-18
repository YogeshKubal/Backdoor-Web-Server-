import java.io.*;
import java.net.*;
import java.util.Scanner;
import sun.misc.Signal;
import sun.misc.SignalHandler;
import java.util.zip.GZIPOutputStream;

public class http
{
	private static Socket socket;

	public static void main(String[] args) 
	{
		int i_port=1000000000;
		try {
			i_port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.out.println("Invalid Port Number");
			System.exit(0);
		}
		 	
		if(!(i_port>=0 && i_port<=65535))
		{
			System.out.println("Invalid Port Number");
			System.exit(0);
		}
		else {
			try
			{			
				ServerSocket serverSocket = new ServerSocket(i_port);
				System.out.println("Server started, listening at port "+String.valueOf(i_port));
				int id = 0;

				while (true) {
					socket = serverSocket.accept();
					ClientServiceThread cThread = new ClientServiceThread(socket, id++);
					cThread.start();

//					Runtime.getRuntime().addShutdownHook(new Thread()
//					{
//						@Override
//						public void run()
//						{
//							System.exit(0);
//						}
//					});
//					while(true)
//					{
//						Thread.sleep(1000);
//					}

Signal.handle(new Signal("INT"), new SignalHandler() {
            public void handle(Signal sig) {
//                System.out.format("\nProgram execution took %f seconds\n", (System.nanoTime() - start) / 1e9f);
                System.exit(0);
            }
        });
//        int counter = 0;
//        while(true) {
//            System.out.println(counter++);
//            Thread.sleep(500);
//        }
				}
			}
						
		
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try {
					socket.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}		
	}
}


class ClientServiceThread extends Thread 
{
	Socket clientSocket;
	int clientID = -1;
	boolean running = true;
	String str_method, str_version;
	String str_msgHeaders;
	String str_url="";
	String str_msgBody="";
	String str_msgStatus = "HTTP/1.1 200 OK\r\n";

	ClientServiceThread(Socket s, int i) {
		clientSocket = s;
		clientID = i;
	}

	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			while (!(clientSocket.isClosed())) 
			{
				String str_message = in.readLine();
				if(str_message != null)
				{
					int i=0;
					System.out.println(str_message);			
					String[] in_msg = str_message.split(" ");
					str_method = in_msg[0];

					if(str_method.equals("GET"))
					{
						str_version = in_msg[(in_msg.length)-1];
						if(str_version.equals("HTTP/1.1"))
						{
							str_url = str_message.replace(str_method, "");
							str_url = str_url.replace(str_version, "");
							str_url = str_url.trim();
							str_url = URLDecoder.decode(str_url);
							str_url = str_url.trim();

							if(str_url.startsWith("/exec/"))
							{
								str_url = str_url.replace("/exec/", "");
								//str_url = str_url.trim();
								//str_url = URLDecoder.decode(str_url);

								if(str_url.isEmpty())
								{
									str_msgBody = "";
									str_msgStatus = str_version+" 404 Not Found"+"\r\n";
								}

								else if(!str_url.startsWith("vim"))
								{
									ProcessBuilder obj_builder = new ProcessBuilder("/bin/bash", "-c", str_url);
									Process p = null;
									try {
										p = obj_builder.start();
										obj_builder.redirectErrorStream(true);
										///////////////////////////////////////////////////////////
										//Scanner obj_scanner = new Scanner(p.getInputStream());
										BufferedReader obj_reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

										String str_cmdLine;

										while(true) {
											str_cmdLine = obj_reader.readLine();
											//System.out.println(str_cmdLine);	
											if(str_cmdLine == null)
											{	//System.out.println("Inside If");
												break;
											}
											else
											{
//System.out.println("Inside else");
												str_msgBody = str_msgBody +str_cmdLine+ "\n";
												str_msgStatus = str_version+" 200 OK"+"\r\n";
												
											}
										}
p.destroy();
//str_msgBody = str_msgBody.substring(0,(str_msgBody.length()-1));
//System.out.println(str_msgBody);
										///////////////////////////////////////////////////////////////
									}
									catch(Exception e) {
										//str_msgStatus = str_version+" 404 Not Found \r\n";
										//str_msgBody = "Command not found";
									}									
								}
								else
								{
									//System.out.println("Inside vim");
									str_msgStatus = str_version+" 200 OK \r\n";
									str_msgBody = "";
								}
							}
							else
							{
								str_msgStatus = str_version+" 404 Not Found \r\n";
								str_msgBody = "";
							}
						}
						else
						{
							str_msgStatus = str_version+" 404 Not Found \r\n";
							str_msgBody = "";
						}
					}
					else
					{
						str_msgStatus = str_version+" 404 Not Found \r\n";
						str_msgBody = "";
					}

					
					//sendMessage(socket, str_msgStatus, str_msgHeaders, str_msgBody);
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					GZIPOutputStream output = new GZIPOutputStream(bout);
					GZIPOutputStream output2 = new GZIPOutputStream(clientSocket.getOutputStream());
					
					output.write(str_msgBody.getBytes(), 0, str_msgBody.length());
					int length = bout.toByteArray().length;
//					str_msgHeaders = "Content-Type: text/plain; charset=UTF-8\r\n"+/*"Content-Encoding: gzip\r\n"+*/"Content-Length: "+str_msgBody.length()+"\r\n"+"\r\n";
					str_msgHeaders = "Content-Type: text/plain; charset=UTF-8\r\n"+"Content-Encoding: gzip\r\n"+"Content-Length: "+String.valueOf(length))+"\r\n"+"\r\n";
					
					out.write(str_msgStatus);
					out.write(str_msgHeaders);
					//out.write(str_msgBody);
					output2.write(str_msgBody.getBytes());

					//out.flush();
					output.close();
					output2.close();
					//out.write(str_msgBody);
					//int i_length = output.toString().length();
					//str_msgHeaders = "Content-Type: text/plain; charset=UTF-8\n"+"Content-Encoding: gzip\n"+"Content-Length: "+Integer.toString(i_length)+"\n"+"\n";
					//out.write(str_msgStatus);
					//out.write(str_msgHeaders);
					//out.write(str_msgStatus);
					//out.write(str_msgHeaders);
					//out.flush();
					//output.flush();
					clientSocket.close();
					//System.out.println("Socket has been closed");
					//System.out.println(str_msgBody.length());
					//System.out.println(Integer.toString(i_length));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			try {
				clientSocket.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
