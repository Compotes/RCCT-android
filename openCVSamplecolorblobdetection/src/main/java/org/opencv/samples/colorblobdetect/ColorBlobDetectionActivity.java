package org.opencv.samples.colorblobdetect;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {

    // Constatns
    private static final String TAG = "OCVSample::Activity";
    private static final String[] BT_ADDRESSES = {"20:15:07:27:32:86", "98:D3:31:20:4B:BE"};
    private static final int NUMBER_OF_OBJECTS = 3;
    private static final double WIDTH = 640; //720
    private static final double CAMERA_VIEW_ANGLE = 120;
    private static final double CAMERA_CENTER_PIXEL = WIDTH / 2;
    private static final double CAMERA_VIEW_ANGLE_COEFFICIENT = CAMERA_VIEW_ANGLE / WIDTH;
    private static final int MIN_RANGE_SCALE_VALUE = 1;
    private static final int DEFAULT_ERROR_VALUE = 420;
    private static final String DEFAULT_SIGN = "+";
    private static final int HSV_H_INDEX = 0;
    private static final int HSV_S_INDEX = 1;
    private static final int HSV_V_INDEX = 2;
    // end Constants

    private boolean mIsColorSelected[] = {false, false, false};
    private int mColorIndex = 0;

    private Mat mRgba;
    private Scalar mBlobColorRgba[] = new Scalar[NUMBER_OF_OBJECTS];

    private Scalar mBlobColorHsv[] = new Scalar[NUMBER_OF_OBJECTS];
    private Scalar mBlobColorHsvMin[] = new Scalar[NUMBER_OF_OBJECTS];

    private SeekBar minSeekH, minSeekS, minSeekV;
    private SeekBar maxSeekH, maxSeekS, maxSeekV;

    private RadioButton firstColor, secondColor, thirdColor;
    private RadioButton attackFirst, attackSecond;

    private CheckBox showScaleButton, showHSV;
    private Button connectLoshalo, connectFerques, go, kick, debug;

    private ColorBlobDetector mDetector;

    private int debugValues[] = new int[480];
    private int numberOfDebugValues[] = new int[480];

    private Mat mSpectrum[] = new Mat[NUMBER_OF_OBJECTS];
    private Scalar rangeForColor[] = new Scalar[NUMBER_OF_OBJECTS];

    private Size SPECTRUM_SIZE;
    private Scalar GOAL_CONTOUR_COLOR,
            BALL_CONTOUR_COLOR,
            EXTREMES_CONTOUR_COLOR,
            BIGGER_COLOR;

    private CameraBridgeViewBase mOpenCvCameraView;

    private Point goalCenterPoint = null;
    private Point ballCenterPoint = null;
    private Point ourGoalCenterPoint = null;

    private boolean movementState = false;
    private boolean debugState = false;

    private int attackColorIndex = 0;

    BluetoothSocket mmSocket = null;
    OutputStream bluetoothOutputStream;
    InputStream bluetoothInputStream;

    // movementState, kickerState
    private String bluetoothAppendData[] = {"", ""};

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    // 800, 500 Martinov // 480, 360
                    // 800, 600 Janov
                    mOpenCvCameraView.setMaxFrameSize((int)WIDTH, 480); // 360
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                    // // TODO: 31.3.2017 TEST !!!
                    //mOpenCvCameraView.setFocusable(false);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public ColorBlobDetectionActivity() {
        //Log.i(TAG, "Instantiated new " + this.getClass());
    }

    MediaPlayer nulaCelychNulaPlayer;
    MediaPlayer ak47Player;

    /**
     * Called when the activity is first created.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        maxSeekH = (SeekBar) findViewById(R.id.maxSeekBarH);
        maxSeekS = (SeekBar) findViewById(R.id.maxSeekBarS);
        maxSeekV = (SeekBar) findViewById(R.id.maxSeekBarV);

        minSeekH = (SeekBar) findViewById(R.id.minSeekBarScaleH);
        minSeekS = (SeekBar) findViewById(R.id.minSeekBarScaleS);
        minSeekV = (SeekBar) findViewById(R.id.minSeekBarScaleV);

        firstColor = (RadioButton) findViewById(R.id.firstColor);
        secondColor = (RadioButton) findViewById(R.id.secondColor);
        thirdColor = (RadioButton) findViewById(R.id.thirdColor);

        attackFirst = (RadioButton) findViewById(R.id.attackFirst);
        attackSecond = (RadioButton) findViewById(R.id.attackSecond);

        showScaleButton = (CheckBox) findViewById(R.id.showScale);
        showHSV = (CheckBox) findViewById(R.id.showHSV);

        connectLoshalo = (Button) findViewById(R.id.connectLoshalo);
        connectFerques = (Button) findViewById(R.id.connectFerques);
        debug = (Button) findViewById(R.id.debug);

        go = (Button) findViewById(R.id.go);
        kick = (Button) findViewById(R.id.kick);

        nulaCelychNulaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.jetamnulacelychnulanula2);
        ak47Player = MediaPlayer.create(getApplicationContext(), R.raw.ak47b);

        attackFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attackColorIndex = 0;
            }
        });

        attackSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attackColorIndex = 2;
            }
        });

        kick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothAppendData[1] = "k";
            }
        });

        debug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (debugState == false) {
                    for (int i = 0; i < 480; i++) {
                        debugValues[i] = 0;
                        numberOfDebugValues[i] = 0;
                    }
                    debugState = true;
                } else {
                    debugState = false;
                    String result = "";

                    for (int i = 0; i < 480; i++) {
                        if(numberOfDebugValues[i] != 0) {
                            result += String.valueOf(i) + " " + String.valueOf(debugValues[i] / numberOfDebugValues[i]) + "\n\r";
                        }
                    }
                    Log.w("debugA", result);
                }
            }
        });

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // p for GO
                // s for STOP
                bluetoothAppendData[0] = (movementState == false) ? "p" : "s";
                movementState = !movementState;
            }
        });

        connectFerques.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            BluetoothDevice robot = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(BT_ADDRESSES[0]);
            BluetoothSocket tmp = null;

            UUID UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            try {
                tmp = robot.createInsecureRfcommSocketToServiceRecord(UUID);
            } catch (Exception e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;

            try {
                mmSocket.connect();
                MediaPlayer m = MediaPlayer.create(getApplicationContext(), R.raw.buzeran);
                m.start();
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                bluetoothOutputStream = mmSocket.getOutputStream();
                bluetoothInputStream = mmSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            }
        });

        connectLoshalo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothDevice robot = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(BT_ADDRESSES[1]);
                BluetoothSocket tmp = null;

                UUID UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                try {
                    tmp = robot.createInsecureRfcommSocketToServiceRecord(UUID);
                } catch (Exception e) {
                    Log.e(TAG, "create() failed", e);
                }
                mmSocket = tmp;

                try {
                    mmSocket.connect();
                    MediaPlayer m = MediaPlayer.create(getApplicationContext(), R.raw.buzeran);
                    m.start();
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    bluetoothOutputStream = mmSocket.getOutputStream();
                    bluetoothInputStream = mmSocket.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        maxSeekH.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                minSeekH.setMax(i - 10);
                updateMaxHsvValues(i, HSV_H_INDEX);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        maxSeekS.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                minSeekS.setMax(i - 10);
                updateMaxHsvValues(i, HSV_S_INDEX);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        maxSeekV.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                minSeekV.setMax(i - 10);
                updateMaxHsvValues(i, HSV_V_INDEX);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        minSeekH.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateMinHsvValues(i, HSV_H_INDEX);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        minSeekS.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateMinHsvValues(i, HSV_S_INDEX);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        minSeekV.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateMinHsvValues(i, HSV_V_INDEX);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        firstColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mColorIndex = 0;
                    setProgressBarData(0);
                }
            }
        });

        secondColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mColorIndex = 1;
                    setProgressBarData(1);
                }
            }
        });

        thirdColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mColorIndex = 2;
                    setProgressBarData(2);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private boolean indexIsSet(int index) {
        if (mIsColorSelected[index]) {
            return true;
        }

        Toast t = Toast.makeText(getApplicationContext(), "This color is not set", Toast.LENGTH_SHORT);
        t.show();
        return false;
    }

    /**
     *
     * @param index - Index of color which will be set in progress bar
     */
    private void setProgressBarData(int index) {
        if (indexIsSet(index)) {
            maxSeekH.setProgress((int) mBlobColorHsv[index].val[0]);
            maxSeekS.setProgress((int) mBlobColorHsv[index].val[1]);
            maxSeekV.setProgress((int) mBlobColorHsv[index].val[2]);

            minSeekH.setProgress((int) mBlobColorHsvMin[index].val[0]);
            minSeekS.setProgress((int) mBlobColorHsvMin[index].val[1]);
            minSeekV.setProgress((int) mBlobColorHsvMin[index].val[2]);
        }
    }

    /**
     *
     * @param value - values from progress bar
     * @param tIndex - index of HSV color starting with 0
     */
    private void updateMaxHsvValues(int value, int tIndex) {
        mBlobColorHsv[mColorIndex].val[tIndex] = value;
        mDetector.setHsvColor(mBlobColorHsv[mColorIndex], mBlobColorHsvMin[mColorIndex]);
        Imgproc.resize(mDetector.getSpectrum(), mSpectrum[mColorIndex], SPECTRUM_SIZE);
    }

    /**
     *
     * @param value - value from progress bar
     * @param tIndex - index of HSV color starting with 0
     */
    private void updateMinHsvValues(int value, int tIndex) {
        mBlobColorHsvMin[mColorIndex].val[tIndex] = value;
        mDetector.setHsvColor(mBlobColorHsv[mColorIndex], mBlobColorHsvMin[mColorIndex]);
        Imgproc.resize(mDetector.getSpectrum(), mSpectrum[mColorIndex], SPECTRUM_SIZE);
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();

        for (int i = 0; i < mBlobColorRgba.length; i++) {
            mBlobColorRgba[i] = new Scalar(255);
            mBlobColorHsv[i] = new Scalar(255);
            mSpectrum[i] = new Mat();
        }

        SPECTRUM_SIZE = new Size(200, 64);

        GOAL_CONTOUR_COLOR = new Scalar(0, 0, 255, 255);
        BALL_CONTOUR_COLOR = new Scalar(0, 255, 0, 255);
        EXTREMES_CONTOUR_COLOR = new Scalar(0, 0, 255, 255);
        BIGGER_COLOR = new Scalar(255, 20, 147);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (!firstColor.isChecked() && !secondColor.isChecked() && !thirdColor.isChecked()) {
            Toast t = Toast.makeText(getApplicationContext(), "First select color!", Toast.LENGTH_SHORT);
            t.show();
            return false;
        }

        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x > 4) ? x - 4 : 0;
        touchedRect.y = (y > 4) ? y - 4 : 0;

        touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv[mColorIndex] = Core.sumElems(touchedRegionHsv);

        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv[mColorIndex].val.length; i++)
            mBlobColorHsv[mColorIndex].val[i] /= pointCount;

        // set lower bounding
        mBlobColorHsvMin[mColorIndex] =
                new Scalar(mBlobColorHsv[mColorIndex].val[0] - 10,
                        mBlobColorHsv[mColorIndex].val[1],
                        mBlobColorHsv[mColorIndex].val[2]);

        mBlobColorRgba[mColorIndex] = converScalarHsv2Rgba(mBlobColorHsv[mColorIndex]);

        ////Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba[mColorIndex].val[0] + ", " + mBlobColorRgba[mColorIndex].val[1] +
        //        ", " + mBlobColorRgba[mColorIndex].val[2] + ", " + mBlobColorRgba[mColorIndex].val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv[mColorIndex], mBlobColorHsvMin[mColorIndex]);
        Imgproc.resize(mDetector.getSpectrum(), mSpectrum[mColorIndex], SPECTRUM_SIZE);

        mIsColorSelected[mColorIndex] = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        // opponent goal
        List<Point> centersOfObjects = null;
        // ball
        List<Point> ballCentersOfObjects = null;

        if (mIsColorSelected[0] && !mIsColorSelected[2]) {
            attackColorIndex = 0;
        }

        if (mIsColorSelected[0]) {
            mDetector.process(mRgba, mBlobColorHsv[attackColorIndex], mBlobColorHsvMin[attackColorIndex]);
            List<MatOfPoint> contours = mDetector.getContours();
            if (contours != null) {
                centersOfObjects = this.determineMinMax(contours, 30);

                List<MatOfPoint> max = new ArrayList<MatOfPoint>();
                Imgproc.drawContours(mRgba, contours, -1, GOAL_CONTOUR_COLOR);
                Imgproc.drawContours(mRgba, max, -1, EXTREMES_CONTOUR_COLOR);

                if (centersOfObjects != null) {
                    goalCenterPoint = this.centerPoint(centersOfObjects);
                }
            }
        }

        if (mIsColorSelected[1]) {
            mDetector.process(mRgba, mBlobColorHsv[1], mBlobColorHsvMin[1]);
            List<MatOfPoint> contours = mDetector.getContours();

            if (contours != null) {
                //ballCentersOfObjects = this.determineMinMax(contours, -1);
                ballCentersOfObjects = this.findBallMinMax(contours);

                if (ballCentersOfObjects != null) {
                    int width = (int) (ballCentersOfObjects.get(0).x - ballCentersOfObjects.get(1).x);
                    ballCenterPoint = this.centerPoint(ballCentersOfObjects);

                    Core.putText(mRgba, String.valueOf(width), new Point(10, 10), 1, 1, BIGGER_COLOR, 1);
                    Core.putText(mRgba, String.valueOf(ballCenterPoint.y), new Point(10, 20), 1, 1, BIGGER_COLOR, 1);
                    if (debugState) {
                        debugValues[(int) ballCenterPoint.y] += width;
                        numberOfDebugValues[(int) ballCenterPoint.y]++;
                    }
                }
            }

            Imgproc.drawContours(mRgba, contours, -1, BALL_CONTOUR_COLOR);
        }

        if (goalCenterPoint != null)
            Core.rectangle(mRgba, goalCenterPoint, goalCenterPoint, BIGGER_COLOR, 5);
        if(ourGoalCenterPoint != null)
            Core.rectangle(mRgba, ourGoalCenterPoint, ourGoalCenterPoint, BIGGER_COLOR, 5);
        if (ballCenterPoint != null) {
            Core.rectangle(mRgba, ballCenterPoint, ballCenterPoint, BIGGER_COLOR, 5);
            Core.putText(mRgba, String.valueOf(calculateAngle((int) ballCenterPoint.x)),
                    ballCenterPoint, 1, 1, GOAL_CONTOUR_COLOR);
        }
        if (centersOfObjects != null)
            this.drawExtremes(centersOfObjects);

        if (mmSocket != null) {
            Point goal = goalCenterPoint;
            Point ball = ballCenterPoint;
            Point ourGoal = ourGoalCenterPoint;

            String xGoalSign = DEFAULT_SIGN;
            String xBallSign = DEFAULT_SIGN;
            String xOurGoalSign = DEFAULT_SIGN;

            int goalAngle = DEFAULT_ERROR_VALUE, yGoal = DEFAULT_ERROR_VALUE;
            if (goal != null) {
                goalAngle = (int) calculateAngle((int) goal.x);
                yGoal = (int) goal.y;

                if (goalAngle < 0) {
                    xGoalSign = "-";
                    goalAngle = -goalAngle;
                }
            }

            int ballAngle = DEFAULT_ERROR_VALUE, yBall = DEFAULT_ERROR_VALUE;
            if (ball != null) {
                ballAngle = (int) calculateAngle((int) ball.x);
                yBall = (int) ball.y;

                if (ballAngle < 0) {
                    xBallSign = "-";
                    ballAngle = -ballAngle;
                }
            }

            int ourGoalAngle = DEFAULT_ERROR_VALUE, yOurGoal = DEFAULT_ERROR_VALUE;
            if (ourGoal != null) {
                ourGoalAngle = (int) calculateAngle((int) ourGoal.x);
                yOurGoal = (int) ourGoal.y;

                if (ourGoalAngle < 0) {
                    xOurGoalSign = "-";
                    ourGoalAngle = -ourGoalAngle;
                }
            }

            String shoot = "b";
            if (centersOfObjects != null && ball != null) {
                Core.putText(mRgba, "x:" + String.valueOf(ball.x) + " left " + centersOfObjects.get(0).x + "right" + centersOfObjects.get(1).x,
                        new Point(100, 100), 1, 1, BALL_CONTOUR_COLOR);
                if (centersOfObjects.get(0).x > ball.x && centersOfObjects.get(1).x < ball.x) {
                    if (nulaCelychNulaPlayer.isPlaying()) {
                        nulaCelychNulaPlayer.stop();
                    }
                    if (!ak47Player.isPlaying()) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ak47Player.start();
                            }
                        });
                        t.start();
                    }
                    Core.putText(mRgba, "KICK",
                            new Point(100, 120), 1, 1, BALL_CONTOUR_COLOR);
                    shoot = "a";
                }
            }

            String out = "x" + String.valueOf(goalAngle) + xGoalSign +
                    "y" + String.valueOf(yGoal) + DEFAULT_SIGN +
                    "z" + String.valueOf(ballAngle) + xBallSign +
                    "i" + String.valueOf(yBall) + DEFAULT_SIGN +
                    bluetoothAppendData[0] + bluetoothAppendData[1] + shoot;

            if (!nulaCelychNulaPlayer.isPlaying() && !ak47Player.isPlaying()) {
                if (ballAngle > -5 && ballAngle < 5) {
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            nulaCelychNulaPlayer.start();
                        }
                    });
                    t.start();
                }
            }

            if (!bluetoothAppendData[1].equals("")) {
                bluetoothAppendData[1] = "";
            }

            byte[] bytes = out.getBytes();
            try {
                bluetoothOutputStream.write(bytes);
                bluetoothOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ballCenterPoint = null;
        goalCenterPoint = null;
        ourGoalCenterPoint = null;

        if (showScaleButton.isChecked()) {
            Mat colorLabel = mRgba.submat(36, 70, 4, 36);
            colorLabel.setTo(mBlobColorRgba[1]);
            Mat spectrumLabel = mRgba.submat(6 + mSpectrum[0].rows(), 6 + mSpectrum[1].rows() + mSpectrum[0].rows()
                    , 70, 70 + mSpectrum[1].cols());
            mSpectrum[1].copyTo(spectrumLabel);

            Mat colorLabel2 = mRgba.submat(4, 36, 4, 36);
            colorLabel2.setTo(mBlobColorRgba[attackColorIndex]);
            Mat spectrumLabel2 = mRgba.submat(4, 4 + mSpectrum[0].rows(), 70, 70 + mSpectrum[0].cols());
            mSpectrum[attackColorIndex].copyTo(spectrumLabel2);
        }

        if (showHSV.isChecked()) {
            Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGB2HSV);
        }
        return mRgba;
    }

    /*
     * Pseudo-calculation
     */
    private double calculateAngle(int pixel) {
        return (pixel - CAMERA_CENTER_PIXEL) * CAMERA_VIEW_ANGLE_COEFFICIENT;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    /**
     *
     * @param y - parameter from camere 0-480
     * @return max width
     */
    private double maxBallSizeFromY(double y) {
        return 0.439166496856627 * y - 5;
    }

    class BallProperties {
        double width;
        double maxX, maxY;
        double minX, minY;
    }

    private List<Point> findBallMinMax(List<MatOfPoint> contours) {
        List<MatOfPoint> probablyBall = new ArrayList<MatOfPoint>();
        List<BallProperties> ballProperties = new ArrayList<BallProperties>();

        // Determine if is in range of width
        for (Iterator<MatOfPoint> objects = contours.iterator(); objects.hasNext(); ) {
            MatOfPoint pointsOfConture = objects.next();

            // Find min max X, Y
            double maxX = 0, maxY = 0,
                    minX = 9999, minY = 0;
            for (Iterator<Point> pointIterator = pointsOfConture.toList().iterator(); pointIterator.hasNext(); ) {
                Point point = pointIterator.next();
                if (point.x > maxX) {
                    maxX = point.x;
                    maxY = point.y;
                } else if (point.x < minX) {
                    minX = point.x;
                    minY = point.y;
                }
            }

            if (maxX - minX < maxBallSizeFromY((minY + maxY) / 2)) {
                BallProperties ballObject = new BallProperties();

                ballObject.width = maxX - minX;
                ballObject.minX = minX;
                ballObject.minY = minY;
                ballObject.maxX = maxX;
                ballObject.maxY = maxY;

                ballProperties.add(ballObject);
                probablyBall.add(pointsOfConture);
            }
        }

        int ballIndexInList = -1;
        int width = 0;

        for (int i = 0; i < probablyBall.size(); i++) {
            if (ballProperties.get(i).width > width) {
                ballIndexInList = i;
            }
        }

        if (ballIndexInList != -1) {
            List<Point> maxBallLikeObject = new ArrayList<Point>();
            maxBallLikeObject.add(new Point(ballProperties.get(ballIndexInList).maxX, ballProperties.get(ballIndexInList).maxY));
            maxBallLikeObject.add(new Point(ballProperties.get(ballIndexInList).minX, ballProperties.get(ballIndexInList).minY));
            return maxBallLikeObject;
        }

        return null;
    }


    private List<Point> determineMinMax(List<MatOfPoint> contours, int size) {
        List<Point> centersOfObjects = new ArrayList<Point>();

        MatOfPoint largestMat = this.findLargestMat(contours);
        double maxX = 0, maxY = 0,
                minX = 9999, minY = 0;
        if (largestMat != null) {
            for (Iterator<Point> pointIterator = largestMat.toList().iterator(); pointIterator.hasNext(); ) {
                Point point = pointIterator.next();
                if (point.x > maxX) {
                    maxX = point.x;
                    maxY = point.y;
                } else if (point.x < minX) {
                    minX = point.x;
                    minY = point.y;
                }
            }

            if (size != -1) {
                if (maxX - minX < size) {
                    return null;
                }
            }
            centersOfObjects.add(new Point(maxX, maxY));
            centersOfObjects.add(new Point(minX, minY));

            return centersOfObjects;
        }
        return null;
    }

    private MatOfPoint findLargestMat(List<MatOfPoint> contours) {
        int sizeOfMatOfPoint = 0;
        MatOfPoint largestMat = null;
        for (Iterator<MatOfPoint> iterator = contours.iterator(); iterator.hasNext(); ) {
            MatOfPoint currentMatOfPoint = iterator.next();
            if (currentMatOfPoint.toList().size() > sizeOfMatOfPoint) {
                sizeOfMatOfPoint = currentMatOfPoint.toList().size();
                largestMat = currentMatOfPoint;
            }
        }
        return largestMat;
    }

    private Point centerPoint(List<Point> minMaxPoints) {
        double x = 0, y = 0;
        for (int i = 0; i < minMaxPoints.size(); i++) {
            x += minMaxPoints.get(i).x;
            y += minMaxPoints.get(i).y;
        }
        return new Point(x / minMaxPoints.size(), y / minMaxPoints.size());
    }

    private void drawExtremes(List<Point> centersOfObjects) {
        for (int i = 0; i < centersOfObjects.size(); i++) {
            Core.rectangle(mRgba, centersOfObjects.get(i),
                    new Point(centersOfObjects.get(i).x + 1, centersOfObjects.get(i).y + 1), EXTREMES_CONTOUR_COLOR, 5);
            /*Core.putText(mRgba, String.valueOf(this.calculateAngle(centersOfObjects.get(i).x)) + " A",
                    centersOfObjects.get(i), 0, 0.5, EXTREMES_CONTOUR_COLOR);*/
        }
    }
}
