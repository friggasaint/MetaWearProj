package de.tum.far.metawearproj;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.widget.TextView;

// MetaWear
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Bmi160Gyro;
import com.mbientlab.metawear.module.Bmi160Gyro.*;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.Bmi160Accelerometer.AccRange;
import com.mbientlab.metawear.module.Bmi160Accelerometer.OutputDataRate;

//Writting logs
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements ServiceConnection{

    private MetaWearBleService.LocalBinder serviceBinder;
    private final String MW_MAC_ADDRESS = "E6:E3:C3:AA:2F:F1",
            ACCEL_DATA = "accel_data",
            GYRO_DATA = "gyro_data";
    private static final String LOG_TAG = "MetaWearProj";
    private MetaWearBoard mwBoard;
    private Bmi160Accelerometer accelModule;
    private Bmi160Gyro gyroModule;
    private SendService sendService;
    private TextView viewAccel, viewAccelX, viewAccelY, viewAccelZ;
    private TextView viewGyro, viewGyroX, viewGyroY, viewGyroZ;
    private TextView viewConnected, viewContainerCounter, viewOrientation, viewOrientationChange;
    private String accelMessage;
    private String gyroMessage;
    private String orientationMessage, initOrientationMessage;
    static String orientationChange;

    private Boolean writeLog = false;
    private Boolean initOrientation = false;

    private int containerCounter = 0;
    private int[] positionContainerX = new int[250];
    private int[] positionContainerY = new int[250];
    private int[] positionContainerZ = new int[250];
    private int positionX = 0, positionY = 0, positionZ = 0;
    private int[] angleContainerX = new int[250];
    private int[] angleContainerY = new int[250];
    private int[] angleContainerZ = new int[250];
    private int angleX = 0, angleY = 0, angleZ = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewAccel = (TextView)findViewById(R.id.showAccel);
        viewAccelX = (TextView)findViewById(R.id.accelX);
        viewAccelY = (TextView)findViewById(R.id.accelY);
        viewAccelZ = (TextView)findViewById(R.id.accelZ);
        viewGyro = (TextView)findViewById(R.id.showGyro);
        viewGyroX = (TextView)findViewById(R.id.gyroX);
        viewGyroY = (TextView)findViewById(R.id.gyroY);
        viewGyroZ = (TextView)findViewById(R.id.gyroZ);
        viewConnected = (TextView)findViewById(R.id.showConnected);
        viewContainerCounter = (TextView)findViewById(R.id.containerCounter);
        viewOrientation = (TextView)findViewById(R.id.viewOrientation);
        viewOrientationChange = (TextView)findViewById(R.id.viewOrientationChange);


        // init Containers
        for (int i = 0; i < positionContainerX.length -1; i++){
            positionContainerX[i] = 0;
            positionContainerY[i] = 0;
            positionContainerZ[i] = 0;
            angleContainerX[i] = 0;
            angleContainerY[i] = 0;
            angleContainerZ[i] = 0;
        }
        startService(new Intent(this, SendService.class));

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);

        findViewById(R.id.startAccel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accelModule.enableAxisSampling();
                accelModule.start();
                gyroModule.start();
            }
        });

        findViewById(R.id.stopAccel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accelModule.enableAxisSampling();
                accelModule.stop();
                gyroModule.stop();
            }
        });

        findViewById(R.id.startLog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeLog = true;
            }
        });

        findViewById(R.id.stopLog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeLog = false;
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Restarted Activity", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                recreate();
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // Typecast the binder to the service's LocalBinder class
        serviceBinder = (MetaWearBleService.LocalBinder) service;

        final BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);

        // Create a MetaWear board object for the Bluetooth Device
        mwBoard = serviceBinder.getMetaWearBoard(remoteDevice);
        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                Log.i(LOG_TAG, "Connected");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewConnected.setText("Connected");
                    }
                });


                try {
                    accelModule = mwBoard.getModule(Bmi160Accelerometer.class);
                    gyroModule  = mwBoard.getModule(Bmi160Gyro.class);

                    //get Data from Sensors
                    gyroModule.configure()
                            .setFullScaleRange(FullScaleRange.FSR_2000)
                            .setOutputDataRate(Bmi160Gyro.OutputDataRate.ODR_25_HZ)
                            .commit();

                    gyroModule.routeData().fromAxes().stream(GYRO_DATA).commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe(GYRO_DATA, new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(com.mbientlab.metawear.Message message) {

                                            // upping counter
                                            if (containerCounter < 249) {
                                                containerCounter++;
                                            } else {
                                                containerCounter = 0;
                                            }
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    viewContainerCounter.setText(String.valueOf(containerCounter));
                                                }
                                            });

                                            // measure
                                            gyroMessage = message.getData(CartesianFloat.class).toString();
                                            angleContainerX[containerCounter] = (int)Math.round(message.getData(CartesianFloat.class).x());
                                            angleContainerY[containerCounter] = (int)Math.round(message.getData(CartesianFloat.class).y() - 0.4f);
                                            angleContainerZ[containerCounter] = (int)Math.round(message.getData(CartesianFloat.class).z() + 0.5f);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    viewGyro.setText(gyroMessage);
                                                    viewGyroX.setText(String.valueOf(containerSumUp(angleContainerX)));
                                                    viewGyroY.setText(String.valueOf(containerSumUp(angleContainerY)));
                                                    viewGyroZ.setText(String.valueOf(containerSumUp(angleContainerZ)));
                                                }
                                            });
