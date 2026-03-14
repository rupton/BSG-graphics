package org.cryptnerd;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.util.Random;

public class GreenScreenTopoRadar extends JPanel implements ActionListener {

    private final Timer timer;
    private double sweepAngle = 0.0; // radians, 0 = top, increasing clockwise
    private final Random random = new Random();

    private float[][] heightField;
    private final int fieldW = 220;
    private final int fieldH = 220;

    public GreenScreenTopoRadar() {
        setPreferredSize(new Dimension(1000, 1000));
        setBackground(Color.BLACK);
        generateHeightField();
        timer = new Timer(33, this);
        timer.start();
    }

    private void generateHeightField() {
        heightField = new float[fieldW][fieldH];

        for (int y = 0; y < fieldH; y++) {
            for (int x = 0; x < fieldW; x++) {
                double nx = (double) x / fieldW;
                double ny = (double) y / fieldH;

                double value =
                        1.3 * gaussian(nx, ny, 0.30, 0.35, 0.11) +
                        0.9 * gaussian(nx, ny, 0.72, 0.42, 0.16) +
                        1.0 * gaussian(nx, ny, 0.50, 0.76, 0.13) +
                        0.5 * gaussian(nx, ny, 0.18, 0.72, 0.09) +
                        0.25 * Math.sin(12 * nx) * Math.cos(10 * ny) +
                        0.12 * Math.sin(30 * nx + 8 * ny) +
                        0.08 * Math.cos(22 * ny - 4 * nx);

                value = (value + 0.5) / 2.6;
                value = Math.max(0, Math.min(1, value));
                heightField[x][y] = (float) value;
            }
        }
    }

