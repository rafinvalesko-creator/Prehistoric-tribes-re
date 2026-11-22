/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Font
 *  javax.microedition.lcdui.Graphics
 */
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/*
 * Duplicate member names - consider using --renamedupmembers true
 */
public final class d {
    g a;
    byte[] a;
    short[] a;
    byte[] b;
    short[] b;
    byte[] c;
    short[] c;
    byte[][] a;
    short[][] a;
    String[] a;
    byte[][] b;
    byte[][] c;
    byte[] d;
    short[] d;
    short[] e;
    short[] f;
    byte[] e;
    byte a;
    byte b;
    byte c;
    byte[] f;
    int a;
    InputStream a;
    int b = new short[230];
    int c = new short[315];
    int d;
    int e;
    int f = -1;
    int g;
    int h;
    byte d;
    int i;
    int j;
    int k;
    int l;
    int m;
    int n;
    int o;
    int p;
    short[] g;
    short[] h;
    int q;
    int r;
    int s;
    int t;
    Font a;
    boolean a = null;
    int u;
    int v;
    int w;
    int x;
    int y;
    int z;

    public d(g g2) {
        this.a = g2;
        this.a = this.a.a;
        this.a = this.a.a;
        this.a = Font.getFont((int)64, (int)0, (int)8);
    }

    final void a() {
        this.k = 13;
        while (this.k < 33) {
            this.f[this.k] = 0;
            ++this.k;
        }
        this.b = 0;
        this.c = 0;
        this.k = 0;
        while (this.k < 122) {
            this.f[this.k] = 0;
            ++this.k;
        }
    }

    final void a(int n) {
        this.a.w = -1;
        this.g = n;
        this.c = this.a[308];
        while (this.c < this.a[309]) {
            if (n < this.a[this.c]) {
                this.c -= this.a[308];
                break;
            }
            ++this.c;
        }
        if (this.f != this.c) {
            this.f = this.c;
            this.b(2);
        }
    }

    final void b(int n) {
        try {
            try {
                this.a = this.getClass().getResourceAsStream("/" + g.a + this.a[n] + (n == 2 ? String.valueOf(this.f) : ""));
                if (this.a != null) {
                    if (n < 2) {
                        this.d = 0;
                        this.a[n][0] = 0;
                        this.c = 0;
                    } else {
                        this.d = 76;
                        this.c = this.a[1][76];
                    }
                    while ((this.b = this.a.read()) != -1) {
                        if (this.b == 124) {
                            ++this.d;
                            this.a[n][this.d] = (short)this.c;
                            continue;
                        }
                        if (this.b == 31) {
                            this.e = 0;
                            while (this.e < this.a.e.length()) {
                                this.a[n][this.c] = this.a[this.a[280] + (byte)this.a.e.charAt(this.e)];
                                ++this.e;
                                ++this.c;
                            }
                            --this.c;
                        } else {
                            String string = "no content";
                            if (this.b == 21) {
                                string = this.a.a;
                            } else if (this.b == 29) {
                                string = this.a.b;
                            } else if (this.b == 22) {
                                string = this.a.c;
                            } else if (this.b == 30) {
                                string = this.a.d;
                            }
                            if (string != "no content") {
                                this.e = 0;
                                while (this.e < string.length()) {
                                    this.a[n][this.c] = (byte)string.charAt(this.e);
                                    ++this.e;
                                    ++this.c;
                                }
                                --this.c;
                            } else {
                                this.a[n][this.c] = this.d != 72 || n == 0 ? this.a[this.a[280] + this.b] : (byte)this.b;
                            }
                        }
                        ++this.c;
                    }
                }
                this.a.close();
                this.a = null;
            }
            catch (IOException iOException) {
                return;
            }
        }
        catch (Exception exception) {}
    }

    final void a(int n, int n2, int n3, String string, int n4) {
        this.a = 0;
        while (this.a < string.length()) {
            this.b[this.b[228] + this.a] = this.a[this.a[280] + (byte)string.charAt(this.a)];
            ++this.a;
        }
        this.b[229] = (short)(this.b[228] + string.length());
        this.a(n, n2, n3, 228, n4);
    }

