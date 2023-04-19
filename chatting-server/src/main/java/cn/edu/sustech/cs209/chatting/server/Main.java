package cn.edu.sustech.cs209.chatting.server;



import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static ConcurrentHashMap<String, Socket> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");
        ServerSocket server = new ServerSocket(1234);

        while (true) {
            Socket s = server.accept();
            new Thread(() -> {
                try {
                    Scanner in = new Scanner(s.getInputStream(), "UTF-8");
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
                    String username = in.next();
                    if(clients.containsKey(username)){
                        System.out.println("User "+ username +" creation failed");
                        out.println("CreationFailed");
                        out.flush();
                        s.close();
                    }else {
                        System.out.println("User "+ username +" creation succeeded");
                        out.println("CreationSucceeded");
                        out.flush();
                        clients.put(username, s);
                        while (true){
                            if(!in.hasNext()) break;
                            String command = in.next();
                            System.out.println("Command: " + command); //debug
                            if(command.equals("GetUsers")){
                                StringBuilder sb = new StringBuilder();
                                out.println("SendUserList");
                                out.flush();
                                for(String key: clients.keySet()){
                                    sb.append(key);
                                    sb.append(",");
                                }
                                out.println(sb);
                                out.flush();
                            }
                            if(command.equals("SendMessage")){
                                Long time = in.nextLong();
                                String sentBy = in.next();
                                String sendTo = in.next();
                                String data = in.nextLine();
                                    System.out.println("ReceiveMessage: "  + time + " sentBy: "+ sentBy + " sendTo: " + sendTo + " " + data );
                                Socket receiveSocket = clients.get(sendTo);
                                PrintWriter outR = new PrintWriter(receiveSocket.getOutputStream());
                                outR.println("ReceiveMessage "  + time + " "+ sentBy + " " + sendTo + " " + data );
                                outR.flush();
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Client disconnected: " + s.getInetAddress().getHostAddress());
                    try {
                        if(clients.containsValue(s))
                            clients.remove(s);
                        s.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } finally {
                        System.out.println("Quit");
                    try {
                        if(clients.containsValue(s)) {
                            for (String key: clients.keySet()){
                                if(clients.get(key) == s)
                                    clients.remove(key);
                            }
                        }
                        s.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }
}
