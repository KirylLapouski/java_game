import java.awt.*;
import java.io.Serializable;

public class Player implements Serializable {
	
	// FIELDS
	private int x;
	private int y;
	private int r;
	
	private int dx;
	private int dy;
	private int speed;
	
	private boolean left;
	private boolean right;
	private boolean up;
	private boolean down;
	
	private boolean firing;
	private long firingTimer;
	private long  firingDelay;
	
	private boolean recovering;
	private long recoveryTimer;
	
	private Color color1;
	private Color color2;
	
	private int score;

	private int lives;
	private int powerLevel;
	private int power;
	private int[] requiredPower = {
		1, 2, 3, 4, 5
	};

	public boolean needToSend=false;
	
	// CONSTRUCTOR
	public Player() {
		
		x = GamePanel.WIDTH / 2;
		y = GamePanel.HEIGHT / 2;
		r = 5;
		
		dx = 0;
		dy = 0;
		speed = 5;
		
		lives = 3;
		color1 = Color.WHITE;
		color2 = Color.RED;
		
		firing = false;
		firingTimer = System.nanoTime();
		firingDelay = 200;
		
		recovering = false;
		recoveryTimer = 0;
		
		score = 0;
		
	}
	
	// FUNCTIONS
	
	public int getx() { return x; }
	public int gety() { return y; }
	public int getr() { return r; }

	public int getScore() { return score; }
	
	public int getLives() { return lives; }
	
	public boolean isDead() {
		needToSend=true;
		return lives <= 0; }
	public boolean isRecovering() { return recovering; }
	
	public void setLeft(boolean b) { left = b; }
	public void setRight(boolean b) { right = b; }
	public void setUp(boolean b) { up = b; }
	public void setDown(boolean b) { down = b; }

	public void setFiring(boolean b) { firing = b; }
	
	public void addScore(int i) {
		needToSend=true;
		score += i; }
	
	public void gainLife() {
		lives++;
		needToSend=true;
	}
	
	public void loseLife() {
		lives--;
		recovering = true;
		recoveryTimer = System.nanoTime();
		needToSend=true;
	}
	
	public void increasePower(int i) {
		power += i;
		needToSend=true;
		if(powerLevel == 4) {
			if(power > requiredPower[powerLevel]) {
				power = requiredPower[powerLevel];
			}
			return;
		}
		if(power >= requiredPower[powerLevel]) {
			power -= requiredPower[powerLevel];
			powerLevel++;
		}
	}
	
	public int getPower() { return power; }
	public int getRequiredPower() { return requiredPower[powerLevel]; }
	
	public void update() {
		
		if(left) {
			dx = -speed;
			needToSend=true;
		}
		if(right) {
			dx = speed;
			needToSend=true;
		}
		if(up) {
			dy = -speed;
			needToSend=true;
		}
		if(down) {
			dy = speed;
			needToSend=true;
		}
		
		x += dx;
		y += dy;
		
		if(x < r) x = r;
		if(y < r) y = r;
		if(x > GamePanel.WIDTH - r) x = GamePanel.WIDTH - r;
		if(y > GamePanel.HEIGHT - r) y = GamePanel.HEIGHT - r;
		
		dx = 0;
		dy = 0;
		
		// firing
		if(firing) {
			long elapsed = (System.nanoTime() - firingTimer) / 1000000;
			
			if(elapsed > firingDelay) {
				
				firingTimer = System.nanoTime();
				
				if(powerLevel < 2) {
					GamePanel.bullets.add(new Bullet(270, x, y));
				}
				else if(powerLevel < 4) {
					GamePanel.bullets.add(new Bullet(270, x + 5, y));
					GamePanel.bullets.add(new Bullet(270, x - 5, y));
				}
				else {
					GamePanel.bullets.add(new Bullet(270, x, y));
					GamePanel.bullets.add(new Bullet(275, x + 5, y));
					GamePanel.bullets.add(new Bullet(265, x - 5, y));
				}
				
			}
		}
		
		if(recovering) {
			long elapsed = (System.nanoTime() - recoveryTimer) / 1000000;
			if(elapsed > 2000) {
				recovering = false;
				recoveryTimer = 0;
				needToSend=true;
			}
		}
		
	}
	public void update(int _x,int _y){
		this.x=_x;
		this.y=_y;

	}

	public void update(Player _player)
	{
		this.x=_player.getx();
		this.y=_player.gety();
		this.recovering = _player.recovering;

	}

	public void draw(Graphics2D g) {
		
		if(recovering) {
			g.setColor(color2);
			g.fillOval(x - r, y - r, 2 * r, 2 * r);
			
			g.setStroke(new BasicStroke(3));
			g.setColor(color2.darker());
			g.drawOval(x - r, y - r, 2 * r, 2 * r);
			g.setStroke(new BasicStroke(1));
		}
		else {
			g.setColor(color1);
			g.fillOval(x - r, y - r, 2 * r, 2 * r);
			
			g.setStroke(new BasicStroke(3));
			g.setColor(color1.darker());
			g.drawOval(x - r, y - r, 2 * r, 2 * r);
			g.setStroke(new BasicStroke(1));
		}
		
	}
	
}
















