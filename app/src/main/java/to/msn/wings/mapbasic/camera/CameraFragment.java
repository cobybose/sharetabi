package to.msn.wings.mapbasic.camera;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

import to.msn.wings.mapbasic.R;

/**
 * Created by Tomohiro Tengan on 2017/11/28.
 */

public class CameraFragment extends Fragment implements View.OnClickListener {

    //パーミッションのリクエストコード
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    //テクスチャビュー
    private TextureView mTextureView;

    //写真撮影後、ファイルに保存したり、DBに保存するためのスレッド
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    //カメラ情報
    private Camera.CameraInfo mCameraInfo;
    //カメラデバイス
    private CameraDevice mCameraDevice;
    //セッション
    private CameraCaptureSession mCaptureSession;
    //撮影リクエストを作るためのビルダー
    private CaptureRequest.Builder mCaptureRequestBuilder;
    //撮影リクエスト
    private CaptureRequest mCaptureRequest;
    //撮影音のためのMediaActionSound
    private MediaActionSound mSound;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        view.findViewById(R.id.ShutterButton).setOnClickListener(this);
        mTextureView = (TextureView)view.findViewById(R.id.PreviewTexture);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //撮影音をロードする
        mSound = new MediaActionSound();
        mSound.load(MediaActionSound.SHUTTER_CLICK);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //MediaActionSoundを解放する
        mSound.release();
    }

    @Override
    public void onResume() {
        super.onResume();
        startCamera();
    }

    //カメラの起動処理を行う
    private void startCamera() {
        //画像処理を行うためのスレッドを立てる
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        if (mTextureView.isAvailable()) {
            //TextureViewの準備ができているなら、カメラを開く
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            //TextureViewの準備ができるのを待つ
            mTextureView.setSurfaceTextureListener(mTextureListener);
        }
    }

    private final TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //カメラデバイスへの接続を開始する
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            //プレビューを、新しいサイズに合わせて変形する
            transformTexture(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private void openCamera(int width, int height) {

        //望ましいカメラデバイスを選択する
        mCameraInfo = new CameraChooser(getActivity(), width, height).chooseCamera();

        if (mCameraInfo == null) {
            //候補が見つからなかった場合は、何も行わない
            return;
        }

        //画像処理を行うImageReaderを生成する
        mImageReader = ImageReader.newInstance(
                mCameraInfo.getPicuterSize().getWidth(),
                mCameraInfogetPictureSize().getHeight(), ImageFormat.JPEG, 2);
        //画像が得られるたびに呼ばれるリスナーと、そのリスナーが動作するスレッドを設定する
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

        //TextureViewのサイズと端末の向きに合わせて、プレビューを変形させる
        transformTexture(width, height);

        //カメラを開く
        CameraManager manager = (CameraManager)getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            manager.openCamera(mCameraInfo.getCameraId(), mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            //カメラが開かれ、プレビューセッションが開始できる状態になった
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };

    //プレビューのセッションを作る
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();

            //バッファサイズを、プレビューサイズに合わせる
            texture.setDefaultBufferSize(
                    mCameraInfo.getPreviewSize().getWidth(),
                    mCameraInfo.getPreviewSize().getHeight());

            //プレビューが描画されるsurface
            Surface surface = new Surface(texture);

            //プレビュー用のセッションを設定する
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(surface);
            //プレビュー用のセッション生成を要求する
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),mSessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            //カメラが閉じてしまっていた場合
            if (mCameraDevice == null) {
                return;
            }

            mCaptureSession = cameraCaptureSession;
            try {
                //オートフォーカスを設定する
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //カメラプレビューを表示する
                mCaptureRequest = mCaptureRequestBuilder.build();
                mCaptureSession.setRepeatingRequest(mCaptureRequest, null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

        }
    };
}
