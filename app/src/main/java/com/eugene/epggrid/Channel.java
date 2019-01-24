package com.eugene.epggrid;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Random;

public class Channel {
    private String groupName;
    private ArrayList<Item> programsList = new ArrayList<>();
    private String channelName;
    private Bitmap icon;

    public Channel(int from, String groupName, String channelName, Bitmap icon) {
        this.groupName = groupName;
        this.channelName = channelName;
        this.icon = icon;

        testData(from);
    }

    public String getGroupName() {
        return groupName;
    }

    public ArrayList<Item> getProgramsList() {
        return programsList;
    }

    public String getChannelName() {
        return channelName;
    }

    public Bitmap getIcon() {
        return icon;
    }

    private void testData(int from) {
        insertItems(generateItems(from, from + 24 * 3600));
    }

    private void insertItems(ArrayList<Item> items) {
        if (items.size() < 1) return;

        programsList = items;
    }

    private ArrayList<Item> generateItems(int from, int to) {
        ArrayList<Item> itemArrayList = new ArrayList<>();
        int length = 15 * 60 + new Random().nextInt(3600);

        int i = 0;
        while (from + length < to) {
            String title = "Prog_" + i;
            String descr = "Prog_" + i + "_descr";
            int start = from;
            int end = from + length;
            Item item = new Item(title, descr, start, end, this);
            itemArrayList.add(item);

            from = from + length;
            length = 15 * 60 + new Random().nextInt(3600);

            i += 1;
        }
        return itemArrayList;
    }
}

class Item {
    private String programTitle, programDescription;
    private int start, end;
    private Channel channel;

    public Item(String programTitle, String programDescription, int start, int end, Channel channel) {
        this.programTitle = programTitle;
        this.programDescription = programDescription;
        this.start = start;
        this.end = end;
        this.channel = channel;
    }

    public String getProgramTitle() {
        return programTitle;
    }

    public String getProgramDescription() {
        return programDescription;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public Channel getChannel() {
        return channel;
    }
}
