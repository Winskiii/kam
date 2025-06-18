package app.nbs.kam;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<HistoryItem> historyList;

    public HistoryAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_card_layout, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem currentItem = historyList.get(position);

        holder.roadName.setText(currentItem.getRoadName());

        String details = String.format(Locale.getDefault(), "Type: %s\nScale: %s\nConfidence: %s",
                currentItem.getDamageType(),
                currentItem.getDamageScale(),
                currentItem.getConfidenceLevel());
        holder.damageDetails.setText(details);

        File imgFile = new File(currentItem.getImagePath());
        if (imgFile.exists()) {
            holder.imageView.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView roadName;
        TextView damageDetails;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.history_image);
            roadName = itemView.findViewById(R.id.history_road_name);
            damageDetails = itemView.findViewById(R.id.history_damage_details);
        }
    }
}