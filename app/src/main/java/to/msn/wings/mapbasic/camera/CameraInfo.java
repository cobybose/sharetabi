package to.msn.wings.mapbasic.camera;

import android.util.Size;

/**
 * Created by Tomohiro Tengan on 2017/11/28.
 */

public class CameraInfo {
    private String cameraId;
    private Size previewSize;
    private Size pictureSize;
    private int sensorOrientation;

    public String getCameraId(String cameraId) {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public Size getPreviewSize() {
        return previewSize;
    }

    public void setPictureSize(Size pictureSize) {
        this.pictureSize = pictureSize;
    }

    public int getSensorOrientation() {
        return sensorOrientation;
    }

    public void setSensorOrientation(int sensorOrientation) {
        this.sensorOrientation = sensorOrientation;
    }
}