//                                            if (writeLog){
//                                                appendLog(gyroMessage, "logs/_test.txt");
//                                            }
                                        }
                                    });
                                }
                            });


                    accelModule.configureAxisSampling()
                            .setFullScaleRange(AccRange.AR_16G)
                            .setOutputDataRate(Bmi160Accelerometer.OutputDataRate.ODR_25_HZ)
                            .commit();

                    accelModule.routeData().fromAxes().stream(ACCEL_DATA).commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe(ACCEL_DATA, new RouteManager.MessageHandler() {
                                @Override
                                public void process(com.mbientlab.metawear.Message message) {
                                    accelMessage = message.getData(CartesianFloat.class).toString();
                                    positionContainerX[containerCounter] = (int)Math.round(message.getData(CartesianFloat.class).x());
                                    positionContainerY[containerCounter] = (int)Math.round(message.getData(CartesianFloat.class).y());
                                    positionContainerZ[containerCounter] = (int)Math.round(message.getData(CartesianFloat.class).z());
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            viewAccel.setText(accelMessage);
                                            viewAccelX.setText(String.valueOf(containerSumUp(positionContainerX)));
                                            viewAccelY.setText(String.valueOf(containerSumUp(positionContainerY)));
                                            viewAccelZ.setText(String.valueOf(containerSumUp(positionContainerZ)));
                                        }
                                    });

                                }
                            });
                        }
                    });

                    accelModule.enableOrientationDetection();
                    accelModule.routeData().fromOrientation().stream("orientation").commit()
                            .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe("orientation", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message message) {
                                            // capture init state of sensor
                                            if (!initOrientation){
                                                initOrientationMessage = message.getData(Bmi160Accelerometer.SensorOrientation.class).toString();
                                                initOrientation = !initOrientation;
                                            }
                                            orientationMessage = message.getData(Bmi160Accelerometer.SensorOrientation.class).toString();

                                            if(orientationMessage.equals(initOrientationMessage))
                                                orientationChange = "took a sip";
                                            else
                                                orientationChange = "nothing";

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    viewOrientation.setText(orientationMessage) ;
                                                    viewOrientationChange.setText(orientationChange);
                                                }
                                            });

                                        }
                                    });
                                }
                            });

                } catch (UnsupportedModuleException e) {
                    Log.e(LOG_TAG, "Module cannot be found", e);
                }
            }

            @Override
            public void disconnected() {
                Log.i(LOG_TAG, "Connected Lost");
            }

            @Override
            public void failure(int status, Throwable error) {
                Log.e(LOG_TAG, "Error connecting", error);
            }
        });
        mwBoard.connect();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    public int containerSumUp(int[] container){
        int sum = 0;
        for (int i = 0; i < container.length-1; i++){
            sum += container[i];
        }
        return sum;
    }

    public void appendLog(String text, String path)
    {
        verifyStoragePermissions(this);
        File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), path);
        if (!logFile.exists())
        {
            try
            {
                logFile.getParentFile().mkdir();
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

//    /* Checks if external storage is available for read and write */
//    public boolean isExternalStorageWritable() {
//        String state = Environment.getExternalStorageState();
//        if (Environment.MEDIA_MOUNTED.equals(state)) {
//            return true;
//        }
//        return false;
//    }
//
//    /* Checks if external storage is available to at least read */
//    public boolean isExternalStorageReadable() {
//        String state = Environment.getExternalStorageState();
//        if (Environment.MEDIA_MOUNTED.equals(state) ||
//                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
//            return true;
//        }
//        return false;
//    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


}
