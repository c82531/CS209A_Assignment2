package cn.edu.sustech.cs209.chatting.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static ConcurrentHashMap<String, Socket> clients = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, List<String>> chatUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");
        ServerSocket server = new ServerSocket(1234);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                for(String key: clients.keySet()){
                    Socket socket = clients.get(key);
                    PrintWriter o = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                    o.println("ServerOffline");
                    o.flush();
                    System.out.println("ServerOffline");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

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
                            if(command.equals("GetGroupUsers")){
                                StringBuilder sb = new StringBuilder();
                                out.println("SendGroupUserList");
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
                                    System.out.println("ReceiveMessage: "  + time + " sentBy: "+ sentBy + " sendTo: " + sendTo + " message: " + data );
                                Socket receiveSocket = clients.get(sendTo);
                                if(!chatUsers.containsKey(sendTo)){
                                    chatUsers.put(sendTo, new ArrayList<>());
                                }
                                chatUsers.get(sendTo).add(sentBy);

                                if(!chatUsers.containsKey(sentBy)){
                                    chatUsers.put(sentBy, new ArrayList<>());
                                }
                                chatUsers.get(sentBy).add(sendTo);
                                PrintWriter outR = new PrintWriter(receiveSocket.getOutputStream());
                                outR.println("ReceiveMessage "  + time + " "+ sentBy + " " + sendTo + " " + data );
                                outR.flush();
                            }
                            if(command.equals("SendGroupMessage")){
                                Long time = in.nextLong();
                                String sentBy = in.next();
                                String sendTo = in.next();
                                String show = in.next();
                                String data = in.nextLine();
                                    System.out.println("ReceiveGroupMessage: "  + time + " sentBy: "+ sentBy + " sendTo: " + sendTo
                                            +" show: " + show + " message: " + data );
                                String[] receivers = sendTo.split(",");
                                    System.out.println(Arrays.toString(receivers));
                                for (String receiver : receivers) {
                                    if(clients.containsKey(receiver)) {
                                        Socket receiveSocket = clients.get(receiver);
                                        if (receiveSocket != s) {
                                            PrintWriter outR = new PrintWriter(receiveSocket.getOutputStream());
                                            outR.println("ReceiveGroupMessage " + time + " " + sentBy + " "
                                                    + receiver + " " + sendTo + " " + show + " " + data);
                                            outR.flush();
                                        }
                                    }
                                }
                            }
                            if(command.equals("SendFile")){
                                String sentBy = in.next();
                                String sendTo = in.next();
                                    System.out.println("Send file to " + sendTo);
                                String fileName = in.next();
                                int size = Integer.parseInt(in.next());
//                                String savePath = "C:\\Users\\15405\\IdeaProjects\\CS209A_Assignment2\\chatting-server\\src\\main\\";
//                                File file = new File(savePath + fileName);
//                                FileOutputStream fos = new FileOutputStream(file);
                                BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
                                byte[] bytes = new byte[size];
                                int bytesRead;
                                while ((bytesRead = bis.read(bytes)) != -1) {
//                                    fos.write(bytes, 0, bytesRead);
                                    System.out.println("byes: "+ bytesRead);
                                    break;
                                }
//                                fos.flush();
                                    System.out.println("receive bytes");

                                Socket receiveSocket = clients.get(sendTo);
                                PrintWriter outR = new PrintWriter(receiveSocket.getOutputStream());
                                outR.println("ReceiveFile " + fileName + " " + size);
                                outR.flush();

                                if(!chatUsers.containsKey(sendTo)){
                                    chatUsers.put(sendTo, new ArrayList<>());
                                }
                                if(!chatUsers.get(sendTo).contains(sentBy)) {
                                    chatUsers.get(sendTo).add(sentBy);
                                }
                                if(!chatUsers.containsKey(sentBy)){
                                    chatUsers.put(sentBy, new ArrayList<>());
                                }
                                if(!chatUsers.get(sentBy).contains(sendTo)) {
                                    chatUsers.get(sentBy).add(sendTo);
                                }
                                try {
                                    OutputStream os = receiveSocket.getOutputStream();
                                    os.write(bytes, 0, bytes.length);
                                    os.flush();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
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
                    String user = null;
                    for(String key: clients.keySet()){
                        if(clients.get(key) == s){
                            user = key;
                            System.out.println("Delete: " + user);
                            break;
                        }
                    }
                        System.out.println("User " + user + " Quit");
                    if(user != null && chatUsers.containsKey(user)){
                        for(String key: chatUsers.get(user)){
                            if(clients.containsKey(key)) {
                                try {
                                    PrintWriter outR = new PrintWriter(clients.get(key).getOutputStream());
                                    outR.println("UserLeave " + user);
                                    outR.flush();
                                        System.out.print("SendLeave to ");
                                        System.out.println(chatUsers.get(user));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        chatUsers.remove(user);
                    }
                    try {
                        if (user != null) {
                            clients.remove(user);
                        }
                        if(!s.isClosed()) {
                            s.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }
}
