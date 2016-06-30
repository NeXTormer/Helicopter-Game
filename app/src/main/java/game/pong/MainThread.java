package game.pong;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * Created by Felix on 19/03/16.
 */
public class MainThread extends Thread {

    private int FPS = 30;
    private double averageFPS;
    private final SurfaceHolder surfaceHolder;
    private GamePanel gamePanel;
    private boolean running;
    public static Canvas canvas;



    public MainThread(SurfaceHolder surfaceHolder, GamePanel gamePanel) {
        super();
        this.surfaceHolder = surfaceHolder;
        this.gamePanel = gamePanel;

    }

    @Override
    public void run() {
        long startTime;
        long timeMillis;
        long waitTime;
        long totalTime = 0;
        int frameCount = 0;
        long targetTime = 1000/FPS;

        while(running) {
            startTime = System.nanoTime();
            canvas = null;

            //Try locking the canvas for pixel editing

            try {
                canvas = this.surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    this.gamePanel.update();
                    this.gamePanel.draw(canvas);
                }
            } catch (Exception e) { }
            finally {
                if(canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            timeMillis = (System.nanoTime() - startTime) / 1000000;
            waitTime = targetTime - timeMillis;
            try {
                //=========================================================== FPSMax = INFINITE
                //sleep(waitTime);
            } catch (Exception e) {}



            totalTime += System.nanoTime() - startTime;
            frameCount++;
            if(frameCount == FPS) {
                averageFPS = 1000.0/((totalTime/frameCount)/1000000.0);
                frameCount = 0;
                totalTime = 0;
                System.out.println(averageFPS);
                gamePanel.handoverDebugInfos(averageFPS, waitTime, timeMillis);
            }
        }
    }

    public void setRunning(boolean b) {
        running = b;
    }

}