    final void a(int n, int n2, int n3, int n4, int n5) {
        this.h = n2 == -100 ? 13 + this.b : this.a;
        if (this.h >= 13 && this.f[this.h] != 0) {
            return;
        }
        this.d[this.h] = (byte)(this.b[n4 + 1] - this.b[n4]);
        this.e[this.h] = (byte)n5;
        if (this.h >= 13) {
            n5 = 1;
        }
        this.d[this.h] = (short)((this.d[this.h] - 1) * (0 + n5));
        this.f[this.h] = (short)n;
        this.e[this.h] = (short)(this.h >= 13 ? 0 : n2);
        this.a = 0;
        while (this.a < this.d[this.h]) {
            this.d = this.b[this.b[n4] + this.a];
            this.c[this.h][this.a] = (byte)(this.d / 30);
            this.b[this.h][this.a] = (byte)(this.d % 30);
            int n6 = this.h;
            this.d[n6] = (short)(this.d[n6] + ((this.a[this.a[281 + n5] + this.c[this.h][this.a] * 19 + this.b[this.h][this.a] + 1] & 0xFF) - (this.a[this.a[281 + n5] + this.c[this.h][this.a] * 19 + this.b[this.h][this.a]] & 0xFF)));
            ++this.a;
        }
        if (n4 == 97) {
            this.a.a(1049, 26, n2 - 2);
            this.a.a(1055, 211, n2 - 2);
        }
        if (this.h < 13) {
            int n7 = this.h;
            this.f[n7] = (short)(this.f[n7] - this.d[this.h] * n3 / 2);
            this.a = (byte)(this.a + 1);
            return;
        }
        this.b = (byte)(this.b + 1);
        if (this.b >= 20) {
            this.b = 0;
        }
    }

    final void a(int n, int n2, int n3, int n4, int n5, int n6, int n7) {
        if (n4 >= 0) {
            this.a(n, n2, 0, n4, n7);
            this.i = this.d[this.h] + 1;
            this.j = this.h;
        }
        if (n5 >= 0) {
            this.a(n + this.i, n2, 0, n5, n7);
            this.i += this.d[this.h] + 1;
        }
        if (n6 >= 0) {
            this.a(n + this.i, n2, 0, n6, n7);
            this.i += this.d[this.h];
        }
        while (this.j < this.a) {
            int n8 = this.j++;
            this.f[n8] = (short)(this.f[n8] - this.i * n3 / 2);
        }
    }

    final void a(Graphics graphics) {
        this.k = 0;
        while (this.k < this.a) {
            this.m = this.f[this.k];
            this.l = 0;
            while (this.l < this.d[this.k]) {
                this.n = this.e[this.k];
                this.n += this.a[this.a[285] + this.c[this.k][this.l]];
                this.p = this.m;
                this.o = (this.a[this.a[281 + this.e[this.k]] + this.c[this.k][this.l] * 19 + this.b[this.k][this.l] + 1] & 0xFF) - (this.a[this.a[281 + this.e[this.k]] + this.c[this.k][this.l] * 19 + this.b[this.k][this.l]] & 0xFF);
                g.a(graphics, this.a.a[0][25 + this.e[this.k]], this.a[this.a[281 + this.e[this.k]] + this.c[this.k][this.l] * 19 + this.b[this.k][this.l]] & 0xFF, this.a[this.a[284] + this.e[this.k] * 3 + this.c[this.k][this.l]], this.o, this.a[this.a[283] + this.e[this.k] * 3 + this.c[this.k][this.l]], 0 + this.p, 0 + this.n, 20);
                this.m += this.o + 0 + this.e[this.k];
                ++this.l;
            }
            ++this.k;
        }
        this.a = 0;
    }

