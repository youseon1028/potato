package com.example.potato;


import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.imageview.ShapeableImageView;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import petrov.kristiyan.colorpicker.ColorPicker;


public class LedAcitivity extends AppCompatActivity {
    ImageView colorPick; // 색상 선택하는 이미지
    ShapeableImageView selectedColor; // 선택된 색상 보여주는 이미지
    TextView currentState; // 현재 색상과 밝기 표시할 텍스트뷰
    SeekBar bar; // 밝기 조절할 시크바
    ImageButton btnRainbow; // 레인보우 버튼
    ImageButton btnPower; // 전원 버튼
    ImageButton btnColorPicker; // 컬러피커 버튼
    ImageButton btnConnect; // 블루투스 재접속 버튼

    byte[] colors = new byte[5];
    boolean powerOn = false; // 전원이 켜졌는지 여부

    String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    private static final int REQUEST_ENABLE_BT = 100;
    private static final int REQUEST_PERMISSIONS = 101;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // 블루투스 어댑터
    BluetoothDevice bluetoothDevice;    // 블루투스 장치
    Set<BluetoothDevice> pairedDevices; // 페어링된 장치 집합
    Set<BluetoothDevice> unpairedDevices = new HashSet<>(); // 페어링되지 않은 장치 집합
    ArrayAdapter<String> adapter; // 기기 검색에 필요한 어댑터
    List<String> unpairedList = new ArrayList<>(); // 페어링되지 않은 장치이름 목록
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream;
    InputStream inputStream;



