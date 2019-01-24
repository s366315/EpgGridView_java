package com.eugene.epggrid;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class EpgGridView extends View {
    private GridTask gridTask = new GridTask();
    private GestureDetector gestureDetector;
    private Scroller scroller;
    private int scale = dpToInt(200); //dp per hour
    private int channelHeight = dpToInt(70); //dp per channel
    private int channelScaleWidth = dpToInt(70); //dp per channel
    private long startTime;
    private long endtime;
    private int timeScaleHeight = dpToInt(50);
    private int bigStripeSize = dpToInt(15);
    private int smallStripeSize = dpToInt(10);
    private float textSize = dpToInt(14);
    private ArrayList<Channel> channels = new ArrayList<>();
    private ArrayList<Channel> tempChannels = new ArrayList<>();
    private final int INTERVAL = 12 * 3600;
    private ArrayList<Integer> loadedData = new ArrayList<>();
    private boolean sizechanged = false;
    private Context context;
    private String groupsFilter = "";
    private String dateToShow;
    private Paint timescaleBgPaint, bitmapPaint, descPaint, titlePaint, colorChannelsItemsPaint, textChannelsItemsPaint, channelBarPaint, currentTimeStripe, timescaleTextPaint;
    private Bitmap drawableShadow;
    private Bitmap chIconBitmap;
    private RectF rf;
    private Rect newrect;
    private Channel chi;
    private int channelsBgColor;
    private int channelsBgColor1;
    private int order = 1;
    private EpgGridViewListener viewListener;
    private Timer mTimer;
    private boolean isNeedToRefresh = false;
    private long requestedDataOnTimestamp = 0;

    public EpgGridView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public EpgGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public EpgGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public interface EpgGridViewListener {
        /**
         * используется для запросов к API сервера
         *
         * @param timestamp время, на которое требуется запросить список программ
         * @param period    интервал часов от текущего времени
         */
        void getNextEpg(long timestamp, int period);

        /**
         * @param item программа передач, по которой был клик
         */
        void onGridItemClick(Item item);

        /**
         * срабатывает, когда пользователь двигает сетку
         *
         * @param state
         */
        void onGridMotionByUser(boolean state);

        /**
         * возвращает день, месяц, число в котором находитсятекущая позиция в сетке
         *
         * @param dateToShow
         */
        void setCurrentDate(String dateToShow);
    }

    /**
     * основные события сетки
     *
     * @param listener
     */
    public void setGridViewListener(EpgGridViewListener listener) {
        viewListener = listener;
    }

    /**
     * установка фильтра по группам
     *
     * @param groupsFilter
     */
    public void setFilter(String groupsFilter) {
        this.groupsFilter = groupsFilter;
        setScrollY(0);
        tempChannels.clear();

        if (groupsFilter.isEmpty()) {
            tempChannels.addAll(channels);
            invalidate();
            return;
        }


        for (Channel channel : channels) {
            if (channel.getGroupName().toLowerCase().equals(groupsFilter.toLowerCase())) {
                tempChannels.add(channel);
            }
        }
        invalidate();
    }

    /**
     * точка входа данных для отображения на сетке
     *
     * @param data
     */
    public void setGridViewData(ArrayList<Channel> data) {
        for (int i = 0; i < data.size(); i++) {
            for (int s = 0; s < data.get(i).getProgramsList().size(); s++) {

            }
        }
        this.channels.clear();
        this.channels.addAll(data);
        setFilter(groupsFilter);//rebuild tempChannels with groupsFilter
    }

    /**
     * передвигает сетку на текущее реальное время
     */
    public void setToCurrentTime() {
        int curtime = (int) (System.currentTimeMillis() / 1000);
        long curpos = ((long) curtime - startTime) * scale / 3600;
        setScrollX((int) curpos - (getWidth() / 2));
        postInvalidate();
    }

    /**
     * переводит dp в int
     *
     * @param dp
     * @return
     */
    private int dpToInt(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (sizechanged) return;
        int curTime = (int) (System.currentTimeMillis() / 1000);
        long curPos = ((long) curTime - startTime) * scale / 3600;
        this.scrollTo((int) curPos - channelScaleWidth - (w - channelScaleWidth) / 2, 0);
        sizechanged = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = gestureDetector.onTouchEvent(event);

        if (!handled && event.getAction() == MotionEvent.ACTION_UP) {

        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
            if (viewListener != null)
                viewListener.onGridMotionByUser(true);
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            if (viewListener != null)
                viewListener.onGridMotionByUser(false);
        }


        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {//отменяем fling, если таковой есть, когда пользователь коснулся экрана
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }
        }

        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }

        return true;
    }

    /**
     * принимает true, чтобы разрешить таймер перерисовки сетки (чтоб двигалась красная линия текущего времени)
     *
     * @param refresh
     */
    public void refreshContentByTimer(boolean refresh) {
        isNeedToRefresh = refresh;
    }

    /**
     * возвращает программу, по которой был клик
     *
     * @param x
     * @param y
     * @return
     */
    private Item findEpg(int x, int y) {
        int chnum = (y - timeScaleHeight) / channelHeight;
        if (chnum > tempChannels.size() - 1)
            return null;

        if (y < timeScaleHeight + getScrollY())
            return null;

        if (x < channelScaleWidth + getScrollX())
            return null;
        else {
            Channel chi1 = tempChannels.get(chnum);
            for (int i = 0; i < chi1.getProgramsList().size(); i++) {
                Item epg = chi1.getProgramsList().get(i);
                int epgLeft = (int) ((epg.getStart() - startTime) * scale / 3600);
                int epgRight = (int) ((epg.getEnd() - startTime) * scale / 3600);
                if (x >= epgLeft && x < epgRight) {
                    return epg;
                }
            }

        }
        return null;
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        // Load attributes
        this.context = context;

        mTimer = new Timer();
        mTimer.schedule(gridTask, 2000, 2000);

        timescaleBgPaint = new Paint();
        timescaleBgPaint.setColor(getResources().getColor(R.color.grid_scale_background_color)); //цвет фона линейки времени

        timescaleTextPaint = new Paint();
        timescaleTextPaint.setColor(getResources().getColor(android.R.color.white));
        timescaleTextPaint.setAntiAlias(true);
        timescaleTextPaint.setTextAlign(Paint.Align.CENTER);
        timescaleTextPaint.setTextSize(dpToInt(14)); //цвет шрифта линейки времени

        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);//иконки каналов
        bitmapPaint.setStyle(Paint.Style.STROKE); //

        descPaint = new Paint();//описание программы
        descPaint.setAntiAlias(true);
        descPaint.setTextSize(dpToInt(14));
        descPaint.setColor(getResources().getColor(android.R.color.darker_gray));//цвет описания текста на элементах

        titlePaint = new Paint();//заголовок программы
        titlePaint.setAntiAlias(true);
        titlePaint.setTextSize(dpToInt(16));
        titlePaint.setColor(getResources().getColor(android.R.color.darker_gray));//цвет заголовка текста на элементах

        //цвет фона элементов
        colorChannelsItemsPaint = new Paint();
        channelsBgColor = getResources().getColor(R.color.grid_channel_items_background_color);
        channelsBgColor1 = getResources().getColor(R.color.grid_channel_items_background_color_1);

        textChannelsItemsPaint = new Paint();
        int color2 = getResources().getColor(android.R.color.white);
        textChannelsItemsPaint.setColor(color2);//цвет текста на элементах

        //панель с названиями каналов
        channelBarPaint = new Paint();
        channelBarPaint.setColor(getResources().getColor(R.color.grid_channel_bar_background_color));

        currentTimeStripe = new Paint();//индикатор реального времени
        currentTimeStripe.setColor(getResources().getColor(R.color.grid_currenttime_vertical_indicator));//цвет вертикальной линии

        drawableShadow = BitmapFactory.decodeResource(getResources(), R.drawable.shadow_bitmap);//тень под панелью с иконками каналов
        drawableShadow = drawableShadow.createScaledBitmap(drawableShadow, 23, channelHeight, false);

        startTime = (int) (System.currentTimeMillis() / 1000) - 14 * 24 * 3600; //14 дней назад
        endtime = startTime + 28 * 24 * 3600; //14 дней вперёд

        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);

        gestureDetector = new GestureDetector(context, new MyGestureListener());
        scroller = new Scroller(context);
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = scroller.getCurrX();
            int y = scroller.getCurrY();
            scrollTo(x, y);
            if (oldX != getScrollX() || oldY != getScrollY()) {
                onScrollChanged(getScrollX(), getScrollY(), oldX, oldY);
            }
        }
    }

    /**
     * убираем из массива элементы, которые не отображаются
     * @param y
     * @param z
     */
    private void cleanInvisible(long y, long z) {
        for (int i = 0; i < tempChannels.size(); i++) {
            Channel ch1 = tempChannels.get(i);
            int j = 0;
            while (j < ch1.getProgramsList().size()) {
                Item item = ch1.getProgramsList().get(j);
                long time1 = item.getStart();
                if (time1 < y || time1 >= z) ch1.getProgramsList().remove(j);
                else j++;
            }
        }
        int j = 0;
        while (j < loadedData.size()) {
            long ld = loadedData.get(j);
            if (ld < y || ld >= z) loadedData.remove(j);
            else j++;
        }
    }

    /**
     * запрос недостающих данных
     * @param y
     * @param z
     */
    private void loadData(long y, long z) {
        if (requestedDataOnTimestamp == y) {
            return;
        }

        requestedDataOnTimestamp = y;
        for (int i = (int) y; i < z; i += INTERVAL) {
            if (!loadedData.contains(i)) {
                if (viewListener != null)
                    viewListener.getNextEpg(i, INTERVAL / 3600);
                loadedData.add(i);
            }
        }
        for (long v : loadedData) {
            if (v < y || v >= z) {
                cleanInvisible(y, z);
                break;
            }
        }
    }

    /**
     * рассчёт, какие данные нужно запросить, с упреждением
     */
    public void loadMissingData() {
        Rect cliprect = new Rect();
        cliprect.left = getScrollX();
        cliprect.top = getScrollY();
        cliprect.right = cliprect.left + this.getWidth();
        cliprect.bottom = cliprect.top + this.getHeight();
        long drawstarttime = (long) cliprect.left * 3600 / scale + startTime;
        long drawendtime = (long) cliprect.right * 3600 / scale + startTime;
        long a = drawstarttime - (drawendtime - drawstarttime);//предзагрузочный экран слева
        long b = drawendtime + (drawendtime - drawstarttime);//предзагрузочный экран справа
        long y = (long) Math.floor(((float) a / INTERVAL)) * INTERVAL;
        long z = (long) Math.ceil(((float) b / INTERVAL)) * INTERVAL;

        loadData(y, z);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Rect cliprect = canvas.getClipBounds();
        long drawStartTime = (long) cliprect.left * 3600 / scale + startTime;
        long drawEndTime = (long) cliprect.right * 3600 / scale + startTime;

        if (scroller.isFinished()) {//запрос данных только тогда, когда нет прокрутки
            loadMissingData();
        }

        long drawStartChannel = (long) (cliprect.top) / channelHeight;
        long drawEndChannel = (long) (cliprect.bottom - timeScaleHeight) / channelHeight + 1;
        if (drawEndChannel > tempChannels.size() - 1)
            drawEndChannel = tempChannels.size() - 1;
        if (drawStartChannel < 0)
            drawStartChannel = 0;

        canvas.drawRect(cliprect.left, 0 + cliprect.top, cliprect.right, timeScaleHeight + cliprect.top, timescaleBgPaint);
        long drawTime = (int) drawStartTime / (15 * 60);
        drawTime = drawTime * 15 * 60;

        //линейка времени
        while (drawTime < drawEndTime) {
            int x = (int) ((drawTime - startTime) * scale / 3600);
            int stripeSize;
            if ((drawTime % 3600) == 0) {
                stripeSize = bigStripeSize;
                Date date1 = new Date();
                date1.setTime((long) drawTime * 1000);
                int hours = date1.getHours();
                canvas.drawText(hours + ":00", x, timeScaleHeight - stripeSize - (timeScaleHeight - stripeSize - textSize) / 2 + cliprect.top, timescaleTextPaint);
            } else {
                stripeSize = smallStripeSize;
            }
            canvas.drawLine(x, timeScaleHeight - stripeSize + cliprect.top, x, timeScaleHeight + cliprect.top, timescaleTextPaint);
            drawTime += 15 * 60;
        }
        //рассчёт времени в котором находится сетка
        Date date1 = new Date();
        date1.setTime(drawStartTime * 1000);

        String txt = date1.getDate() + ".";
        int mon = date1.getMonth() + 1;
        if (mon < 10) txt += "0";
        txt += mon;
        txt += "." + (date1.getYear() + 1900);
        if ((dateToShow == null) || (!dateToShow.equals(txt))) {
            dateToShow = txt;
            if (viewListener != null)
                viewListener.setCurrentDate(dateToShow);
        }
        //рисование программ
        if (drawStartChannel <= drawEndChannel && drawEndChannel != -1) {
            int channeltop = 0;
            canvas.clipRect(cliprect.left, cliprect.top + timeScaleHeight, cliprect.right, cliprect.bottom, Region.Op.REPLACE);

            int epgleft;
            int epgright;
            Rect oldcr = canvas.getClipBounds();

            for (long ch = drawStartChannel; ch <= drawEndChannel; ch++) {
                channeltop = (int) (timeScaleHeight + ch * channelHeight);
                chi = tempChannels.get((int) ch);

                //строки чередуются  двумя цветами
                colorChannelsItemsPaint.setColor(ch % 2 == 0 ? channelsBgColor : channelsBgColor1);

                for (Item epg : chi.getProgramsList()) {
                    if (epg.getEnd() < drawStartTime || epg.getStart() > drawEndTime)
                        continue;

                    epgleft = (int) ((epg.getStart() - startTime) * scale / 3600);
                    epgright = (int) ((epg.getEnd() - startTime) * scale / 3600);
                    //рисуется только то, что в видимой области
                    newrect = new Rect(epgleft, channeltop, epgright, channeltop + channelHeight);
                    if (newrect.left < cliprect.left) //!!!!!
                        newrect.left = cliprect.left;
                    if (newrect.top < cliprect.top + timeScaleHeight)
                        newrect.top = cliprect.top + timeScaleHeight;
                    if (newrect.bottom > cliprect.bottom) newrect.bottom = cliprect.bottom;
                    if (newrect.right > cliprect.right) newrect.right = cliprect.right;
                    canvas.clipRect(newrect, Region.Op.REPLACE);

                    rf = new RectF(epgleft, channeltop, epgright - 1, channeltop + channelHeight - 1);
                    canvas.drawRoundRect(rf, 1, 1, colorChannelsItemsPaint);

                    canvas.drawText(epg.getProgramTitle(), epgleft + dpToInt(5), channeltop + textSize + dpToInt(10), titlePaint);//название программы
                    canvas.drawText(epg.getProgramDescription(), epgleft + dpToInt(5), channeltop + textSize + dpToInt(10) + textSize + dpToInt(10), descPaint);//описание программы
                }


            }
            canvas.clipRect(oldcr, Region.Op.REPLACE);

            for (long ch = drawStartChannel; ch <= drawEndChannel; ch++) {
                channeltop = (int) (timeScaleHeight + ch * channelHeight);
                chi = tempChannels.get((int) ch);

                canvas.drawBitmap(drawableShadow, cliprect.left + channelScaleWidth, channeltop, channelBarPaint);            //тень под панелью с названиями каналов
                canvas.drawRect(cliprect.left, channeltop, cliprect.left + channelScaleWidth, channeltop + channelHeight, channelBarPaint);

                chIconBitmap = chi.getIcon();
                if (chIconBitmap != null)
                    canvas.drawBitmap(chIconBitmap, cliprect.left + 10, channeltop + channelHeight / 4, bitmapPaint); //иконки каналов
            }

            //индикатор реального времени
            int curtime = (int) (System.currentTimeMillis() / 1000);
            long curpos = ((long) curtime - startTime) * scale / 3600;

            if (curpos - cliprect.left > this.channelScaleWidth) {
                canvas.drawRect(curpos - 2, timeScaleHeight, curpos + 2, (this.tempChannels.size() * this.channelHeight + this.timeScaleHeight), currentTimeStripe);
            }
        }
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return (int) ((endtime - startTime) * scale / 3600) - this.getWidth() + this.channelScaleWidth;
    }

    @Override
    protected int computeVerticalScrollRange() {
        return this.tempChannels.size() * this.channelHeight + this.timeScaleHeight - this.getHeight();
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        boolean isEnabled = true;

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            int x = (int) event.getX() + getScrollX();
            int y = (int) event.getY() + getScrollY();

            Item item = findEpg(x, y);

            if (item != null) {
                if (viewListener != null)
                    viewListener.onGridItemClick(item);
            }
            return super.onSingleTapConfirmed(event);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (tempChannels.size() * channelHeight > getHeight()) {
                isEnabled = true;
            } else {
                isEnabled = false;
            }
            return super.onDown(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (getScrollY() + distanceY < 0)
                distanceY = -getScrollY();
            int h = tempChannels.size() * channelHeight + timeScaleHeight;
            int b = h - getHeight();
            if (getScrollY() + distanceY > b)
                distanceY = -getScrollY() + b;
            if (isEnabled) {
                scrollBy((int) distanceX, (int) distanceY);
            } else {
                scrollBy((int) distanceX, 0);
            }

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            int maxX = computeHorizontalScrollRange();
            int maxY = computeVerticalScrollRange();
            if (isEnabled) {
                scroller.fling(getScrollX(), getScrollY(), -(int) velocityX, -(int) velocityY, 0, maxX, 0, maxY);
            } else {
                scroller.fling(getScrollX(), 0, -(int) velocityX, -(int) velocityY, 0, maxX, 0, maxY);
            }
            awakenScrollBars(scroller.getDuration());

            return true;
        }
    }

    class GridTask extends TimerTask {
        @Override
        public void run() {
            if (isNeedToRefresh) {
                postInvalidate();
            }
        }
    }
}

