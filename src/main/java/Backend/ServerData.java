package Backend;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServerData {
    private JSONObject data;
    private String id;
    public static final String ISACTIVE = "isActive";
    public static final String TIMER = "timer";
    private static final String READY = "ready";
    public static final String FILENAME = "filename";
    public static final String LINK = "link";
    public static final String PLAYER = "player";
    public static final String SEEKER = "seeker";
    public static final long ISSTOP = 0;
    public static final long ISPLAY = 2;
    public static final long ISREADY = 1;
    public static final long ISPAUSE = 3;
    
    public ServerData(){

    }

    public ServerData withID(String id) {
        this.id = id;
        return this;
    }

    public ServerData get() {
        data = getNow(this.id);
        return this;
    }

    public JSONObject getData() {
        return data;
    }

    public static boolean putNow(String id, JSONObject data) {
        try {
            ViHttp codeGen = new ViHttp(Global.f.database + id + ".json", data.toJSONString(), ViHttp.PUT).start();
            while (!codeGen.control().isFinished())
                ;
            if (codeGen.control().isError()) {
                Global.pl("Error : " + codeGen.getStringResponse());
                return false;
            }
            Global.pl(codeGen.getStringResponse());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public static JSONObject getNow(String id) {
        try {
            ViHttp resp = new ViHttp(Global.f.database + id + ".json").start();
            while (!resp.control().isFinished())
                ;
            if (resp.control().isError())
                return null;
            String value = resp.getStringResponse();
            JSONObject serverData = null;
            JSONParser parser = new JSONParser();
            ContainerFactory containerFactory = new ContainerFactory() {
                @Override
                public Map createObjectContainer() {
                    return new LinkedHashMap<>();
                }

                @Override
                public List creatArrayContainer() {
                    return new LinkedList<>();
                }
            };
            try {
                serverData = (JSONObject) parser.parse(value);
                if (serverData == null) {
                    return null;
                }
                Long time = System.currentTimeMillis();
                serverData.put(ServerData.ISACTIVE,
                        (time - Long.parseLong(serverData.get(ServerData.TIMER).toString()) < 86000000));
                serverData.put(ServerData.READY,
                        (Long.parseLong(serverData.get(ServerData.TIMER).toString()) - time > 1000));
                serverData.forEach((k, v) -> System.out.println("Key : " + k + " Value : " + v));
            } catch (ParseException pe) {
                System.out.println("position: " + pe.getPosition());
                System.out.println(pe);
            }
            return serverData;
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;

    }
}