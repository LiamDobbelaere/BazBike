package link.diga.bazbike.persistence;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class LocationGoal {
    @PrimaryKey
    public int id;

    @ColumnInfo(name = "location_name")
    public String locationName;

    @ColumnInfo(name = "lat")
    public double lat;

    @ColumnInfo(name = "lng")
    public double lng;
}
