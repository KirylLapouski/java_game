import sun.plugin2.main.server.ServerPrintHelper;
import sun.security.x509.IPAddressName;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.text.ParseException;
import java.util.*;

import static java.lang.System.exit;

/**
 * Created by user on 03.12.2016.
 */
public class GamePanel extends JPanel implements Runnable, KeyListener  {

        // FIELDS
        public static int WIDTH = 400;
        public static int HEIGHT = 400;

        private Thread thread;
        private boolean running;

        private BufferedImage image;
        private Graphics2D g;

        private int FPS = 30;
        private double averageFPS;

        public Player player;
        public Player serverPlayer;

        public static ArrayList<Bullet> bullets;
        public static ArrayList<Enemy> enemies;
        public static ArrayList<PowerUp> powerups;
        public static ArrayList<Text> texts;

        private long waveStartTimer;
        private long waveStartTimerDiff;
        private int waveNumber;
        private boolean waveStart;
        private int waveDelay = 2000;

        private long slowDownTimer;
        private long slowDownTimerDiff;
        private int slowDownLength = 6000;

        Socket fromserver = null;
        private BufferedReader in;
        private PrintWriter out;
        private BufferedReader inu;
        private ObjectOutputStream outObject;

        public static int send=0;
        private String hostname;
        MaskFormatter mf;



    // CONSTRUCTOR
        public GamePanel() {
            super();
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            requestFocus();
        }

        // FUNCTIONS
        public void addNotify() {
            super.addNotify();
            if(thread == null) {
                thread = new Thread(this);
                thread.start();
            }
            addKeyListener(this);
        }