    private double gaussian(double x, double y, double cx, double cy, double sigma) {
        double dx = x - cx;
        double dy = y - cy;
        return Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma));
    }

    // Radar bearing helpers:
    // 0° = top, 90° = right, 180° = bottom, 270° = left
    private double radarX(int cx, double angle, double distance) {
        return cx + Math.sin(angle) * distance;
    }

    private double radarY(int cy, double angle, double distance) {
        return cy - Math.cos(angle) * distance;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h);
        int cx = w / 2;
        int cy = h / 2;
        int radius = (int) (size * 0.42);

        drawBackgroundGlow(g2, cx, cy, radius);

        Shape radarCircle = new Ellipse2D.Double(cx - radius, cy - radius, radius * 2.0, radius * 2.0);
        g2.setClip(radarCircle);

        drawCrtBackdrop(g2, cx, cy, radius);
        drawRadarGrid(g2, cx, cy, radius);
        drawTopography(g2, cx, cy, radius);
        drawSweep(g2, cx, cy, radius);
        drawNoise(g2, cx, cy, radius);

        g2.setClip(null);

        drawOuterRing(g2, cx, cy, radius);
        drawCenterMarker(g2, cx, cy);
        drawHudText(g2, w, h, cx, cy, radius);

        g2.dispose();
    }

    private void drawBackgroundGlow(Graphics2D g2, int cx, int cy, int radius) {
        float[] dist = {0.0f, 0.7f, 1.0f};
        Color[] colors = {
                new Color(0, 80, 0, 180),
                new Color(0, 25, 0, 140),
                new Color(0, 0, 0, 255)
        };
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Double(cx, cy),
                radius * 1.35f,
                dist,
                colors
        );
        g2.setPaint(paint);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawCrtBackdrop(Graphics2D g2, int cx, int cy, int radius) {
        Shape radarCircle = new Ellipse2D.Double(cx - radius, cy - radius, radius * 2.0, radius * 2.0);

        g2.setColor(new Color(0, 18, 0));
        g2.fill(radarCircle);

        for (int y = cy - radius; y <= cy + radius; y += 3) {
            int alpha = (y % 6 == 0) ? 28 : 12;
            g2.setColor(new Color(30, 255, 80, alpha));
            g2.drawLine(cx - radius, y, cx + radius, y);
        }

        float[] dist = {0.0f, 0.75f, 1.0f};
        Color[] colors = {
                new Color(0, 0, 0, 0),
                new Color(0, 0, 0, 40),
                new Color(0, 0, 0, 130)
        };
        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point2D.Double(cx, cy),
                radius,
                dist,
                colors
        );
        g2.setPaint(vignette);
        g2.fill(radarCircle);
    }

    private void drawRadarGrid(Graphics2D g2, int cx, int cy, int radius) {
        g2.setStroke(new BasicStroke(1.2f));

        for (int i = 1; i <= 5; i++) {
            int r = radius * i / 5;
            g2.setColor(new Color(80, 255, 120, 55));
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
        }

        g2.setColor(new Color(80, 255, 120, 70));
        g2.drawLine(cx - radius, cy, cx + radius, cy);
        g2.drawLine(cx, cy - radius, cx, cy + radius);

        for (int deg = 30; deg < 360; deg += 30) {
            double a = Math.toRadians(deg);
            int x = (int) radarX(cx, a, radius);
            int y = (int) radarY(cy, a, radius);
            int xOpp = (int) radarX(cx, a + Math.PI, radius);
            int yOpp = (int) radarY(cy, a + Math.PI, radius);

            g2.setColor(new Color(80, 255, 120, 35));
            g2.drawLine(x, y, xOpp, yOpp);
        }
    }

    private void drawTopography(Graphics2D g2, int cx, int cy, int radius) {
        int left = cx - radius;
        int top = cy - radius;
        int width = radius * 2;
        int height = radius * 2;

        float[] levels = {0.18f, 0.28f, 0.38f, 0.48f, 0.58f, 0.68f, 0.78f, 0.88f};

        for (int i = 0; i < levels.length; i++) {
            float threshold = levels[i];

            int alpha = 35 + i * 18;
            alpha = Math.min(alpha, 190);

            g2.setColor(new Color(100, 255, 140, alpha));
            g2.setStroke(new BasicStroke(i < 4 ? 1.0f : 1.5f));

            for (int y = 0; y < fieldH - 1; y++) {
                for (int x = 0; x < fieldW - 1; x++) {
                    float a = heightField[x][y];
                    float b = heightField[x + 1][y];
                    float c = heightField[x + 1][y + 1];
                    float d = heightField[x][y + 1];

                    int mask = 0;
                    if (a > threshold) mask |= 1;
                    if (b > threshold) mask |= 2;
                    if (c > threshold) mask |= 4;
                    if (d > threshold) mask |= 8;

                    if (mask == 0 || mask == 15) continue;

                    double x0 = left + (x / (double) fieldW) * width;
                    double y0 = top + (y / (double) fieldH) * height;
                    double x1 = left + ((x + 1) / (double) fieldW) * width;
                    double y1 = top + ((y + 1) / (double) fieldH) * height;

                    Point2D topMid = new Point2D.Double((x0 + x1) / 2, y0);
                    Point2D rightMid = new Point2D.Double(x1, (y0 + y1) / 2);
                    Point2D bottomMid = new Point2D.Double((x0 + x1) / 2, y1);
                    Point2D leftMid = new Point2D.Double(x0, (y0 + y1) / 2);

                    drawMarchingSquaresSegment(g2, mask, topMid, rightMid, bottomMid, leftMid);
                }
            }
        }
    }

    private void drawMarchingSquaresSegment(Graphics2D g2, int mask,
                                            Point2D topMid, Point2D rightMid,
                                            Point2D bottomMid, Point2D leftMid) {

        switch (mask) {
            case 1:
            case 14:
                drawLine(g2, leftMid, topMid);
                break;
            case 2:
            case 13:
                drawLine(g2, topMid, rightMid);
                break;
            case 3:
            case 12:
                drawLine(g2, leftMid, rightMid);
                break;
            case 4:
            case 11:
                drawLine(g2, rightMid, bottomMid);
                break;
            case 5:
                drawLine(g2, leftMid, topMid);
                drawLine(g2, rightMid, bottomMid);
                break;
            case 6:
            case 9:
                drawLine(g2, topMid, bottomMid);
                break;
            case 7:
            case 8:
                drawLine(g2, leftMid, bottomMid);
                break;
            case 10:
                drawLine(g2, topMid, rightMid);
                drawLine(g2, leftMid, bottomMid);
                break;
        }
    }

    private void drawLine(Graphics2D g2, Point2D p1, Point2D p2) {
        g2.draw(new Line2D.Double(p1, p2));
    }

    private void drawSweep(Graphics2D g2, int cx, int cy, int radius) {
        double beamWidth = Math.toRadians(38);

        for (int i = 0; i < 28; i++) {
            double frac = i / 28.0;

            // trailing wedge behind the current beam, clockwise motion
            double a1 = sweepAngle - beamWidth * frac;
            double a2 = sweepAngle - beamWidth * (frac + 1.0 / 28.0);

            int alpha = (int) (140 * (1.0 - frac));
            alpha = Math.max(alpha, 0);

            Path2D wedge = new Path2D.Double();
            wedge.moveTo(cx, cy);
            wedge.lineTo(radarX(cx, a1, radius), radarY(cy, a1, radius));
            wedge.lineTo(radarX(cx, a2, radius), radarY(cy, a2, radius));
            wedge.closePath();

            g2.setColor(new Color(80, 255, 120, alpha / 3));
            g2.fill(wedge);
        }

        g2.setStroke(new BasicStroke(2.0f));
        g2.setColor(new Color(180, 255, 180, 180));
        int ex = (int) radarX(cx, sweepAngle, radius);
        int ey = (int) radarY(cy, sweepAngle, radius);
        g2.drawLine(cx, cy, ex, ey);

        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Double(cx, cy),
                24f,
                new float[]{0f, 1f},
                new Color[]{new Color(180, 255, 180, 180), new Color(180, 255, 180, 0)}
        );
        g2.setPaint(glow);
        g2.fill(new Ellipse2D.Double(cx - 24, cy - 24, 48, 48));
    }

    private void drawNoise(Graphics2D g2, int cx, int cy, int radius) {
        for (int i = 0; i < 280; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = radius * Math.sqrt(random.nextDouble());
            int x = (int) radarX(cx, angle, dist);
            int y = (int) radarY(cy, angle, dist);

            int brightness = 120 + random.nextInt(136);
            int alpha = random.nextInt(70);
            int size = random.nextInt(2) + 1;

            g2.setColor(new Color(80, brightness, 80, alpha));
            g2.fillRect(x, y, size, size);
        }

        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = radius * (0.15 + 0.8 * random.nextDouble());
            int x = (int) radarX(cx, angle, dist);
            int y = (int) radarY(cy, angle, dist);

            RadialGradientPaint ping = new RadialGradientPaint(
                    new Point2D.Double(x, y),
                    10f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(180, 255, 180, 160), new Color(180, 255, 180, 0)}
            );
            g2.setPaint(ping);
            g2.fill(new Ellipse2D.Double(x - 10, y - 10, 20, 20));
        }
    }

    private void drawOuterRing(Graphics2D g2, int cx, int cy, int radius) {
        g2.setStroke(new BasicStroke(3.0f));
        g2.setColor(new Color(120, 255, 150, 120));
        g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(new Color(200, 255, 210, 80));
        g2.drawOval(cx - radius - 6, cy - radius - 6, radius * 2 + 12, radius * 2 + 12);
    }

    private void drawCenterMarker(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(180, 255, 180, 160));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx - 6, cy - 6, 12, 12);
        g2.drawLine(cx - 12, cy, cx + 12, cy);
        g2.drawLine(cx, cy - 12, cx, cy + 12);
    }

    private void drawHudText(Graphics2D g2, int w, int h, int cx, int cy, int radius) {
        g2.setColor(new Color(140, 255, 160, 170));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));

        int sweepDegrees = ((int) Math.toDegrees(sweepAngle)) % 360;
        if (sweepDegrees < 0) sweepDegrees += 360;

        g2.drawString("TOPOGRAPHIC RADAR DISPLAY", 30, 35);
        g2.drawString("MODE: TERRAIN CONTOUR", 30, 58);
        g2.drawString(String.format("SWEEP: %03d°", sweepDegrees), 30, 81);

        g2.drawString("RANGE 120 NM", w - 180, 35);
        g2.drawString("CRT PHOSPHOR EMULATION", w - 250, h - 28);

        // Bearing labels: 000 at top, increasing clockwise
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        for (int deg = 0; deg < 360; deg += 30) {
            double a = Math.toRadians(deg);
            int tx = (int) radarX(cx, a, radius + 18);
            int ty = (int) radarY(cy, a, radius + 18);
            String label = String.format("%02d", deg);
            g2.drawString(label, tx - 8, ty + 4);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Clockwise sweep
        sweepAngle += 0.03;
        if (sweepAngle >= Math.PI * 2) {
            sweepAngle -= Math.PI * 2;
        }
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            JFrame frame = new JFrame("Green Screen Topographical Radar");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new GreenScreenTopoRadar());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}