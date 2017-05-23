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
import android.os.Handler;
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
import android.widget.EditText;
import android.widget.TextView;

// MetaWear
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
//import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Bmi160Gyro;
import com.mbientlab.metawear.module.Bmi160Gyro.*;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.Bmi160Accelerometer.AccRange;
//import com.mbientlab.metawear.module.Bmi160Accelerometer.OutputDataRate;
import com.mbientlab.metawear.module.I2C;

import java.lang.*;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements ServiceConnection{

    private MetaWearBleService.LocalBinder serviceBinder;
    private final String MW_MAC_ADDRESS = "E6:E3:C3:AA:2F:F1",
            ACCEL_DATA = "accel_data",
            GYRO_DATA = "gyro_data",
            I2Cads1115read = "ads1115read_data",
            I2Cads1115config = "8e83"; //default "8583"
    private static final String LOG_TAG = "MetaWearProj";
    private MetaWearBoard mwBoard;
    private Bmi160Accelerometer accelModule;
    private Bmi160Gyro gyroModule;
    private I2C i2cModule;
    //private SendService sendService;
    private TextView viewAccel, viewAccelX, viewAccelY, viewAccelZ, viewAccelZTilt;
    private TextView viewGyro, viewGyroX, viewGyroY, viewGyroZ;
    private TextView viewConnected, viewContainerCounter, viewOrientation, viewOrientationChange;
    private TextView viewI2C,viewScaleStatus,viewLastDrink;
    private EditText editTiltThreshhold;
    private String accelMessage;
    private String gyroMessage;
    private String orientationMessage, initOrientationMessage;
    static String orientationChange;

    private Boolean initOrientation = false;

    static Boolean tiltedStatus = false; //tilted, yes or not
    private float tiltThreshhold = 30.0f;

    private Handler h = new Handler();
    private int delay = 500; //milliseconds on I2C refresh
    private int calcA, calcB;
    private ScaleHandler scaleObject = new ScaleHandler();

    private float accelTiltX = 42.0f;
    private float accelTiltY = 42.0f;
    private float accelTiltZ = 42.0f;
//    private int positionX = 0, positionY = 0, positionZ = 0;
    private int containerCounter = 0; //should be at max array size
    private int[] angleContainerX = new int[50];
    private int[] angleContainerY = new int[50];
    private int[] angleContainerZ = new int[50];
//    private int angleX = 0, angleY = 0, angleZ = 0;
    private String I2CReading = "XXXX";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewAccel = (TextView)findViewById(R.id.showAccel);
        viewAccelX = (TextView)findViewById(R.id.accelX);
        viewAccelY = (TextView)findViewById(R.id.accelY);
        viewAccelZ = (TextView)findViewById(R.id.accelZ);
        viewAccelZTilt = (TextView)findViewById(R.id.accelZTilt);
        viewGyro = (TextView)findViewById(R.id.showGyro);
        viewGyroX = (TextView)findViewById(R.id.gyroX);
        viewGyroY = (TextView)findViewById(R.id.gyroY);
        viewGyroZ = (TextView)findViewById(R.id.gyroZ);
        viewConnected = (TextView)findViewById(R.id.showConnected);
        viewContainerCounter = (TextView)findViewById(R.id.containerCounter);
        viewOrientation = (TextView)findViewById(R.id.viewOrientation);
        viewOrientationChange = (TextView)findViewById(R.id.viewOrientationChng);
        viewI2C = (TextView)findViewById(R.id.viewI2C);
        viewLastDrink = (TextView)findViewById(R.id.viewLastDrink);
        viewScaleStatus = (TextView)findViewById(R.id.viewScaleStatus);
        editTiltThreshhold  = (EditText)findViewById(R.id.editTiltThreshhold);


        // init Containers
        for (int i = 0; i < angleContainerX.length -1; i++){
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

        //Buttons -------------------------------------------------------------------------------
        findViewById(R.id.buttonEditThreshhold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTiltThreshhold(Float.valueOf(editTiltThreshhold.getText().toString()));
            }
        });

        findViewById(R.id.buttonRescale).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scaleObject.settingNewCorrection();
            }
        });
        //---------------------------------------------------------------------------------------
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
                //Log.i(LOG_TAG, "Connected");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewConnected.setText("Connected");
                    }
                });

