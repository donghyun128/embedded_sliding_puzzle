/*
 * Copyright (c) 2016. Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package vivek.com.sliddingpuzzle.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import vivek.com.sliddingpuzzle.MainActivity;
import vivek.com.sliddingpuzzle.R;

public class TileItem extends ImageView {

    private Position startingPosition;
    private Position currentPosition;
    private boolean hint_used;
    private Bitmap tile_bitmap;
    Boolean isBlank = false;

    public TileItem(Context context ) {
        super(context);
    }

    public RelativeLayout.LayoutParams setLayout() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                MainActivity.boardWidth/MainActivity.numberOfRows,
                MainActivity.boardWidth/MainActivity.numberOfRows
        );
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        params.leftMargin = (currentPosition.getxAxis() * (MainActivity.boardWidth/MainActivity.numberOfRows));
        params.topMargin = (currentPosition.getyAxis() * (MainActivity.boardWidth / MainActivity.numberOfRows));

        return params;
    }


    public void swapPositionWith(Position itemPosition) {
        int oldLeftMargin = (currentPosition.getxAxis() * (MainActivity.boardWidth/MainActivity.numberOfRows));
        int oldTopMargin = (currentPosition.getyAxis() * (MainActivity.boardWidth / MainActivity.numberOfRows));

        setCurrentPosition(itemPosition);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();

        int newleftMargin = (currentPosition.getxAxis() * (MainActivity.boardWidth/MainActivity.numberOfRows));
        int newTopMargin = (currentPosition.getyAxis() * (MainActivity.boardWidth / MainActivity.numberOfRows));

        if(newleftMargin == oldLeftMargin){
            if(oldTopMargin > newTopMargin) {
                for(int i = oldTopMargin; i >= newTopMargin; i--) {
                    params.leftMargin = oldLeftMargin;
                    params.topMargin = i;
                    setLayoutParams(params);

                }
            } else {
                for(int i = oldTopMargin; i<= newTopMargin; i++) {
                    params.leftMargin = oldLeftMargin;
                    params.topMargin = i;
                    setLayoutParams(params);
                }
            }
        } else if(newTopMargin == oldTopMargin) {
            if(oldLeftMargin < newleftMargin) {
                for(int i = oldLeftMargin; i<= newleftMargin; i++) {
                    params.leftMargin = i;
                    params.topMargin = oldTopMargin;
                    setLayoutParams(params);
                }
            } else {
                for(int i = oldLeftMargin; i>=newleftMargin; i--) {
                    params.leftMargin = i;
                    params.topMargin = oldTopMargin;
                    setLayoutParams(params);
                }
            }
        }
    }

    public void setHint_used(boolean a)
    {
        hint_used = a;
    }

    public boolean getHint_used()
    {
        return hint_used;
    }

    public void setImage(Bitmap image) {
        if(image == null) {
            setBackgroundColor(getContext().getResources().getColor(R.color.white));
            setAlpha(0);
            isBlank = true;
        } else {
            setImageBitmap(image);
            tile_bitmap = image;
        }

    }

    public Bitmap getImage()
    {
        return tile_bitmap;
    }


    public Position getStartingPosition() {
        return startingPosition;
    }

    public void setStartingPosition(Position startingPosition) {
        this.startingPosition = startingPosition;
    }

    public Position getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Position currentPosition) {
        this.currentPosition = currentPosition;
    }

    public void setDimension(int width) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, width);
        setLayoutParams(params);
    }

    public Boolean getIsBlank() {
        return isBlank;
    }

    public void setIsBlank(Boolean isBlank) {
        this.isBlank = isBlank;
    }

    @Override
    public String toString() {
        return "TileItem{" +
                "isBlank=" + isBlank +
                '}';
    }

    public boolean isLeftOf(TileItem matchTile) {

        return (currentPosition.yAxis == matchTile.currentPosition.yAxis
                && currentPosition.xAxis == matchTile.currentPosition.xAxis - 1);
    }

    public boolean isRightOf(TileItem matchTile) {
        return  (currentPosition.yAxis == matchTile.currentPosition.yAxis
                && currentPosition.xAxis == matchTile.currentPosition.xAxis + 1);
    }

    public boolean isbelowOf(TileItem matchTile) {
        return (currentPosition.xAxis == matchTile.currentPosition.xAxis
                && currentPosition.yAxis == matchTile.currentPosition.yAxis+1);
    }

    public boolean isAboveOf(TileItem matchTile) {
        return (currentPosition.xAxis == matchTile.currentPosition.xAxis
                && currentPosition.yAxis == matchTile.currentPosition.yAxis-1);
    }
}