    boolean canConnectFlag = false;
    int canConnectNum = 0;
    boolean paired=false;
//    Thread receiveThread;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // 브로드캐스트 리시버
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {   // 전달받은 액션이 ACTION_FOUND 라면
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    if(device.getName() != null) {
                        add(device);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_acitivity);

        Button Button2 = (Button) findViewById(R.id.button2);
        Button2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                startActivity(intent);
            }
        });

        checkPermissions(permissions);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        btnPower = findViewById(R.id.btnPower);
        selectedColor =  findViewById(R.id.selectedColor);
        colorPick = findViewById(R.id.colorPick);
        btnRainbow = findViewById(R.id.btnRainbow);
        currentState = findViewById(R.id.currentState);
        bar = findViewById(R.id.bar);
        btnColorPicker = findViewById(R.id.btnColorPicker);

        currentState.setVisibility(View.INVISIBLE);

        // 색상 선택하면 색상 데이터 전송해서 LED 불빛 제어
        colorPick.setOnTouchListener(new View.OnTouchListener() { // 터치 리스너 상속
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) { // 색상이 터치되면
                // 터치된 곳의 좌표 가져오기
                final int evX = (int) event.getX();
                final int evY = (int) event.getY();

                Bitmap mBitmap = getBitmapFromView(colorPick);

                try {
                    int pixel = mBitmap.getPixel(evX, evY); // 비트맵 객체로부터 터치된 곳의 픽셀 가져오기
                    sendData(pixel); // 해당 픽셀의 색상 데이터를 전송해서 LED 나오게 하기
                    powerOn=true; // 전원 켜짐
                }catch (IllegalStateException e){
                    e.printStackTrace();
                } catch(IllegalArgumentException e) {
                    e.printStackTrace();
                }
                mBitmap.recycle(); // 더 이상 참조하는 곳이 없다면 버려지도록 가비지 콜렉터로 보냄
                return true;
            }
        });

        // 초기 색상은 하얀색으로 설정(전원 버튼을 위한 설정)
        colors[0] = (byte) 255;
        colors[1] = (byte) 255;
        colors[2] = (byte) 255;
        // 전원 버튼
        btnPower.setOnClickListener(e-> {
            power();
        });
        // 블루투스 접속 버튼
            checkBluetooth();

        // 레인보우 버튼
        btnRainbow.setOnClickListener(e-> {
            rainbow();
            selectedColor.setImageResource(R.drawable.rainbow); // 선택된 색상 표시 이미지에 레인보우 이미지 나타내기
        });
        // 컬러피커
        btnColorPicker.setOnClickListener(e-> {
            colorPicker();
        });
        // 밝기 조절
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                colors[3] = (byte) seekBar.getProgress(); // 현재 시크바 값을 밝기 데이터로 설정하기
                sendData();                                  // 데이터 전송
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    @Override
    protected void onDestroy() {
        try{
            if (inputStream != null)
                inputStream.close(); // 입력 스트림 닫아주기
            if (outputStream != null)
                outputStream.close(); // 출력 스트림 닫아주기
            if (bluetoothSocket != null)
                bluetoothSocket.close(); // 소켓 닫아주기
//            receiveThread.interrupt();


        } catch(IOException e) {
            e.printStackTrace();
        }
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void add(BluetoothDevice device) {
        if(!(pairedDevices.contains(device))) { // 페어링된 장치에 없고
            if(unpairedDevices.add(device)) { // 장치가 추가된다면(중복이 아니라면)
                unpairedList.add(device.getName());    // 페어링되지 않은 장치이름 목록에 이름 추가
            }
        }
        adapter.notifyDataSetChanged(); // 변화 반영
        Toast.makeText(this, device.getName()+" " + getString(R.string.search), Toast.LENGTH_SHORT).show();
    }

    // 장치가 블루투스 지원하는 확인
    private void checkBluetooth() {
        if(bluetoothAdapter == null) // 블루투스 지원하지 않을 때
        {
            //empty
        }
        else { // 디바이스가 블루투스를 지원 할 때
            if(bluetoothAdapter.isEnabled())
            { // 블루투스가 활성화 상태 (기기에 블루투스가 켜져있음)
                selectPairedDevice(); // 블루투스 디바이스 선택 함수 호출
            }
            else { // 블루투스가 비 활성화 상태 (기기에 블루투스가 꺼져있음)
                // 블루투스를 활성화 하기 위한 다이얼로그 출력
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // 선택한 값이 onActivityResult 함수에서 콜백된다.
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }
    }
    // 블루투스 활성화 여부에 따른 결과
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT :
                if(requestCode == RESULT_OK) { // '사용'을 눌렀을 때
                    selectPairedDevice(); // 블루투스 디바이스 선택 함수 호출
                }
                else { // '취소'를 눌렀을 때
                    // 여기에 처리 할 코드를 작성하세요.
                }
                break;
        }
    }
    // 기기검색
    // 이미 페어링된 블루투스 기기 검색
    private void selectPairedDevice() {
        pairedDevices = bluetoothAdapter.getBondedDevices();

        AlertDialog.Builder builder = new AlertDialog.Builder(this); // 다이얼로그 생성
//        builder.setTitle("기기 선택");
        builder.setTitle(R.string.select_device);

        List<String> pairedList = new ArrayList<>(); // 페어링된 기기이름 목록
        for(BluetoothDevice device : pairedDevices) { // 페어링된 기기 집합에서
            pairedList.add(device.getName());        // 장치 이름 전부 기기목록에 추가
        }
//        pairedList.add("취소"); // 취소버튼 추가
        pairedList.add(getString(R.string.cancel)); // 취소버튼 추가

        final CharSequence[] devices = pairedList.toArray(new CharSequence[pairedList.size()]);
        builder.setItems(devices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == pairedList.size()-1) { // 맨 마지막 버튼이 취소 버튼
                    finish(); // Home 화면으로 다시 돌아가기
                } else {
                    bluetoothAdapter.cancelDiscovery();
                    paired = true;
                    connectDevice(devices[which].toString(), paired); // 선택된 인덱스에 해당하는 기기 연결
                }
            }
        });
        builder.setCancelable(true);    // 배경을 선택하면 무력화
        AlertDialog dialog = builder.create(); // 빌더로 다이얼로그 만들기
        dialog.show();   // 다이얼로그 시작
    }

    // 페어링되지 않은 기기 검색
    private void selectUnpairedDevice() {
        // 이미 검색 중이라면 검색을 종료하고, 다시 검색 시작
        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        AlertDialog.Builder builder = new AlertDialog.Builder(this); // 다이얼로그 생성
//        builder.setTitle("기기 검색");
        builder.setTitle(getString(R.string.search_device));

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, unpairedList);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bluetoothAdapter.cancelDiscovery();
                String name = adapter.getItem(which);
                unpairedList.remove(name);
                paired = false;
                connectDevice(name, paired);
            }
        });
        AlertDialog dialog = builder.create(); // 빌더로 다이얼로그 만들기
        dialog.show();
    }

    // 기기 연결 준비
    // 블루투스 페어링된 목록에서 디바이스 장치 가져오기
    private BluetoothDevice getPairedDevice(String name) {
        BluetoothDevice selectedDevice = null;

        for(BluetoothDevice device : pairedDevices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }
    // 페어링되지 않은 장치 목록에서 디바이스 장치 가져오기
    private BluetoothDevice getUnpairedDevice(String name) {
        BluetoothDevice selectedDevice = null;

        for(BluetoothDevice device : unpairedDevices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    // 기기 연결
    private void connectDevice(String selectedDeviceName, boolean paired) {
        final Handler mHandler = new Handler() {
            @SuppressLint("HandlerLeak")
            public void handleMessage(Message msg) {
                if(msg.what==1) {
                    try{
                        outputStream = bluetoothSocket.getOutputStream();
                        inputStream = bluetoothSocket.getInputStream();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread thread = new Thread(new Runnable() {
            public void run() {
                if(paired)
                    bluetoothDevice = getPairedDevice(selectedDeviceName);
                else
                    bluetoothDevice = getUnpairedDevice(selectedDeviceName);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

                try {
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                    bluetoothSocket.connect();
                    mHandler.sendEmptyMessage(1);
                } catch (IOException e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(-1);
                }
            }
        });
        thread.start();
    }


    // 뷰로부터 비트맵 객체 만들기
    public Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }
    private void setCurrentState() {
        currentState.setVisibility(View.VISIBLE);
        currentState.setText("R: " + (colors[0] & 0xFF) + " G: " + (colors[1] & 0xFF) +
                " B: " + (colors[2] & 0xFF) + "\n" + getString(R.string.brightness) +
                ": "+ (colors[3] & 0xFF));
    }
    // 색상 및 밝기 데이터 블루투스로 보내기
    private void sendData() {
        try {
            outputStream.write(colors); // 출력 스트림으로 데이터 배열 전송
        } catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.cannot_send_data), Toast.LENGTH_SHORT).show();
        }
        setCurrentState();
    }
    // color나 pixel을 파라미터로 전달받았을 때 동작
    private void sendData(int cOrp) {
        colors[0] = (byte) Color.red(cOrp);
        colors[1] = (byte) Color.green(cOrp);
        colors[2] = (byte) Color.blue(cOrp);
        colors[3] = (byte) bar.getProgress();
        selectedColor.setImageDrawable(null);
        selectedColor.setBackgroundColor(Color.rgb(Color.red(cOrp), Color.green(cOrp),
                Color.blue(cOrp)));
        try {
            outputStream.write(colors);
        } catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.cannot_send_data), Toast.LENGTH_SHORT).show();
        }
        setCurrentState();
    }
    // 레인보우 함수
    private void rainbow() {
        colors[4] = 1;
        sendData();
        currentState.setText("rainbow");
        powerOn=true;
        colors[4] = 0;
    }
    // 전원 함수
    private void power() {
        if(powerOn) {
            bar.setProgress(0);
            sendData();
            powerOn=false;
        }
        else { // 전원이 꺼져있다면
            bar.setProgress(255);
            sendData();
            powerOn=true;
        }
    }

    private void colorPicker() {
        final ColorPicker colorPicker = new ColorPicker(this);
        ArrayList<String> colors = new ArrayList<>();

        colors.add("#cc4125");
        colors.add("#e8472e");
        colors.add("#e06666");
        colors.add("#ff8c8c");
        colors.add("#f77f23");
        colors.add("#e38436");
        colors.add("#f6b26b");
        colors.add("#ffd966");
        colors.add("#f0e090");
        colors.add("#98b84d");
        colors.add("#a3d17b");
        colors.add("#6ecf69");
        colors.add("#6ec48c");
        colors.add("#5aa392");
        colors.add("#4b8bbf");
        colors.add("#6a88d4");
        colors.add("#736dc9");
        colors.add("#864fbd");
        colors.add("#d169ca");
        colors.add("#d5a6bd");
        colors.add("#cccccc");
        colors.add("#b7b7b7");
        colors.add("#999999");
        colors.add("#666666");
        colors.add("#000000");

        colorPicker.setColors(colors)
                .setTitle("색깔을 선택하세요.")
                .setColumns(5)
                .setRoundColorButton(true)
                .setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
                    @Override
                    public void onChooseColor(int position, int color) {
                        sendData(color);
                        powerOn=true;
                    }

                    @Override
                    public void onCancel() {}
                }).show();
    }


    private void checkPermissions(String[] permissions) {
        ArrayList<String> requestList = new ArrayList<>();

        for(int i=0; i<permissions.length; i++) {
            String curPermission = permissions[i];
            int permissionCheck = ContextCompat.checkSelfPermission(this, curPermission);
            if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(getApplicationContext(), curPermission+ " "+getString(R.string.has_permission), Toast.LENGTH_SHORT).show();
            } else {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,curPermission)) {
//                    Toast.makeText(getApplicationContext(), curPermission+" " + getString(R.string.need_explain_permission), Toast.LENGTH_SHORT).show();
                } else {
                    requestList.add(curPermission);
                }

            }
        }
        if(requestList.size()>0) {
            String[] requests = requestList.toArray(new String[requestList.size()]);
            ActivityCompat.requestPermissions(LedAcitivity.this, requests, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, (i+1)+getString(R.string.index_permission_approved), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, (i+1)+getString(R.string.index_permission_denied), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_bar_menu, menu);
        return true;
    }

}
