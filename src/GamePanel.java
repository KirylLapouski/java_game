import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static java.lang.System.exit;

public class GamePanel extends JPanel implements Runnable, KeyListener {
	
	// FIELDS
	public static int WIDTH = 400;
	public static int HEIGHT = 400;
	
	private Thread thread;
	private boolean running;
	
	private BufferedImage image;
	private Graphics2D g;
	
	private int FPS = 30;
	private double averageFPS;

	public Player serverPlayer;

	public Player clients;
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

	//Server Variable
	public static int send=0;
	private BufferedReader in = null;
	private PrintWriter    out= null;
	private ServerSocket servers = null;
	private Socket fromclient = null;

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

		serverPlayer = new Player();

		clients = new Player();
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
		int maxFrameCount = 15;
		
		long targetTime = 1000 / FPS;
		
		//hue = 0;


		// create server socket
		try {
			servers = new ServerSocket(4444);
		} catch (IOException e) {
			System.out.println("Couldn't listen to port 4444");
			System.exit(-1);
		}

		g.setColor(new Color(0, 100, 255));
		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.setColor(Color.WHITE);
		g.setFont(new Font("Century Gothic", Font.PLAIN, 16));
		String stringOut = "Connection...";
		int length1 = (int) g.getFontMetrics().getStringBounds(stringOut, g).getWidth();
		g.drawString(stringOut, (WIDTH - length1) / 2, HEIGHT / 2);
		gameDraw();

		try {
			System.out.println("Waiting for a client...");
			fromclient= servers.accept();
			System.out.println("Client connected");
		} catch (IOException e) {
			System.out.println("Can't accept");
			System.exit(-1);
		}

		try {
			in  = new BufferedReader(new InputStreamReader(fromclient.getInputStream()));
			out = new PrintWriter(fromclient.getOutputStream(),true);

		} catch (IOException e) {
			e.printStackTrace();
		}


		System.out.println("Wait for messages");
		clients = new Player();



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
		s = "Final Score: " + serverPlayer.getScore();
		length = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
		g.drawString(s, (WIDTH - length) / 2, HEIGHT / 2 + 30);
		gameDraw();

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		//CLOSE STREAMS
		try {
			out.close();
			in.close();
			fromclient.close();
			servers.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		exit(0);
	}
	
