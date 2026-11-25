package org.fog.cache;

import java.awt.*;
import java.util.List;
import javax.swing.*;

public class ResultChart extends JPanel {
    private final List<CacheMetrics> data;
    private final Color[] cols = {new Color(0x4FC3F7), new Color(0xFFB74D), new Color(0x81C784)};

    ResultChart(List<CacheMetrics> d) { data = d; setPreferredSize(new Dimension(860, 480)); }

    public static void show(List<CacheMetrics> d) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Edge Caching with ML-Based Content Prediction (iFogSim2)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new ResultChart(d));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int W = getWidth(), H = getHeight(), L = 70, R = 30, T = 70, B = 60, pw = W - L - R, ph = H - T - B;
        g.setPaint(new GradientPaint(0, 0, new Color(0x0F2027), W, H, new Color(0x2C5364)));
        g.fillRect(0, 0, W, H);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Cache hit ratio over simulated time (iFogSim2)", L, 35);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= 5; i++) {
            int y = T + ph - ph * i / 5;
            g.setColor(new Color(255, 255, 255, 40));
            g.drawLine(L, y, L + pw, y);
            g.setColor(Color.WHITE);
            g.drawString(i * 20 + "%", L - 40, y + 4);
        }
        for (int k = 0; k < data.size(); k++) {
            List<Double> s = data.get(k).series;
            if (s.size() < 2) continue;
            g.setColor(cols[k % cols.length]);
            g.setStroke(new BasicStroke(2.5f));
            for (int i = 1; i < s.size(); i++) {
                int x1 = L + pw * (i - 1) / (s.size() - 1), x2 = L + pw * i / (s.size() - 1);
                g.drawLine(x1, T + ph - (int) (ph * s.get(i - 1)), x2, T + ph - (int) (ph * s.get(i)));
            }
            g.fillRect(L + k * 150, H - 30, 14, 14);
            g.setColor(Color.WHITE);
            g.drawString(data.get(k).node, L + k * 150 + 20, H - 18);
        }
    }
}
