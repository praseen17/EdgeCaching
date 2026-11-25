package org.fog.cache;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/** Desktop control panel for configuring and running the edge cache simulation. */
public final class EdgeCacheInterface extends JFrame {
    private final JTextField users = field(EdgeCacheMain.USERS_PER_EDGE);
    private final JTextField interval = field(EdgeCacheMain.REQUEST_INTERVAL_MS);
    private final JTextField duration = field(EdgeCacheMain.SIM_TIME_MS / 1000);
    private final JTextField catalog = field(EdgeCacheMain.CATALOG);
    private final JTextField cacheSize = field(EdgeCacheMain.CACHE_SIZE);
    private final JTextField zipf = field(EdgeCacheMain.ZIPF_ALPHA);
    private final JTextField shift = field(EdgeCacheMain.SHIFT_EVERY_MS / 1000);
    private final JTextField contentSize = field(EdgeCacheMain.CONTENT_SIZE);
    private final JTextField originLatency = field(EdgeCacheMain.ORIGIN_MS);
    private final JLabel status = new JLabel("Ready. Configure the run and click Run simulation.");
    private final JButton runButton = new JButton("Run simulation");
    private final DefaultTableModel resultsModel = new DefaultTableModel(
            new Object[] {"Edge policy", "Requests", "Hits", "Hit rate", "Avg latency (ms)", "Backhaul (MB)"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JPanel chartHost = new JPanel(new BorderLayout());

    private EdgeCacheInterface() {
        super("Edge Caching Simulator | iFogSim2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 700));
        setSize(1120, 790);
        setLocationRelativeTo(null);

        JLabel heading = new JLabel("Edge caching with content prediction");
        heading.setFont(new Font("SansSerif", Font.BOLD, 22));
        heading.setForeground(new Color(0x17324D));
        JLabel subtitle = new JLabel("Compare LRU, LFU and ML-Predict on the same iFogSim2 workload.");
        JPanel title = new JPanel(new BorderLayout(4, 4));
        title.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));
        title.add(heading, BorderLayout.NORTH);
        title.add(subtitle, BorderLayout.SOUTH);

        JPanel settings = buildSettings();
        JTable table = new JTable(resultsModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        chartHost.setBackground(new Color(0x0F2027));
        chartHost.setPreferredSize(new Dimension(860, 300));
        chartHost.add(emptyChartMessage(), BorderLayout.CENTER);
        JSplitPane output = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), new JScrollPane(chartHost));
        output.setResizeWeight(0.38);
        output.setBorder(BorderFactory.createEmptyBorder());

        runButton.setFocusPainted(false);
        runButton.addActionListener(event -> runSimulation());
        status.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));
        actions.add(status, BorderLayout.CENTER);
        actions.add(runButton, BorderLayout.EAST);

        add(title, BorderLayout.NORTH);
        add(settings, BorderLayout.WEST);
        add(output, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel buildSettings() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Simulation settings"),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        panel.setPreferredSize(new Dimension(265, 0));
        String[] labels = {"Users per edge", "Request interval (ms)", "Duration (seconds)",
                "Content catalog size", "Cache capacity (items)", "Zipf popularity (α)",
                "Popularity shift (seconds)", "Content size (bytes)", "Origin latency (ms)"};
        JTextField[] fields = {users, interval, duration, catalog, cacheSize, zipf, shift, contentSize, originLatency};
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 2, 11, 2);
        JLabel note = new JLabel("All three policies use the same request workload.");
        note.setFont(note.getFont().deriveFont(11f));
        panel.add(note, c);
        c.gridwidth = 1;
        for (int i = 0; i < labels.length; i++) {
            c.gridy++;
            c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 2, 5, 8);
            panel.add(new JLabel(labels[i]), c);
            c.gridx = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(5, 0, 5, 2);
            panel.add(fields[i], c);
        }
        c.gridy++; c.gridx = 0; c.gridwidth = 2; c.weighty = 1; c.fill = GridBagConstraints.VERTICAL;
        panel.add(new JLabel(), c);
        return panel;
    }

    private void runSimulation() {
        final int userCount;
        final int durationSeconds;
        final int catalogSize;
        final int capacity;
        final long bytes;
        final double requestInterval;
        final double alpha;
        final double shiftSeconds;
        final double originMs;
        try {
            userCount = positiveInt(users, "Users per edge");
            requestInterval = positiveDouble(interval, "Request interval");
            durationSeconds = positiveInt(duration, "Duration");
            catalogSize = positiveInt(catalog, "Catalog size");
            capacity = positiveInt(cacheSize, "Cache capacity");
            alpha = Double.parseDouble(zipf.getText().trim());
            shiftSeconds = positiveDouble(shift, "Popularity shift");
            bytes = positiveLong(contentSize, "Content size");
            originMs = Double.parseDouble(originLatency.getText().trim());
            if (!Double.isFinite(alpha) || alpha < 0 || alpha > 2) {
                throw new IllegalArgumentException("Zipf popularity α must be between 0 and 2.");
            }
            if (!Double.isFinite(originMs) || originMs < 0) {
                throw new IllegalArgumentException("Origin latency must be zero or greater.");
            }
            if (capacity > catalogSize) {
                throw new IllegalArgumentException("Cache capacity cannot exceed the catalog size.");
            }
            Math.multiplyExact(durationSeconds, 1000);
            Math.multiplyExact((long) userCount, 1L);
        } catch (RuntimeException ex) {
            String message = ex instanceof IllegalArgumentException ? ex.getMessage() : "Enter valid values within the supported numeric range.";
            JOptionPane.showMessageDialog(this, message, "Check simulation settings", JOptionPane.WARNING_MESSAGE);
            return;
        }

        EdgeCacheMain.USERS_PER_EDGE = userCount;
        EdgeCacheMain.REQUEST_INTERVAL_MS = requestInterval;
        EdgeCacheMain.SIM_TIME_MS = durationSeconds * 1000;
        EdgeCacheMain.CATALOG = catalogSize;
        EdgeCacheMain.CACHE_SIZE = capacity;
        EdgeCacheMain.ZIPF_ALPHA = alpha;
        EdgeCacheMain.SHIFT_EVERY_MS = shiftSeconds * 1000;
        EdgeCacheMain.CONTENT_SIZE = bytes;
        EdgeCacheMain.ORIGIN_MS = originMs;
        resultsModel.setRowCount(0);
        chartHost.removeAll();
        chartHost.add(emptyChartMessage(), BorderLayout.CENTER);
        chartHost.revalidate();
        chartHost.repaint();
        setInputsEnabled(false);
        status.setText("Simulation running… this can take a little while.");

        new SwingWorker<List<CacheMetrics>, Void>() {
            @Override protected List<CacheMetrics> doInBackground() throws Exception {
                return EdgeCacheMain.run();
            }

            @Override protected void done() {
                setInputsEnabled(true);
                try {
                    List<CacheMetrics> metrics = get();
                    for (CacheMetrics m : metrics) {
                        resultsModel.addRow(new Object[] {m.node, m.requests, m.hits,
                                String.format("%.2f%%", m.hitRatio() * 100),
                                String.format("%.2f", m.avgLatency()),
                                String.format("%.1f", m.backhaulBytes / 1024.0)});
                    }
                    chartHost.removeAll();
                    chartHost.add(new ResultChart(metrics), BorderLayout.CENTER);
                    chartHost.revalidate();
                    chartHost.repaint();
                    status.setText("Finished. Results were saved to results.csv in the project working directory.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    status.setText("Simulation failed.");
                    JOptionPane.showMessageDialog(EdgeCacheInterface.this,
                            cause.getMessage() == null ? cause.toString() : cause.getMessage(),
                            "Simulation failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setInputsEnabled(boolean enabled) {
        users.setEnabled(enabled); interval.setEnabled(enabled); duration.setEnabled(enabled);
        catalog.setEnabled(enabled); cacheSize.setEnabled(enabled); zipf.setEnabled(enabled);
        shift.setEnabled(enabled); contentSize.setEnabled(enabled); originLatency.setEnabled(enabled);
        runButton.setEnabled(enabled);
    }

    private static JLabel emptyChartMessage() {
        JLabel label = new JLabel("Run a simulation to view cache hit ratio over time", JLabel.CENTER);
        label.setForeground(Color.WHITE);
        return label;
    }

    private static JTextField field(Object value) { return new JTextField(String.valueOf(value), 9); }
    private static int positiveInt(JTextField field, String name) {
        int value = Integer.parseInt(field.getText().trim());
        if (value <= 0) throw new IllegalArgumentException(name + " must be greater than zero.");
        return value;
    }
    private static long positiveLong(JTextField field, String name) {
        long value = Long.parseLong(field.getText().trim());
        if (value <= 0) throw new IllegalArgumentException(name + " must be greater than zero.");
        return value;
    }
    private static double positiveDouble(JTextField field, String name) {
        double value = Double.parseDouble(field.getText().trim());
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException(name + " must be greater than zero.");
        return value;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { /* use Swing's default look and feel */ }
            new EdgeCacheInterface().setVisible(true);
        });
    }
}