	private void gameUpdate() throws IOException {

		//RECEIVE FROM CLIENT
		String[][] tmp = new String[5][];
		String str[];

		str=in.readLine().split(";");

		for(int i=0;i<str.length;i++)
			tmp[i]=str[i].split(":");

		for(int i=0;i<str.length;i++)
			System.out.println(str[i]);

			//UPDATE CLIENT OBJECTS
		for(int l=0;l<str.length;l++) {
			if(tmp[l][0].equals("exit"))
				running=false;
			if (!tmp[l][0].equals("empty")) {
				if (tmp[l][0].equals("player")) {
					clients.update(Integer.valueOf(tmp[l][1]), Integer.valueOf(tmp[l][2]));
				} else if (tmp[l][0].equals("bullet")) {
					for (int j = 0; j < tmp[l].length / 4; j++) {
						bullets.add(new Bullet(Float.valueOf(tmp[l][j * 2 + 3]), Integer.valueOf(tmp[l][j * 2 + 1]), Integer.valueOf(tmp[l][j * 2 + 2]), false));
					}
				}
			}
		}

		// player update

		serverPlayer.update();
		clients.update();

		//bullets update
		for (int i = 0; i < bullets.size(); i++) {
			boolean remove = bullets.get(i).update();
			if (remove) {
				bullets.remove(i);
				i--;
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

		// create enemies
		if(waveStart && enemies.size() == 0) {
			createNewEnemies();
		}
		

		// enemy update
		for(int i = 0; i < enemies.size(); i++) {
			enemies.get(i).update();
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

		// bullet-enemy collision
		for(int i = 0; i < bullets.size(); i++) {

			Bullet b = bullets.get(i);
			double bx = b.getx();
			double by = b.gety();
			double br = b.getr();

			for(int j = 0; j < enemies.size(); j++) {

				Enemy e = enemies.get(j);
				double ex = e.getx();
				double ey = e.gety();
				double er = e.getr();

				double dx = bx - ex;
				double dy = by - ey;
				double dist = Math.sqrt(dx * dx + dy * dy);

				if(dist < br + er) {
					e.hit();
					bullets.remove(i);
					i--;
					break;
				}

			}

		}

		// check dead enemies
		for(int i = 0; i < enemies.size(); i++) {

			if(enemies.get(i).isDead()) {

				Enemy e = enemies.get(i);

				// chance for powerup
				double rand = Math.random();
				if(rand < 0.001) powerups.add(new PowerUp(1, e.getx(), e.gety()));
				else if(rand < 0.020) powerups.add(new PowerUp(3, e.getx(), e.gety()));
				else if(rand < 0.120) powerups.add(new PowerUp(2, e.getx(), e.gety()));
				else if(rand < 0.130) powerups.add(new PowerUp(4, e.getx(), e.gety()));

				serverPlayer.addScore(e.getType() + e.getRank());
				enemies.remove(i);
				i--;

				e.explode();

			}

		}

			// check dead player
				if (serverPlayer.isDead())
					running = false;
				if(clients.isDead())
					running=false;



		// player-enemy collision
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
		if (!clients.isRecovering()) {
			int px = clients.getx();
			int py = clients.gety();
			int pr = clients.getr();
			for (int j = 0; j < enemies.size(); j++) {

				Enemy e = enemies.get(j);
				double ex = e.getx();
				double ey = e.gety();
				double er = e.getr();

				double dx = px - ex;
				double dy = py - ey;
				double dist = Math.sqrt(dx * dx + dy * dy);

				if (dist < pr + er) {
					clients.loseLife();
				}

			}
		}

		// player-powerup collision
			int px = serverPlayer.getx();
			int py = serverPlayer.gety();
			int pr = serverPlayer.getr();
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
						serverPlayer.gainLife();
						texts.add(new Text(serverPlayer.getx(), serverPlayer.gety(), 2000, "Extra Life"));
					}
					if (type == 2) {
						serverPlayer.increasePower(1);
						texts.add(new Text(serverPlayer.getx(),serverPlayer.gety(), 2000, "Power"));
					}
					if (type == 3) {
						serverPlayer.increasePower(2);
						texts.add(new Text(serverPlayer.getx(),serverPlayer.gety(), 2000, "Double Power"));
					}
					if (type == 4) {
						slowDownTimer = System.nanoTime();
						for (int k = 0; k < enemies.size(); k++) {
							enemies.get(k).setSlow(true);
						}
						texts.add(new Text(serverPlayer.getx(), serverPlayer.gety(), 2000, "Slow Down"));
					}

					powerups.remove(j);
					j--;

				}

			}


		 px = clients.getx();
		 py = clients.gety();
		 pr = clients.getr();
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
					clients.gainLife();
					texts.add(new Text(clients.getx(), clients.gety(), 2000, "Extra Life"));
				}
				if (type == 2) {
					clients.increasePower(1);
					texts.add(new Text(clients.getx(),clients.gety(), 2000, "Power"));
				}
				if (type == 3) {
					clients.increasePower(2);
					texts.add(new Text(clients.getx(),clients.gety(), 2000, "Double Power"));
				}
				if (type == 4) {
					slowDownTimer = System.nanoTime();
					for (int k = 0; k < enemies.size(); k++) {
						enemies.get(k).setSlow(true);
					}
					texts.add(new Text(clients.getx(), clients.gety(), 2000, "Slow Down"));
				}

