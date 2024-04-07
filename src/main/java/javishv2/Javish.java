package javishv2;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

//Convert to virtualCoord which includes negatives
//Determine closest of -1/0/+1 translation of previous virtualCoord
//Store and Convert to realCoord for any display or speed calcs
//Convert virtualCoord to screenCoord when displaying, factoring in offset
//Do not actually modify the coord data by offset.
//Only virtualCoord may have any notion of offset, by being negative
//Make utility calls for conversion, and getting page translation offset
//Make utility call for translation by applying offset
//Make utility call for determining closest of 3 page options to previous point
//
// Better:
// Draw points on every map
// Render only the points which are on screen
// Render any line segments which connect to onscreen points
// Add framerate cap for gpu usage

public class Javish extends JPanel {

    private static final long serialVersionUID = 7503572529729904779L;

    public static Javish self;

    private InputHandler input;
    public GameMap gameMap;
    public CoordProvider coordProvider;

    public Javish() {
        this.input = new InputHandler();
        this.gameMap = new GameMap();
        this.coordProvider = new CoordProvider();
        self = this;

        setFocusable(true);
        requestFocus();
        setPreferredSize(new Dimension(1200, 800));
        addMouseListener(input);
        addMouseMotionListener(input);
        addKeyListener(input);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        gameMap.render(g);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Javish");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new Javish());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}
