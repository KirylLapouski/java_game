import java.awt.*;

public class Enemy {
	
	// FIELDS
	private int x;
	private int y;
	private int r;
	
	private double dx;
	private double dy;
	private double rad;
	private double speed;
	
	private int health;
	private int type;
	private int rank;
	
	private Color color1;
	
	private boolean dead;
	
	private boolean hit;
	private long hitTimer;
	
	private boolean slow;
	
	// CONSTRUCTOR
	public Enemy(int type, int rank,int x,int y) {

		this.type = type;
		this.rank = rank;

		// default enemy
		if(type == 1) {
			//color1 = Color.BLUE;
			color1 = new Color(0, 0, 255, 128);
			if(rank == 1) {
				speed = 2;
				r = 5;
				health = 1;
			}
			if(rank == 2) {
				speed = 2;
				r = 10;
				health = 2;
			}
			if(rank == 3) {
				speed = 1.5;
				r = 20;
				health = 3;
			}
			if(rank == 4) {
				speed = 1.5;
				r = 30;
				health = 4;
			}
		}
		// stronger, faster default
		if(type == 2) {
			//color1 = Color.RED;
			color1 = new Color(255, 0, 0, 128);
			if(rank == 1) {
				speed = 3;
				r = 5;
				health = 2;
			}
			if(rank == 2) {
				speed = 3;
				r = 10;
				health = 3;
			}
			if(rank == 3) {
				speed = 2.5;
				r = 20;
				health = 3;
			}
			if(rank == 4) {
				speed = 2.5;
				r = 30;
				health = 4;
			}
		}
		// slow, but hard to kill
		if(type == 3) {
			//color1 = Color.GREEN;
			color1 = new Color(0, 255, 0, 128);
			if(rank == 1) {
				speed = 1.5;
				r = 5;
				health = 3;
			}
			if(rank == 2) {
				speed = 1.5;
				r = 10;
				health = 4;
			}
			if(rank == 3) {
				speed = 1.5;
				r = 25;
				health = 5;
			}
			if(rank == 4) {
				speed = 1.5;
				r = 45;
				health = 5;
			}
		}

		this.x = x;
		this.y = y;


		this.dx = dx;
		this.dy =dy;

		dead = false;

		hit = false;
		hitTimer = 0;

	}
	// FUNCTIONS
	public double getx() { return x; }
	public double gety() { return y; }
	public int getr() { return r; }

	public void setSlow(boolean b) { slow = b; }
	

	



	
	public void draw(Graphics2D g) {
		

			g.setColor(color1);
			g.fillOval((int) (x - r), (int) (y - r), 2 * r, 2 * r);
			
			g.setStroke(new BasicStroke(3));
			g.setColor(color1.darker());
			g.drawOval((int) (x - r), (int) (y - r), 2 * r, 2 * r);
			g.setStroke(new BasicStroke(1));

		
	}
	
	
}

