				powerups.remove(j);
				j--;

			}

		}


		// slowdown update
		if(slowDownTimer != 0) {
			slowDownTimerDiff = (System.nanoTime() - slowDownTimer) / 1000000;
			if(slowDownTimerDiff > slowDownLength) {
				slowDownTimer = 0;
				for(int j = 0; j < enemies.size(); j++) {
					enemies.get(j).setSlow(false);
				}
			}
		}

		//SEND TO CLIENT
		String message="";
		 if(running==false)
		 {
		 	message+="exit";
		 }else {
			 for (int i = 0; i < bullets.size(); i++) {
				 if (bullets.get(i).needToSend) {
					 message += "bullet:" + bullets.get(i).getx() + ":" + bullets.get(i).gety() + ":" + bullets.get(i).getAngle() + ";";
					 bullets.get(i).needToSend = false;
				 }
				 send = 2;
			 }
			 for (int i = 0; i < enemies.size(); i++) {
				 if (enemies.get(i).needToSend) {
					 message += "enemy:" + enemies.get(i).getx() + ":" + enemies.get(i).gety() + ":" + enemies.get(i).getType() + ":" + enemies.get(i).getRank() + ";";
					 enemies.get(i).needToSend = false;
				 }
			 }
			 for (int i = 0; i < powerups.size(); i++) {
				 if (powerups.get(i).needToSend) {
					 message += "powerUp:" + powerups.get(i).getx() + ":" + powerups.get(i).gety() + ":" + powerups.get(i).getType() + ";";
					 powerups.get(i).needToSend = false;
				 }
				 send = 4;
			 }
			 if (serverPlayer.needToSend) {
				 message += "server:" + serverPlayer.getx() + ":" + serverPlayer.gety() + ";";
				 serverPlayer.needToSend = false;
				 send = 1;
			 }
			 if (clients.needToSend) {
				 message += "client:" + serverPlayer.getScore() + ";";
			 }
			 if (send == 0)
				 message += "empty";
		 }

		out.println(message);
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
		
		// draw player
		serverPlayer.draw(g);
		clients.draw(g);
		
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
			if(alpha > 255) alpha = 255;
			g.setColor(new Color(255, 255, 255, alpha));
			g.drawString(s, WIDTH / 2 - length / 2, HEIGHT / 2);
		}
		
		// draw THIS player lives
		for(int i = 0; i <serverPlayer.getLives(); i++) {
			g.setColor(Color.WHITE);
			g.fillOval(20 + (20 * i), 20, serverPlayer.getr() * 2, serverPlayer.getr() * 2);
			g.setStroke(new BasicStroke(3));
			g.setColor(Color.WHITE.darker());
			g.drawOval(20 + (20 * i), 20, serverPlayer.getr() * 2,serverPlayer.getr() * 2);
			g.setStroke(new BasicStroke(1));
		}
		
		// draw THIS player power
		g.setColor(Color.YELLOW);
		g.fillRect(20, 40,serverPlayer.getPower() * 8, 8);
		g.setColor(Color.YELLOW.darker());
		g.setStroke(new BasicStroke(2));
		for(int i = 0; i < serverPlayer.getRequiredPower(); i++) {
			g.drawRect(20 + 8 * i, 40, 8, 8);
		}
		g.setStroke(new BasicStroke(1));
		
		// draw players score
		g.setColor(Color.WHITE);
		g.setFont(new Font("Century Gothic", Font.PLAIN, 14));
		g.drawString("Score: " + serverPlayer.getScore(), WIDTH - 100, 30);
		
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
		
		enemies.clear();
		Enemy e;
		
		if(waveNumber == 1) {
			for(int i = 0; i < 4; i++) {
				enemies.add(new Enemy(1, 1));
			}
		}
		if(waveNumber == 2) {
			for(int i = 0; i < 8; i++) {
				enemies.add(new Enemy(1, 1));
			}
		}
		if(waveNumber == 3) {
			for(int i = 0; i < 4; i++) {
				enemies.add(new Enemy(1, 1));
			}
			enemies.add(new Enemy(1, 2));
			enemies.add(new Enemy(1, 2));
		}
		if(waveNumber == 4) {
			enemies.add(new Enemy(1, 3));
			enemies.add(new Enemy(1, 4));
			for(int i = 0; i < 4; i++) {
				enemies.add(new Enemy(2, 1));
			}
		}
		if(waveNumber == 5) {
			enemies.add(new Enemy(1, 4));
			enemies.add(new Enemy(1, 3));
			enemies.add(new Enemy(2, 3));
		}
		if(waveNumber == 6) {
			enemies.add(new Enemy(1, 3));
			for(int i = 0; i < 4; i++) {
				enemies.add(new Enemy(2, 1));
				enemies.add(new Enemy(3, 1));
			}
		}
		if(waveNumber == 7) {
			enemies.add(new Enemy(1, 3));
			enemies.add(new Enemy(2, 3));
			enemies.add(new Enemy(3, 3));
		}
		if(waveNumber == 8) {
			enemies.add(new Enemy(1, 4));
			enemies.add(new Enemy(2, 4));
			enemies.add(new Enemy(3, 4));
		}
		if(waveNumber == 9) {
			running = false;
		}
		
	}
	
	public void keyTyped(KeyEvent key) {}
	public void keyPressed(KeyEvent key) {
		int keyCode = key.getKeyCode();
		if(keyCode == KeyEvent.VK_LEFT) {
			serverPlayer.setLeft(true);
		}
		if(keyCode == KeyEvent.VK_RIGHT) {
			serverPlayer.setRight(true);
		}
		if(keyCode == KeyEvent.VK_UP) {
			serverPlayer.setUp(true);
		}
		if(keyCode == KeyEvent.VK_DOWN) {
			serverPlayer.setDown(true);
		}
		if(keyCode == KeyEvent.VK_SPACE) {
			serverPlayer.setFiring(true);
		}
	}
	public void keyReleased(KeyEvent key) {
		int keyCode = key.getKeyCode();
		if(keyCode == KeyEvent.VK_LEFT) {
			serverPlayer.setLeft(false);
		}
		if(keyCode == KeyEvent.VK_RIGHT) {
			serverPlayer.setRight(false);
		}
		if(keyCode == KeyEvent.VK_UP) {
			serverPlayer.setUp(false);
		}
		if(keyCode == KeyEvent.VK_DOWN) {
			serverPlayer.setDown(false);
		}
		if(keyCode == KeyEvent.VK_SPACE) {
			serverPlayer.setFiring(false);
		}
	}
	
}
























