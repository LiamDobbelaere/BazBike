package link.diga.bazbike;

public class XPHelper {
    public static float XPToLevel(float xp) {
        return (float) Math.floor( (Math.sqrt(625f + 100f * xp) - 25f)/50f );
    }

    public static float LevelToXP(float level) {
        return 25f * level * (1f + level);
    }
}
