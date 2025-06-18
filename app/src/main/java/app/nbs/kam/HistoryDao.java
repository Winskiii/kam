package app.nbs.kam;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HistoryDao {

    @Insert
    void insert(HistoryItem historyItem);

    @Query("SELECT * FROM history_table ORDER BY id DESC")
    List<HistoryItem> getAllHistory();

    @Query("DELETE FROM history_table")
    void deleteAll();
}