        public void run() {

            running = true;

            image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            g = (Graphics2D) image.getGraphics();
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            player = new Player();

            bullets = new ArrayList<Bullet>();
            enemies = new ArrayList<Enemy>();
            powerups = new ArrayList<PowerUp>();
            texts = new ArrayList<Text>();

            waveStartTimer = 0;
            waveStartTimerDiff = 0;
            waveStart = true;
            waveNumber = 0;

            long startTime;
            long URDTimeMillis;
            long waitTime;
            long totalTime = 0;

            int frameCount = 0;
            int maxFrameCount = 30;

            long targetTime = 1000 / FPS;


                setLayout(null);



            setBackground( new Color(0, 100, 255));
            JLabel label = new JLabel("Enter IP");
            label.setFont(new Font("Century Gothic", Font.PLAIN, 20));
            label.setSize(100, 30);
            label.setLocation(150, 170);
            label.setVisible(true);
            add(label);

                try {
                    mf = new MaskFormatter("###.###.#*.#*");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                JFormattedTextField textField = new JFormattedTextField(mf);
                textField.setFont(new Font("Century Gothic", Font.PLAIN, 12));
                textField.setSize(260, 30);
                textField.setLocation(30, 210);
                textField.setVisible(true);
                add(textField);

                JButton button = new JButton("Connect");
                button.setFont(new Font("Century Gothic", Font.PLAIN, 12));
                button.setLocation(290, 210);
                button.setSize(80, 30);
                button.setVisible(true);
                add(button);

                repaint();

                Scanner cs =   new Scanner(System.in);

                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        hostname = textField.getText().replace(" ", "");
                        System.out.println(hostname);
                    }
                });

                textField.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        hostname = textField.getText().replace(" ", "");
                        System.out.println(hostname);
                    }
                });

            cs.nextInt();


            try {
                fromserver = new Socket(InetAddress.getByName(hostname),4444);
                in  = new BufferedReader(new InputStreamReader(fromserver.getInputStream()));
                out = new PrintWriter(fromserver.getOutputStream(),true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            g.setColor(new Color(0, 100, 255));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Century Gothic", Font.PLAIN, 16));
            String stringOut = "Turn on server and try again.";
            int length1 = (int) g.getFontMetrics().getStringBounds(stringOut, g).getWidth();
            g.drawString(stringOut, (WIDTH - length1) / 2, HEIGHT / 2);
            gameDraw();

            remove(button);
            remove(label);
            remove(textField);
            serverPlayer =new Player();

            // GAME LOOP
            while(running) {

                startTime = System.nanoTime();


                try {
                    gameUpdate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                gameRender();
                gameDraw();

                URDTimeMillis = (System.nanoTime() - startTime) / 1000000;

                waitTime = targetTime - URDTimeMillis;

                try {
                    Thread.sleep(waitTime);
                }
                catch(Exception e) {
                }

                frameCount++;
                if(frameCount == maxFrameCount) {
                    averageFPS = 1000.0 / ((totalTime / frameCount) / 1000000);
                    frameCount = 0;
                    totalTime = 0;
                }

            }

            g.setColor(new Color(0, 100, 255));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Century Gothic", Font.PLAIN, 16));
            String s = "G A M E   O V E R";
            int length = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
            g.drawString(s, (WIDTH - length) / 2, HEIGHT / 2);
            s = "Final Score: " + player.getScore();
            length = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
            g.drawString(s, (WIDTH - length) / 2, HEIGHT / 2 + 30);
            gameDraw();


            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            try {
                out.close();
                in.close();
                fromserver.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            exit(0);
        }

        private void gameUpdate() throws IOException {

            String[] tmp;
            String[][] buf = new String[1000][];

            //SEND TO SERVER
            String message="";
            for (int i = 0; i < bullets.size(); i++) {
                if (bullets.get(i).needToSend)
                {
                    message+="bullet:"+bullets.get(i).getx()+":"+bullets.get(i).gety()+":"+bullets.get(i).getAngle()+";";
                    bullets.get(i).needToSend=false;
                }
                send=2;
            }
            if(player.needToSend) {
                message+="player:" + player.getx() + ":" + player.gety()+";";
                player.needToSend=false;
                send=1;
            }
             if(send==0){
                message+="empty";
            }

            out.println(message);


            // check dead player
            if (serverPlayer.isDead()) {
                running = false;
            }
            if(player.isDead())
            {
                running=false;
            }


            if(running==false)
                out.println("exit");
            // player-enemy collision
            if (!player.isRecovering()) {
                int px = player.getx();
                int py = player.gety();
                int pr = player.getr();
                for (int j = 0; j < enemies.size(); j++) {

                    Enemy e = enemies.get(j);
                    double ex = e.getx();
                    double ey = e.gety();
                    double er = e.getr();

                    double dx = px - ex;
                    double dy = py - ey;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < pr + er) {
                        player.loseLife();
                    }

                }
            }
            if (!serverPlayer.isRecovering()) {
                int px = serverPlayer.getx();
                int py = serverPlayer.gety();
                int pr = serverPlayer.getr();
                for (int j = 0; j < enemies.size(); j++) {

                    Enemy e = enemies.get(j);
                    double ex = e.getx();
                    double ey = e.gety();
                    double er = e.getr();

                    double dx = px - ex;
                    double dy = py - ey;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < pr + er) {
                        serverPlayer.loseLife();
                    }

                }
            }


            //player-powerup collision
            int px = player.getx();
            int py = player.gety();
            int pr = player.getr();
            for(int j = 0; j < powerups.size(); j++) {
                PowerUp p = powerups.get(j);
                double x = p.getx();
                double y = p.gety();
                double r = p.getr();
                double dx = px - x;
                double dy = py - y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                // collected powerup
                if (dist < pr + r) {

                    int type = p.getType();

                    if (type == 1) {
                        player.gainLife();
                        texts.add(new Text(player.getx(), player.gety(), 2000, "Extra Life"));
                    }
                    if (type == 2) {
                        player.increasePower(1);
                        texts.add(new Text(player.getx(),player.gety(), 2000, "Power"));
                    }
                    if (type == 3) {
                        player.increasePower(2);
                        texts.add(new Text(player.getx(),player.gety(), 2000, "Double Power"));
                    }
                    if (type == 4) {
                        slowDownTimer = System.nanoTime();
                        for (int k = 0; k < enemies.size(); k++) {
                            enemies.get(k).setSlow(true);
                        }
                        texts.add(new Text(player.getx(), player.gety(), 2000, "Slow Down"));
                    }

                    powerups.remove(j);
                    j--;

                }

            }

            // player-powerup collision
            px = serverPlayer.getx();
            py = serverPlayer.gety();
            pr = serverPlayer.getr();
            for (int j = 0; j < powerups.size(); j++) {
                PowerUp p = powerups.get(j);
                double x = p.getx();
                double y = p.gety();
                double r = p.getr();
                double dx = px - x;
                double dy = py - y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                // collected powerup
                if (dist < pr + r) {

                    int type = p.getType();

                    if (type == 1) {
                        texts.add(new Text(serverPlayer.getx(), serverPlayer.gety(), 2000, "Extra Life"));
                    }
                    if (type == 2) {
                        texts.add(new Text(serverPlayer.getx(),serverPlayer.gety(), 2000, "Power"));
                    }
                    if (type == 3) {
                        texts.add(new Text(serverPlayer.getx(),serverPlayer.gety(), 2000, "Double Power"));
                    }
                    if (type == 4) {
                        slowDownTimer = System.nanoTime();
                        texts.add(new Text(serverPlayer.getx(), serverPlayer.gety(), 2000, "Slow Down"));
                    }

                    powerups.remove(j);
                    j--;

                }

            }



            // new wave
            if(waveStartTimer == 0 && enemies.size() == 0) {
                waveNumber++;
                waveStart = false;
                waveStartTimer = System.nanoTime();
            }
            else {
                waveStartTimerDiff = (System.nanoTime() - waveStartTimer) / 1000000;
                if(waveStartTimerDiff > waveDelay) {
                    waveStart = true;
                    waveStartTimer = 0;
                    waveStartTimerDiff = 0;
                }
            }
            // player update
            player.update();
            serverPlayer.update();

            //CREATE NEW ENEMIES
            createNewEnemies();

              // bullet update
            for(int i = 0; i < bullets.size(); i++) {
                boolean remove = bullets.get(i).update();
                if(remove) {
                    bullets.remove(i);
                    i--;
                }
            }




            // slowdown update
            if(slowDownTimer != 0) {
                slowDownTimerDiff = (System.nanoTime() - slowDownTimer) / 1000000;
                if(slowDownTimerDiff > slowDownLength) {
                    slowDownTimer = 0;
                }
            }

            // powerup update
            for(int i = 0; i < powerups.size(); i++) {
                boolean remove = powerups.get(i).update();
                if(remove) {
                    powerups.remove(i);
                    i--;
                }
            }



            // text update
            for(int i = 0; i < texts.size(); i++) {
                boolean remove = texts.get(i).update();
                if(remove) {
                    texts.remove(i);
                    i--;
                }
            }



            //RECEIVE FROM SERVER
            tmp=in.readLine().split(";");
            for(int i=0;i<tmp.length;i++)
                buf[i]=tmp[i].split(":");

            for(int i=0;i<tmp.length;i++)
            System.out.print(tmp[i]);

            System.out.println("");

            enemies.clear();
            //UPDATE SERVER OBJECT
            for(int l=0;l<tmp.length;l++)
            {
                if(buf[l][0].equals("exit"))
                    running=false;
                if(!buf[l][0].equals("empty")) {

                    if (buf[l][0].equals("server")) {
                        serverPlayer.setX(Integer.valueOf(buf[l][1]));
                        serverPlayer.setY(Integer.valueOf(buf[l][2]));
                    }
                    if (buf[l][0].equals("client")) {
                        player.setScore(Integer.valueOf(buf[l][1]));
                    }
                    if (buf[l][0].equals("bullet")) {

                        for (int j = 0; j < buf[l].length / 4; j++) {
                            bullets.add( new Bullet(Float.valueOf(buf[l][j * 2 + 3]),Integer.valueOf(buf[l][j * 2 + 1]), Integer.valueOf(buf[l][j * 2 + 2]),false));
                        }
                    }
                    if(buf[l][0].equals("enemy")){
                        for(int i=0;i<buf[l].length/5;i++)
                            enemies.add(new Enemy(Integer.valueOf(buf[l][i*4+3]),Integer.valueOf(buf[l][i*4+4]),Integer.valueOf(buf[l][i*4+1]),Integer.valueOf(buf[l][i*4+2])));
                    }if(buf[l][0].equals("powerUp")){
                        for (int i=0;i<buf[l].length/4;i++)
                        {
                            powerups.add(new PowerUp(Integer.valueOf(buf[l][i*4+3]),Integer.valueOf(buf[l][i*4+1]),Integer.valueOf(buf[l][i*4+2])));
                        }
                    }

                }
            }
            send=0;
        }

        private void gameRender() {

            // draw background
            g.setColor(new Color(0, 100, 255));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // draw slowdown screen
            if(slowDownTimer != 0) {
                g.setColor(new Color(255, 255, 255, 64));
                g.fillRect(0, 0, WIDTH, HEIGHT);
            }

            // draw players
            player.draw(g);
            serverPlayer.draw(g);

            // draw bullet
            for(int i = 0; i < bullets.size(); i++) {
                bullets.get(i).draw(g);
            }

            // draw enemy
            for(int i = 0; i < enemies.size(); i++) {
                enemies.get(i).draw(g);
            }

            // draw powerups
            for(int i = 0; i < powerups.size(); i++) {
                powerups.get(i).draw(g);
            }

            // draw text
           for(int i = 0; i < texts.size(); i++) {
                texts.get(i).draw(g);
            }

            // draw wave number
            if(waveStartTimer != 0) {
                g.setFont(new Font("Century Gothic", Font.PLAIN, 18));
                String s = "- W A V E   " + waveNumber + "   -";
                int length = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
                int alpha = (int) (255 * Math.sin(3.14 * waveStartTimerDiff / waveDelay));

                g.setColor(new Color(255, 255, 255, alpha%255));
                g.drawString(s, WIDTH / 2 - length / 2, HEIGHT / 2);
            }

            // draw THIS player lives
            for(int i = 0; i < player.getLives(); i++) {
                g.setColor(Color.WHITE);
                g.fillOval(20 + (20 * i), 20, player.getr() * 2, player.getr() * 2);
                g.setStroke(new BasicStroke(3));
                g.setColor(Color.WHITE.darker());
                g.drawOval(20 + (20 * i), 20, player.getr() * 2, player.getr() * 2);
                g.setStroke(new BasicStroke(1));
            }

            // draw THIS player power
            g.setColor(Color.YELLOW);
            g.fillRect(20, 40, player.getPower() * 8, 8);
            g.setColor(Color.YELLOW.darker());
            g.setStroke(new BasicStroke(2));
            for(int i = 0; i < player.getRequiredPower(); i++) {
                g.drawRect(20 + 8 * i, 40, 8, 8);
            }
            g.setStroke(new BasicStroke(1));

            // draw players score
            g.setColor(Color.WHITE);
            g.setFont(new Font("Century Gothic", Font.PLAIN, 14));
            g.drawString("Score: " + player.getScore(), WIDTH - 100, 30);

            // draw slowdown meter
            if(slowDownTimer != 0) {
                g.setColor(Color.WHITE);
                g.drawRect(20, 60, 100, 8);
                g.fillRect(20, 60,
                        (int) (100 - 100.0 * slowDownTimerDiff / slowDownLength), 8);
            }


        }

        private void gameDraw() {
            Graphics g2 = this.getGraphics();
            g2.drawImage(image, 0, 0, null);
            g2.dispose();
        }

        private void createNewEnemies() {

            if(waveNumber == 9) {
                running = false;
            }

        }

        public void keyTyped(KeyEvent key) {}
        public void keyPressed(KeyEvent key) {
            int keyCode = key.getKeyCode();
            if(keyCode == KeyEvent.VK_LEFT) {
                player.setLeft(true);
            }
            if(keyCode == KeyEvent.VK_RIGHT) {
                player.setRight(true);
            }
            if(keyCode == KeyEvent.VK_UP) {
                player.setUp(true);
            }
            if(keyCode == KeyEvent.VK_DOWN) {
                player.setDown(true);
            }
            if(keyCode == KeyEvent.VK_SPACE) {
                player.setFiring(true);
            }
        }
        public void keyReleased(KeyEvent key) {
            int keyCode = key.getKeyCode();
            if(keyCode == KeyEvent.VK_LEFT) {
                player.setLeft(false);
            }
            if(keyCode == KeyEvent.VK_RIGHT) {
                player.setRight(false);
            }
            if(keyCode == KeyEvent.VK_UP) {
                player.setUp(false);
            }
            if(keyCode == KeyEvent.VK_DOWN) {
                player.setDown(false);
            }
            if(keyCode == KeyEvent.VK_SPACE) {
                player.setFiring(false);
            }
        }

    }

