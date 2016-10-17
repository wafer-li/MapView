package com.onlylemi.mapview;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class RoutePositionChanger implements Runnable {

    public interface RoutePositionChangerCallback {
        void onCallback(PointF point);
    }

    // Trajectory status
    private int index_;
    private PointF delta_;
    private float splitIndex_;
    private float splitNum_;

    private PointF getCurrentPoint() {
        PointF base = new PointF(points_.get(index_).x, points_.get(index_).y);
        if (splitNum_ != 0) {
            base.x += delta_.x / splitNum_ * splitIndex_;
            base.y += delta_.y / splitNum_ * splitIndex_;
        }
        return base;
    }

    private void setNextDelta(int index) {
        splitIndex_ = 0;
        index_ = index;
        if (index + 1 < points_.size()) {
            // Next point is available
            PointF curr = points_.get(index);
            PointF next = points_.get(index + 1);

            delta_.x = next.x - curr.x;
            delta_.y = next.y - curr.y;
            float distance =  delta_.x * delta_.x + delta_.y * delta_.y;
            distance = (float) Math.sqrt(distance);
            float delay = distance * distanceToDelay_;
            splitNum_ = delay / interval_;
        } else {
            splitNum_ = 0;
        }
    }

    private long getNextDelay() {
        if (index_ + 1 < points_.size()) {
            if (splitIndex_ + 1 >= splitNum_) {
                // Problem: the next key frame will be missed if the timer goes on as ordinary interval.
                // Solution: force the next frame to synchronize to the next point.
                long result = (long) (interval_ * (splitNum_ - splitIndex_));
                setNextDelta(index_ + 1);
                return result;
            } else {
                ++splitIndex_;
                return interval_;
            }
        } else {
            // All points has been processed;
            return -1;
        }
    }

    // Animation properties
    private int interval_;
    private float distanceToDelay_;

    // Execution related
    private ArrayList<PointF> points_;
    private RoutePositionChangerCallback callback_;
    private ScheduledThreadPoolExecutor executor_;

    @Override
    public void run() {
        PointF result = getCurrentPoint();
        callback_.onCallback(result);
        long delay = getNextDelay();
        if (delay == -1) {
            executor_.shutdown();
            executor_ = null;
        } else {
            executor_.schedule(this, delay, TimeUnit.MILLISECONDS);
        }
    }

    public RoutePositionChanger(RoutePositionChangerCallback callback,
                                float distancePerSecond, float framePerSecond) {
        callback_ = callback;
        distanceToDelay_ = 1000 / distancePerSecond;
        interval_ = (int) (1000 / framePerSecond);
        delta_ = new PointF(0, 0);
    }

    public void start(List<PointF> points) {
        if (executor_ != null) {
            executor_.shutdownNow();
        }
        points_ = new ArrayList<>(points);
        setNextDelta(0);
        executor_ = new ScheduledThreadPoolExecutor(1);
        executor_.schedule(this, 0, TimeUnit.MILLISECONDS);
    }

    public boolean isRunning() {
        return executor_ != null;
    }

    public void stop() {
        if (executor_ == null) throw new RuntimeException("Already stopped");
        executor_.shutdownNow();
    }

    public static void main(String[] args) throws InterruptedException {
        RoutePositionChanger changer = new RoutePositionChanger(new RoutePositionChangerCallback() {
            @Override
            public void onCallback(PointF point) {
                System.out.println(point);
            }
        }, 2, 5);

        ArrayList<PointF> list = new ArrayList<>();
        list.add(new PointF(0, 0));
        list.add(new PointF(0, 1));
        list.add(new PointF(0, 2));
        list.add(new PointF(0, 3));
        list.add(new PointF(0, 6));
        list.add(new PointF(0, 9));
        list.add(new PointF(0, 12));

        System.out.println("Starting animation");
        changer.start(list);
        Thread.sleep(2000);
        System.out.println("Restarting animation");
        changer.start(list);
    }
}
