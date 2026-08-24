package forge.screens.home.settings;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import forge.GuiDesktop;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FLabel;
import forge.toolbox.FTextField;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Temporary UI probe: renders the real Preferences panel under Xvfb, prints geometry and colors of
 * the Ultima LLM section, saves screenshots, and verifies startup sync + save-on-focus. Delete after use.
 */
public class UltimaLlmUiProbe {

    private static boolean initialized = false;
    private static boolean headless = false;

    @BeforeClass
    public void initializeModel() {
        if (initialized) {
            return;
        }
        // This probe renders real Swing components, so it needs an X display. When running headless
        // (e.g. CI without Xvfb), skip initialization and let the test method no-op cleanly instead of
        // throwing HeadlessException out of SplashFrame -> FSkin.loadLight.
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            headless = true;
            initialized = true;
            return;
        }
        // Mirror production startup (Main.java): set the GUI interface, then let Singletons wire up
        // the view + model. initializeOnce(true) sets Singletons.view = FView.SINGLETON_INSTANCE, whose
        // enum constructor builds SplashFrame -> FSkin.loadLight(...), and then runs FModel.initialize.
        GuiBase.setInterface(new GuiDesktop());
        forge.Singletons.initializeOnce(true);
        initialized = true;
    }

    private static VSubmenuPreferences buildView() throws Exception {
        ForgePreferences prefs = FModel.getPreferences();

        // Production calls FSkin.loadFull(true) inside FControl.initialize() to register all sprite
        // icons (cursors, buttons, etc.). Do the same here; loadLight alone leaves cursor images null
        // and DragTab construction NPEs. Requires Singletons.getView() non-null (set in initializeOnce).
        forge.toolbox.FSkin.loadFull(true);

        prefs.setPref(FPref.ULTIMA_LLM_ENDPOINT, "http://localhost:11434/v1");
        prefs.setPref(FPref.ULTIMA_LLM_API_KEY, "sk-ult-test-key-1234567890");
        prefs.setPref(FPref.ULTIMA_LLM_MODEL, "qwen3:8b");
        prefs.setPref(FPref.ULTIMA_LLM_TEMPERATURE, "0.1");

        VSubmenuPreferences view = VSubmenuPreferences.SINGLETON_INSTANCE;
        CSubmenuPreferences.SINGLETON_INSTANCE.initialize();
        return view;
    }

    private static void dumpComponent(String name, java.awt.Component c) {
        if (c == null) {
            System.out.println("  " + name + ": NULL");
            return;
        }
        Rectangle r = c.getBounds();
        Color fg = (c instanceof FLabel) ? ((FLabel) c).getForeground() : null;
        Color bg = c.getBackground();
        boolean visible = c.isVisible();
        System.out.printf("  %-28s bounds=%-30s visible=%-5s fg=%s bg=%s%n",
                name, r, visible, fg, bg);
    }

    /** Render a component standalone into its own PNG and report how much bright (text) pixel area it has. */
    private static void renderStandalone(String name, java.awt.Component c, int scale) throws Exception {
        if (c == null) {
            System.out.println("  " + name + ": NULL");
            return;
        }
        final int w = Math.max(c.getWidth(), c.getPreferredSize().width);
        final int h = Math.max(c.getHeight(), c.getPreferredSize().height);
        final BufferedImage img = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = img.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.scale(scale, scale);
                // Paint the component's own background first so we can tell text from bg.
                Color bg = c.getBackground();
                if (bg != null) {
                    g.setColor(bg);
                    g.fillRect(0, 0, w, h);
                }
                c.paint(g);
            } finally {
                g.dispose();
            }
        });

        // Count bright pixels (likely text) vs total.
        int bright = 0;
        int mid = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int lum = (((rgb >> 16) & 255) + ((rgb >> 8) & 255) + (rgb & 255)) / 3;
                if (lum > 170) {
                    bright++;
                } else if (lum > 90) {
                    mid++;
                }
            }
        }
        int total = img.getWidth() * img.getHeight();
        String path = "/tmp/llm_" + name + ".png";
        javax.imageio.ImageIO.write(img, "png", new java.io.File(path));
        System.out.printf("  %-28s %dx%d bright=%.1f%% mid=%.1f%% -> %s%n",
                name, w, h, 100.0 * bright / total, 100.0 * mid / total, path);
    }

    /** The LLM field is wrapped in a MigLayout panel that also holds its label. Return the label sibling. */
    private static FLabel findSiblingLabel(FTextField field) {
        JComponent parent = (JComponent) field.getParent();
        if (parent == null) {
            return null;
        }
        for (java.awt.Component c : parent.getComponents()) {
            if (c instanceof FLabel) {
                return (FLabel) c;
            }
        }
        return null;
    }

    @Test(groups = { "UnitTest" }, enabled = true)
    public void probeLlmSection() throws Exception {
        // Rendering probe: needs an X display (Xvfb in CI). Skip cleanly when headless.
        if (headless) {
            System.out.println("UltimaLlmUiProbe: running headless, skipping render assertions");
            return;
        }
        final VSubmenuPreferences view = buildView();

        final JFrame frame = new JFrame("Ultima LLM UI Probe");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JComponent scroll = findScrContent(view);
        final JPanel pnl = (JPanel) findPnlPrefs(view);
        frame.add(scroll);
        frame.setSize(1000, 720);
        frame.setLocation(50, 30);

        SwingUtilities.invokeAndWait(() -> {
            frame.setVisible(true);
            pnl.revalidate();
            scroll.revalidate();
            scroll.repaint();
        });
        Thread.sleep(1200);

        System.out.println("=== LLM section component dump ===");
        FLabel lblEndpoint = findSiblingLabel(view.getTxtLLMEndpoint());
        FLabel lblApiKey = findSiblingLabel(view.getTxtLLMApiKey());
        FLabel lblModel = findSiblingLabel(view.getTxtLLMModel());
        FLabel lblTemp = findSiblingLabel(view.getTxtLLMTemperature());

        dumpComponent("lblEndpoint", lblEndpoint);
        dumpComponent("lblApiKey", lblApiKey);
        dumpComponent("lblModel", lblModel);
        dumpComponent("lblTemp", lblTemp);
        dumpComponent("txtLLMEndpoint", view.getTxtLLMEndpoint());
        dumpComponent("txtLLMApiKey", view.getTxtLLMApiKey());
        dumpComponent("txtLLMModel", view.getTxtLLMModel());
        dumpComponent("txtLLMTemperature", view.getTxtLLMTemperature());

        // Preferred sizes: is the 1.3M width real or clamped by the viewport?
        System.out.println("=== preferred sizes ===");
        for (java.awt.Component c : new java.awt.Component[]{view.getTxtLLMEndpoint(), lblEndpoint}) {
            if (c == null) continue;
            JComponent wrap = (JComponent) c.getParent();
            System.out.printf("  %-16s field.pref=%-20s wrapper.bounds=%-30s wrapper.pref=%s%n",
                    c.getName() != null ? c.getName() : "field",
                    c.getPreferredSize(), wrap.getBounds(), wrap.getPreferredSize());
        }

        // Outer container widths: is pnlPrefs itself ballooning, or just the row?
        System.out.println("=== outer containers ===");
        System.out.printf("  pnlPrefs.bounds=%-30s pnlPrefs.pref=%s%n", pnl.getBounds(), pnl.getPreferredSize());
        javax.swing.JViewport vp = viewportOf(scroll);
        System.out.printf("  scroll.viewport=%-28s scroll.viewpref=%s%n", vp.getSize(), vp.getView().getPreferredSize());

        saveScreen(frame, "/tmp/llm_ui_full.png");

        // Scroll to the Ultima AI section (temperature field) and screenshot.
        SwingUtilities.invokeAndWait(() -> {
            Rectangle target = lblTemp == null ? new Rectangle(0, 4000, 10, 10)
                    : SwingUtilities.convertRectangle(lblTemp, lblTemp.getBounds(), pnl);
            viewportOf(scroll).scrollRectToVisible(target);
        });
        Thread.sleep(800);
        saveScreen(frame, "/tmp/llm_ui_section.png");

        // Standalone per-label render + white-pixel ratio (proves text is painted).
        System.out.println("=== standalone label renders ===");
        renderStandalone("lblEndpoint", lblEndpoint, 3);
        renderStandalone("lblApiKey", lblApiKey, 3);
        renderStandalone("btnTestLLMConnection", view.getBtnTestLLMConnection(), 3);

        // Startup sync: fields reflect pref values after initialize().
        assertEquals(view.getTxtLLMEndpoint().getText(), "http://localhost:11434/v1", "endpoint startup sync");
        assertEquals(view.getTxtLLMApiKey().getText(), "sk-ult-test-key-1234567890", "api key startup sync");
        assertEquals(view.getTxtLLMModel().getText(), "qwen3:8b", "model startup sync");
        assertEquals(view.getTxtLLMTemperature().getText(), "0.1", "temperature startup sync");

        // Live update: type into the field, drop focus, verify pref + system property bridge.
        FTextField key = view.getTxtLLMApiKey();
        SwingUtilities.invokeAndWait(() -> {
            key.setText("sk-live-update-9876543210");
            java.awt.event.FocusEvent fe = new java.awt.event.FocusEvent(key,
                    java.awt.event.FocusEvent.FOCUS_LOST);
            for (java.awt.event.FocusListener fl : key.getFocusListeners()) {
                fl.focusLost(fe);
            }
        });
        Thread.sleep(300);
        String prefAfter = FModel.getPreferences().getPref(FPref.ULTIMA_LLM_API_KEY);
        System.out.println("pref after focus-lost: " + prefAfter);
        assertEquals(prefAfter, "sk-live-update-9876543210", "live update saves on focus loss");

        FModel.syncUltimaLlmProperties();
        String sysProp = System.getProperty("forge.ai.ultima_llm_api_key");
        System.out.println("system property after sync: " + sysProp);
        assertEquals(sysProp, "sk-live-update-9876543210", "bridge pushes live value to AI side");

        assertNotNull(lblEndpoint, "endpoint label exists");
        assertNotNull(lblApiKey, "api key label exists");
        assertNotNull(lblModel, "model label exists");
        assertNotNull(lblTemp, "temperature label exists");
        assertTrue(lblApiKey.getWidth() > 20 && lblApiKey.getHeight() > 10, "label has non-trivial size");

        frame.dispose();
    }

    private static void saveScreen(JFrame frame, String path) {
        try {
            java.awt.Dimension d = frame.getSize();
            BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            frame.paint(g);
            g.dispose();
            javax.imageio.ImageIO.write(img, "png", new File(path));
            System.out.println("saved screenshot: " + path);
        } catch (Exception e) {
            System.out.println("screenshot failed: " + e);
            e.printStackTrace();
        }
    }

    private static java.awt.Component findPnlPrefs(VSubmenuPreferences view) throws Exception {
        java.lang.reflect.Field f = VSubmenuPreferences.class.getDeclaredField("pnlPrefs");
        f.setAccessible(true);
        return (java.awt.Component) f.get(view);
    }

    private static JComponent findScrContent(VSubmenuPreferences view) throws Exception {
        java.lang.reflect.Field f = VSubmenuPreferences.class.getDeclaredField("scrContent");
        f.setAccessible(true);
        return (JComponent) f.get(view);
    }

    private static javax.swing.JViewport viewportOf(JComponent scroll) {
        if (scroll instanceof javax.swing.JScrollPane) {
            return ((javax.swing.JScrollPane) scroll).getViewport();
        }
        // FScrollPane may wrap the viewport; search children.
        for (java.awt.Component c : scroll.getComponents()) {
            if (c instanceof javax.swing.JViewport) {
                return (javax.swing.JViewport) c;
            }
        }
        throw new IllegalStateException("no viewport found in " + scroll.getClass());
    }
}
