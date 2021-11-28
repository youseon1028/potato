package com.example.potato;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MusicActivity extends AppCompatActivity implements View.OnClickListener  {


    private static final int REQUEST_ENABLE_BT = 100;
    BluetoothAdapter bluetoothAdapter;

    public String log_name = "musicplayer";
    public ListView listView;
    private String songNames[];
    private ArrayList<Uri> songs;
    private String title = "";

    static private TextView titleTv;
    static public Button btnPlay, forwardIv, backwardIv;
    private Button btnBack, btnFor;

    static public SeekBar seekBar;
    static public MediaPlayer mediaPlayer;
    static private int pos = -1;       //노래 포지션

    static public boolean bearPhoneOneCheck = false;    //bearPhoneOneCheck 이어폰이 한번 뽑히면 true, 그렇지 않으면 false
    static public int CheckearPhone = 0;   //이어폰을 빼면 0, 연결되어 있으면 1이상 값
    static public boolean bpause = false;    //일시 정지 버튼을 눌렀다면, true
    static public int repeatPlayMode = 0;  //반복재생 모드 0: 전곡 반복 재생 / 1: 한곡 반복 재생

    private Runnable runnable;

    private Handler handler;

    private Uri uri;

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_acitivity);

        titleTv = findViewById(R.id.titleTv);
        listView = findViewById(R.id.listView);



        Button Button2 = (Button) findViewById(R.id.button2);
        Button2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                startActivity(intent);
            }


        });

        btnPlay = findViewById(R.id.btnPlay);
        btnBack = findViewById(R.id.btnBack);
        btnFor = findViewById(R.id.btnFor);
        seekBar = findViewById(R.id.seekbar);
        forwardIv = findViewById(R.id.forwardIv);
        backwardIv = findViewById(R.id.backwardIv);

        handler = new Handler();

        btnFor.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        forwardIv.setOnClickListener(this);
        backwardIv.setOnClickListener(this);



        //음악이 재생중이였다면 seekBar, 노래제목 표시하기
        if(mediaPlayer != null)
        {
            if(mediaPlayer.isPlaying())
            {
                if(!title.equals("")){
                    titleTv.setSelected(true);
                    titleTv.setText(title);
                }

                seekBar.setMax(mediaPlayer.getDuration());
                seekBar.setVisibility(ProgressBar.VISIBLE);
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                btnPlay.setText("||");
            }
            else if(!mediaPlayer.isPlaying())
            {
                if(!title.equals("")){
                    titleTv.setSelected(true);
                    titleTv.setText(title);
                }

                seekBar.setMax(mediaPlayer.getDuration());
                seekBar.setVisibility(ProgressBar.VISIBLE);
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                btnPlay.setText(">");
            }

        }

        // MP3 경로를 가질 문자열 배열.
        String[] resultPath = null;

        String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        // 찾고자하는 파일 확장자명.
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3");

        String[] selectionArgsMp3 = new String[]{ mimeType };

        Cursor c = null;

        c = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME}, selectionMimeType, selectionArgsMp3, null);


        if (c.getCount() != 0){

            ArrayList<Uri> arrayList = new ArrayList<Uri>();
            songNames = new String[c.getCount()];

            //커서로 조회한 mp3 상대경로 와 mp3 제목을 저장한다.
            while (c.moveToNext()) {

                //음악파일이 저장되어 있는 상대 경로를 uri로 저장.
                Uri contentUri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                );

                //음악이 저장되어 있는 상대 경로 저장
                arrayList.add(contentUri);

                //음악 제목 저장
                songNames[c.getPosition()] = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)).replace(".mp3", "");

                Log.d(log_name, contentUri.toString());
                Log.d(log_name, songNames[c.getPosition()]);
            }

            Log.d(log_name, String.valueOf(c.getCount()));
            Log.d(log_name, String.valueOf(arrayList.size()));
            songs = arrayList;

        }

        //음악 리스트를 위한 어댑터 구현
        //listview => song_layout textView 위젯에 음악 개수 만큼 음악 제목을 표시한다.
        ArrayAdapter<String> adaper = new ArrayAdapter<String>(getApplicationContext(), R.layout.song_layout
                , R.id.textView, songNames);

        listView.setAdapter(adaper);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                pos = position;

                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }

                titleTv.setSelected(true);
                titleTv.setText(songNames[pos]);

                title = (String)titleTv.getText();

                mediaPlayer = MediaPlayer.create(MusicActivity.this, songs.get(pos));

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        //곡을 클릭하면 곡의 전체시간을 seekBar에 Max값으로 넣는다.
                        seekBar.setMax(mp.getDuration());
                        mediaPlayer.start();
                        bearPhoneOneCheck = true;
                        changeSeekbar();
                    }
                });

            }
        });




        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    //seekBar를 누르면 눌린 시간으로 mp3 시간을 이동
                    mediaPlayer.seekTo(progress);
                }
                int m = progress / 60000;
                int s = (progress % 60000) / 1000;
                String strTime = String.format("%02d:%02d", m, s);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void changeSeekbar(){

        //현재 mp3 재생시간을 seekBar에 넣는다.
        seekBar.setProgress(mediaPlayer.getCurrentPosition());

    }

    @Override
    public void onClick(View v) {
        if(mediaPlayer != null) {

            switch (v.getId()) {
                case R.id.btnPlay:
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        bpause = true;
                        btnPlay.setText(">");
                    } else {
                        mediaPlayer.start();
                        btnPlay.setText("||");
                        bpause = false;
                        bearPhoneOneCheck = true;
                        Thread();
                        changeSeekbar();
                    }
                    break;

                case R.id.btnFor:
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 5000);
                    break;

                case R.id.btnBack:
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 5000);
                    break;

                case R.id.forwardIv:
                    if(pos != -1 && songNames.length - 1 > pos)
                    {
                        pos += 1;
                        playMusic(pos);
                    }
                    else if(songNames.length - 1 == pos)
                    {
                        pos = 0;
                        playMusic(pos);
                    }
                    break;

                case R.id.backwardIv:
                    if(pos > 0)
                    {
                        pos -= 1;
                        playMusic(pos);
                    }
                    else if(pos == 0)
                    {
                        pos = songNames.length - 1;
                        playMusic(pos);
                    }
                    break;

            }
        }
    }

    private void playMusic(int position)
    {

        Log.d(log_name,"playMusic position: " + position);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        Log.d(log_name,"playMusic uri: " + songs.get(position).toString());

        titleTv.setSelected(true);
        titleTv.setText(songNames[position]);

        title = (String)titleTv.getText();

        mediaPlayer = MediaPlayer.create(MusicActivity.this, songs.get(position));

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //곡을 클릭하면 곡의 전체시간을 seekBar에 Max값으로 넣는다.
                seekBar.setMax(mp.getDuration());
                mediaPlayer.start();
                changeSeekbar();

            }
        });
    }

    public void Thread() {
        Runnable task = new Runnable() {


            public void run() {
                // 음악이 재생중일때
                while (mediaPlayer.isPlaying()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                }
            }
        };
        Thread thread = new Thread(task);
        thread.start();


    }


}