    final void b() {
        this.k = 13 + this.c;
        while (this.k != 13 + this.b) {
            int n = this.k;
            this.e[n] = (short)(this.e[n] + (this.e[this.k] / 4 + 1));
            if (this.e[this.k] >= 15) {
                this.f[this.k] = 0;
                this.c = (byte)(this.c + 1);
                if (this.c >= 20) {
                    this.c = 0;
                }
            }
            if (this.k == 32) {
                this.k = 12;
            }
            ++this.k;
        }
        this.k = 0;
        while (this.k < 122) {
            if (this.f[this.k] != 0 && this.k % 3 == this.a.c % 3) {
                this.a(this.k, -100, 0, (this.f[this.k] > 0 ? "+" : "") + this.f[this.k], this.f[this.k] > 0 ? 2 : (this.k <= 50 ? 1 : 0));
                this.f[this.k] = 0;
            }
            ++this.k;
        }
    }

    final void a(int n, int n2, int n3) {
        this.k = 13 + this.c;
        while (this.k != 13 + this.b) {
            if (this.f[this.k] == n) {
                this.m = n2 - this.d[this.k] / 2 + this.a.a(255, 5) - 2;
                this.n = n3 - 5 - this.e[this.k] * 2;
                this.l = 0;
                while (this.l < this.d[this.k]) {
                    this.o = (this.a[this.a[282] + this.c[this.k][this.l] * 19 + this.b[this.k][this.l] + 1] & 0xFF) - (this.a[this.a[282] + this.c[this.k][this.l] * 19 + this.b[this.k][this.l]] & 0xFF);
                    g.a(g.a[1], this.a.a[0][26], this.a[this.a[282] + this.c[this.k][this.l] * 19 + this.b[this.k][this.l]] & 0xFF, this.a[this.a[284] + 3 + this.c[this.k][this.l]] + this.a[this.a[283] + 3 + 0] * this.e[this.k], this.o, this.a[this.a[283] + 3 + this.c[this.k][this.l]], 0 + this.m, 0 + this.n, 20);
                    this.m += this.o + 0 + 1;
                    ++this.l;
                }
            }
            if (this.k >= 32) {
                this.k = 12;
            }
            ++this.k;
        }
    }

    final void b(int n, int n2, int n3, int n4, int n5, int n6, int n7) {
        ++n;
        n3 -= 2;
        n2 += 4;
        n4 -= 4;
        if (n5 >= 76) {
            n5 += this.a[this.a[309] + this.g] & 0xFF;
        }
        this.a = n5 == 72;
        this.v = 0;
        this.t = 0;
        this.g[0] = this.a[1][n5];
        this.w = 0;
        this.u = this.a[1][n5] & 0xFFFF;
        while (this.u < (this.a[1][n5 + 1] & 0xFFFF)) {
            if (!this.a && this.a[1][this.u] == 44 && (this.a[1][this.u + 1] < 90 || this.a[1][this.u + 1] >= 112) || this.a[1][this.u] == 127) {
                this.v = this.u + 1;
                this.x = this.w;
            }
            if (this.a && this.a[1][this.u] == 32) {
                this.v = this.u + 1;
                this.x = this.w;
            }
            if (this.a || this.a[1][this.u] < 127) {
                int n8;
                int n9;
                d d2;
                this.w += 0 + n7;
                if (this.a) {
                    d d3 = this;
                    d2 = d3;
                    n9 = d3.w;
                    n8 = this.a.charWidth((char)((this.a[1][this.u] & 0xFF) + ((this.a[1][this.u] & 0xFF) >= 192 && (this.a[1][this.u] & 0xFF) <= 255 ? 848 : 0)));
                } else {
                    d d4 = this;
                    d2 = d4;
                    n9 = d4.w;
                    n8 = this.a[1][this.u] >= 90 ? (this.a[1][this.u] < 107 ? 15 : this.a[this.a[285] + this.a[1][this.u] - 90]) : (this.a[this.a[281 + n7] + this.a[1][this.u] / 30 * 19 + this.a[1][this.u] % 30 + 1] & 0xFF) - (this.a[this.a[281 + n7] + this.a[1][this.u] / 30 * 19 + this.a[1][this.u] % 30] & 0xFF);
                }
                d2.w = n9 + n8;
            }
            if (this.w > n3 && this.v > (this.g[this.t] & 0xFFFF) || !this.a && this.a[1][this.u] == 127) {
                ++this.t;
                this.g[this.t] = (short)this.v;
                this.h[this.t - 1] = (short)(n + (n3 - this.x) * (n6 & 1) / 2);
                this.u = this.v - 1;
                this.w = 0;
            }
            if (!(this.a || this.a[1][this.u] != 70 && this.a[1][this.u] != 73 && this.a[1][this.u] != 109)) {
                this.v = this.u + 1;
                this.x = this.w;
            }
            if (this.a && (this.a[1][this.u] == 45 || this.a[1][this.u] == 46)) {
                this.v = this.u + 1;
                this.x = this.w;
            }
            ++this.u;
        }
        if (this.w > 0) {
            ++this.t;
            this.h[this.t - 1] = (short)(n + (n3 - this.w) * (n6 & 1) / 2);
            this.g[this.t] = this.a[1][n5 + 1];
        }
        this.q = n2 + (n6 == 3 && this.t * 17 < n4 ? (n4 - this.t * 17) / 2 : 0);
        this.r = n7;
        this.s = n4 / 17;
    }

