import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by user on 03.12.2016.
 */
public class Client {
    public static void main(String[] args) throws IOException {

        JFrame window = new JFrame("Client");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        window.setResizable(false);
        window.setContentPane(new GamePanel());

        window.pack();
        window.setVisible(true);


    }
}
