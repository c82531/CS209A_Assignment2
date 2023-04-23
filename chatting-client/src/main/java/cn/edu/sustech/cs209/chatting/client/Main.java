package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.setTitle("Chatting Client");
        stage.show();

//        Controller controller = fxmlLoader.getController();
        stage.setOnCloseRequest(event -> {
//            try {
//                PrintWriter out = new PrintWriter(controller.s.getOutputStream());
//                out.println("Quit");
//                out.flush();
//                controller.s.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            System.exit(0);
        });
    }
}
