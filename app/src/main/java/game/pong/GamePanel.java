package game.pong;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {
    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;
    private double averagefps = 0.0;
    private long threadWaitTime = 0;
    private long threadTimeMilis = 0;

    private long smokeStartTime;
    private long missileStartTime;
    private MainThread thread;
    private Background bg;
    private Player player;
    private ArrayList<Smokepuff> smoke;
    private ArrayList<Missile> missiles;
    private ArrayList<TopBorder> topborder;
    private ArrayList<BotBorder> botborder;
    private Random rand = new Random();
    private boolean newGameCreated;
    private Bitmap missile;
    private Bitmap border;
    private boolean debugMode = true;

    //increase to slow down difficulty progression, decrease to speed up difficulty progression
    private int progressDenom = 20;

    private Explosion explosion;
    private long startReset;
    private boolean reset;
    private boolean dissapear;
    private boolean started;
    public int best = 0;


    public GamePanel(Context context) {
        super(context);


        //add the callback to the surfaceholder to intercept events
        getHolder().addCallback(this);


        //make gamePanel focusable so it can handle events
        setFocusable(true);

        best = loadInt("bestScore");
        //getRootView().setLayerType(View.LAYER_TYPE_HARDWARE, null);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        boolean retry = true;
        int counter = 0;
        while (retry && counter < 1000) {
            counter++;
            try {
                thread.setRunning(false);
                thread.join();
                retry = false;
                thread = null;

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.grassbg1));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.helicopter), 65, 25, 3);
        missile = BitmapFactory.decodeResource(getResources(), R.drawable.missile);
        border = BitmapFactory.decodeResource(getResources(), R.drawable.brick);
        smoke = new ArrayList<>();
        missiles = new ArrayList<>();
        topborder = new ArrayList<>();
        botborder = new ArrayList<>();
        smokeStartTime = System.nanoTime();
        missileStartTime = System.nanoTime();

        thread = new MainThread(getHolder(), this);
        //we can safely start the game loop
        thread.setRunning(true);
        thread.start();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            if (!player.getPlaying() && newGameCreated && reset) {
                player.setPlaying(true);
                player.setUp(true);
            }
            if (player.getPlaying()) {

                if (!started) started = true;
                reset = false;
                player.setUp(true);
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            player.setUp(false);
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void update() {
        if (player.getPlaying()) {

            if (botborder.isEmpty()) {
                player.setPlaying(false);
                return;
            }
            if (topborder.isEmpty()) {
                player.setPlaying(false);
                return;
            }


            bg.update();
            player.update();


            if (player.getScore() * 3 > best) {
                best = player.getScore() * 3;
                saveInt("bestScore", best);

            }


            //calculate the threshold of height the border can have based on the score
            //max and min border heart are updated, and the border switched direction when either max or
            //min is met

            //cap max border height so that borders can only take up a total of 1/2 the screen

            //check bottom border collision
//            for(int i = 0; i<botborder.size(); i++)
//            {
//                if(collision(botborder.get(i), player)){
//                    System.out.println("Y: " + player.getY());
//                    player.setPlaying(false);
//                }
//            }
//
//            //check top border collision
//            for(int i = 0; i <topborder.size(); i++)
//            {
//                if(collision(topborder.get(i),player)) {
//                    System.out.println("Y: " + player.getY());
//                    player.setPlaying(false);
//                }
//            }

            //if(player.getY() < 10 || player.getY() > 415) player.setPlaying(false);
            if (player.getY() < 10 || player.getY() > HEIGHT - 65) player.setPlaying(false);


            //update top border
            this.updateTopBorder();

            //udpate bottom border
            this.updateBottomBorder();

            //add missiles on timer
            long missileElapsed = (System.nanoTime() - missileStartTime) / 1000000;
            if (missileElapsed > (2000 - player.getScore() / 4)) {


                //first missile always goes down the middle


                missiles.add(new Missile(missile,
                        WIDTH + 10, (int) (rand.nextDouble() * (HEIGHT - 40)) + 15, 45, 15, player.getScore(), 13));


                //reset timer
                missileStartTime = System.nanoTime();
            }
            //loop through every missile and check collision and remove
            for (int i = 0; i < missiles.size(); i++) {
                //update missile
                missiles.get(i).update();

                if (collision(missiles.get(i), player)) {
                    missiles.remove(i);
                    player.setPlaying(false);
                    break;
                }
                //remove missile if it is way off the screen
                if (missiles.get(i).getX() < -100) {
                    missiles.remove(i);
                    break;
                }
            }

            //add smoke puffs on timer
            long elapsed = (System.nanoTime() - smokeStartTime) / 1000000;
            if (elapsed > 120) {
                smoke.add(new Smokepuff(player.getX(), player.getY() + 10));
                smokeStartTime = System.nanoTime();
            }

            for (int i = 0; i < smoke.size(); i++) {
                smoke.get(i).update();
                if (smoke.get(i).getX() < -10) {
                    smoke.remove(i);
                }
            }
        } else {
            player.resetDY();
            if (!reset) {
                newGameCreated = false;
                startReset = System.nanoTime();
                reset = true;
                dissapear = true;
                explosion = new Explosion(BitmapFactory.decodeResource(getResources(), R.drawable.explosion), player.getX(),
                        player.getY() - 30, 100, 100, 25);
            }

            explosion.update();
            long resetElapsed = (System.nanoTime() - startReset) / 1000000;

            if (resetElapsed > 1000 && !newGameCreated) {
                newGame();
            }


        }

    }

    public boolean collision(GameObject a, GameObject b) {
        return Rect.intersects(a.getRectangle(), b.getRectangle());
    }

    public void setavgFPS(double avgfps) {
        averagefps = avgfps;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (canvas != null) {
            //scaling
            final float scaleFactorX = getWidth() / (856 * 1.f);
            final float scaleFactorY = getHeight() / (480 * 1.f);

            final int savedState = canvas.save();

            canvas.scale(scaleFactorX, scaleFactorY);

            bg.draw(canvas);

            if (!dissapear) {
                player.draw(canvas);
            }


            //draw smokepuffs

            for (Smokepuff sp : smoke) {
                sp.draw(canvas);
            }

            //draw missiles

            for (Missile m : missiles) {
                m.draw(canvas);
            }


            //draw topborder

            for (TopBorder tb : topborder) {
                tb.draw(canvas);
            }


            //draw botborder

            for (BotBorder bb : botborder) {
                bb.draw(canvas);
            }


            //draw explosion

            if (started) {
                explosion.draw(canvas);
            }

            drawText(canvas);

            canvas.restoreToCount(savedState);

        }
    }

    public void updateTopBorder() {
        //every 50 points, insert randomly placed top blocks that break the pattern
//        if(player.getScore()%50 ==0)
//        {
//            topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick
//            ),topborder.get(topborder.size()-1).getX()+20,0,(int)((rand.nextDouble()*(maxBorderHeight
//            ))+1)));
//        }
        for (int i = 0; i < topborder.size(); i++) {
            topborder.get(i).update();
            if (topborder.get(i).getX() < -20) {
                topborder.remove(i);
                //remove element of arraylist, replace it by adding a new one

                //calculate topdown which determines the direction the border is moving (up or down)
//                if(topborder.get(topborder.size()-1).getHeight()>=maxBorderHeight)
//                {
//                    topDown = false;
//                }
//                if(topborder.get(topborder.size()-1).getHeight()<=minBorderHeight)
//                {
//                    topDown = true;
//                }
                topborder.add(new TopBorder(border, topborder.get(topborder.size() - 1).getX() + 20,
                        0, topborder.get(topborder.size() - 1).getHeight()));


            }
        }

    }

    public void updateBottomBorder() {
        //every 40 points, insert randomly placed bottom blocks that break pattern
//        if(player.getScore()%40 == 0)
//        {
//            botborder.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
//                    botborder.get(botborder.size()-1).getX()+20,(int)((rand.nextDouble()
//                    *maxBorderHeight)+(HEIGHT-maxBorderHeight))));
//        }

        //update bottom border
        for (int i = 0; i < botborder.size(); i++) {
            botborder.get(i).update();

            //if border is moving off screen, remove it and add a corresponding new one
            if (botborder.get(i).getX() < -20) {
                botborder.remove(i);


                //determine if border will be moving up or down
//                if (botborder.get(botborder.size() - 1).getY() <= HEIGHT-maxBorderHeight) {
//                    botDown = true;
//                }
//                if (botborder.get(botborder.size() - 1).getY() >= HEIGHT - minBorderHeight) {
//                    botDown = false;
//                }

                botborder.add(new BotBorder(border, botborder.get(botborder.size() - 1).getX() + 20, botborder.get(botborder.size() - 1
                ).getY()));

            }
        }
    }

    public void newGame() {
        dissapear = false;

        botborder.clear();
        topborder.clear();

        missiles.clear();
        smoke.clear();


        player.resetDY();
        player.resetScore();
        player.setY(HEIGHT / 2);


        //create initial borders

        //initial top border
        for (int i = 0; i * 20 < WIDTH + 40; i++) {
            //first top border create
            if (i == 0) {
                topborder.add(new TopBorder(border, i * 20, 0, 10));
            } else {
                topborder.add(new TopBorder(border, i * 20, 0, topborder.get(i - 1).getHeight()));
            }
        }
        //initial bottom border
        for (int i = 0; i * 20 < WIDTH + 40; i++) {
            //first border ever created
            if (i == 0) {
                botborder.add(new BotBorder(border, i * 20, HEIGHT - 40));
            }
            //adding borders until the initial screen is filed
            else {
                botborder.add(new BotBorder(border,
                        i * 20, botborder.get(i - 1).getY()));
            }
        }

        newGameCreated = true;


    }

    public void drawText(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("DISTANCE: " + (player.getScore() * 3), 10, HEIGHT - 10, paint);
        canvas.drawText("BEST: " + "" + best, WIDTH - 215, HEIGHT - 10, paint);
        //paint.setColor(Color.DKGRAY);
        canvas.drawText("FPS: " + averagefps, WIDTH / 3, HEIGHT - 10, paint);
        if (debugMode) {
            drawDebugMode(canvas);
        }

        if (!player.getPlaying() && newGameCreated && reset) {
            Paint paint1 = new Paint();
            paint1.setColor(Color.WHITE);
            paint1.setTextSize(40);
            paint1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("PRESS TO START", WIDTH / 2 - 50, HEIGHT / 2, paint1);

        }
    }

    public void saveInt(String key, int val) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(key, val);
        editor.commit();
    }

    public int loadInt(String key) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getInt(key, 0);
    }

    public void handoverDebugInfos(double averagefps, long waitTime, long timeMilis) {
        threadTimeMilis = timeMilis;
        threadWaitTime = waitTime;
        this.averagefps = averagefps;
    }

    public void drawDebugMode(Canvas c) {
        Paint p = new Paint();
        p.setColor(Color.DKGRAY);
        p.setTextSize(14);
        c.drawText("ThreadTime: " + threadTimeMilis, (WIDTH / 4) * 3, 25, p);
        c.drawText("ThreadWaitTime: " + threadWaitTime, (WIDTH / 4) * 3, 40, p);
        c.drawText("FPS: " + averagefps, (WIDTH / 4) * 3, 55, p);

    }


}