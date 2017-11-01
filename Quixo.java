// Mark & Elizabeth
// CS 201
// Final Project


import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

/*public class Quixo extends Applet implements ActionListener {*/
public class Quixo extends Panel implements ActionListener {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static GameBoard c;
    Button ruleButton, undoButton, clearButton;
    static Label title, turnDisplay, bottomDisplay;
    static Panel w;
	/*---------------------------------------------------------*/
	public static void main(String[] args) {
		Frame f = new Frame();
  		f.addWindowListener(new java.awt.event.WindowAdapter() {
       			public void windowClosing(java.awt.event.WindowEvent e) {
       				System.exit(0);
       			};
     		});

  		UnderlineText q = new Quixo();
  		q.setSize(800,600); // same size as defined in the HTML APPLET
  		q.add(ut);
  		q.pack();
  		q.init();
  		q.setSize(800,600 + 20); // add 20, seems enough for the Frame title,
  		q.show();
  	}
    /*----------------------------------------------------------*/
    // Initializes the game
    public void init () {
    	
    	setLayout(new BorderLayout());
    	bottomDisplay = new Label("", Label.CENTER);
    	ruleButton = new Button("Rules");
    	ruleButton.setBackground(Color.white);
    	ruleButton.addActionListener(this);
    	clearButton = new Button("New Game");
    	clearButton.setBackground(Color.white);
    	clearButton.addActionListener(this);
    	undoButton = new Button("Undo");
    	undoButton.setBackground(Color.white);
    	undoButton.addActionListener(this);

        c = new GameBoard();
        Panel w = makeSidePanel();
        c.setBackground(Color.black);
        c.addMouseListener(c);
        c.addMouseMotionListener(c);
    	c.addKeyListener(c); // tell canvas to listen to key presses
        add("Center", c);
        add("West", w);
        add("North", makeTopPanel());
        add("South", bottomDisplay);
        bottomDisplay.setBackground(Color.black);
    }
    
    // Sets up the panel of buttons
    public Panel makeSidePanel() {
    	Panel sidePanel = new Panel();
        sidePanel.setLayout(new GridLayout(3,1));
        sidePanel.setBackground(Color.black);
        sidePanel.add(ruleButton);
        sidePanel.add(clearButton);
        sidePanel.add(undoButton);
        return sidePanel;
    }
    
    // Sets up the title 
    public Panel makeTopPanel() {
    	Panel sidePanel = new Panel();
    	turnDisplay = new Label("Player1's Turn", Label.CENTER);
        turnDisplay.setBackground(Color.black);
        turnDisplay.setForeground(Color.green);
    	title = new Label("QUIXO", Label.CENTER);
        title.setBackground(Color.black);
        title.setForeground(Color.white);
        title.setFont(new Font(null, Font.PLAIN, 24));;
        sidePanel.setLayout(new GridLayout(2,1));
        sidePanel.setBackground(Color.black);
        sidePanel.add(title);
        sidePanel.add(turnDisplay);
        return sidePanel;
    }
    
    // Links buttons with their respective methods
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == clearButton) {
            c.clear();
        } 
        if (e.getSource() == undoButton) {
            c.undo();
        }
        if (e.getSource() == ruleButton) {
				c.rule();
        } 
    }	
}

