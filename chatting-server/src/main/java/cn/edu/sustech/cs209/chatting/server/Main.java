package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class Main {
    private static HashMap<String, Socket> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");
        ServerSocket server = new ServerSocket(1234);

        while (true) {
            Socket s = server.accept();
//            ObjectOutputStream outputStream = new ObjectOutputStream(s.getOutputStream());
//            ObjectInputStream inputStream = new ObjectInputStream(s.getInputStream());
            new Thread(() -> {
                try {
                    Scanner in = new Scanner(s.getInputStream());
                    PrintWriter out = new PrintWriter(s.getOutputStream());
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
                                  System.out.println("Command: " + command);
                            if(command.equals("GetUsers")){
                                StringBuilder sb = new StringBuilder();
                                for(String key: clients.keySet()){
                                    sb.append(key);
                                    sb.append(",");
                                }
                                out.println(sb);
                                out.flush();
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
                    try {
                        if(clients.containsValue(s))
                            clients.remove(s);
                        s.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }
}
