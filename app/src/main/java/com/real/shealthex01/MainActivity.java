package com.real.shealthex01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{

    public static Context value_temp;
    public float value;

    private String TAG = MainActivity.class.getName();
    private GoogleApiClient googleApiClient;
    private boolean authInProgress = false;
    private OnDataPointListener onDataPointListener;
    private static final int AUTH_REQUEST = 1;

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.BODY_SENSORS
    };

    private static final int REQUEST_PERMISSION_CODE = 12345;
    private List<String> missingPermission = new ArrayList<>();

    private boolean bCheckStarted = false;
    private boolean bCheckStop = false;
    private boolean bGoogleConnected = false;

    private Button btnStart;
    //private Button btnPlay;
    private ProgressBar spinner;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private TextView textMon;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        value_temp = this;

        //심박패턴을 측정하는 동안 화면이 꺼지지 않도록 제어하기 위해 전원관리자를 얻어옵니다
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "WAKELOCK");
        initUI();
        //필요한 권한을 얻었는지 확인하고, 얻지 않았다면 권한 요청을 하기 위한 코드를 호출합니다
        checkAndRequestPermissions();
    }

    private void initUI() {
        //심박수를 측정하는 Google API의 호출을 위해 API 클라이언트를 초기화 합니다
        initGoogleApiClient();

        textMon = findViewById(R.id.textMon);
        spinner = findViewById(R.id.progressBar1);
        spinner.setVisibility(View.INVISIBLE);
        btnStart = findViewById(R.id.btnStart);
        btnStart.setText("Wait please ...");
        btnStart.setEnabled(false);
        //btnPlay = findViewById(R.id.btnPlay);
        //btnPlay.setEnabled(false);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bCheckStarted) {
                    //btnStart.setText(R.string.msg_start);
                    btnStart.setText("시작하기");
                    bCheckStarted = false;

                    unregisterFitnessDataListener();

                    spinner.setVisibility(View.INVISIBLE);

                    wakeLock.release();

                } else {
                    //버튼을 처음 클릭할 경우 Google API 클라이언트에 로그인이 되어있는 상태인지를 확인합니다.
                    //만약 로그인이 되어 있는 상태라면,
                    if (bGoogleConnected == true) {
                        //심박수를 측정하기 위한 API를 설정합니다
                        findDataSources();
                        //심박수의 측정이 시작되면 심박수 정보를 얻을 콜백함수를 등록/설정하는 함수를 호출합니다
                        registerDataSourceListener(DataType.TYPE_HEART_RATE_BPM);
                        btnStart.setText("Stop");
                        //btnStart.setText(R.string.msg_stop);
                        bCheckStarted = true;

                        spinner.setVisibility(View.VISIBLE);
                        //화면이 꺼지지 않도록 설정합니다
                        wakeLock.acquire();

                    }
                    // Google API 클라이언트에 로그인이 되어 있지 않다면,
                    else {
                        //Google API 클라이언트에 로그인 합니다
                        if (MainActivity.this.googleApiClient != null)
                            MainActivity.this.googleApiClient.connect();
                    }
                }
            }
        });
    }

    private void initGoogleApiClient() {
        this.googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.SENSORS_API)
                .addScope(new Scope(Scopes.FITNESS_BODY_READ))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            //Google API 클라이언트의 로그인에 성공하면 호출이 되는 콜백입니다
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.d(TAG, "initGoogleApiClient() onConnected good...");
                                bGoogleConnected = true;
                                btnStart.setText("심박수 확인");
                                btnStart.setEnabled(true);
                            }

                            @Override
                            public void onConnectionSuspended(int i) {

                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.d(TAG, "onConnectionSuspended() network_lost bad...");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.d(TAG, "onConnectionSuspended() service_disconnected bad...");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {

                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                Log.d(TAG, "Connection failed. Cause: " + result.toString());

                                if (!result.hasResolution()) {
                                    MainActivity.this.finish();
                                    return;
                                }

                                if (!authInProgress) {
                                    try {
                                        Log.d(TAG, "Attempting to resolve failed connection");
                                        authInProgress = true;
                                        result.startResolutionForResult(MainActivity.this,
                                                AUTH_REQUEST);
                                    } catch (IntentSender.SendIntentException e) {
                                        Log.e(TAG,
                                                "Exception while starting resolution activity", e);
                                        finish();
                                    }
                                }
                                else {
                                    MainActivity.this.finish();
                                }
                            }
                        }
                )
                .build();
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            if (MainActivity.this.googleApiClient != null)
                MainActivity.this.googleApiClient.connect();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        } else {
            if (MainActivity.this.googleApiClient != null)
                MainActivity.this.googleApiClient.connect();
        }

    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            initGoogleApiClient();
            if (MainActivity.this.googleApiClient != null)
                MainActivity.this.googleApiClient.connect();
        } else {
            Toast.makeText(getApplicationContext(), "Failed get permissions", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void findDataSources() {
        Fitness.SensorsApi.findDataSources(googleApiClient, new DataSourcesRequest.Builder()
                .setDataTypes(DataType.TYPE_HEART_RATE_BPM)
                // .setDataTypes(DataType.TYPE_SPEED)
                // .setDataTypes(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setDataSourceTypes(DataSource.TYPE_RAW)
                .build())
                .setResultCallback(new ResultCallback<DataSourcesResult>() {
                    @Override
                    public void onResult(DataSourcesResult dataSourcesResult) {

                        for (DataSource dataSource : dataSourcesResult.getDataSources()) {

                            if (dataSource.getDataType().equals(DataType.TYPE_HEART_RATE_BPM)
                                    && onDataPointListener == null) {
                                Log.d(TAG, "findDataSources onResult() registering dataSource=" + dataSource);
                                registerDataSourceListener(DataType.TYPE_HEART_RATE_BPM);

                            }
                        }
                    }
                });

    }


    private void registerDataSourceListener(DataType dataType) {
        onDataPointListener = new OnDataPointListener() {
            // 심박수가 측정되면 심박수를 얻어올 수 있는 콜백입니다
            @Override
            public void onDataPoint(DataPoint dataPoint) {
                for (Field field : dataPoint.getDataType().getFields()) {
                    Value aValue = dataPoint.getValue(field);
                    //Log.d(TAG, "Detected DataPoint field: " + field.getName());
                    //Log.d(TAG, "Detected DataPoint value: " + aValue);

                    //addContentToView("dataPoint=" + field.getName() + " " + aValue + "\n");
                    addContentToView(aValue.asFloat());
                }
            }
        };

        Fitness.SensorsApi.add(
                googleApiClient,
                new SensorRequest.Builder()
                        .setDataType(dataType)
                        .setSamplingRate(2, TimeUnit.SECONDS)
                        .setAccuracyMode(SensorRequest.ACCURACY_MODE_DEFAULT)
                        .build(),
                onDataPointListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "onDataPointListener  registered good");
                        } else {
                            Log.d(TAG, "onDataPointListener failed to register bad");
                        }
                    }
                });

    }

    private void unregisterFitnessDataListener() {
        if (this.onDataPointListener == null) {
            return;
        }

        if (this.googleApiClient == null) {
            return;
        }

        if (this.googleApiClient.isConnected() == false) {
            return;
        }

        Fitness.SensorsApi.remove(
                this.googleApiClient,
                this.onDataPointListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "Listener was removed!");
                        } else {
                            Log.d(TAG, "Listener was not removed.");
                        }
                    }
                });
        // [END unregister_data_listener]
    }


    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onStart connect attempted");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterFitnessDataListener();

        if (this.googleApiClient != null && this.googleApiClient.isConnected()) {
            this.googleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTH_REQUEST) {
            authInProgress = false;

            if (resultCode == RESULT_OK) {

                if (!this.googleApiClient.isConnecting() && !this.googleApiClient.isConnected()) {
                    this.googleApiClient.connect();
                    Log.d(TAG, "onActivityResult googleApiClient.connect() attempted in background");

                }
            }
        }
    }

    private synchronized void addContentToView(final float value) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (spinner.getVisibility() == View.VISIBLE)
                    spinner.setVisibility(View.INVISIBLE);

                Log.d(TAG,"Heart Beat Rate Value : " + value);
                textMon.setText("현재 심박수 : " + value);
                //textMon.setText("현재 감정상태를 측정 중...");
            }
        });
    }

}