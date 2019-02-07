package link.diga.bazbike.persistence;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {LocationGoal.class}, version = 1)
public abstract class BazBikeDatabase extends RoomDatabase {
    public abstract LocationGoalDao locationGoalDao();
}
