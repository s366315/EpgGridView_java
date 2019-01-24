package com.eugene.epggrid;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.eugene.epggrid.EpgGridView.EpgGridViewListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private EpgGridView gridView;
    private FloatingActionButton fab;
    private AlertDialog channelDetailDialog, groupsFilterDialog;
    private ArrayList<Channel> channelsArrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = findViewById(R.id.grid_view);
        fab = findViewById(R.id.fab);

        gridView.setGridViewListener(gridViewListener());
        gridView.refreshContentByTimer(true);

        fab.setOnClickListener(fabClickListener());
    }

    private ArrayList<Channel> generateData(long timestamp, int period) {//сгенерируем 10 каналов и по 6 часов по обе стороны текущего реального времени
        channelsArrayList = new ArrayList<>();
        // генерация каналов
        for (int i = 0; i < 25; i++) {
            Channel channel = new Channel((int) timestamp, "group" + i, "channel" + i, null);
            channelsArrayList.add(channel);
        }

        return channelsArrayList;
    }

    private EpgGridViewListener gridViewListener() {
        return new EpgGridViewListener() {
            @Override
            public void getNextEpg(long timestamp, int period) {
                gridView.setGridViewData(generateData(timestamp, period));
            }

            @Override
            public void onGridItemClick(Item item) {
                buildChannelDetailDialog(item);
            }

            @Override
            public void onGridMotionByUser(boolean state) {
                if (state) {
                    fab.hide();
                } else {
                    fab.show();
                }
            }

            @Override
            public void setCurrentDate(String dateToShow) {
                //если нужно отобразить где-нибудь дату, в которой скроллится пользователь
            }
        };
    }

    private View.OnClickListener fabClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gridView.setToCurrentTime();
            }
        };
    }

    private void buildChannelDetailDialog(Item item) {//диалог для выбора программы
        if (channelDetailDialog != null) channelDetailDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getChannel().getChannelName());
        builder.setMessage(item.getProgramDescription());

        builder.setPositiveButton("Play", null);
        builder.setNegativeButton("Dismiss", null);

        channelDetailDialog = builder.create();
        channelDetailDialog.show();
    }

    private void buildGroupsFilterDialog() {//диалог для указания фильтра по группам каналов
        if (groupsFilterDialog != null) groupsFilterDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select group");

        final ArrayList<String> groups = new ArrayList<>();
        groups.add("All");
        for (int i = 0; i < channelsArrayList.size(); i++) {
            groups.add(channelsArrayList.get(i).getGroupName());
        }

        builder.setItems(groups.toArray(new CharSequence[groups.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    gridView.setFilter("");
                } else {
                    gridView.setFilter(groups.get(which));
                }
            }
        });

        builder.setNegativeButton("Dismiss", null);

        groupsFilterDialog = builder.create();
        groupsFilterDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_filter:
                buildGroupsFilterDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
