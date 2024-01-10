package com.mti.facedetectuc;

import android.content.Context;
import android.content.SharedPreferences;

import com.genexus.android.core.externalapi.ExternalApiDefinition;
import com.genexus.android.core.externalapi.ExternalApiFactory;
import com.genexus.android.core.framework.GenexusModule;
import com.genexus.android.core.usercontrols.UcFactory;
import com.genexus.android.core.usercontrols.UserControlDefinition;
@androidx.camera.core.ExperimentalGetImage
public class ModuleClassMain implements GenexusModule {
    @Override
    public void initialize(Context context) {
        UserControlDefinition basicUserControl = new UserControlDefinition(
                FaceDetectUC.NAME,
                FaceDetectUC.class);
        UcFactory.addControl(basicUserControl);
    }
}
