/*
 * Copyright (c) 2021. Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package vivek.com.sliddingpuzzle;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

public class GameSetting extends AppCompatActivity {

    static
    {
        System.loadLibrary("CLDriver");
    }

    private native static Bitmap gridImage(Bitmap src,int size);

    public static Context context_main;
    public ImageView img_origin;
    public int size = 0;
    public Bitmap img;
    public Bitmap grid_img;
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_setting);

        context_main = this;
        final EditText et_size = (EditText)findViewById(R.id.size_editText);
        Button btn_load = (Button)findViewById(R.id.image_load);
        Button btn_start = (Button)findViewById(R.id.start_Button);
        TextView txt_mv = (TextView)findViewById(R.id.size_textView);
        // 이미지 로드 확인용 임시 코드임. 원래 그 부분엔 앱 표지 화면을 넣을 예정.
        img_origin = (ImageView)findViewById(R.id.title_image);

        // 이미지 로드 버튼 이벤트
        btn_load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent,1);
            }
        });

        // 시작 버튼 이벤트
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (img_origin != null && size != 0) {
                    grid_img = gridImage(grid_img,size);
                    Intent gIntent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(gIntent);
                }

                else
                {
                    if (img_origin == null) {
                        Toast empty_msg = Toast.makeText(getApplicationContext(), "이미지를 입력해주세요", Toast.LENGTH_SHORT);
                        empty_msg.show();
                    }
                    else if (size == 0)
                    {
                        Toast empty_msg = Toast.makeText(getApplicationContext(), "퍼즐 크기를 입력해주세요", Toast.LENGTH_SHORT);
                        empty_msg.show();
                    }
                }
            }


        });

        // Edittext 입력으로 size 값 갱신
        et_size.addTextChangedListener(new TextWatcher() {


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (et_size.getText() != null) {
                    size = Integer.parseInt(et_size.getText().toString());
                }
                else
                    return;

            }
        });

    }

    // img_origin에 갤러리 사진을 입력
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1)
        {
            if (resultCode == RESULT_OK)
            {
                try
                {
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    img = BitmapFactory.decodeStream(in);
                    img = resizeBitmapImage(img,300);
                    img = resizeSquare(img,300);
                    grid_img = img;
                    in.close();
                    img_origin.setImageBitmap(img);

                } catch (Exception e){e.printStackTrace();}

            }
        }
    }

    // 정사각형 crop 함수
    public static Bitmap resizeSquare(Bitmap src, int max) {
        if(src == null)
            return null;

        return Bitmap.createScaledBitmap(src, max, max, true);
    }

    // 이미지 resizing 함수
    public Bitmap resizeBitmapImage(Bitmap source, int maxResolution)
    {
        int width = source.getWidth();
        int height = source.getHeight();
        int newWidth = width;
        int newHeight = height;
        float rate = 0.0f;

        if(width > height)
        {
            if(maxResolution < width)
            {
                rate = maxResolution / (float) width;
                newHeight = (int) (height * rate);
                newWidth = maxResolution;
            }
        }
        else
        {
            if(maxResolution < height)
            {
                rate = maxResolution / (float) height;
                newWidth = (int) (width * rate);
                newHeight = maxResolution;
            }
        }

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }
}
