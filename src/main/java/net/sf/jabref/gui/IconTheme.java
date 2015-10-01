package net.sf.jabref.gui;

import net.sf.jabref.logic.l10n.Localization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.swingx.HorizontalLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class IconTheme {

    public static Font FONT;
    public static Font FONT_16;

    static {
        try {
            FONT = Font.createFont(Font.TRUETYPE_FONT, FontBasedIcon.class.getResourceAsStream("/fonts/fontawesome-webfont.ttf"));
            FONT_16 = FONT.deriveFont(Font.PLAIN, 16f);
        } catch (FontFormatException | IOException e) {
            // PROBLEM!
            e.printStackTrace();
        }
    }

    public enum JabRefIcon {

        ADD("\uf067", Color.GREEN),
        CLIPBOARD("\uf0ea"),
        FOLDER("\uf07b"),
        REMOVE("\uf068", Color.RED),
        FILE("\uf0f6"),
        PDF_FILE("\uf1c1"),
        SEARCH("\uf002"),
        TAGS("\uf02c");

        private final String code;
        private final Color color;

        JabRefIcon(String code) {
            this(code, Color.BLACK);
        }

        JabRefIcon(String code, Color color) {
            this.code = code;
            this.color = color;
        }

        public FontBasedIcon getIcon() {
            return new FontBasedIcon(this.code, this.color);
        }

        public ImageIcon getImageIcon() {
            return new ImageIcon() {

                private FontBasedIcon icon = getIcon();

                @Override
                public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
                    icon.paintIcon(c, g, x, y);
                }

                @Override
                public int getIconWidth() {
                    return icon.getIconWidth();
                }

                @Override
                public int getIconHeight() {
                    return icon.getIconHeight();
                }

                @Override
                public Image getImage() {
                    int w = icon.getIconWidth();
                    int h = icon.getIconHeight();
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice gd = ge.getDefaultScreenDevice();
                    GraphicsConfiguration gc = gd.getDefaultConfiguration();
                    BufferedImage image = gc.createCompatibleImage(w, h);
                    Graphics2D g = image.createGraphics();
                    icon.paintIcon(null, g, 0, 0);
                    g.dispose();
                    return image;
                }
            };
        }

        public JLabel getLabel() {
            JLabel label = new JLabel(this.code);
            label.setForeground(this.color);
            label.setFont(FONT_16);
            return label;
        }
    }

    public static class FontBasedIcon implements Icon {

        private final String iconCode;
        private final Color iconColor;

        public FontBasedIcon(String code, Color iconColor) {
            this.iconCode = code;
            this.iconColor = iconColor;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();

            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHints(rh);

            g2.setFont(FONT_16);
            g2.setColor(iconColor);
            FontMetrics fm = g2.getFontMetrics();

            g2.translate(x, y + fm.getAscent());
            g2.drawString(iconCode, 0, 0);

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

    private static final Log LOGGER = LogFactory.getLog(IconTheme.class);

    private static final Map<String, String> KEY_TO_ICON = readIconThemeFile(IconTheme.class.getResource("/images/crystal_16/Icons.properties"), "/images/crystal_16/");
    private static final String DEFAULT_ICON_PATH = "/images/crystal_16/red.png";

    /**
     * Get a Map of all application icons mapped from their keys.
     *
     * @return A Map containing all icons used in the application.
     */
    public static Map<String, String> getAllIcons() {
        return Collections.unmodifiableMap(KEY_TO_ICON);
    }

    /**
     * Constructs an ImageIcon for the image representing the given function, in the resource
     * file listing images.
     *
     * @param name The name of the icon, such as "open", "save", "saveAs" etc.
     * @return The ImageIcon for the function.
     */
    public static ImageIcon getImage(String name) {
        return JabRefIcon.values()[new Random().nextInt(JabRefIcon.values().length)].getImageIcon();
        //return new ImageIcon(getIconUrl(name));
    }

    /**
     * Looks up the URL for the image representing the given function, in the resource
     * file listing images.
     *
     * @param name The name of the icon, such as "open", "save", "saveAs" etc.
     * @return The URL to the actual image to use.
     */
    private static URL getIconUrl(String name) {
        String key = Objects.requireNonNull(name, "icon name");
        if (!KEY_TO_ICON.containsKey(key)) {
            LOGGER.warn("could not find icon url by name " + name + ", so falling back on default icon " + DEFAULT_ICON_PATH);
        }
        String path = KEY_TO_ICON.getOrDefault(key, DEFAULT_ICON_PATH);
        return Objects.requireNonNull(IconTheme.class.getResource(path), "url");
    }

    /**
     * Read a typical java property url into a Map. Currently doesn't support escaping
     * of the '=' character - it simply looks for the first '=' to determine where the key ends.
     * Both the key and the value is trimmed for whitespace at the ends.
     *
     * @param url    The URL to read information from.
     * @param prefix A String to prefix to all values read. Can represent e.g. the directory
     *               where icon files are to be found.
     * @return A Map containing all key-value pairs found.
     */
    private static Map<String, String> readIconThemeFile(URL url, String prefix) {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(prefix, "prefix");

        Map<String, String> result = new HashMap<>();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.contains("=")) {
                    continue;
                }

                int index = line.indexOf("=");
                String key = line.substring(0, index).trim();
                String value = prefix + line.substring(index + 1).trim();
                result.put(key, value);
            }
        } catch (IOException e) {
            LOGGER.warn(Localization.lang("Unable to read default icon theme."), e);
        }
        return result;
    }
}
