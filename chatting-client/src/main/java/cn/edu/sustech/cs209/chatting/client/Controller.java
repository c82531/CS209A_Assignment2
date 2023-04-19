package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    Socket s;
    @FXML
    Label currentUsername;
    @FXML
    ListView<Message> chatContentList;
    @FXML
    ListView chatList;
    @FXML
    TextArea inputArea;

    ComboBox<String> userSel;

    HashMap<String, ObservableList<Message>> userMsgList = new HashMap<>();

    String username;
    Scanner in;
    PrintWriter out;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        while (true) {
            Dialog<String> dialog = new TextInputDialog();
            dialog.setTitle("Login");
            dialog.setHeaderText(null);
            dialog.setContentText("Username:");
            Optional<String> input = dialog.showAndWait();

            if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
                username = input.get();
                try {
                    s = new Socket("localhost", 1234);
                    in = new Scanner(s.getInputStream(),"UTF-8");
                    out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
                    out.println(username);
                    out.flush();
                    String creation = in.next();
                    if (creation.equals("CreationFailed")) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Invalid Username");
                        alert.setHeaderText(null);
                        alert.setContentText("Invalid Username " + input.get() + " exiting\n" +
                                "Please enter a valid username.");
                        alert.showAndWait();
                    }else {
                        new ServerListener().start();
                        break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Empty Input");
                System.exit(0);
            }
//            dialog.close();
        }

        chatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // 更新 chatContentList 的值
                String receiver = (String) chatList.getSelectionModel().getSelectedItem();
                ObservableList<Message> msgList = userMsgList.get(receiver);
                chatContentList.setItems(msgList);
            }
        });

        currentUsername.setText("Current User: " + username);
        userMsgList.put(username, FXCollections.observableList(new ArrayList<Message>()));
        chatContentList.setCellFactory(new MessageCellFactory());
    }

    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        userSel = new ComboBox<>();
        userSel.setMinWidth(100);

        // FIXME: get the user list from server, the current user's name should be filtered out
        out.println("GetUsers");
        out.flush();
        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();

            boolean alreadyChatted = false;
            for (Object chatItem : chatList.getItems()) {
                if (chatItem.equals(user.get())) {
                    alreadyChatted = true;
                    break;
                }
            }
            if (!alreadyChatted) {
                // create a new chat item in the left panel
                ObservableList<Message> msgList = FXCollections.observableList(new ArrayList<Message>());
                userMsgList.put(user.get(), msgList);
                chatList.getItems().add(user.get());
                chatList.getSelectionModel().select(user.get());
            }
        });
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();
        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {

    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage(){
        // TODO
        String data = inputArea.getText().trim();
        if(data.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Invalid Message");
            alert.setHeaderText(null);
            alert.setContentText("You can't send empty message");
            alert.showAndWait();
            return;
        }
        long time = System.currentTimeMillis();
        String receiver = (String) chatList.getSelectionModel().getSelectedItem();
        Message message = new Message(time, username, receiver, data);
        ObservableList<Message> msgList = userMsgList.get(receiver);
        msgList.add(message);
        chatContentList.setItems(msgList);

        if(data.contains("\n")){
            StringBuilder sb = new StringBuilder(data);
            for (int i = 0; i < data.length(); i++) {
                if (sb.charAt(i) == '\n'){
                    sb.setCharAt(i, '|');
                }
            }
            data = sb.toString();
        }
        data = data.trim();
            System.out.println("Send: " + data);
        out.println("SendMessage " + time + " "+ username + " " + receiver + " " + data );
        out.flush();
        inputArea.clear();
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                        super.getChildren().clear();
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    class ServerListener extends Thread {
        @Override
        public void run() {
            while (true) {
                if(!in.hasNext()) break;
                String command = in.next();
                if(command.equals("CreationFailed")){
                    System.out.println("The username already exited, please change the username");
                }
                if(command.equals("SendUserList")){
                    String[] names = in.next().split(",");
                    Platform.runLater(() -> {
                        for(String name: names){
                            if(!name.equals(username)) userSel.getItems().add(name);
                        }
                    });
                }
                if(command.equals("ReceiveMessage")){
                    Long time = in.nextLong();
                    String sentBy = in.next();
                    String sendTo = in.next();
                    String data = in.nextLine();
                    StringBuilder sb = new StringBuilder(data);
                    if(data.contains("|")){
//                        StringBuilder sb = new StringBuilder(data);
                        for (int i = 0; i < data.length(); i++) {
                            if (sb.charAt(i) == '|'){
                                sb.setCharAt(i, '\n');
                            }
                        }
//                        data = sb.toString();
                    }
                        System.out.println("received");
                        System.out.println(username + sendTo);
                        System.out.println("SentBy: " +sentBy);
                        System.out.println(data);
                        System.out.println(sb.toString().trim());
                    if(sendTo.equals(username)) {
                        Platform.runLater(() -> {
                            Message message = new Message(time, sentBy, sendTo, sb.toString().trim());
//                            String selectedUser = (String) chatList.getSelectionModel().getSelectedItem();
                            if(!userMsgList.containsKey(sentBy)){
                                ObservableList<Message> msgList = FXCollections.observableList(new ArrayList<Message>());
                                userMsgList.put(sentBy, msgList);
                                chatList.getItems().add(sentBy);
                            }
                            chatList.getSelectionModel().select(sentBy);
                            ObservableList<Message> msgList = userMsgList.get(sentBy);
                            msgList.add(message);
                            chatContentList.setItems(msgList);
                        });
                    }
                }
            }
        }
    }
//    class ChatItem {
//        private String name;
////        private LocalDateTime lastMessageTime;
//
//        public ChatItem(String name) {
//            this.name = name;
////            this.lastMessageTime = lastMessageTime;
//        }
//        public String getName() {
//            return name;
//        }
//
//        @Override
//        public String toString() {
//            // Customize the string representation of the chat item
//            return String.format("%s", name);
//        }
//    }
}
