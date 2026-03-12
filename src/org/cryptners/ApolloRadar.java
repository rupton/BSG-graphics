package org.cryptners;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ApolloRadar1978
 *
 * A single-file Java Swing app that recreates a retro Battlestar Galactica-style
 * green radar/computer display similar to the reference image.
 *
 * Run:
 *   javac ApolloRadar1978.java
 *   java ApolloRadar1978
 *
 * Optional export:
 *   java ApolloRadar1978 output.png
 */
public class ApolloRadar extends JPanel {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    private final List<Cloud> clouds;

    public ApolloRadar() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        clouds = buildClouds();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ApolloRadar panel = new ApolloRadar();

            if (args.length > 0) {
                try {
                    panel.exportPng(args[0]);
                    System.out.println("Saved image to: " + args[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            JFrame frame = new JFrame("Apollo Radar 1978");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private void exportPng(String path) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        applyQuality(g2);
        paintScene(g2, WIDTH, HEIGHT);
        g2.dispose();
        ImageIO.write(image, "png", new File(path));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        applyQuality(g2);
        paintScene(g2, getWidth(), getHeight());
        g2.dispose();
    }

    private void paintScene(Graphics2D g2, int w, int h) {
        Color bg = new Color(3, 10, 5);
        Color grid = new Color(40, 120, 80, 140);
        Color dimText = new Color(90, 180, 110, 185);
        Color glow = new Color(0, 255, 120, 70);
        Color bright = new Color(20, 255, 120, 215);
        Color brightLine = new Color(110, 255, 180, 145);

        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);

        // Mild CRT bloom / vignette.
        paintVignette(g2, w, h);
        paintScanlines(g2, w, h);

        int cx = w / 2;
        int cy = h / 2 + 12;
        int r = Math.min(w, h) / 2 - 58;

        // Header text.
        g2.setFont(new Font("Monospaced", Font.PLAIN, 26));
        g2.setColor(dimText);
        g2.drawString("Apollo sees an unknown ship obscured by clouds", 18, 34);

        // Radar rings and guides.
        g2.setStroke(new BasicStroke(2.2f));
        g2.setColor(grid);
        g2.draw(new Ellipse2D.Double(cx - r, cy - r, 2.0 * r, 2.0 * r));
        g2.draw(new Ellipse2D.Double(cx - r * 0.73, cy - r * 0.73, 2.0 * r * 0.73, 2.0 * r * 0.73));
        g2.draw(new Ellipse2D.Double(cx - r * 0.43, cy - r * 0.43, 2.0 * r * 0.43, 2.0 * r * 0.43));

        g2.drawLine(cx, cy - r, cx, cy + r);
        g2.drawLine(cx - r, cy, cx + r, cy);
        g2.drawLine((int) (cx - r * 0.87), (int) (cy - r * 0.87), (int) (cx + r * 0.87), (int) (cy + r * 0.87));
        g2.drawLine((int) (cx - r * 0.87), (int) (cy + r * 0.87), (int) (cx + r * 0.87), (int) (cy - r * 0.87));

        // Outer arcs similar to the frame markings.
        g2.draw(new Arc2D.Double(cx - r - 26, cy - r - 10, 2.0 * r + 52, 2.0 * r + 30, 18, 144, Arc2D.OPEN));
        g2.draw(new Arc2D.Double(cx - r - 26, cy - r - 10, 2.0 * r + 52, 2.0 * r + 30, 198, 144, Arc2D.OPEN));

        // Ship silhouette behind clouds.
        paintShip(g2, cx, cy, brightLine, glow);

        // Clouds on top of the ship.
        for (Cloud cloud : clouds) {
            paintCloud(g2, cloud, glow, bright);
        }

        // Subtle border noise.
        g2.setColor(new Color(30, 70, 40, 50));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(2, 2, w - 5, h - 5);
    }

    private void paintShip(Graphics2D g2, int cx, int cy, Color brightLine, Color glow) {
        Path2D ship = new Path2D.Double();

        // Rough hand-shaped line art based on the right-facing ship silhouette in the reference.
        int x = cx + 40;
        int y = cy + 52;
        ship.moveTo(x - 110, y - 18);
        ship.lineTo(x - 55, y - 18);
        ship.lineTo(x - 34, y - 40);
        ship.lineTo(x + 20, y - 42);
        ship.lineTo(x + 28, y - 58);
        ship.lineTo(x + 84, y - 60);
        ship.lineTo(x + 115, y - 46);
        ship.lineTo(x + 180, y - 44);
        ship.lineTo(x + 210, y - 34);
        ship.lineTo(x + 250, y - 34);
        ship.lineTo(x + 282, y - 12);
        ship.lineTo(x + 332, y - 8);
        ship.lineTo(x + 332, y + 3);
        ship.lineTo(x + 274, y - 1);
        ship.lineTo(x + 248, y - 18);
        ship.lineTo(x + 214, y - 18);
        ship.lineTo(x + 172, y - 8);
        ship.lineTo(x + 72, y - 6);
        ship.lineTo(x + 34, y + 3);
        ship.lineTo(x - 16, y + 3);
        ship.lineTo(x - 40, y + 18);
        ship.lineTo(x - 110, y + 18);
        ship.closePath();

        // Main ship glow.
        g2.setColor(glow);
        for (int i = 14; i >= 5; i -= 3) {
            g2.setStroke(new BasicStroke(i, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(ship);
        }

        g2.setColor(brightLine);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(ship);

        // Details.
        g2.draw(new Line2D.Double(x - 20, y - 18, x - 20, y + 6));
        g2.draw(new Line2D.Double(x - 2, y - 18, x - 2, y + 1));
        g2.draw(new Line2D.Double(x + 16, y - 35, x + 16, y + 1));
        g2.draw(new Line2D.Double(x + 96, y - 51, x + 114, y - 63));
        g2.draw(new Line2D.Double(x + 196, y - 24, x + 264, y - 24));
        g2.draw(new Line2D.Double(x + 192, y - 19, x + 248, y + 4));
        g2.draw(new Line2D.Double(x + 258, y - 30, x + 268, y - 42));
        g2.draw(new Ellipse2D.Double(x + 263, y - 41, 9, 9));

        // Tiny pilot-like figure at the tip for that retro technical-screen feel.
        int fx = x + 270;
        int fy = y - 42;
        g2.draw(new Ellipse2D.Double(fx, fy, 8, 8));
        g2.draw(new Line2D.Double(fx + 4, fy + 8, fx + 4, fy + 20));
        g2.draw(new Line2D.Double(fx + 4, fy + 12, fx - 4, fy + 18));
        g2.draw(new Line2D.Double(fx + 4, fy + 12, fx + 12, fy + 18));
        g2.draw(new Line2D.Double(fx + 4, fy + 20, fx - 2, fy + 30));
        g2.draw(new Line2D.Double(fx + 4, fy + 20, fx + 10, fy + 30));
    }

    private void paintCloud(Graphics2D g2, Cloud cloud, Color glow, Color bright) {
        Area area = new Area();
        for (Ellipse2D ellipse : cloud.puffs) {
            area.add(new Area(ellipse));
        }
        area.add(new Area(new RoundRectangle2D.Double(cloud.baseX, cloud.baseY, cloud.baseW, cloud.baseH, 40, 40)));

        // Glow passes.
        for (int i = 24; i >= 8; i -= 4) {
            g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 25 + i));
            g2.setStroke(new BasicStroke(i, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(area);
        }

        // Fill.
        GradientPaint gp = new GradientPaint(
                0, (float) cloud.baseY, new Color(18, 255, 130, 200),
                0, (float) (cloud.baseY + cloud.baseH), new Color(0, 220, 110, 230)
        );
        g2.setPaint(gp);
        g2.fill(area);

        g2.setColor(bright);
        g2.setStroke(new BasicStroke(1.8f));
        g2.draw(area);
    }

    private void paintVignette(Graphics2D g2, int w, int h) {
        Point2D center = new Point2D.Float(w / 2f, h / 2f);
        float radius = Math.max(w, h) * 0.65f;
        float[] dist = {0f, 0.7f, 1f};
        Color[] colors = {
                new Color(0, 90, 30, 10),
                new Color(0, 0, 0, 20),
                new Color(0, 0, 0, 160)
        };
        RadialGradientPaint rgp = new RadialGradientPaint(center, radius, dist, colors);
        Paint old = g2.getPaint();
        g2.setPaint(rgp);
        g2.fillRect(0, 0, w, h);
        g2.setPaint(old);
    }

    private void paintScanlines(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 28));
        for (int y = 0; y < h; y += 4) {
            g2.drawLine(0, y, w, y);
        }
    }

    private List<Cloud> buildClouds() {
        List<Cloud> list = new ArrayList<>();
        list.add(makeCloud(120, 300, 360, 108, 16, 42));
        list.add(makeCloud(272, 365, 210, 102, 13, 34));
        list.add(makeCloud(515, 260, 125, 82, 9, 26));
        list.add(makeCloud(585, 360, 155, 94, 10, 28));
        list.add(makeCloud(292, 120, 92, 56, 7, 16));
        list.add(makeCloud(195, 518, 350, 70, 15, 36));
        list.add(makeCloud(355, 470, 172, 70, 11, 22));
        return list;
    }

    private Cloud makeCloud(int x, int y, int w, int h, int puffs, int seed) {
        Random rand = new Random(seed);
        List<Ellipse2D> ellipses = new ArrayList<>();
        for (int i = 0; i < puffs; i++) {
            double ew = w * (0.16 + rand.nextDouble() * 0.18);
            double eh = h * (0.45 + rand.nextDouble() * 0.4);
            double ex = x + rand.nextDouble() * (w - ew);
            double ey = y - h * 0.18 + rand.nextDouble() * (h * 0.45);
            ellipses.add(new Ellipse2D.Double(ex, ey, ew, eh));
        }
        return new Cloud(x, y, w, h, ellipses);
    }

    private static void applyQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static class Cloud {
        final int baseX;
        final int baseY;
        final int baseW;
        final int baseH;
        final List<Ellipse2D> puffs;

        Cloud(int baseX, int baseY, int baseW, int baseH, List<Ellipse2D> puffs) {
            this.baseX = baseX;
            this.baseY = baseY;
            this.baseW = baseW;
            this.baseH = baseH;
            this.puffs = puffs;
        }
    }
}
