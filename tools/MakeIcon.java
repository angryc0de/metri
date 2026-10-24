import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * 生成 Metri 的 launcher icon：
 * 黑色底 + 白色 "Metri"（粗体斜体），输出自适应图标的矢量前景与各密度位图。
 *
 * 用法：java MakeIcon <res 目录绝对路径>
 */
public class MakeIcon {

    static final String TEXT = "Metri";
    static final int VIEW = 108;          // 自适应图标 viewport
    /*
     * 自适应图标的前景会被系统按 72dp 视窗裁切并放大，
     * 所以 108 坐标系里的文字宽度要按 72 安全区打折，留出放大余量。
     * 44 是实测值：再大右边就会被图标边缘切掉。
     */
    static final double TEXT_W = 44.0;

    public static void main(String[] args) throws Exception {
        String res = args[0];
        Font font = new Font("Arial", Font.BOLD | Font.ITALIC, 100);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector gv = font.createGlyphVector(frc, TEXT);
        Shape shape = gv.getOutline();

        Rectangle2D b = shape.getBounds2D();
        double scale = TEXT_W / b.getWidth();
        double w = b.getWidth() * scale;
        double h = b.getHeight() * scale;

        AffineTransform t = new AffineTransform();
        t.translate((VIEW - w) / 2.0, (VIEW - h) / 2.0);
        t.scale(scale, scale);
        t.translate(-b.getX(), -b.getY());
        Shape centered = t.createTransformedShape(shape);

        String pathData = toPathData(centered);
        writeForeground(res, pathData);
        writeBackground(res);

        int[][] sizes = {{48, 0}, {72, 1}, {96, 2}, {144, 3}, {192, 4}};
        String[] dirs = {"mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"};
        for (int i = 0; i < sizes.length; i++) {
            int px = sizes[i][0];
            writeBitmap(new File(res, dirs[i] + "/ic_launcher.png"), px, false);
            writeBitmap(new File(res, dirs[i] + "/ic_launcher_round.png"), px, true);
        }
        System.out.println("icon generated, path chars=" + pathData.length());
    }

    /** Java2D 的轮廓坐标和 Android VectorDrawable 一样是 y 轴向下，可以直接搬。 */
    static String toPathData(Shape s) {
        StringBuilder sb = new StringBuilder();
        PathIterator it = s.getPathIterator(null);
        double[] c = new double[6];
        while (!it.isDone()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO:
                    sb.append("M").append(f(c[0])).append(",").append(f(c[1])).append(" ");
                    break;
                case PathIterator.SEG_LINETO:
                    sb.append("L").append(f(c[0])).append(",").append(f(c[1])).append(" ");
                    break;
                case PathIterator.SEG_QUADTO:
                    sb.append("Q").append(f(c[0])).append(",").append(f(c[1])).append(" ")
                            .append(f(c[2])).append(",").append(f(c[3])).append(" ");
                    break;
                case PathIterator.SEG_CUBICTO:
                    sb.append("C").append(f(c[0])).append(",").append(f(c[1])).append(" ")
                            .append(f(c[2])).append(",").append(f(c[3])).append(" ")
                            .append(f(c[4])).append(",").append(f(c[5])).append(" ");
                    break;
                case PathIterator.SEG_CLOSE:
                    sb.append("Z ");
                    break;
            }
            it.next();
        }
        return sb.toString().trim();
    }

    static String f(double v) {
        String s = String.format(java.util.Locale.US, "%.2f", v);
        if (s.endsWith(".00")) s = s.substring(0, s.length() - 3);
        else if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s;
    }

    static void writeForeground(String res, String pathData) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    android:width=\"108dp\"\n"
                + "    android:height=\"108dp\"\n"
                + "    android:viewportWidth=\"108\"\n"
                + "    android:viewportHeight=\"108\">\n"
                + "    <path\n"
                + "        android:fillColor=\"#FFFFFF\"\n"
                + "        android:pathData=\"" + pathData + "\" />\n"
                + "</vector>\n";
        write(new File(res, "drawable/ic_launcher_foreground.xml"), xml);
    }

    /** 纯黑底。用 App 的夜间背景色 #0B0D10，和界面是同一套。 */
    static void writeBackground(String res) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    android:width=\"108dp\"\n"
                + "    android:height=\"108dp\"\n"
                + "    android:viewportWidth=\"108\"\n"
                + "    android:viewportHeight=\"108\">\n"
                + "    <path\n"
                + "        android:fillColor=\"#0B0D10\"\n"
                + "        android:pathData=\"M0,0h108v108h-108z\" />\n"
                + "</vector>\n";
        write(new File(res, "drawable/ic_launcher_background.xml"), xml);
    }

    static void write(File f, String content) throws IOException {
        f.getParentFile().mkdirs();
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try {
            w.write(content);
        } finally {
            w.close();
        }
    }

    /** 传统位图（API 26 以下与部分启动器会直接用它）。 */
    static void writeBitmap(File out, int px, boolean round) throws IOException {
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x0B0D10));
        if (round) {
            g.fillOval(0, 0, px, px);
        } else {
            double r = px * 0.22;
            g.fill(new RoundRectangle2D.Double(0, 0, px, px, r, r));
        }

        // 圆形的内接宽度更小，文字要相应收窄，否则会被裁掉
        double targetW = px * (round ? 0.42 : 0.52);
        Font font = new Font("Arial", Font.BOLD | Font.ITALIC, 100);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector gv = font.createGlyphVector(frc, TEXT);
        Shape shape = gv.getOutline();
        Rectangle2D b = shape.getBounds2D();
        double scale = targetW / b.getWidth();
        AffineTransform t = new AffineTransform();
        t.translate(px / 2.0 - b.getWidth() * scale / 2.0, px / 2.0 - b.getHeight() * scale / 2.0);
        t.scale(scale, scale);
        t.translate(-b.getX(), -b.getY());
        g.setColor(Color.WHITE);
        g.fill(t.createTransformedShape(shape));
        g.dispose();

        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}
