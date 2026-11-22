/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Canvas
 *  javax.microedition.lcdui.Graphics
 *  javax.microedition.lcdui.Image
 */
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/*
 * Duplicate member names - consider using --renamedupmembers true
 */
public final class e
extends Canvas {
    byte a;
    boolean a = 0;

    public e(tribes tribes2) {
        this.setFullScreenMode(true);
    }

    public final void paint(Graphics graphics) {
        graphics.drawImage(g.a[0], 0, 0, 0);
    }

    final void a() {
        if (!this.a) {
            this.a = true;
            g.a[0] = Image.createImage((int)242, (int)320);
            g.a[0] = g.a[0].getGraphics();
            g.f = new int[]{0, 5251341, 16704820, 16033043, 12010496};
            g.l = new byte[]{0, 1, 1, 1, 1};
            g.m = new byte[]{0, 1, 1, 2, 3};
            g.n = new byte[]{100, 98, 0, 0, 0};
            g.o = new byte[]{5, 3, 1, 1, 1};
        }
        try {
            if (this.a < 4) {
                int n = 0xFFFFFF;
                g.a[0].setColor(n);
                e.b();
                g.a[0].drawImage(Image.createImage((String)("/l" + this.a)), 120, 150, 3);
            }
        }
        catch (Exception exception) {
            this.a = (byte)(this.a + 1);
            this.a();
        }
        this.repaint();
    }

    private static void b() {
        g.a[0].fillRect(0, 0, 240, 301);
    }

    protected final void keyPressed(int n) {
    }
}

