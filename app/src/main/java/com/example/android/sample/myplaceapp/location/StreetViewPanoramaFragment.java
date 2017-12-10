package com.example.android.sample.myplaceapp.location;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.example.android.sample.myplaceapp.SubActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Tomohiro Tengan on 2017/12/08.
 */

public class StreetViewPanoramaFragment extends MapFragment implements OnStreetViewPanoramaReadyCallback{

    // 位置情報を読み込むローダー
    private static final int PLACE_LOADER = 1;

    // プロットする日付をBundleに詰めるためのキー
    private static final String ARGS_DATE = "date";

    private StreetViewPanorama mStreetViewPanorama;
    // デフォルトのズーム
    private static final float DEFAULT_ZOOM = 15f;
    // 地図を操作するためのオブジェクト
    private GoogleMap mGoogleMap;
    // DB検索結果のカーソル
    private Cursor mCursor;
    // コンテントプロバイダへの変更を画面に反映するためのHandler
    private Handler mHandler = new Handler();

    public static StreetViewPanoramaFragment newInstance(String date) {
        StreetViewPanoramaFragment fragment = new StreetViewPanoramaFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGS_DATE, date);

        fragment.setArguments(arguments);

        return fragment;
    }

    private LoaderManager.LoaderCallbacks<Cursor> mCallback
            = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (id == PLACE_LOADER) {
                // Bundleから日付文字列を取り出す
                String arg_date = args.getString(ARGS_DATE);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    // 日付文字列が示す日時（0時0分0秒）
                    long dayStart = sdf.parse(arg_date).getTime();
                    // 同日の23:59:59
                    long dayEnd = dayStart + PlaceRepository.DAY - 1;

                    return new CursorLoader(getActivity(), PlaceProvider.CONTENT_URI, null,
                            PlaceDBHelper.COLUMN_TIME + " BETWEEN ? AND ?",
                            new String[]{String.valueOf(dayStart), String.valueOf(dayEnd)},
                            PlaceDBHelper.COLUMN_REGISTER_TIME);

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mCursor = data;
            // コンテントプロバイダに変更が加えられた場合に再検索するためのオブザーバを登録する
            mCursor.registerContentObserver(mObserver);
            // 地図のカメラを操作する
            handleCamera(mCursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (mCursor != null) {
                mCursor.unregisterContentObserver(mObserver);
                mCursor = null;
            }
        }
    };

    // コンテントプロバイダの変更を監視するオブザーバ
    private ContentObserver mObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            // Loaderを初期化する
            getLoaderManager().restartLoader(PLACE_LOADER, getArguments(), mCallback);
        }
    };

    // 地図を表示する視点を操作する
    private void handleCamera(@NonNull Cursor cursor) {

        // 位置情報がない場合には、何もしない
        if (cursor.getCount() == 0) return;

        // カーソルから情報を集めて、Placeのリストにする
        List<Place> places = new ArrayList<>();
        while (cursor.moveToNext()) {
            places.add(PlaceRepository.cursorToPlace(cursor));
        }

        if (places.size() == 1) {
            // 1地点しかない場合
            Place place = places.get(0);
            LatLng latLng = new LatLng(place.getLatitude(), place.getLongitude());

            mStreetViewPanorama.setPosition(latLng);

        } else {
            // 複数地点の場合
            // 複数地点を結ぶ領域を作成するBuilder
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            // 各地点をBuilderに含めていく
            for(Place place : places) {
                LatLng point = new LatLng(place.getLatitude(), place.getLongitude());
                builder.include(point);
            }
        }

        // 地図上にプロットする
        showPanorama(places);

    }

    // 地図上に地点をプロットする
    private void showPanorama(List<Place> places) {

        int size = places.size();
        for(int i = 0; i < size; i++) {
            Place place = places.get(i);

            // 線分表示オプションに地点を含める
            LatLng latLng = new LatLng(place.getLatitude(), place.getLongitude());

            // マーカー追加
            mStreetViewPanorama.setPosition(latLng);
        }
    }


    // 表示する日付を変更する
    public void setDate(String dateString) {
        getArguments().putString(ARGS_DATE, dateString);

        // Loaderを初期化する
        getLoaderManager().restartLoader(PLACE_LOADER, getArguments(), mCallback);
    }


    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {

    }
}
