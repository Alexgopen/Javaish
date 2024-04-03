package javishv2;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
