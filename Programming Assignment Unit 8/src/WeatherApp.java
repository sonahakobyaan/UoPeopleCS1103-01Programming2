import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

/**
 * Weather Information App - main class and Swing user interface.
 *
 * <p>Screen layout (BorderLayout on a time-of-day gradient):</p>
 * <pre>
 *  NORTH : search bar (location, Search, unit selectors) + status line
 *  CENTER: current-conditions card (icon, temperature, humidity, wind ...)
 *  EAST  : history of recent searches with timestamps
 *  SOUTH : 24-hour forecast strip (eight 3-hour cards)
 * </pre>
 * Network calls run in a {@link SwingWorker} so the window never freezes.
 */
public final class WeatherApp {

    // ---- formatting constants ------------------------------------------
    private static final DateTimeFormatter CITY_CLOCK =
            DateTimeFormatter.ofPattern("EEEE d MMMM, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter SLOT_CLOCK =
            DateTimeFormatter.ofPattern("EEE HH:mm", Locale.ENGLISH);
    private static final Font BASE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final int HISTORY_CAPACITY = 15;

    // ---- model / services ------------------------------------------------
    private final WeatherService service;
    private final SearchHistory history = new SearchHistory(HISTORY_CAPACITY);
    private WeatherData currentWeather;               // null until a search succeeds
    private List<ForecastEntry> currentForecast = List.of();

    // ---- GUI components --------------------------------------------------
    private final BackgroundPanel root = new BackgroundPanel();
    private final JTextField locationField = new JTextField(18);
    private final JButton searchButton = new JButton("Search");
    private final JComboBox<UnitConverter.TempUnit> tempUnitBox =
            new JComboBox<>(UnitConverter.TempUnit.values());
    private final JComboBox<UnitConverter.WindUnit> windUnitBox =
            new JComboBox<>(UnitConverter.WindUnit.values());
    private final JLabel statusLabel = new JLabel(" ");

    private final WeatherIcons.IconComponent bigIcon = new WeatherIcons.IconComponent(130);
    private final JLabel cityLabel = makeLabel("Search for a city to begin", 26, Font.BOLD);
    private final JLabel clockLabel = makeLabel(" ", 14, Font.PLAIN);
    private final JLabel temperatureLabel = makeLabel("--", 64, Font.BOLD);
    private final JLabel conditionLabel = makeLabel(" ", 18, Font.PLAIN);
    private final JLabel feelsLikeLabel = makeLabel("Feels like: --", 15, Font.PLAIN);
    private final JLabel humidityLabel = makeLabel("Humidity: --", 15, Font.PLAIN);
    private final JLabel windLabel = makeLabel("Wind: --", 15, Font.PLAIN);
    private final JLabel pressureLabel = makeLabel("Pressure: --", 15, Font.PLAIN);

    private final JPanel forecastPanel = new JPanel(new GridLayout(1, 0, 8, 0));
    private final DefaultListModel<SearchHistory.Entry> historyModel = new DefaultListModel<>();
    private final JList<SearchHistory.Entry> historyList = new JList<>(historyModel);

    // ======================================================================
    // Construction
    // ======================================================================

    public WeatherApp(WeatherService service) {
        this.service = service;
        buildInterface();
        wireEvents();
    }

    /** The top-level component to place inside a JFrame. */
    public JComponent getRoot() {
        return root;
    }

    private void buildInterface() {
        root.setLayout(new BorderLayout(14, 14));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.add(buildSearchBar(), BorderLayout.NORTH);
        root.add(buildCurrentCard(), BorderLayout.CENTER);
        root.add(buildHistoryPanel(), BorderLayout.EAST);
        root.add(buildForecastSection(), BorderLayout.SOUTH);
    }

    private JComponent buildSearchBar() {
        JPanel bar = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bar.add(makeLabel("Location:", 14, Font.BOLD));
        locationField.setToolTipText("City name (Paris, London,GB) or coordinates (51.5, -0.12)");
        locationField.setFont(BASE_FONT);
        bar.add(locationField);
        bar.add(searchButton);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(makeLabel("Temperature:", 14, Font.BOLD));
        bar.add(tempUnitBox);
        bar.add(makeLabel("Wind:", 14, Font.BOLD));
        bar.add(windUnitBox);

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 0, 0));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(bar);
        north.add(statusLabel);
        return north;
    }

    private JComponent buildCurrentCard() {
        JPanel card = new RoundedPanel(new BorderLayout(20, 0));
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel iconHolder = new JPanel(new BorderLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(bigIcon, BorderLayout.NORTH);
        card.add(iconHolder, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(cityLabel);
        info.add(clockLabel);
        info.add(temperatureLabel);
        info.add(conditionLabel);
        info.add(Box.createVerticalStrut(14));

        JPanel details = new JPanel(new GridLayout(2, 2, 24, 8));
        details.setOpaque(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(feelsLikeLabel);
        details.add(humidityLabel);
        details.add(windLabel);
        details.add(pressureLabel);
        info.add(details);

        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildHistoryPanel() {
        JPanel panel = new RoundedPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setPreferredSize(new Dimension(290, 0));
        panel.add(makeLabel("Recent searches", 16, Font.BOLD), BorderLayout.NORTH);

        historyList.setOpaque(false);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        historyList.setCellRenderer(new HistoryRenderer());
        historyList.setToolTipText("Click an entry to search it again");

        JScrollPane scroll = new JScrollPane(historyList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildForecastSection() {
        JPanel section = new RoundedPanel(new BorderLayout(0, 6));
        section.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        section.add(makeLabel("Next 24 hours (every 3 hours)", 16, Font.BOLD), BorderLayout.NORTH);
        forecastPanel.setOpaque(false);
        forecastPanel.setPreferredSize(new Dimension(0, 130));
        section.add(forecastPanel, BorderLayout.CENTER);
        return section;
    }

    private void wireEvents() {
        searchButton.addActionListener(e -> search(locationField.getText()));
        locationField.addActionListener(e -> search(locationField.getText()));   // Enter key

        // Changing a unit only re-renders stored data: no new API call is needed.
        tempUnitBox.addActionListener(e -> refreshDisplay());
        windUnitBox.addActionListener(e -> refreshDisplay());

        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = historyList.locationToIndex(e.getPoint());
                if (index >= 0 && historyList.getCellBounds(index, index).contains(e.getPoint())) {
                    SearchHistory.Entry entry = historyModel.get(index);
                    locationField.setText(entry.query());
                    search(entry.query());
                }
            }
        });
    }

    // ======================================================================
    // Searching (background thread) and result handling
    // ======================================================================

    /** Starts a lookup without blocking the Swing event-dispatch thread. */
    private void search(String input) {
        if (!service.hasKey() && !askForApiKey()) {
            showError("An API key is required. See the README for how to get a free one.");
            return;
        }
        final String query = input == null ? "" : input.trim();
        setBusy(true);

        new SwingWorker<WeatherService.Result, Void>() {
            @Override
            protected WeatherService.Result doInBackground() throws WeatherException {
                return service.fetch(query);                    // runs off the EDT
            }

            @Override
            protected void done() {                             // back on the EDT
                setBusy(false);
                try {
                    showResult(get(), query);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    showError(cause instanceof WeatherException
                            ? cause.getMessage()
                            : "Unexpected error: " + cause);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("The search was interrupted.");
                }
            }
        }.execute();
    }

    /** Displays a successful result and records it in the history. */
    void showResult(WeatherService.Result result, String query) {
        currentWeather = result.current();
        currentForecast = result.forecast();
        history.add(query, currentWeather.displayName(), LocalDateTime.now());
        refreshHistory();
        root.setPhase(TimeOfDay.of(currentWeather));     // dynamic background
        statusLabel.setText(" ");
        refreshDisplay();
    }

    /** Shows an error message in the status line (never a stack trace). */
    void showError(String message) {
        statusLabel.setForeground(new Color(255, 214, 120));
        statusLabel.setText("⚠ " + message);
    }

    private void setBusy(boolean busy) {
        searchButton.setEnabled(!busy);
        locationField.setEnabled(!busy);
        root.setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        if (busy) {
            statusLabel.setForeground(Color.WHITE);
            statusLabel.setText("Loading weather...");
        }
    }

    /** Asks for the API key once and saves it to config.properties. */
    private boolean askForApiKey() {
        String key = JOptionPane.showInputDialog(root,
                "Enter your free OpenWeatherMap API key:", "API key needed",
                JOptionPane.QUESTION_MESSAGE);
        if (key == null || key.isBlank()) {
            return false;
        }
        service.setApiKey(key.trim());
        try {
            WeatherService.saveApiKey(key);        // remembered for the next start
        } catch (java.io.IOException e) {
            showError("Key accepted for this session but could not be saved: " + e.getMessage());
        }
        return true;
    }

    // ======================================================================
    // Rendering from the stored (metric) data in the chosen units
    // ======================================================================

    /** Public for the screenshot tool; selects units programmatically. */
    void setUnits(UnitConverter.TempUnit temp, UnitConverter.WindUnit wind) {
        tempUnitBox.setSelectedItem(temp);
        windUnitBox.setSelectedItem(wind);
    }

    void setLocationText(String text) {
        locationField.setText(text);
    }

    SearchHistory getHistory() {
        return history;
    }

    void refreshHistory() {
        historyModel.clear();
        for (SearchHistory.Entry entry : history.entries()) {
            historyModel.addElement(entry);
        }
    }

    private void refreshDisplay() {
        if (currentWeather == null) {
            return;
        }
        UnitConverter.TempUnit tu = (UnitConverter.TempUnit) tempUnitBox.getSelectedItem();
        UnitConverter.WindUnit wu = (UnitConverter.WindUnit) windUnitBox.getSelectedItem();
        WeatherData w = currentWeather;

        bigIcon.setCode(w.iconCode());
        cityLabel.setText(w.displayName());
        clockLabel.setText("Local time: " + w.localTime().format(CITY_CLOCK));
        temperatureLabel.setText(UnitConverter.formatTemperature(w.temperatureC(), tu));
        conditionLabel.setText(capitalise(w.description()));
        feelsLikeLabel.setText("Feels like: " + UnitConverter.formatTemperature(w.feelsLikeC(), tu));
        humidityLabel.setText("Humidity: " + w.humidityPercent() + "%");
        windLabel.setText("Wind: " + UnitConverter.formatWind(w.windSpeedMs(), wu));
        pressureLabel.setText("Pressure: " + w.pressureHpa() + " hPa");

        forecastPanel.removeAll();
        for (ForecastEntry slot : currentForecast) {
            forecastPanel.add(buildForecastCard(slot, tu));
        }
        forecastPanel.revalidate();
        forecastPanel.repaint();
    }

    private JComponent buildForecastCard(ForecastEntry slot, UnitConverter.TempUnit unit) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60)));

        JLabel time = makeLabel(slot.localTime().format(SLOT_CLOCK), 12, Font.BOLD);
        time.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel temp = makeLabel(UnitConverter.formatTemperature(slot.temperatureC(), unit), 16, Font.BOLD);
        temp.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel rain = makeLabel(Math.round(slot.rainChance() * 100) + "% rain", 11, Font.PLAIN);
        rain.setHorizontalAlignment(SwingConstants.CENTER);

        WeatherIcons.IconComponent icon = new WeatherIcons.IconComponent(44);
        icon.setCode(slot.iconCode());
        icon.setToolTipText(capitalise(slot.description()));

        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.setOpaque(false);
        bottom.add(temp);
        bottom.add(rain);

        card.add(time, BorderLayout.NORTH);
        card.add(icon, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    // ======================================================================
    // Small helpers
    // ======================================================================

    private static JLabel makeLabel(String text, int size, int style) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", style, size));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static String capitalise(String text) {
        return text == null || text.isEmpty()
                ? "" : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Semi-transparent dark rounded panel so white text is readable on any background. */
    @SuppressWarnings("serial")
    static class RoundedPanel extends JPanel {
        RoundedPanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 85));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Draws each history row as white text on the transparent list. */
    @SuppressWarnings("serial")
    private static class HistoryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focus);
            label.setText(((SearchHistory.Entry) value).display());
            label.setFont(new Font("Monospaced", Font.PLAIN, 12));
            label.setForeground(Color.WHITE);
            label.setOpaque(selected);
            label.setBackground(new Color(255, 255, 255, 60));
            label.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
            return label;
        }
    }

    // ======================================================================
    // Program entry point
    // ======================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default look and feel is fine if this fails.
            }
            WeatherApp app = new WeatherApp(new WeatherService(WeatherService.loadApiKey()));
            JFrame frame = new JFrame("Weather Information App");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(app.getRoot());
            frame.setMinimumSize(new Dimension(1000, 640));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
