package app.nbs.kam;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history_table")
public class HistoryItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "road_name")
    private String roadName;

    @ColumnInfo(name = "damage_type")
    private String damageType;

    @ColumnInfo(name = "damage_scale")
    private String damageScale;

    @ColumnInfo(name = "confidence_level")
    private String confidenceLevel;

    @ColumnInfo(name = "image_path")
    private String imagePath; // Path ke file gambar di penyimpanan internal

    // Constructor
    public HistoryItem(String roadName, String damageType, String damageScale, String confidenceLevel, String imagePath) {
        this.roadName = roadName;
        this.damageType = damageType;
        this.damageScale = damageScale;
        this.confidenceLevel = confidenceLevel;
        this.imagePath = imagePath;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRoadName() { return roadName; }
    public String getDamageType() { return damageType; }
    public String getDamageScale() { return damageScale; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public String getImagePath() { return imagePath; }
}