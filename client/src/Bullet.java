import java.awt.*;

public class Bullet {
	
	// FIELDS
	private int x;
	private int y;
	private int r;
	
	private double dx;
	private double dy;
	private double rad;
	private double speed;

	private float angle;
	private Color color1;

	public boolean needToSend=true;

	// CONSTRUCTOR
	public Bullet(double angle, int x, int y) {
		
		this.x = x;
		this.y = y;
		r = 2;
		
		rad = Math.toRadians(angle);
		speed = 10;
		dx = Math.cos(rad) * speed;
		dy = Math.sin(rad) * speed;

		this.angle= (float) angle;
		color1 = Color.YELLOW;
		
	}

	//CONSTRUCTOR FOR SERVER BULLETS
	public Bullet(double angle, int x, int y,boolean update) {

		this.x = x;
		this.y = y;
		r = 2;

		rad = Math.toRadians(angle);
		speed = 10;
		dx = Math.cos(rad) * speed;
		dy = Math.sin(rad) * speed;

		this.angle= (float) angle;
		color1 = Color.YELLOW;

		needToSend=false;
	}
	// FUNCTIONS

	public float getAngle() {
		return angle;
	}
	public int getx() { return x; }
	public int gety() { return y; }
	public double getr() { return r; }
	public double getdx(){return dx;}
	public double getDy() {return dy;}

	public boolean update() {
		
		x += dx;
		y += dy;
		
		if(x < -r || x > GamePanel.WIDTH + r ||
			y < -r || y > GamePanel.HEIGHT + r) {
			return true;
		}
		
		return false;
		
	}
	
	public void draw(Graphics2D g) {
		
		g.setColor(color1);
		g.fillOval((int) (x - r), (int) (y - r), 2 * r, 2 * r);
		
	}
	
}











