    final void a(Graphics graphics, int n) {
        try {
            if (this.a) {
                graphics.setFont(this.a);
                graphics.setColor(0);
            }
            this.k = n;
            while ((this.a || this.k < n + this.s) && this.k < this.t) {
                this.m = this.h[this.k];
                this.l = this.g[this.k] & 0xFFFF;
                while (this.l < (this.g[this.k + 1] & 0xFFFF)) {
                    if (this.a[1][this.l] != 127 || this.a) {
                        this.n = this.q + (this.k - n) * 17;
                        if (this.a[1][this.l] > 112 && !this.a) {
                            this.y = this.a[1][this.l] - 112 - 1 + 19;
                            this.a(24, this.n, 0, String.valueOf(this.y == 24 ? this.a.a[0][this.y] * 10 + this.a.a(65535, 10) : this.a.a[0][this.y]), 0);
                            this.a(216, this.n, 2, String.valueOf(this.y == 24 ? this.a.a[1][this.y] * 10 + this.a.a(65535, 10) : this.a.a[1][this.y]), 0);
                        } else {
                            int n2;
                            d d2;
                            this.y = this.a[1][this.l] / 30;
                            if (!this.a) {
                                this.n += this.a[this.a[285] + this.y];
                            }
                            this.z = this.a[1][this.l] % 30;
                            if (this.a) {
                                d2 = this;
                                n2 = this.a.charWidth((char)((this.a[1][this.l] & 0xFF) + ((this.a[1][this.l] & 0xFF) >= 192 && (this.a[1][this.l] & 0xFF) <= 255 ? 848 : 0)));
                            } else {
                                d2 = this;
                                n2 = this.y >= 3 ? (this.z < 17 ? 15 : this.a[this.a[285] + this.z]) : (this.a[this.a[281 + this.r] + this.y * 19 + this.z + 1] & 0xFF) - (this.a[this.a[281 + this.r] + this.y * 19 + this.z] & 0xFF);
                            }
                            d2.o = n2;
                            this.p = this.m;
                            if (this.a) {
                                graphics.drawChar((char)((this.a[1][this.l] & 0xFF) + ((this.a[1][this.l] & 0xFF) >= 192 && (this.a[1][this.l] & 0xFF) <= 255 ? 848 : 0)), 0 + this.p, 0 + this.n, 20);
                            } else if (this.y >= 3) {
                                this.a.a(562 + this.a[1][this.l] - 90, this.p, this.n + -1);
                            } else {
                                g.a(graphics, this.a.a[0][25 + this.r], this.a[this.a[281 + this.r] + this.y * 19 + this.z] & 0xFF, this.a[this.a[284] + this.r * 3 + this.y], this.o, this.a[this.a[283] + this.r * 3 + this.y], 0 + this.p, 0 + this.n, 20);
                            }
                            this.m += this.o + 0 + this.r;
                        }
                    }
                    ++this.l;
                }
                ++this.k;
            }
            this.a.R = n == 0;
            if (n + this.s < this.t) {
                this.a.S = false;
                return;
            }
            this.a.S = true;
        }
        catch (Exception exception) {}
    }
}

