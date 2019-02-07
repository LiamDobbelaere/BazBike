package link.diga.bazbike.persistence;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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