class GameBoard extends Canvas implements MouseListener, MouseMotionListener, KeyListener, Runnable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	Image offscreen;
    Dimension offscreensize;
    Graphics g2;
	
	// instance variables representing the game go here
	static int turn = 1; // Keep's track of player's turns
	boolean player1HasWon = false;
	boolean player2HasWon = false;		
	boolean isSelected = false; // Is a square currently selected by user
	Point mouseDrag = new Point(); // Location of mouse dragging square
	Point hover = new Point(); // Stores block coordinates, mouse is "hovering" over
	Point emptyBox = new Point(); // Previous location of square being moved
	int grid = 5; // Grid dimensions (5 is Standard)
	int[][] board = new int[grid][grid]; // Stores state of current board
    int size = 70; // size of each block
    Dimension border = new Dimension(size+size/2, size+size/2); // border around game
    Thread t;
	boolean animate = false; // Does the sliding animation need to run
	Point boxOffset = new Point(0,0); // How far boxes need to slide
	ArrayList<Point> animateBox = new ArrayList<Point>(); // List of boxes to animate
	Stack<int[][]> moves = new Stack<int[][]>(); // stack used to keep track of moves for undo
	
	
    // draw the boxes
    
    public void update(Graphics g) {
        Dimension d = Quixo.c.getSize();
        size = Math.min(d.width, d.height)/(grid+3);
        border.setSize((d.width-grid*size)/2, (d.height-grid*size)/2);

	// initially (or when size changes) create new image
        if ((offscreen == null)
	    || (d.width != offscreensize.width)
	    || (d.height != offscreensize.height)) {
            offscreen = createImage(d.width, d.height);
            offscreensize = d;
            g2 = offscreen.getGraphics();
            g2.setFont(getFont());
        }
	
	// erase old contents:
        g2.setColor(getBackground());
        g2.fillRect(0, 0, d.width, d.height);

	// now, draw as usual, but use g2 instead of g
    	
        // Basic GameBoard Drawing
        for (int i = 0; i < grid; i++) {
            for (int j = 0; j < grid; j++) {
            	if (!animateBox.contains(new Point(i,j))) {
            		int x = i * size + border.width;
            		int y = j * size + border.height;
                	setBoxColor(g,board[i][j]);
                	g2.fillRect(x, y, size, size);
                	g2.setColor(Color.black);
                	g2.drawRect(x, y, size, size);
                }
            }
        }
        // Draws Sliding Blocks
        for (int i = 0; i < animateBox.size(); i++) {
            int x = animateBox.get(i).x * size + border.width + boxOffset.x;
            int y = animateBox.get(i).y * size + border.height + boxOffset.y;
        	setBoxColor(g,board[animateBox.get(i).x][animateBox.get(i).y]);
            g2.fillRect(x, y, size, size);
            g2.setColor(Color.black);
            g2.drawRect(x, y, size, size);
        }
        // Draws Selected Block Around Mouse
        if (isSelected) {
            highlightLegalMoves(emptyBox);
        	setBoxColor(g, turn);
            g2.fillRect(mouseDrag.x-(size/2), mouseDrag.y-(size/2), size, size);
            g2.setColor(Color.black);
            int a = size/12;
            g2.drawRect(mouseDrag.x-(size/2), mouseDrag.y-(size/2), size, size);
    		g2.drawRect(mouseDrag.x-(size/2)+a, mouseDrag.y-(size/2)+a, size-2*a, size-2*a);
        } else {
        	// Highlights Block that mouse is hovering over, if it is a legal move.
        	if (hover.x >= 0 && hover.x < grid &&
        			hover.y >= 0 && hover.y < grid &&
                    (hover.x == 0 || hover.x == (grid-1) || hover.y == 0 || hover.y == (grid-1)) &&
                    (board[hover.x][hover.y] == turn || board[hover.x][hover.y] == 0) &&
                    !animate) {
        		g2.setColor(Color.black);
        		int a = size/12;
        		g2.drawRect((hover.x)*size+border.width+a, (hover.y)*size+border.height+a, size-2*a, size-2*a);
//        		highlightLegalMoves(hover);
        	} else {}
        }
        // Starts and Stops Sliding Animation
        if (animate) {
        	start();
        } else {
        	stop();
        }    
        
    	// Finally, draw the image on top of the old one
    	g.drawImage(offscreen, 0, 0, null);	
    }
    
    // helper function called from update to set color of each box
    public void setBoxColor(Graphics g, int x) {
    	if (x == -1) {
            g2.setColor(Color.black);
    	} else if (x == 1) {
    		g2.setColor(Color.green);
    	} else if (x == 2) {
    		g2.setColor(Color.red);
    	} else {
            g2.setColor(Color.white);
    	}
    }
    
    // this is called when window is uncovered or resized
    public void paint(Graphics g) {
    	update(g);
    }
    
    // handle mouse events
	public void mouseMoved(MouseEvent event) {
		if (!player1HasWon && !player2HasWon) {
			setHover(event.getPoint());
		repaint();
		}
	}
	
	public void mousePressed(MouseEvent event) {
		// makes sure the game hasn't finished
		if (!player1HasWon && !player2HasWon) {
			Point p = event.getPoint();
			setHover(p);

			mouseDrag.setLocation(hover.x-size/2,hover.y-size/2);
			// check if clicked in board area
			if (hover.x >= 0 && hover.x < grid &&
					hover.y >= 0 && hover.y < grid &&
					//check if legal
					(hover.x == 0 || hover.x == (grid-1) || hover.y == 0 || hover.y == (grid-1)) &&
					(board[hover.x][hover.y] == turn || board[hover.x][hover.y] == 0)) {
				isSelected = true;
				moves.push(copy(board)); //updates moves record
				emptyBox.setLocation(hover.x,hover.y);
			} else {
				isSelected = false;
			}
			repaint();
		}
	}
    
	@Override
	public void mouseDragged(MouseEvent event) {
        if (isSelected) {
        	mouseDrag = event.getPoint();;
        	board[emptyBox.x][emptyBox.y] = -1;
        	repaint();
        }
	}

	// Responds to releasing the mouse
	public void mouseReleased(MouseEvent event) {
		// checks if a square has been selected
		if (isSelected) {
			Point p = event.getPoint();
			int x = (p.x - border.width);
			int y = (p.y - border.height);

			setHover(p);	

			// SAME Y PLANE
			if (((x > -size && x <= 0)
					|| (x >= grid*size && x < (grid+1)*size))
					&& (y >= emptyBox.y*size && y <= (emptyBox.y+1)*size)
					&& Math.abs(x-emptyBox.x*size) > size
					&& Math.abs(x-(emptyBox.x+1)*size) > size) {
				// Slide Left
				if (emptyBox.x < x/size) {
					for (int i = emptyBox.x; i < grid-1; i++) {
						board[i][emptyBox.y] = board[i+1][emptyBox.y];
						animateBox.add(new Point(i,emptyBox.y));
					}
					board[grid-1][emptyBox.y] = turn;
					animateBox.add(new Point(grid-1,emptyBox.y));
					boxOffset.x = size;
					// Slide Right
				} else {
					for (int i = emptyBox.x; i > 0; i--) {
						board[i][emptyBox.y] = board[i-1][emptyBox.y];
						animateBox.add(new Point(i,emptyBox.y));
					}
					board[0][emptyBox.y] = turn;
					animateBox.add(new Point(0,emptyBox.y));
					boxOffset.x = -size;
				}
				nextTurn();
				animate = true;
				// SAME X PLANE
			} else if (((y > -size && y <= 0)
					|| (y >= grid*size && y < (grid+1)*size))
					&& (x >= emptyBox.x*size && x <= (emptyBox.x+1)*size)
					&& Math.abs(y-emptyBox.y*size) > size
					&& Math.abs(y-(emptyBox.y+1)*size) > size){
				// Slide Up
				if (emptyBox.y < y/size) {
					for (int j = emptyBox.y; j < grid-1; j++) {
						board[emptyBox.x][j] = board[emptyBox.x][j+1];
						animateBox.add(new Point(emptyBox.x,j));
					}
					board[emptyBox.x][grid-1] = turn;
					animateBox.add(new Point(emptyBox.x,grid-1));
					boxOffset.y = size;
					// Slide Down
				} else {
					for (int j = emptyBox.y; j > 0; j--) {
						board[emptyBox.x][j] = board[emptyBox.x][j-1];
						animateBox.add(new Point(emptyBox.x,j));
					}
					board[emptyBox.x][0] = turn;
					animateBox.add(new Point(emptyBox.x,0));
					boxOffset.y = -size;
				}
				nextTurn();
				animate = true;
			} else {
				board = moves.pop();
				animateBox.add(new Point(emptyBox.x,emptyBox.y));
				boxOffset = new Point(p.x-(size/2)-border.width-(emptyBox.x*size),
						p.y-(size/2)-border.height-(emptyBox.y*size));
				animate = true;
			}
			isSelected = false;
		}
		repaint();
	}


    
    // need these also because we implement a MouseListener
    public void mouseEntered(MouseEvent event) { }
    public void mouseExited(MouseEvent event) { }
    public void mouseClicked(MouseEvent event) { }
    
	@Override
	// Resizes the grid using keys 
	public void keyPressed(KeyEvent event) {
		int keyCode = event.getKeyCode();   
		if (keyCode == KeyEvent.VK_MINUS  && grid > 3) {
		    clear();
		    grid -=1;
			board = new int[grid][grid];
			repaint(); 
		}if (keyCode == KeyEvent.VK_EQUALS) {
		    clear();
		    grid +=1;
			board = new int[grid][grid];
			repaint(); 
		    
		}  		
	}

    // need these also because we implement a KeyListener
	public void keyReleased(KeyEvent arg0) {}
	public void keyTyped(KeyEvent arg0) { }

    // methods called from the event handler of the main applet
    
    // Displays the rules
	public void rule() {
    	JTextArea rules = new JTextArea(
    			"THE GOAL:" +
    			"\nTo get 5 squares of your color in a row (either horizontally, vertically, or diagonally)." +
    			"\n" +
    			"\nHOW TO PLAY" +
    			"\nIn turn, each player chooses a square and moves (by clicking and dragging) it according to the following rules. In no event can a player miss his/her turn." +
    			"\n" +
    			"\nChoosing and taking a square:" +
    			"\nThe player chooses and takes a blank square, or one with his/her color on it, from the board?s periphery. " +
    			"\nIn the first round, the players have no choice but to take a blank square. You are not allowed to take a square bearing your opponent?s color." +
    			"\nWhether the square taken is blank or already bears the player?s color, it will always be replaced by a square with the player?s color." +
    			"\n" +
    			"\nReplacing the square:" +
    			"\nOnce a square is taken, the player can choose at which end of the incomplete columns or rows to place the selected square. The squares will then slide to complete the column or row. You can never replace the square just played back in the position from which it was taken." +
    			"\n" +
    			"\nEND OF GAME" +
    			"\nThe winner is the player to make a horizontal, vertical or diagonal line with 5 squares bearing his/her color. " +
    			"\nThe player to make a line with his/her opponent?s color loses the game, even if he/she makes a line with his/her own color at the same time."
    			);
    		rules.setRows(27);
    		rules.setColumns(40);
    		rules.setBackground(UIManager.getDefaults().getColor("Panel.background"));
    		rules.setLineWrap(true);
    		rules.setWrapStyleWord(true);
    		JOptionPane.showMessageDialog(null,rules,"Rules of QUIXO",JOptionPane.PLAIN_MESSAGE);
    }
    
    // Restarts the game and the board
    public void clear() {
        for (int i = 0; i < grid; i++) {
            for (int j = 0; j < grid; j++) {
            	board[i][j] = 0;
            }
        }
		player1HasWon = false;
		player2HasWon = false;
		turn = 1;
		Quixo.turnDisplay.setForeground(Color.green);
		Quixo.turnDisplay.setText("Player1's Turn");
        moves.clear();
        repaint();
    }
    
    // Repaints the board as it was one turn ago
    public void undo() {
    	if (!moves.empty()) {
            board = moves.pop();
    		player1HasWon = false;
    		player2HasWon = false;
    		repaint();
    		nextTurn();
    	}
    }

    // switches turns (first checking to see if anyone has won)
    protected void nextTurn() {
    	isDiagonalRight();
    	isDiagonalLeft();
    	isVertical();
    	isHorizontal();
    	// If both players have 5 in a row,
    	// the player whose turn it is loses
    	if (player1HasWon && player2HasWon) {
    		if (turn == 1 ) {
        		Quixo.turnDisplay.setForeground(Color.red);
        		Quixo.turnDisplay.setText("Player2 Wins");
    		} else {
        		Quixo.turnDisplay.setForeground(Color.green);
        		Quixo.turnDisplay.setText("Player1 Wins");
    		}	
    	} else if (player1HasWon) {
        	Quixo.turnDisplay.setText("Player1 Wins!");
    	} else if (player2HasWon) {
        	Quixo.turnDisplay.setText("Player2 Wins!");
    	} else {
    		if (turn == 1) {
    			Quixo.turnDisplay.setForeground(Color.red);
    			Quixo.turnDisplay.setText("Player2's Turn");
    		} else {
    			Quixo.turnDisplay.setForeground(Color.green);
    			Quixo.turnDisplay.setText("Player1's Turn");
    		}
    	}
		turn = turn%2 + 1;
    }
    
    // Checks if there are 5 in a row in left diagonal
    public void isDiagonalLeft() {
     	int count = 1;
    	for (int i=1; i<grid; i++) {
    		if (board[0][0] == board[i][i]) {
				count++;
    		}
    	}
		if (count == grid && board[0][0] != 0)
			isWinner(board[0][0]);
    }

    // Checks if there are 5 in a row in right diagonal
    public void isDiagonalRight() {
     	int count = 1;
    	for (int i=1; i<grid; i++) {
    		if (board[grid-1][0] == board[grid-1-i][i]) {
				count++;
    		}
    	}
		if (count == grid && board[0][grid-1] != 0)
			isWinner(board[0][grid-1]);
    }

    // Checks if there are 5 in a row in any row
    public void isHorizontal() {
    	int count = 1;
    	for (int j=0; j<grid; j++) {
    		for (int i=1; i<grid; i++) {
    			if (board[0][j] == board[i][j]) {
    				count++;
    			} 
    		}
    		if (count == grid && board[0][j] != 0) {
    			isWinner(board[0][j]);
    			count = 1;
    		} else {
    			count = 1;
    		}
    	}
    }

    // Checks if there are 5 in a row in any column
    public void isVertical() {
    	int count = 1;
    	for (int i=0; i<grid; i++) {
    		for (int j=1; j<grid; j++) {
    			if (board[i][0] == board[i][j]) {
    				count++;
    			} 
    		}
    		if (count == grid && board[i][0] != 0) {
    			isWinner(board[i][0]);
    			count = 1;
    		} else {
    			count = 1;
    		}
    	}
    }
    
    // Resets winner status
    public void isWinner(int i) {
    	if (i == 1)
    		player1HasWon = true;
    	if (i == 2)
    		player2HasWon = true;
    }
    
    // Draws a rectangle on squares that mark a legal move
    protected void highlightLegalMoves(Point p) {
		int x = p.x * size + border.width;
		int y = p.y * size + border.height;
		setBoxColor(g2, turn);
		if (p.x != 0) {
			g2.drawRect(border.width-size+1, y+1, size-2, size-2);
		}
        if (p.x != grid-1) {
        	g2.drawRect(grid*size+border.width+1, y+1, size-2, size-2);
        }
        if (p.y != 0) {
        	g2.drawRect(x+1, border.height-size+1, size-2, size-2);
        }
        if (p.y != grid-1) {
        	g2.drawRect(x+1, grid*size+border.height+1, size-2, size-2);
        }
    }
    
    // Resets hover using point coordinates
    public void setHover(Point p) {
		if (p.x < border.width)
			hover.x = (p.x-border.width)/size - 1;
		else
			hover.x = (p.x-border.width)/size;
		if (p.y < border.height)
			hover.y = (p.y-border.height)/size - 1;
		else
			hover.y = (p.y-border.height)/ size;
    }
    
    // Stores a copy of the board in an array
    public int[][] copy(int[][] a) {
    	int[][] b = new int[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
            	b[i][j] = a[i][j];
            }
        }
    	return b;
    }
    
    // ANIMATION
    // Adjusts Location of Sliding Blocks
    public void run() {
    	Thread currentThread = Thread.currentThread();
    	while (currentThread == t) {
    		boxOffset.x = 9*boxOffset.x/10;
    		boxOffset.y = 9*boxOffset.y/10;
    		if (Math.abs(boxOffset.x) < size/10 && Math.abs(boxOffset.y) < size/10) {
    			animate = false;
    			animateBox.clear();
    			boxOffset.setLocation(0,0);
    		}
    		try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}		
    		repaint();
    	}
    }

    public void start() {
    	t = new Thread(this);
    	t.start();
    }

    public void stop() {
    	t = null;
    }
}
