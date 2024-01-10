package com.mti.facedetectuc;

import android.content.Context;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

public class LottieAnimationController {

    private LottieAnimationView lottieAnimationView;

    public LottieAnimationController(LottieAnimationView animationView) {
        lottieAnimationView = animationView;
    }

    public void setAnimationFile(String animationFileName) {
        lottieAnimationView.setAnimation(animationFileName);
        lottieAnimationView.setRepeatCount(LottieDrawable.INFINITE);
    }
    public void playAnimation() {
        if (lottieAnimationView != null) {
            lottieAnimationView.playAnimation();
        }
    }

    public void pauseAnimation() {
        if (lottieAnimationView != null) {
            lottieAnimationView.pauseAnimation();
        }
    }

    public void resumeAnimation() {
        if (lottieAnimationView != null) {
            lottieAnimationView.resumeAnimation();
        }
    }

    public void stopAnimation() {
        if (lottieAnimationView != null) {
            lottieAnimationView.cancelAnimation();
        }
    }
}
