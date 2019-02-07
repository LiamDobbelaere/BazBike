package link.diga.bazbike.persistence;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {LocationGoal.class}, version = 1)
public abstract class BazBikeDatabase extends RoomDatabase {
    public abstract LocationGoalDao locationGoalDao();
}
