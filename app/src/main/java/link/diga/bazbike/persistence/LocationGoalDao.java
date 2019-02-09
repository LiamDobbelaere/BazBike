package link.diga.bazbike.persistence;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationGoalDao {
    @Query("SELECT * FROM LocationGoal")
    List<LocationGoal> getAll();

    @Insert
    void insertAll(LocationGoal... locationGoals);

    @Delete
    void delete(LocationGoal locationGoal);

    @Query("DELETE FROM locationgoal")
    void deleteAll();
}
