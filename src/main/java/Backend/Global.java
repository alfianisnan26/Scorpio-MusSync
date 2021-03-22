package Backend;

import org.json.simple.JSONObject;

/**
 * Global Static Method and Variables definition
 */
public class Global {
    /**
     * @param out Input for System out printer
     */
    public static void p(Object out) {
        System.out.print(out);
    }

    /**
     * @param out Input for System out printer with line
     */
    public static void pl(Object out) {
        System.out.println(out);
    }

    /**
     * Print New Line
     */
    public static void pl() {
        System.out.println();
    }

    /**
     * @param out Input for System error printer
     */
    public static void e(Object out) {
        System.err.print(out);
    }

    /**
     * @param out Input for System error printer with line
     */
    public static void el(Object out) {
        System.err.println(out);
    }

    /**
     * Global Variable Static Class
     */
    public static class v {
        public static JSONObject data;
        public static int playOffset = 5000;
    }

    /**
     * Global Final Static Class
     */
    public static class f {
        public static final String database = "https://musync-sync.firebaseio.com/";
        public static final String url = "https://catbox.moe/user/api.php";
    }
}
