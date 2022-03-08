package vivek.com.sliddingpuzzle;

import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import vivek.com.sliddingpuzzle.model.Position;
import vivek.com.sliddingpuzzle.model.TileItem;
import vivek.com.sliddingpuzzle.utils.BitmapSplitter;
import vivek.com.sliddingpuzzle.utils.DeviceProperty;
import vivek.com.sliddingpuzzle.R;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener{

    // 드라이버 로드
    static
    {
        System.loadLibrary("Segment");
        System.loadLibrary("CLDriver");
        System.loadLibrary("ButtonDriver");
        System.loadLibrary("LEDDriver");
    }
    // native 함수
    // segment
    private native static int openSegment(String path);
    private native static void closeSegment();
    private native static void writeSegment(byte[] data,int length);
    // button
    private native static int openButtonDriver(String path);
    private native static char readButtonDriver();
    private native static void closeButtonDriver();
    private native static int clickButton();
    // LED
    private native static int openLEDDriver(String path);
    private native static int closeLEDDriver();
    private native static void writeLEDDriver(byte [] data, int length);

    Bitmap img = ((GameSetting)GameSetting.context_main).img;
    public static int size = ((GameSetting)GameSetting.context_main).size;
    RelativeLayout fullBoardView;
    Bitmap[][] bitmapTiles;
    LinkedHashMap<Integer, TileItem> puzzleItemList;
    LinkedHashMap<Integer, TileItem> shuffledTiles;
    TileItem emptyTile;
    Button ViewOriginalImage;
    ImageView originImage;
    RectF moveableBoundary;
    EditText stepCountView;
    EditText direction;
    EditText itemCount;
    Point deviceDimension;
    int touchPositionX, touchPositionY;
    public static int boardWidth;
    public static int numberOfRows = size;
    public static int PUZZLE_BOARD_LEFT_MARGIN = 20;
    private int tileSize;
    private int stepCount=0;
    int i;
    boolean mThreadRun , mStart;
    private TranseThread mTranseThread;
    private boolean mConnectFlag = false;
    SegmentThread mSegThread;
    public int dir;
    public int match = 0;
    int n_item = 8;
    byte [] item_arr = {1,1,1,1,1,1,1,1};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get the dimension of the device
        deviceDimension = DeviceProperty.getDeviceDimension(MainActivity.this);

        //Get the width of puzzle board leaving equal margin on left and right
        boardWidth = deviceDimension.x - 2*PUZZLE_BOARD_LEFT_MARGIN;
        tileSize = boardWidth/numberOfRows;

        //View to display the number of counts
        stepCountView = (EditText) findViewById(R.id.puzzleStepCounts);
        direction = (EditText) findViewById(R.id.direction);
        itemCount = (EditText) findViewById(R.id.item);
        //Get the Puzzle board and initialize its parameter
        fullBoardView = (RelativeLayout) findViewById(R.id.puzzleFullBoardView);
        LinearLayout.LayoutParams boardParam = new LinearLayout.LayoutParams(boardWidth,boardWidth);
        boardParam.leftMargin = PUZZLE_BOARD_LEFT_MARGIN;
        boardParam.rightMargin = PUZZLE_BOARD_LEFT_MARGIN;
        fullBoardView.setLayoutParams(boardParam);

        // 섞이기 전의 이미지를 보여주는 ImageView. VIEW ORGINAL BUTTON을 누르면 볼 수 있음
        originImage = (ImageView) findViewById(R.id.originalImage);
        originImage.setImageBitmap(img);
        originImage.setLayoutParams(boardParam);

        //VIEW ORIGINAL BUTTON 버튼 생성
        ViewOriginalImage = (Button) findViewById(R.id.originalImageButton);
        ViewOriginalImage.setOnTouchListener(this);

        //Create the sliced bitmap from provided Image
        bitmapTiles = this.createTileBitmaps();

        //Initialize the list of puzzle tiles with sliced bitmaps
        puzzleItemList = this.initializePuzzleTiles(bitmapTiles);

        //Shuffle the tile and render on play board
        this.shuffleAndRenderTiles(puzzleItemList);

        // GPIO button,LED device driver open
        if (open("/dev/sm9s5422_interrupt")<0)
        {
            Toast.makeText(MainActivity.this,"Driver Open Failed",Toast.LENGTH_SHORT);
        }

        if (openLEDDriver("/dev/sm9s5422_led")<0)
        {
            Toast.makeText(MainActivity.this,"Driver Open Failed",Toast.LENGTH_SHORT);
        }

        // 아이템 수를 나타내는 LED 8개를 전부 킨다. 주어지는 아이템 수는 8개이다.
        writeLEDDriver(item_arr,item_arr.length);

        // Step 수가 바뀌었을 때 7-segment로 그 값을 표시한다.
        stepCountView.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            try
            {
                mStart = true;
            } catch (NumberFormatException E)
            {
                Toast.makeText(MainActivity.this, "Input Error",Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    });

        // 특정 버튼을 눌러 다시 섞는 기능을 사용한다. 기능 사용시, 아이템 수가 차감된다. 아이템 수가 0이면 사용할 수 없다.
         direction.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (n_item >0) {
                    switch (dir) {

                        case 1:
                            hint();
                            n_item--;
                            itemCount.setText(String.valueOf(n_item));
                            break;

                        case 5:
                            fullBoardView.removeAllViews();
                            initializePuzzleTiles(createTileBitmaps());
                            shuffleAndRenderTiles(puzzleItemList);
                            n_item--;
                            itemCount.setText(String.valueOf(n_item));
                        default:
                            return;
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

         // n_item의 값이 변경되었을 때 LED로 남은 아이템의 수를 표시해준다. LED가 전부 꺼지면 아이템은 사용할 수 없다.
         itemCount.addTextChangedListener(new TextWatcher() {
             @Override
             public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

             }

             @Override
             public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                 switch (n_item)
                 {
                     case 8:
                         break;
                     case 7:
                         item_arr[7] = 0;
                         break;
                     case 6:
                         item_arr[6] = 0;
                         break;
                     case 5:
                         item_arr[5] = 0;
                         break;
                     case 4:
                         item_arr[4] = 0;
                         break;
                     case 3:
                         item_arr[3] = 0;
                         break;
                     case 2:
                         item_arr[2] = 0;
                         break;
                     case 1:
                         item_arr[1] = 0;
                         break;
                     case 0:
                         item_arr[0] = 0;
                         break;
                 }
                 writeLEDDriver(item_arr,item_arr.length);
             }

             @Override
             public void afterTextChanged(Editable editable) {

             }
         });

    }


    // 수동으로 터치 이벤트를 발생 시키는 코드. 버튼 입력시 발생되도록 하였으나 실행되지 않았다.
    void TouchByCode(View v,float x,float y)
    {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        int metaState = 0;
        MotionEvent motionEvent = MotionEvent.obtain( downTime, eventTime, MotionEvent.ACTION_UP, x, y, metaState );
        v.dispatchTouchEvent(motionEvent);
    }

    // 이미지를 2차원 Bitmap배열로 나눠주는 함수
    private Bitmap[][] createTileBitmaps() {
        //Bitmap image = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.imagepuzzle2);
        return BitmapSplitter.split(img, boardWidth, numberOfRows);
    }

    // 퍼즐을 셔플하는 함수
    private LinkedHashMap<Integer, TileItem> shuffleTiles(LinkedHashMap<Integer, TileItem> puzzleTile) {
        //Remove the list piece
        emptyTile = puzzleTile.get(puzzleTile.size()-1);
        emptyTile.setImage(null);
        emptyTile.setIsBlank(true);

        //shuffle
        List keys = new ArrayList(puzzleTile.keySet());
        List notEmpty_keys = keys.subList(0, numberOfRows*numberOfRows-2);
        Collections.shuffle(notEmpty_keys);

        LinkedHashMap<Integer, TileItem> shuffledTile = new LinkedHashMap<>();
        int i=0;
        for (Object o: keys) {
            TileItem item = puzzleTile.get(o);
            int xAxis = (i < numberOfRows) ? i : (i % numberOfRows);

            int yAxis=0;
            for (int k = 0; k < numberOfRows; k++) {
                if (i >= k * numberOfRows && i <= (k + 1) * numberOfRows - 1)
                    yAxis = k;


            }

            item.setCurrentPosition(new Position(xAxis, yAxis));
            shuffledTile.put((int) o, puzzleTile.get(o));
            i++;

        }
        return shuffledTile;
    }

    // split된 이차원 배열 Bitmap을 입력으로 받음
    // TileItem : 퍼즐이 원래 있어야할 위치(StartingPostion), 현재 위치(CurrentPosition), Layout 내에서의 위치, 퍼즐에 그려진 그림 정보를 가지고 있음.
    // TileItem의 정보를 초기화함.
    // 정수를 key로 가지고, TileItem을 Value로 가지는 LinkedHashMap을 반환
    private LinkedHashMap<Integer, TileItem> initializePuzzleTiles(Bitmap[][] bitmapTiles) {
        LinkedHashMap<Integer, TileItem> puzzleItem = new LinkedHashMap<>();
        int tileWidth = (boardWidth/numberOfRows);
        int bitmapPosition = 0;

        for (int i = 0; i < numberOfRows; i++) {
            for(int j = 0; j< numberOfRows; j++) {
                TileItem tile = new TileItem(getApplicationContext());
                tile.setId(bitmapPosition);
                tile.setStartingPosition(new Position(j, i));
                tile.setImage(bitmapTiles[i][j]);
                tile.setDimension(tileWidth);
                tile.setOnTouchListener(this);
                puzzleItem.put(bitmapPosition++, tile);
            }
        }
        return puzzleItem;
    }

    // 셔플한 퍼즐들을 레이아웃에 그리는 함수
    private void shuffleAndRenderTiles(LinkedHashMap<Integer, TileItem> puzzleItem) {
        shuffledTiles = this.shuffleTiles(puzzleItem);

        for(Map.Entry<Integer, TileItem> entry: shuffledTiles.entrySet()) {
            TileItem item = entry.getValue();
            fullBoardView.addView(item, item.setLayout());
        }

    }

    // 터치를 하여 슬라이드 퍼즐을 움직임
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v.getId() == R.id.originalImageButton) {
            displayOriginalImage(event);
            return true;
        }

        Log.d("onTouch","onTouch");

        TileItem selectedTile = (TileItem) v;
        //Do nothing if the tile is blank
        if(selectedTile.getIsBlank()) {
            return false;
        }

        switch(event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                if(!checkIfValidMove(selectedTile))
                    return false;
                touchPositionX = (int) event.getRawX();
                touchPositionY = (int) event.getRawY();
                selectedTile.bringToFront();
                moveableBoundary = getMoveableBoundry(selectedTile);
                break;

            case MotionEvent.ACTION_MOVE:
                dragTilesAround(selectedTile, event);
                touchPositionX = (int) event.getRawX();
                touchPositionY = (int) event.getRawY();

                break;


            case MotionEvent.ACTION_UP:
                if(tileDraggedMoreThenHalfWay(selectedTile) || isJustClick(selectedTile)) {
                    swapTileWithEmpty(selectedTile);
                    increaseStepCounts();


                } else {
                    bringTileToOriginalPostion(selectedTile);
                }
                Log.v("position: ", "current x_position : " + (float)selectedTile.getCurrentPosition().getxAxis() );
                Log.v("position","current y_position : "+ (float)selectedTile.getCurrentPosition().getyAxis());
                Log.v("position","starting x_position : " + (float)selectedTile.getStartingPosition().getxAxis());
                Log.v("position","starting y_position : " + (float)selectedTile.getStartingPosition().getyAxis());
                break;

        }

        match = 0;
        Log.d("match Initialize","match Initialize");

        // Clear 가능한지 체크한다.
        for(Map.Entry<Integer, TileItem> entry: shuffledTiles.entrySet()) {

            if (entry.getValue().getStartingPosition().getxAxis() == entry.getValue().getCurrentPosition().getxAxis() && entry.getValue().getStartingPosition().getyAxis() == entry.getValue().getCurrentPosition().getyAxis())
            {
                match++;
            }
        }
        Log.v("match count","match : " + match);
        if (match >= numberOfRows * numberOfRows -1)
        {
            fullBoardView.setVisibility(View.GONE);
            originImage.setVisibility(View.VISIBLE);
            ViewOriginalImage.setText("CLEAR");
            Toast.makeText(MainActivity.this,"Game CLEAR!!",Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /**
     * Check if the tile can make a move, that is,
     * if the tile has blank space to its surrounding
     * @param selectedItem
     * @return
     */
    public boolean checkIfValidMove(TileItem selectedItem) {

        return (selectedItem.isAboveOf(emptyTile)
                || selectedItem.isbelowOf(emptyTile)
                || selectedItem.isLeftOf(emptyTile)
                || selectedItem.isRightOf(emptyTile));
    }

    /**
     * Swap the position of tile and empty space
     * @param selectedItem
     */
    public void swapTileWithEmpty(TileItem selectedItem) {
        Position selectedItemPosition = selectedItem.getCurrentPosition();
//
//        ObjectAnimator animator = ObjectAnimator.ofObject(selectedItem, "",
//                new FloatEvaluator(), )
        selectedItem.swapPositionWith(emptyTile.getCurrentPosition());
        emptyTile.swapPositionWith(selectedItemPosition);

    }


    /**
     * In case the swap fails or is illegal move,
     * we bring the selected tile to its original postion
     * @param selectedItem
     */
    public void bringTileToOriginalPostion(TileItem selectedItem) {
        selectedItem.setLayoutParams(selectedItem.setLayout());
    }

    /**
     * Check if the tile has move halfway to the empty tiles
     * @param selectedItem
     * @return
     */
    private boolean tileDraggedMoreThenHalfWay(TileItem selectedItem) {

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) selectedItem.getLayoutParams();
        int originalMargin = 0, currentMargin = 0;

        if(selectedItem.isbelowOf(emptyTile) || selectedItem.isAboveOf(emptyTile)) {
            originalMargin = selectedItem.getCurrentPosition().getyAxis() * tileSize;
            currentMargin = params.topMargin;
        } else {
            originalMargin = selectedItem.getCurrentPosition().getxAxis() * tileSize;
            currentMargin = params.leftMargin;
        }

        if(Math.abs(originalMargin - currentMargin) >= tileSize/2) {
            return true;
        }
        return false;
    }

    /**
     * Check if the touch even was just click
     * @param selectedItem
     * @return
     */
    private boolean isJustClick(TileItem selectedItem) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) selectedItem.getLayoutParams();
        int originalTopMargin = selectedItem.getCurrentPosition().getyAxis() * tileSize;
        int currentTopMargin = params.topMargin;

        int originLeftMargin = selectedItem.getCurrentPosition().getxAxis() * tileSize;
        int currentLeftMargin = params.leftMargin;

        /**
         * Many times clicking move ths side, so to add the better experience
         * we keep 5pixels plus minus.
         */
        return (Math.abs(originalTopMargin - currentTopMargin) < 5
                && (originLeftMargin - currentLeftMargin) < 5);
    }

    /**
     * Display the original image during button press and hide after release
     * @param event
     */
    public void displayOriginalImage(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                fullBoardView.setVisibility(View.GONE);
                originImage.setVisibility(View.VISIBLE);
                break;
            case MotionEvent.ACTION_UP:
                fullBoardView.setVisibility(View.VISIBLE);
                originImage.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Dragging the selected tiles
     * @param selectedTile
     * @param event
     */
    public void dragTilesAround(TileItem selectedTile, MotionEvent event) {
        int xCoordinate = (int) event.getRawX() - touchPositionX;
        int yCoordinate = (int) event.getRawY() - touchPositionY;

        //Get the current boundary of selected tile.
        RectF selectedTileBoundary = getSelectedTileBoundary(selectedTile);

        /**
         * on coordinate might go side ways duing drag, so if its not within the
         * moveableBoundary, do not render the layout
         */
        if(moveableBoundary.contains(selectedTileBoundary)) {

            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) selectedTile.getLayoutParams();
            param.height = tileSize;
            param.width = tileSize;
            if(selectedTile.isRightOf(emptyTile) || selectedTile.isLeftOf(emptyTile)) {
                param.leftMargin = xCoordinate + param.leftMargin;
            } else {
                param.topMargin = yCoordinate + param.topMargin;
            }
            selectedTile.setLayoutParams(param);
        }

    }

    /**
     * Calculate the total boundary on where slide can move
     * summation of selected slide and empty slide
     * @param selectedItem
     * @return
     */
    public RectF getMoveableBoundry(TileItem selectedItem) {
        int boardTop = (int) Math.floor(fullBoardView.getY());
        int boardLeft = (int) Math.floor(fullBoardView.getX());

        int emptyTop = (emptyTile.getCurrentPosition().getyAxis() * tileSize)+boardTop;
        int emptyLeft = (emptyTile.getCurrentPosition().getxAxis() * tileSize)+boardLeft;
        int emptyRght = emptyLeft + tileSize;
        int emptyButtom = emptyTop +tileSize;

        int selectedItemTop = (selectedItem.getCurrentPosition().getyAxis() * tileSize)+boardTop;
        int selectedItemLeft = (selectedItem.getCurrentPosition().getxAxis() * tileSize)+boardLeft;
        int selectedItemRght = selectedItemLeft + tileSize;
        int selectedItemButtom = selectedItemTop + tileSize;

        int left = (emptyLeft <= selectedItemLeft) ? emptyLeft: selectedItemLeft;
        int top = (emptyTop <= selectedItemTop) ? emptyTop: selectedItemTop;
        int right = (emptyRght >= selectedItemRght) ? emptyRght : selectedItemRght;
        int buttom = (emptyButtom >= selectedItemButtom) ? emptyButtom : selectedItemButtom;

        return new RectF(left, top, right, buttom);
    }

    /**
     * Calculate the total boundary of selected Slide at current position
     * Used to measure the boundary when slide is being dragged
     * @param selectedItem
     *
     * @return
     */
    public RectF getSelectedTileBoundary(TileItem selectedItem) {
        int boardTop = (int) Math.floor(fullBoardView.getY());
        int boardLeft = (int) Math.floor(fullBoardView.getX());

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) selectedItem.getLayoutParams();
        int selectedItemLeft = boardLeft + params.leftMargin;
        int selectedItemTop = boardTop + params.topMargin;
        int selectedItemRght = selectedItemLeft + tileSize;
        int selectedItemButtom = selectedItemTop + tileSize;

        return new RectF(selectedItemLeft, selectedItemTop, selectedItemRght, selectedItemButtom);
    }
    // Step의 수를 1 증가시키는 함수
    public void increaseStepCounts() {
        stepCount++;
        stepCountView.setText(String.valueOf(stepCount));
    }

    @Override
    protected void onPause(){
        closeSegment();
        closeLEDDriver();
        mThreadRun=false;
        mSegThread=null;
        super.onPause();
    }

    @Override
    protected void onResume(){
        if(openSegment("dev/sm9s5422_segment")<0){
            Toast.makeText(MainActivity.this,"Driver Open Failed",Toast.LENGTH_SHORT).show();
        }
        if (openLEDDriver("/dev/sm9s5422_led")<0){
            Toast.makeText(MainActivity.this, "Driver Open Failed", Toast.LENGTH_SHORT).show();
        }
        mThreadRun=true;
        mSegThread= new SegmentThread();
        mSegThread.start();
        super.onResume();
    }
    // 버튼 드라이버 핸들러의 메시지를 받는 함수
    public void onReceive(int val)
    {
        Message text = Message.obtain();
        text.arg1 = val;
        dir = text.arg1;
        Log.v("dir","dir" + dir);
        handler.sendMessage(text);
    }

    // 버튼 드라이버 스레드 함수 핸들러
    public Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            Log.v("Handle","msg : " + msg.arg1);
            switch(msg.arg1)
            {
                case 1:
                    direction.setText("1");
                    break;
                case 2:
                    direction.setText("2");
                    break;
                case 3:
                    direction.setText("3");
                    break;
                case 4:
                    direction.setText("4");
                    break;
                case 5:
                    direction.setText("5");
                    break;
            }
        }
    };

    // segment 스레드 함수
    private class SegmentThread extends Thread{
        @Override
        public void run() {
            super.run();
            while(mThreadRun){
                byte[] n = {0,0,0,0,0,0,0};

                if (mStart == false) {writeSegment(n,n.length);}
                else{

                    for (i=0; i<100; i++){
                        n[0] = (byte) (stepCount % 1000000 / 100000);
                        n[1] = (byte) (stepCount % 100000 / 10000);
                        n[2] = (byte) (stepCount % 10000 / 1000);
                        n[3] = (byte) (stepCount % 1000 / 100);
                        n[4] = (byte) (stepCount % 100 / 10);
                        n[5] = (byte) (stepCount % 10 );
                        writeSegment(n,n.length);
                    }

                }
            }

        }
    }
    // 버튼 드라이버 open 함수
    public int open(String driver)
    {
        if (mConnectFlag) return -1;

        if (openButtonDriver(driver)>0)
        {
            mConnectFlag = true;
            mConnectFlag = true;
            mTranseThread = new TranseThread();
            mTranseThread.start();
            return 1;
        }
        else
            return -1;
    }
    // 버튼 드라이버 close 함수
    public void close()
    {
        if (!mConnectFlag) return;
        mConnectFlag = false;
        closeButtonDriver();
    }
    // 버튼 드라이버 finalize() 함수
    protected void finalize() throws Throwable
    {
        close();
        super.finalize();
    }
    // 버튼 드라이버 스레드 함수
    private class TranseThread extends Thread
    {
        public void run()
        {
            super.run();
            try
            {
                while(mConnectFlag)
                {
                    try
                    {
                        Log.d("test","1");
                        //dir = clickButton();
                        onReceive(clickButton());
                        Thread.sleep(1000);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }

                }
            } catch(Exception e)
            {
            }
        }
    }
    
    // 한 퍼즐 조각을 원래 위치로 되돌리는 함수
    void hint()
    {
        dir = 0;
        int isSwapped = 0;
        for(Map.Entry<Integer, TileItem> entry: shuffledTiles.entrySet()) {
            // selectedTile은 원래 위치로 보내려는 타일
            TileItem selectedTile = entry.getValue();
            // selectedTile이 공백 타일인지, 이전에 위치가 바뀐 적이 있는지 check
            if (!selectedTile.getHint_used() && selectedTile != emptyTile)
            {
                // selectedTile이 원래 있어야 할 위치가 아닌 다른 곳에 있는지 check
                if (selectedTile.getStartingPosition().getxAxis() != selectedTile.getCurrentPosition().getxAxis() || selectedTile.getStartingPosition().getyAxis() != selectedTile.getCurrentPosition().getyAxis())
                {

                    Position selectedItemPosition = selectedTile.getCurrentPosition();

                    for (Map.Entry<Integer, TileItem> entry_2 : shuffledTiles.entrySet()) {
                        // e2 : selectedTile과 자리를 바꾸려는 타일
                        TileItem e2 = entry_2.getValue();
                        // e2가 공백타일인지 체크
                        if (e2 != emptyTile) {
                            Log.v("a", "entry1: " + selectedTile.getStartingPosition().getxAxis());
                            Log.v("a", "entry1: " + selectedTile.getStartingPosition().getyAxis());
                            Log.v("a", "entry1: " + entry_2.getValue().getCurrentPosition().getxAxis());
                            Log.v("a", "entry1: " + entry_2.getValue().getCurrentPosition().getyAxis());


                                if (e2.getStartingPosition().getxAxis() != numberOfRows - 1 && e2.getStartingPosition().getyAxis() != numberOfRows - 1) {
                                    selectedTile.setHint_used(true);
                                    // 서로 바뀌는 타일끼리 bitmap image, 위치정보(CurrentPosition)을 서로 바꿈.
                                    Bitmap tmp = selectedTile.getImage();
                                    selectedTile.setImage(entry_2.getValue().getImage());
                                    e2.setImage(tmp);
                                    // swapPositionWith()를 통해 위치 정보와 layout 위치를 바꿈.
                                    selectedTile.swapPositionWith(entry_2.getValue().getCurrentPosition());
                                    e2.swapPositionWith(selectedItemPosition);
                                }
                                isSwapped = 1;
                                break;

                        }
                    }
                }
            }
            if (isSwapped == 1)
            {
                break;
            }
        }
    }



}