//----------------------------------------Measuring starts------------------------------------------
                try {
                    accelModule = mwBoard.getModule(Bmi160Accelerometer.class);
                    gyroModule  = mwBoard.getModule(Bmi160Gyro.class);
                    i2cModule= mwBoard.getModule(I2C.class);

                    //Data I2C viewLastDrink viewScaleStatus
                    i2cModule.writeData((byte) 0x48, (byte) 0x01, hexStringToArray(I2Cads1115config));
                    h.postDelayed(new Runnable(){
                        @Override
                        public void run() {
                            i2cModule.readData((byte) 0x48, (byte) 0x00, (byte) 2).onComplete(new AsyncOperation.CompletionHandler<byte[]>() {
                                @Override
                                public void success(byte[] result) {
                                    calcA = result[0]&0xFF;
                                    calcB = result[1]&0xFF;
                                    scaleObject.addScaleValue(calcA*256+calcB);
                                    I2CReading = Integer.toString(interpolateWeight(scaleObject.returnScaleValue()));
                                    if(!scaleObject.checkValueStability(10))
                                        scaleObject.setSearching(true);
                                    scaleObject.challengeChangeStableWeight(2, interpolateWeight(scaleObject.returnScaleValue()));

                                    //Integer.toString(I2CReading);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            viewI2C.setText(I2CReading);
                                            viewScaleStatus.setText(Integer.toString(scaleObject.getAccumulatedValue()));
                                            //viewLastDrink.setText(Boolean.toString(scaleObject.getSearching()));
                                            viewLastDrink.setText(Integer.toString(scaleObject.getLastChange()));
                                        }
                                    });

                                    //Log.i("MainActivity", String.format("%d", result[0]));
                                }
                            });
                            h.postDelayed(this, delay);
                        }
                    }, delay);

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
                                            if (containerCounter < 49) {
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
                                    accelTiltX = (float)Math.toDegrees(Math.asin(message.getData(CartesianFloat.class).x()));
                                    accelTiltY = (float)Math.toDegrees(Math.asin(message.getData(CartesianFloat.class).y()));
                                    accelTiltZ = (float)Math.toDegrees(Math.asin(message.getData(CartesianFloat.class).z()));
                                    if (accelTiltZ < tiltThreshhold) {
                                        setTiltedStatus(true);
                                    } else {
                                        setTiltedStatus(false);
                                    }
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            viewAccel.setText(accelMessage);
                                            viewAccelX.setText(String.valueOf(accelTiltX));
                                            viewAccelY.setText(String.valueOf(accelTiltY));
                                            viewAccelZ.setText(String.valueOf(accelTiltZ));
                                            viewAccelZTilt.setText(String.valueOf(tiltedStatus));
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
                                                orientationChange = "initPosition";
                                            else
                                                orientationChange = "otherPosition";

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

    private void waitIfLegitTilt(float checkaccelTiltZ){
        long startTime = System.nanoTime();
        boolean failedTilt = false;
        while(1000000000.0 > (System.nanoTime() - startTime)){
            if(checkaccelTiltZ < 60.0) {     //check of isNan or below 60 degress on z AxisTilt
                setTiltedStatus(false);
                failedTilt = false;
                break;
            }
        }
        if (failedTilt) {
            setTiltedStatus(true);
        }
    }

    private void setTiltedStatus(Boolean bool){
        this.tiltedStatus = bool;
    }

    private void setTiltThreshhold(float threshhold){
        this.tiltThreshhold = threshhold;
    }

    //from Documentation
    private static String arrayToHexString(byte[] value) {
        if (value.length == 0) {
            return "";
        }

        StringBuilder builder= new StringBuilder();
        for(byte it: value) {
            builder.append(String.format("%02x", it));
        }

        return builder.toString();
    }

    //from Documentation
    private static byte[] hexStringToArray(String byteArrayString) {
        if (byteArrayString.isEmpty()) {
            return new byte[] { };
        }

        if (byteArrayString.length() % 2 != 0) {
            byteArrayString= String.format("0%s", byteArrayString);
        }

        byte[] bytes= new byte[byteArrayString.length() / 2];
        for(int i= 0, j= 0; i < byteArrayString.length(); i+= 2, j++) {
            bytes[j]= (byte) Short.parseShort(byteArrayString.substring(i, i + 2), 16);;
        }

        return bytes;
    }

    public static int interpolateWeight(int read){
        float weight0 = 0 ,weight1 = 0,sample0 = 0,sample1 = 0;
        int weight;
        //project-kitchenscale: designed for 100gram load cell
        // 730-575;680-534;613-480;576-450;539-420;501-389;466-361;431-333;407-313;354-270;192-139;96-61;20-0
        if ((read>730)) {
            weight1 = 575; weight0 = 534; sample1 = 730; sample0 = 680;
        } else if ((730>=read)&&(read>680)){
            weight1 = 575; weight0 = 534; sample1 = 730; sample0 = 680;
        } else if ((680>=read)&&(read>613)){
            weight1 =534; weight0 = 480; sample1 = 680; sample0 = 613;
        } else if ((613>=read)&&(read>576)){
            weight1 =480; weight0 = 450; sample1 = 613; sample0 = 576;
        } else if ((576>=read)&&(read>539)){
            weight1 =450; weight0 = 420; sample1 = 576; sample0 = 539;
        } else if ((539>=read)&&(read>501)){
            weight1 =420; weight0 = 389; sample1 = 539; sample0 = 501;
        } else if ((501>=read)&&(read>466)){
            weight1 =389; weight0 = 361; sample1 = 501; sample0 = 466;
        } else if ((466>=read)&&(read>431)){
            weight1 =361; weight0 = 333; sample1 = 466; sample0 = 431;
        } else if ((431>=read)&&(read>407)){
            weight1 =333; weight0 = 313; sample1 = 431; sample0 = 407;
        } else if ((407>=read)&&(read>354)){
            weight1 =313; weight0 = 270; sample1 = 407; sample0 = 354;
        } else if ((354>=read)&&(read>192)){
            weight1 =270;  weight0 = 139; sample1 = 354; sample0 = 192;
        } else if ((192>=read)&&(read>96)){
            weight1 =139; weight0 = 61; sample1 = 192; sample0 = 96;
        } else if ((96>=read)&&(read>20)){
            weight1 =61;  weight0 = 0; sample1 = 96; sample0 = 20;
        } else if ((20>=read)){
            weight1 =61;  weight0 = 0; sample1 = 96; sample0 = 20;
        }
        //linear interpolation
        weight = (int)(weight0+((weight1-weight0)/(sample1-sample0))*((float)read-sample0));
        return weight;
    };



}
