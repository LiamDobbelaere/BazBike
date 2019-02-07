package link.diga.bazbike.persistence;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface LocationGoalDao {
    @Query("SELECT * FROM LocationGoal")
    List<LocationGoal> getAll();

    @Insert
    void insertAll(LocationGoal... locationGoals);

    @Delete
    void delete(LocationGoal locationGoal);
}
