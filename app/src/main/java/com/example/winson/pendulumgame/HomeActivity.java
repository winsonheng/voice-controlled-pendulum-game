package com.example.winson.pendulumgame;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    //UI variables
    private AudioRecord aRecorder;
    private RelativeLayout homeLayout;
    private Canvas loadingCanvas;
    private Bitmap loadingBitmap;
    private ImageView loadingImage, playButton, calibrateButton,character,info;
    private double screenWidth, screenHeight;
    private TextView loadingText,scoreText,textView,dummyText,titleText, frequencyText;
    private CeilingImageView ceilingImageView,ceilingImageView2;
    private ObjectAnimator animator, addPendulum,addLamps,characterAnimator;
    private PendulumImageView rightPendulum;
    private SpikesImageView spikes;
    private Vibrator vibrator;
    private View.OnTouchListener touchListener;

    //to be used in gameplay
    private ArrayList<PendulumImageView> pendulumList = new ArrayList<>();
    private int currentPendulum;

    //some important values
    private static final int SAMPLE_RATE = 8000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    private float minY;
    private float maxY;
    private float allowance;
    private float pendulumPlatformHeight;
    private float pendulumHeight;
    private float calibratedValue;
    private boolean isCalibrating, isPlaying,isInMidAir,isScrolling,gameover;
    private float[] forCalibration;
    private float rateOfScroll;//per 10 ms
    private float charVelocityX;
    private float charVelocityY;
    private float charAcceleration;

    private int score;
    private File file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_home);

        isCalibrating=false;
        isPlaying=false;
        homeLayout=(RelativeLayout)findViewById(R.id.home_layout);
        loadingImage=(ImageView)findViewById(R.id.loading_image);
        playButton=(ImageView)findViewById(R.id.play_button);
        calibrateButton=(ImageView)findViewById(R.id.calibrate_button);
        loadingText=(TextView)findViewById(R.id.loading_text);
        textView=(TextView)findViewById(R.id.textView);
        dummyText=(TextView)findViewById(R.id.dummyText);
        Point point=new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        screenWidth=point.x;
        rateOfScroll=(float)(screenWidth/700);
        screenHeight=point.y;
        charAcceleration=(float)screenHeight/20;
        vibrator= (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        loadingScreen();
    }

    public void loadingScreen(){
        calibratedValue=-1;
        record(true);
        isCalibrating=true;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)loadingImage.getLayoutParams();
        params.width=2*(int)screenWidth;
        params.setMargins(0,0,-(int)screenWidth,0);
        loadingImage.setLayoutParams(params);
        loadingBitmap=Bitmap.createBitmap(2*(int)screenWidth,(int)screenHeight/5, Bitmap.Config.ARGB_8888);
        loadingCanvas=new Canvas(loadingBitmap);
        loadingImage.setImageBitmap(loadingBitmap);
        //loadingImage.setBackgroundColor(Color.BLACK);
        Path path =new Path();
        Random r=new Random();
        float width = 2*(float)screenWidth;
        float middle=(float) screenHeight/10;
        int sign=1;
        path.moveTo(0,middle);
        for(int i=1;i<=150;++i){
            if(i==150){
                sign=0;
            }
            path.lineTo((width/300)*i,middle+r.nextFloat()*middle*sign);
            sign*=-1;
        }
        path.lineTo(width,middle);
        Paint paint=new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        loadingCanvas.drawPath(path, paint);
        homeLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                homeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadingImage.setTranslationX(-(int)screenWidth);
                loadingImage.animate().translationX(-loadingImage.getHeight()/2).setDuration(5000).setInterpolator(new LinearInterpolator()).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if(loadingImage.getTranslationX()==-loadingImage.getHeight()/2) {
                            loadingImage.animate().setListener(null);
                            loadingFinished();
                        }
                    }
                }).start();
            }
        });
    }
    public void loadingFinished(){
        loadingImage.animate().translationY(-loadingImage.getBottom()).setDuration(500).setInterpolator(new AccelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                loadingImage.animate().setListener(null);
                homeLayout.removeView(loadingImage);
            }
        }).start();
        loadingText.animate().translationY(loadingText.getTop()).setDuration(500).setInterpolator(new AccelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                loadingText.animate().setListener(null);
                homeLayout.removeView(loadingText);
                mainScreen();
            }
        }).start();

    }
    public void mainScreen(){
        isScrolling=false;
        score=0;
        pendulumList.clear();
        pendulumPlatformHeight=(float)(screenWidth*637/15552);
        ceilingImageView = new CeilingImageView(this);
        RelativeLayout.LayoutParams ceilingParams = new RelativeLayout.LayoutParams((int)screenWidth*2,(int)screenHeight/12);
        ceilingParams.setMargins(0,0,-(int)screenWidth,0);
        homeLayout.addView(ceilingImageView,ceilingParams);
        ceilingImageView2 = new CeilingImageView(this);
        homeLayout.addView(ceilingImageView2,ceilingParams);
        ceilingImageView2.setTranslationX((int)screenWidth);
        ceilingImageView.setTranslationY(-(int)screenHeight/12);
        ceilingImageView2.setTranslationY(-(int)screenHeight/12);
        ceilingImageView.animate().translationY(0).setDuration(700).setInterpolator(new DecelerateInterpolator()).start();
        ceilingImageView2.animate().translationY(0).setDuration(700).setInterpolator(new DecelerateInterpolator()).start();
        ViewGroup.LayoutParams playParams = playButton.getLayoutParams();
        playParams.width=(int)(screenWidth/3.5);
        playParams.height=(int)(screenWidth/3.5/163*69);
        playButton.setLayoutParams(playParams);
        playButton.setTranslationY(-(int)screenHeight/2);
        playButton.setVisibility(View.VISIBLE);
        playButton.animate().translationY(0).setDuration(1000).setInterpolator(new AccelerateDecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(playButton.getTranslationY()==0){
                    playButton.animate().setListener(null);
                    playButton.setClickable(true);
                }
            }
        }).start();
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButton.setClickable(false);
                calibrateButton.setClickable(false);
                playGame();
            }
        });
        if(playButton.getParent()==null){
            homeLayout.addView(playButton,playParams);
        }
        titleText=new TextView(this);
        titleText.setText("Physic Chic");
        titleText.setTextColor(Color.parseColor("#ffa500"));
        titleText.setTypeface(Typeface.createFromAsset(getAssets(),"Xoxoxa.ttf"));
        titleText.setTextSize(50);
        titleText.setPadding(0,0,0,(int)screenHeight/20);
        RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        titleParams.addRule(RelativeLayout.ABOVE,R.id.play_button);
        homeLayout.addView(titleText,titleParams);

        RelativeLayout.LayoutParams calibrateParams = (RelativeLayout.LayoutParams)calibrateButton.getLayoutParams();
        calibrateParams.width=(int)(screenWidth/3.5);
        calibrateParams.height=(int)(screenWidth/3.5/297*104);
        calibrateParams.setMargins(0,(int)screenHeight/30,0,0);
        calibrateButton.setLayoutParams(calibrateParams);
        calibrateButton.setTranslationY(-(int)(screenHeight/2+playButton.getHeight()+20));
        calibrateButton.setVisibility(View.VISIBLE);
        calibrateButton.animate().translationY(0).setDuration(1000).setInterpolator(new AccelerateDecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(calibrateButton.getTranslationY()==0){
                    calibrateButton.animate().setListener(null);
                    calibrateButton.setClickable(true);
                }
            }
        }).start();
        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrate();
            }
        });
        if(calibrateButton.getParent()==null){
            homeLayout.addView(calibrateButton,calibrateParams);
        }
        info=new ImageView(this);
        info.setImageResource(R.drawable.question_mark);
        RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams((int)screenHeight/10,(int)screenHeight/10);
        infoParams.setMargins(0,(int)screenHeight/40,0,0);
        infoParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        infoParams.addRule(RelativeLayout.BELOW,R.id.calibrate_button);
        homeLayout.addView(info,infoParams);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog();
            }
        });

        final PendulumImageView leftPendulum = new PendulumImageView(this);
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams((int)(screenWidth/6),(int)(screenWidth*637/864));
        leftPendulum.setX((float)(screenWidth/8));
        Random random = new Random();
        pendulumHeight=(float)(screenWidth*637/864);
        minY = (float)(screenHeight/2-pendulumHeight);
        if(screenHeight/12>screenHeight-pendulumHeight){
            maxY=(float)(screenHeight-pendulumHeight);
        }
        else{
            maxY=(float)(screenHeight/12);
        }
        allowance=maxY-minY;
        System.out.println(minY+" , allow+ "+allowance);
        float leftY=minY+random.nextFloat()*allowance;
        leftParams.setMargins(0,(int)leftY,0,0);
        leftPendulum.setY(leftY);
        homeLayout.addView(leftPendulum,leftParams);
        leftPendulum.setTranslationY(-leftPendulum.getBottom());
        leftPendulum.animate().translationY(0).setDuration(1000).setInterpolator(new AccelerateDecelerateInterpolator()).start();

        if(spikes==null||spikes.getParent()==null){
            spikes=new SpikesImageView(this);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int)screenWidth,(int)pendulumPlatformHeight/2);
            homeLayout.addView(spikes,params);
        }


        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams((int)(screenWidth/6),(int)pendulumHeight);
        rightPendulum = new PendulumImageView(this);
        rightPendulum.setX((float)(screenWidth-screenWidth/8-screenWidth/6));
        float rightY=minY+random.nextFloat()*allowance;
        rightParams.setMargins(0,(int)rightY,0,0);
        rightPendulum.setY(rightY);
        homeLayout.addView(rightPendulum,rightParams);
        rightPendulum.setTranslationY(-rightPendulum.getBottom());
        rightPendulum.animate().translationY(0).setDuration(1000).setInterpolator(new AccelerateDecelerateInterpolator()).start();

        leftPendulum.pauseAnimation();
        rightPendulum.pauseAnimation();

        pendulumList.add(leftPendulum);
        pendulumList.add(rightPendulum);

        character=new ImageView(this);
        character.setImageResource(R.drawable.main_character);
        character.setX((float)(screenWidth/8+screenWidth/12-screenWidth/48));
        character.setY((float)(pendulumHeight+leftY-pendulumPlatformHeight-screenHeight/12));
        RelativeLayout.LayoutParams characterParams = new RelativeLayout.LayoutParams((int)(screenWidth/24),(int)(screenWidth*11/168));
        homeLayout.addView(character,characterParams);
    }
    public void playGame(){
        if(isCalibrating) {
            stopRecording();
            isCalibrating=false;
            calibratedValue = 50;
        }


        isPlaying=true;
        isInMidAir=false;
        gameover=false;
        record(false);
        currentPendulum=0;
        charVelocityX=0;
        charVelocityY=0;
        pendulumList.get(0).setActive(true);
        homeLayout.removeView(info);
        frequencyText=new TextView(this);
        frequencyText.setText("Frequency");
        frequencyText.setTextColor(Color.parseColor("#ff0000"));
        frequencyText.setTextSize(30);
        homeLayout.addView(frequencyText);
        playButton.animate().translationY(-(int)screenHeight/2).setDuration(400).setInterpolator(new AccelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(playButton.getTranslationY()==-screenHeight/2){
                    playButton.animate().setListener(null);
                    homeLayout.removeView(playButton);
                }
            }
        }).start();
        info.animate().translationY((int)(screenHeight-info.getY())).setDuration(400).setInterpolator(new AccelerateInterpolator()).start();
        titleText.animate().translationY(-(int)screenHeight/2).setDuration(400).setInterpolator(new AccelerateInterpolator()).start();
        calibrateButton.animate().translationY(-(int)(screenHeight/2+playButton.getHeight()+20)).setDuration(400).setInterpolator(new AccelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(calibrateButton.getTranslationY()==-(screenHeight/2+playButton.getHeight()+20)){
                    calibrateButton.animate().setListener(null);
                    homeLayout.removeView(calibrateButton);
                    touchListener=new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if(!isInMidAir) {
                                homeLayout.setOnTouchListener(null);
                                PendulumImageView pendulum = pendulumList.get(currentPendulum);
                                pendulum.setActive(false);
                                float velocity = (float)(pendulum.getAngularVelocity() * (character.getY() - screenHeight/12));
                                double angle=Math.toRadians(pendulum.getRotation());
                                int direction=pendulum.getDirection();
                                if(direction==1) {
                                    charVelocityX = (float) -Math.abs(velocity * Math.cos(angle));
                                    charVelocityY = (float) -Math.abs(velocity * Math.sin(angle));
                                }else if(direction==2){
                                    charVelocityX = (float) -Math.abs(velocity * Math.cos(angle));
                                    charVelocityY = (float) Math.abs(velocity * Math.sin(angle));
                                }else if(direction==3){
                                    charVelocityX = (float) Math.abs(velocity * Math.cos(angle));
                                    charVelocityY = (float) -Math.abs(velocity * Math.sin(angle));
                                }else{
                                    charVelocityX = (float) Math.abs(velocity * Math.cos(angle));
                                    charVelocityY = (float) Math.abs(velocity * Math.sin(angle));
                                }
                                isInMidAir=true;
                                float y = character.getY();
                                character.setX((float)(character.getX()+y*Math.sin(-angle)));
                                character.setY((float)(y*Math.cos(angle)));
                                character.setPivotX(character.getWidth()/2);
                                character.setPivotY(character.getHeight()/2);
                                character.animate().rotation(0).setDuration(0).start();
                                homeLayout.setOnTouchListener(this);
                            }
                            return false;
                        }
                    };
                    homeLayout.setOnTouchListener(touchListener);
                }
            }
        }).start();
        characterAnimator = ObjectAnimator.ofFloat(character,"alpha",1).setDuration(10);
        characterAnimator.setRepeatCount(ValueAnimator.INFINITE);
        characterAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                character.setX(character.getX()+charVelocityX/200);
                character.setY(character.getY()-charVelocityY/200);
                if(character.getY()>=screenHeight||character.getX()<=-screenHeight/2){
                    gameover(true);
                    return;
                }
                if(isInMidAir){
                    charVelocityY-=charAcceleration;
                    if(character.getY()<=screenHeight/12){
                        charVelocityY*=-1;
                        character.setY((float)screenHeight/12+1);
                    }
                    if(pendulumList.size()<=1){
                        return;
                    }
                    PendulumImageView pendulum = pendulumList.get(currentPendulum+1);
                    if((character.getX()>=pendulum.getX()-character.getWidth() && character.getX()<=pendulum.getX()+pendulum.getWidth()) && character.getY()+character.getHeight()>=pendulum.getRequiredCharacterY()-20 &&  character.getY()+character.getHeight()<=pendulum.getRequiredCharacterY()+20){
                        charVelocityX=0;
                        charVelocityX=-rateOfScroll*200;
                        charVelocityY=0;
                        character.setY(pendulum.getRequiredCharacterY());
                        character.setX((float)(pendulum.getX()+pendulum.getWidth()/2-screenWidth/48));
                        character.animate().x(pendulum.getX()-rateOfScroll*50+pendulum.getWidth()/2).setDuration(500).start();
                        if(pendulum.equals(rightPendulum)){
                            startScrolling();
                            rightPendulum.startAnimation();
                        }
                        isInMidAir=false;
                        pendulumList.remove(currentPendulum);
                        pendulum.setActive(true);
                        ++score;
                        scoreText.setText(score+"");
                        vibrator.vibrate(500);
                    }
                }
            }
        });
        characterAnimator.start();
        scoreText=new TextView(this);
        scoreText.setTextSize(30);
        scoreText.setTextColor(Color.WHITE);
        scoreText.setText("0");
        //scoreText.setPadding(0,(int)screenHeight/36,0,0);
        RelativeLayout.LayoutParams scoreParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scoreParams.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
        scoreText.setY((float)screenHeight/9);
        homeLayout.addView(scoreText,scoreParams);
        scoreText.setTranslationY(-(float)screenHeight);
        scoreText.animate().translationY((float)(screenHeight/6)).setDuration(750).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        addPendulum = ObjectAnimator.ofFloat(dummyText,"alpha",1);
        addPendulum.setDuration(7000);
        addPendulum.setRepeatCount(ValueAnimator.INFINITE);
        addPendulum.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                    addPendulum();
            }
        });
        addLamps=ObjectAnimator.ofFloat(textView,"alpha",1);
        addLamps.setDuration(3000);
        addLamps.setRepeatCount(ValueAnimator.INFINITE);
        addLamps.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                final ImageView imageView=new ImageView(HomeActivity.this);
                imageView.setImageResource(R.drawable.wall_lamp);
                imageView.setX((float)screenWidth);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int)screenWidth/9,(int)screenWidth/9);
                params.addRule(RelativeLayout.CENTER_VERTICAL);
                params.setMargins(0,0,(int)-screenWidth,0);
                homeLayout.addView(imageView,params);
                imageView.animate().alphaBy((float)-0.7).xBy(-250*rateOfScroll).setDuration(2500).setInterpolator(new LinearInterpolator()).setListener(new AnimatorListenerAdapter() {
                    int sign=1;
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if(imageView.getX()<=-screenWidth/9){
                            imageView.animate().setListener(null);
                            homeLayout.removeView(imageView);
                            return;
                        }
                        imageView.animate().setListener(null);
                        sign*=-1;
                        imageView.animate().alphaBy((float)-0.7*sign).xBy(-250*rateOfScroll).setDuration(2500).setInterpolator(new LinearInterpolator()).setListener(this).start();
                    }
                }).start();
            }
        });
    }
    public void addPendulum(){
        Random r = new Random();
        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams((int) (screenWidth / 6), (int) pendulumHeight);
        PendulumImageView pendulum = new PendulumImageView(HomeActivity.this);
        System.out.println(minY+" , "+allowance);
        float rightY = minY +r.nextFloat() * allowance;
        rightParams.setMargins(0, (int) rightY, (int) (-screenWidth/6), 0);
        pendulum.setX((float) (screenWidth));
        pendulum.setY(rightY);
        homeLayout.addView(pendulum, rightParams);
        pendulum.setTranslationY(-pendulum.getBottom());
        pendulum.animate().translationY(0).setDuration(0).setInterpolator(new AccelerateDecelerateInterpolator()).start();

        pendulum.startAnimation();
        pendulumList.add(pendulum);
    }
    public void startScrolling(){
        ceilingImageView.animate().translationX(-(int)screenWidth).setDuration(7000).setInterpolator(new LinearInterpolator()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(ceilingImageView.getTranslationX()==-(int)screenWidth){
                    ceilingImageView.animate().setListener(null);
                    ceilingImageView.setTranslationX((int)screenWidth-5);
                    ceilingImageView.animate().translationX(-(int)screenWidth).setDuration(14000).setInterpolator(new LinearInterpolator()).setListener(this).start();
                }
            }
        }).start();
        ceilingImageView2.animate().translationX(-(int)screenWidth).setDuration(14000).setInterpolator(new LinearInterpolator()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(ceilingImageView2.getTranslationX()==-(int)screenWidth){
                    ceilingImageView2.animate().setListener(null);
                    ceilingImageView2.setTranslationX((int)screenWidth-5);
                    ceilingImageView2.animate().translationX(-(int)screenWidth).setDuration(14000).setInterpolator(new LinearInterpolator()).setListener(this).start();
                }
            }
        }).start();
        addPendulum.start();
        addLamps.start();
        isScrolling=true;
    }
    public void gameover(boolean pendulumMode){
        isPlaying=false;
        isInMidAir=false;
        addPendulum.cancel();
        addLamps.cancel();
        characterAnimator.cancel();
        stopRecording();
        homeLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        homeLayout.removeView(character);
        if(pendulumMode){
            System.out.println("gameover");
            final TextView gameover = new TextView(this);
            scoreText.animate().y((float)(screenHeight/2-scoreText.getHeight()/2)).setDuration(1000).setInterpolator(new DecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if(scoreText.getY()==screenHeight/2-scoreText.getHeight()/2){
                        scoreText.animate().setListener(null);
                        homeLayout.removeView(scoreText);
                        TextView score = new TextView(HomeActivity.this);
                        score.setText("Your Score: "+HomeActivity.this.score);
                        score.setTextSize(30);
                        score.setTextColor(Color.WHITE);
                        RelativeLayout.LayoutParams scoreParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        scoreParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                        score.setId(View.generateViewId());
                        homeLayout.addView(score,scoreParams);
                        gameover.setText("Game Over!");
                        gameover.setTypeface(Typeface.createFromAsset(getAssets(), "Xoxoxa.ttf"));
                        gameover.setTextColor(Color.RED);
                        gameover.setTextSize(50);
                        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        textParams.addRule(RelativeLayout.ABOVE,score.getId());
                        gameover.setPadding(0,(int)screenHeight/24,0,(int)screenHeight/24);
                        homeLayout.addView(gameover,textParams);
                        LinearLayout ll = new LinearLayout(HomeActivity.this);
                        ll.setOrientation(LinearLayout.HORIZONTAL);
                        Button ok = new Button(HomeActivity.this);
                        ok.setTextColor(Color.WHITE);
                        ok.setBackgroundColor(Color.GRAY);
                        ok.setText("Return");
                        ok.setPadding(40,0,40,0);
                        LinearLayout.LayoutParams okParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                        okParams.setMargins(50,0,50,0);
                        ll.addView(ok,okParams);
                        ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                homeLayout.removeAllViews();
                                mainScreen();
                            }
                        });
                        Button share= new Button(HomeActivity.this);
                        share.setTextColor(Color.WHITE);
                        share.setBackgroundColor(Color.GRAY);
                        share.setText("Share!");
                        share.setPadding(40,0,40,0);
                        ll.addView(share,okParams);
                        share.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                shareScreenshot(v);
                            }
                        });
                        RelativeLayout.LayoutParams llParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        llParams.setMargins(0,(int)screenHeight/12,0,0);
                        llParams.addRule(RelativeLayout.BELOW,score.getId());
                        llParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        homeLayout.addView(ll, llParams);
                    }
                }
            }).start();
        }
    }
    public void calibrate(){
        calibratedValue=-1;
        playButton.setVisibility(View.INVISIBLE);
        calibrateButton.setVisibility(View.INVISIBLE);
        info.setVisibility(View.INVISIBLE);
        titleText.setVisibility(View.INVISIBLE);
        final ImageView loading = new ImageView(this);
        loading.setBackgroundResource(R.drawable.calibrate_loading);
        loading.setId(View.generateViewId());
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int)(screenHeight/6),(int)(screenHeight/6));
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        homeLayout.addView(loading,params);

        final ImageView tick = new ImageView(HomeActivity.this);
        tick.setImageResource(R.drawable.green_tick);
        tick.setVisibility(View.INVISIBLE);
        homeLayout.addView(tick,params);

        final ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(loading,"rotation",1000000).setDuration(100000000/36);
        rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimator.setInterpolator(new LinearInterpolator());
        rotateAnimator.start();
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final TextView calibrating =new TextView(this);
        calibrating.setText("CALIBRATING");
        calibrating.setTextSize(35);
        calibrating.setTextColor(Color.WHITE);
        calibrating.setTypeface(Typeface.DEFAULT_BOLD);
        textParams.addRule(RelativeLayout.ABOVE,loading.getId());
        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        textParams.setMargins(0,0,0,(int)(screenHeight/24));
        homeLayout.addView(calibrating,textParams);

        final Button button =new Button(HomeActivity.this);
        button.setText("RETURN");
        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.BELOW,loading.getId());
        buttonParams.setMargins(0,(int)(screenHeight/18),0,0);
        button.setPadding(40,0,40,0);
        buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.GRAY);
        homeLayout.addView(button,buttonParams);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeLayout.removeView(calibrating);
                homeLayout.removeView(loading);
                homeLayout.removeView(button);
                homeLayout.removeView(tick);
                playButton.setVisibility(View.VISIBLE);
                calibrateButton.setVisibility(View.VISIBLE);
                info.setVisibility(View.VISIBLE);
                titleText.setVisibility(View.VISIBLE);
                if(isCalibrating){
                    stopRecording();
                    isCalibrating=false;
                }
            }
        });

        homeLayout.setClickable(false);
        //listener called when calibration finishes
        //by performing click when calibration is complete
        homeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(calibratedValue>-1) {
                    homeLayout.setOnClickListener(null);
                    Animation animation = AnimationUtils.loadAnimation(HomeActivity.this,R.anim.scale_disappear);
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            animation.setAnimationListener(null);
                            calibrating.setText("COMPLETE");
                            calibrating.setTextColor(Color.GREEN);
                            loading.setVisibility(View.INVISIBLE);
                            tick.setVisibility(View.VISIBLE);
                            //homeLayout.addView(tick,params);
                            //homeLayout.removeView(remainQuiet);
                            //loading.setVisibility(View.INVISIBLE);
                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    homeLayout.removeView(calibrating);
                                    homeLayout.removeView(loading);
                                    homeLayout.removeView(tick);
                                    homeLayout.removeView(button);
                                    playButton.setVisibility(View.VISIBLE);
                                    calibrateButton.setVisibility(View.VISIBLE);
                                    info.setVisibility(View.VISIBLE);
                                    titleText.setVisibility(View.VISIBLE);
                                }
                            });
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    loading.startAnimation(animation);

                }

            }
        });
        if(!isCalibrating) {
            isCalibrating = true;
            record(true);
        }
    }
    public void finishCalibrate(){
        isCalibrating=false;
        if(playButton.getVisibility()==View.INVISIBLE){

        }
    }

    public void shareScreenshot(View view){

        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        store(getScreenShot(rootView),"screenshot");
        Uri uri = Uri.fromFile(file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");

        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "add subject here");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "add text here");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        try {
            startActivity(Intent.createChooser(intent, "Share Screenshot"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), "No App Available", Toast.LENGTH_SHORT).show();
        }
    }

    public Bitmap getScreenShot(View view) {
        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    public void store(Bitmap bm, String fileName){
        final String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Screenshots";
        File dir = new File(dirPath);
        if(!dir.exists())
            dir.mkdirs();
        file = new File(dirPath, fileName);
        try {
            FileOutputStream fOut = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void record(boolean calibrate){
        forCalibration=new float[]{-100,-100,-100};
        aRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        aRecorder.startRecording();

        /**
         mRecorder=new MediaRecorder();
         mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
         mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
         mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
         mRecorder.setOutputFile("/dev/null");
         try {
         mRecorder.prepare();
         } catch (IOException e) {
         e.printStackTrace();
         }
         mRecorder.start();
         */
        animator=ObjectAnimator.ofFloat(homeLayout,"translationX",0).setDuration(100);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                super.onAnimationRepeat(animation);
                processInput();
            }
        });
        animator.start();
    }

    public void processInput(){
        short[] buffers = new short[BUFFER_SIZE];
        int bufferNumber = 0;

        if(aRecorder!=null){
            bufferNumber = aRecorder.read(buffers, 0, BUFFER_SIZE);
            float sum=0;
            for (int i = 0; i < bufferNumber; i++) {
                sum += Math.abs(buffers[i]);
            }
            float mean = Math.abs((sum / bufferNumber));
            //System.out.println("Mean: "+mean);
            //System.out.println("Buffer number"+bufferNumber);
            if(isCalibrating){
                forCalibration[0]=forCalibration[1];
                forCalibration[1]=forCalibration[2];
                forCalibration[2]=mean;
                if(Math.abs(forCalibration[0]-forCalibration[1])<=30&&Math.abs(forCalibration[1]-forCalibration[2])<=30&&Math.abs(forCalibration[0]-forCalibration[2])<=30){
                    calibratedValue=(forCalibration[0]+forCalibration[1]+forCalibration[2])/3;
                    stopRecording();
                    isCalibrating=false;
                    homeLayout.setClickable(true);
                    homeLayout.performClick();
                }
            }
            else if(isPlaying){
                int numSamples = buffers.length;
                int numCrossing = 0;
                for (int p = 0; p < numSamples - 1; p++) {
                    if ((buffers[p] > 0 && buffers[p + 1] <= 0) ||
                            (buffers[p] < 0 && buffers[p + 1] >= 0)) {
                        numCrossing++;
                    }
                }
                float numSecondsRecorded = (float) numSamples / (float) SAMPLE_RATE;
                float numCycles = numCrossing / 2;
                float frequency = numCycles / numSecondsRecorded;
                frequencyText.setText("Frequency: "+frequency);

                pendulumList.get(currentPendulum).sway(frequency,mean-calibratedValue);
            }
        }
    }
    public void showAlertDialog(){


        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.tutorial));
        alertDialog.setMessage(getString(R.string.tutorial_content));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();

    }
    public void stopRecording(){
        if(animator!=null){
            System.out.println("CANCELLED");
            animator.cancel();
        }
        if(aRecorder!=null&&aRecorder.getState()==AudioRecord.STATE_INITIALIZED) {
            System.out.println("STOP AND RELEASED");
            aRecorder.stop();
            aRecorder.release();
        }
    }
    public void onPause(){
        super.onPause();
        stopRecording();
        System.out.println("Paused");
    }
    public void onResume(){
        super.onResume();
        if(isCalibrating){
            if(aRecorder.getState()!=AudioRecord.STATE_INITIALIZED) {
                System.out.println("CALIBRATING");
                record(true);
            }
            return;
        }
        if(isPlaying){
            if(aRecorder.getState()!=AudioRecord.STATE_INITIALIZED){
                record(false);
                System.out.println("PLAYING");
            }
        }
    }

    public class PendulumImageView extends ImageView{
        private boolean active,nextSwing, moving;
        private float requiredCharacterY;
        private ObjectAnimator animatorSet ;
        private float frequency, amplitude, maxAmplitude, omegaTimesAmplitude, currentAmplitude;
        private int direction;

        public PendulumImageView(Context context){
            super(context);
            setImageResource(R.drawable.pendulum);
            active=false;
            moving=false;
            nextSwing=false;
            ceilingImageView.bringToFront();
            ceilingImageView2.bringToFront();
            amplitude=0;
            frequency=100;
            direction=3;
            animatorSet = ObjectAnimator.ofFloat(this,"alpha",1).setDuration(10);
            animatorSet.setRepeatCount(ValueAnimator.INFINITE);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationRepeat(Animator animation) {
                    /**
                     if(getRotation()==0&&angularVelocity==0){
                     //angularVelocity=(float)(0.04*Math.PI*frequency);
                     angularVelocity=(float)(-amplitude);
                     }
                     else if(Math.abs(getRotation())>=amplitude){
                     if(getRotation()<=0) {
                     //angularAcceleration = (float) (Math.pow(2 * Math.PI * frequency, 2) * amplitude);
                     angularAcceleration=(float)(Math.sin(amplitude));
                     }
                     else{
                     //angularAcceleration = (float) (-Math.pow(2 * Math.PI * frequency, 2) * amplitude);
                     angularAcceleration=(float)(-Math.sin(amplitude));
                     }
                     angularVelocity+=0.5*angularAcceleration;
                     }
                     else{
                     //angularAcceleration=(float)(-Math.pow(2*Math.PI*frequency,2)*getRotation());
                     angularAcceleration=(float)(-Math.sin(getRotation()));
                     angularVelocity+=0.5*angularAcceleration;
                     }
                     System.out.println(rotation+"amplitude is, "+amplitude);
                     if(Math.abs(rotation)>amplitude){
                     if(rotation<=0) {
                     timeTimesOmega = (float) Math.asin(-1);
                     }
                     else{
                     timeTimesOmega=(float)Math.asin(1);
                     }
                     }
                     else {
                     timeTimesOmega = (float) Math.asin(rotation / amplitude);
                     }
                     float velocity=(float)(omega*amplitude*Math.cos(timeTimesOmega));
                     if(velocity>0){
                     direction=1;
                     }
                     else if(velocity==0){
                     if(rotation<=0){
                     direction=1;
                     }
                     else{
                     direction=-1;
                     }
                     }
                     else{
                     direction=-1;
                     }
                     rotation=(float)(direction*Math.abs(rotation-amplitude*Math.sin(timeTimesOmega+0.02*omega))+rotation);
                     //System.out.println("Angular Acceleration: "+angularAcceleration+" , velocity:  "+angularVelocity);
                     //setRotation((float)(getRotation()+0.02*angularVelocity));
                     //System.out.println(timeTimesOmega+", new angle is "+ amplitude*Math.sin(timeTimesOmega+0.04*Math.PI*frequency));
                     setRotation(rotation);
                     */
                    if(moving) {
                        setX((float) (getX() - rateOfScroll));
                        if(getX()<=-screenWidth/2){
                            homeLayout.removeView(PendulumImageView.this);
                            pendulumList.remove(PendulumImageView.this);
                        }
                    }
                    if(nextSwing&&active) {
                        nextSwing=false;
                        omegaTimesAmplitude=(float)(2*Math.PI*frequency*Math.toRadians(amplitude));
                        currentAmplitude=amplitude;
                        if (getRotation() == 0) {
                            if (direction == 1) {
                                animate().rotation(amplitude).setDuration((long) (1000 / frequency)).setInterpolator(new DecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        animate().setListener(null);
                                        nextSwing = true;
                                    }
                                }).start();
                                character.animate().rotation(amplitude).setDuration((long) (1000 / frequency)).setInterpolator(new DecelerateInterpolator()).start();
                                direction = 2;
                            } else {
                                animate().rotation(-amplitude).setDuration((long) (1000 / frequency)).setInterpolator(new DecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        animate().setListener(null);
                                        nextSwing = true;
                                    }
                                }).start();
                                character.animate().rotation(-amplitude).setDuration((long) (1000 / frequency)).setInterpolator(new DecelerateInterpolator()).start();
                                direction = 4;
                            }
                        } else if (getRotation() < 0){
                            animate().rotation(0).setDuration((long)(1000/frequency)).setInterpolator(new AccelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    animate().setListener(null);
                                    nextSwing=true;
                                }
                            }).start();
                            character.animate().rotation(0).setDuration((long)(1000/frequency)).setInterpolator(new AccelerateInterpolator()).start();
                            direction=1;
                        }else{
                            animate().rotation(0).setDuration((long)(1000/frequency)).setInterpolator(new AccelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    animate().setListener(null);
                                    nextSwing=true;
                                }
                            }).start();
                            character.animate().rotation(0).setDuration((long)(1000/frequency)).setInterpolator(new AccelerateInterpolator()).start();
                            direction=3;
                        }
                    }
                }
            });
        }
        public int getDirection(){
            return direction;
        }
        public float getAngularVelocity(){
            if(Math.abs(getRotation())>currentAmplitude){
                if(getRotation()<0) {
                    return (float) (omegaTimesAmplitude * Math.cos(Math.asin(-1)));
                }
                else{
                    return (float) (omegaTimesAmplitude * Math.cos(Math.asin(1)));
                }
            }
            return (float)(omegaTimesAmplitude*Math.cos(Math.asin(getRotation()/currentAmplitude)));
        }
        public void pauseAnimation(){
            moving=false;
        }
        public void startAnimation(){
            moving=true;
        }
        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            ceilingImageView.bringToFront();
            ceilingImageView2.bringToFront();
            requiredCharacterY=(float)(pendulumHeight+getY()-pendulumPlatformHeight-screenHeight/12);
            setPivotX((float)(screenWidth/12));
            setPivotY((float)(screenHeight/12-getY()));
            maxAmplitude=10000-calibratedValue;
            if(!animatorSet.isStarted()){
                animatorSet.start();
            }
        }

        public float getRequiredCharacterY(){
            return requiredCharacterY;
        }
        public void setActive(boolean active){
            this.active=active;
            if(active) {
                nextSwing=true;
                character.setPivotX(character.getWidth() / 2);
                character.setPivotY((float) (-character.getY() + screenHeight / 12));
            }
            else{
                character.animate().rotationBy(0).setDuration(0).start();
                animate().translationYBy((float)(screenHeight-getY())).setDuration((long)((screenHeight-getY())/screenHeight*1000)).setInterpolator(new AccelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if(getTranslationX()==screenHeight-getY()) {
                            PendulumImageView.this.animate().setListener(null);
                            homeLayout.removeView(PendulumImageView.this);
                        }
                    }
                }).start();
            }
        }
        public void sway(float frequency, float amp){
            if(!active){
                return;
            }
            this.frequency=frequency/150;
            if(amp<calibratedValue){
                amplitude=1;
            }
            else if(amp>=maxAmplitude){
                amplitude=80;
            }
            else {
                if(amp==0){
                    ++amp;
                }
                amplitude = (amp / maxAmplitude) * 80;
            }
            if(!animatorSet.isRunning()){
                animatorSet.start();
            }
        }
    }

    public class CeilingImageView extends ImageView{
        private Bitmap bitmap;
        private Canvas canvas;
        private Paint paint;
        private double width;
        private double height;

        public CeilingImageView(Context context){
            super(context);
            width=screenWidth*2;
            height=screenHeight/12;
            bitmap= Bitmap.createBitmap((int)width,(int)height, Bitmap.Config.ARGB_8888);
            paint=new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(4);
            paint.setColor(Color.WHITE);
            canvas = new Canvas(bitmap);
            setImageBitmap(bitmap);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            Paint fill = new Paint();
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(Color.GRAY);
            canvas.drawPaint(fill);

            float space=(float)screenWidth/80;
            float verticalMargin=(float)height/4;
            for(int i=1;i<159;i+=4){
                canvas.drawLine(space*i,verticalMargin,space*(i+2),(float)height-verticalMargin,paint);
            }
            canvas.drawLine(0,(float)height,(float)width,(float)height,paint);
        }
    }

    public class SpikesImageView extends ImageView{
        private Bitmap bitmap;
        private Canvas canvas;
        private Paint paint;
        private double width;
        private double height;

        public SpikesImageView(Context context){
            super(context);
            width=screenWidth;
            height=pendulumPlatformHeight/2;
            bitmap= Bitmap.createBitmap((int)width,(int)height, Bitmap.Config.ARGB_8888);
            paint=new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(1);
            paint.setColor(Color.WHITE);
            canvas = new Canvas(bitmap);
            setImageBitmap(bitmap);
            invalidate();
        }
        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            Path path=new Path();
            for(int i=0;i<40;++i){
                path.moveTo((float)width/40*i,(float)height);
                path.lineTo((float)width/80*(i*2+1),0);
                path.lineTo((float)width/40*(i+1),(float)height);
                path.lineTo((float)width/40*i,(float)height);
            }
            canvas.drawPath(path,paint);
            setY((float)(screenHeight-height));
        }
    }

}
